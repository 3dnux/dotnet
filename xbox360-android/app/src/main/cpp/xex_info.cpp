// Lector de metadatos XEX2 (ID de título, nombre e icono).
// Formato y claves tomados de xenia/cpu/xex_module.cc y xenia/kernel/util/xdbf_utils.cc.
#include "xex_info.h"

#include <unistd.h>

#include <algorithm>
#include <cstring>

extern "C" {
#include "third_party/crypto/rijndael-alg-fst.h"
}
#include "third_party/mspack/lzx.h"
#include "third_party/mspack/mspack.h"

// mspack (de Xenia) espera que el host defina xenia_log; aquí no hace falta registrarlo.
extern "C" void xenia_log(const char*, ...) {}

namespace {

constexpr size_t kMaxHeader = 8u << 20;
constexpr size_t kMaxImage = 256u << 20;

constexpr uint8_t kRetailKey[16] = {0x20, 0xB1, 0x85, 0xA5, 0x9D, 0x28, 0xFD, 0xC3,
                                    0x40, 0x58, 0x3F, 0xBB, 0x08, 0x96, 0xBF, 0x91};
constexpr uint8_t kDevkitKey[16] = {0};

uint16_t Be16(const uint8_t* p) { return uint16_t(p[0] << 8 | p[1]); }
uint32_t Be32(const uint8_t* p) {
  return uint32_t(p[0]) << 24 | uint32_t(p[1]) << 16 | uint32_t(p[2]) << 8 | p[3];
}
uint64_t Be64(const uint8_t* p) { return uint64_t(Be32(p)) << 32 | Be32(p + 4); }

bool ReadAt(int fd, uint64_t offset, uint8_t* dst, size_t size) {
  while (size) {
    ssize_t n = pread(fd, dst, size, static_cast<off_t>(offset));
    if (n <= 0) return false;
    dst += n; offset += n; size -= n;
  }
  return true;
}

// AES-128-CBC (IV a cero), descifrado en bloques de 16 bytes. `iv` se actualiza
// para poder encadenar varias llamadas (compresión "basic").
void AesCbcDecrypt(const uint8_t* key, const uint8_t* in, size_t size, uint8_t* out, uint8_t iv[16]) {
  uint32_t rk[4 * (MAXNR + 1)];
  int nr = rijndaelKeySetupDec(rk, key, 128);
  for (size_t n = 0; n + 16 <= size; n += 16) {
    uint8_t ct[16];
    memcpy(ct, in + n, 16);  // por si in == out
    rijndaelDecrypt(rk, nr, ct, out + n);
    for (int i = 0; i < 16; i++) out[n + i] ^= iv[i];
    memcpy(iv, ct, 16);
  }
}

// ---- E/S en memoria para mspack ----
struct MemFile {
  uint8_t* data;
  off_t size;
  off_t pos;
};
int MemRead(mspack_file* f, void* buf, int n) {
  auto* m = reinterpret_cast<MemFile*>(f);
  off_t k = std::min<off_t>(n, m->size - m->pos);
  memcpy(buf, m->data + m->pos, k);
  m->pos += k;
  return static_cast<int>(k);
}
int MemWrite(mspack_file* f, void* buf, int n) {
  auto* m = reinterpret_cast<MemFile*>(f);
  off_t k = std::min<off_t>(n, m->size - m->pos);
  memcpy(m->data + m->pos, buf, k);
  m->pos += k;
  return static_cast<int>(k);
}
void* MemAlloc(mspack_system*, size_t n) { return calloc(n, 1); }
void MemFree(void* p) { free(p); }
void MemCopy(void* src, void* dst, size_t n) { memcpy(dst, src, n); }

bool LzxDecompress(uint8_t* src, size_t src_len, uint8_t* dst, size_t dst_len, uint32_t window_size) {
  if (window_size == 0 || (window_size & (window_size - 1))) return false;
  uint32_t bits = 0;
  while ((1u << bits) != window_size) bits++;
  mspack_system sys{};
  sys.read = MemRead; sys.write = MemWrite; sys.alloc = MemAlloc; sys.free = MemFree; sys.copy = MemCopy;
  MemFile in{src, static_cast<off_t>(src_len), 0};
  MemFile out{dst, static_cast<off_t>(dst_len), 0};
  lzxd_stream* lzxd = lzxd_init(&sys, reinterpret_cast<mspack_file*>(&in), reinterpret_cast<mspack_file*>(&out),
                                bits, 0, 0x8000, static_cast<off_t>(dst_len), 0);
  if (!lzxd) return false;
  int rc = lzxd_decompress(lzxd, static_cast<off_t>(dst_len));
  lzxd_free(lzxd);
  return rc == 0;
}

// ---- Cabecera ----
struct Header {
  std::vector<uint8_t> raw;  // cabecera completa (header_size bytes)
  uint32_t header_size = 0;
  // Devuelve un puntero a la cabecera opcional `key`, o null si falta o si sus
  // `min_len` bytes no caben dentro de la cabecera (archivos corruptos).
  const uint8_t* OptHeader(uint32_t key, size_t min_len) const {
    uint32_t count = Be32(&raw[0x14]);
    for (uint32_t i = 0; i < count && 0x18 + size_t(i) * 8 + 8 <= raw.size(); i++) {
      const uint8_t* e = &raw[0x18 + size_t(i) * 8];
      if (Be32(e) != key) continue;
      if ((key & 0xFF) <= 1) return min_len <= 4 ? e + 4 : nullptr;  // valor inline
      uint32_t off = Be32(e + 4);
      return (off <= raw.size() && min_len <= raw.size() - off) ? &raw[off] : nullptr;
    }
    return nullptr;
  }
};

// Reconstruye la imagen PE. `image` queda con la imagen completa si tiene éxito.
bool BuildImage(const Header& h, const uint8_t* fmt, const uint8_t* sec, const std::vector<uint8_t>& payload,
                const uint8_t* key, std::vector<uint8_t>* image) {
  uint32_t info_size = Be32(fmt);
  uint16_t enc = Be16(fmt + 4), comp = Be16(fmt + 6);
  if (enc > 1) return false;

  // Clave de sesión: la clave AES del XEX descifrada (ECB, un bloque) con la clave de consola.
  uint8_t session[16], iv0[16] = {0};
  AesCbcDecrypt(key, sec + 0x150, 16, session, iv0);

  std::vector<uint8_t> data = payload;
  auto decrypt_all = [&]() {
    if (enc != 1) return;
    uint8_t iv[16] = {0};
    AesCbcDecrypt(session, data.data(), data.size(), data.data(), iv);
  };

  if (comp == 0) {
    decrypt_all();
    *image = std::move(data);
    return true;
  }
  if (comp == 1) {  // basic: bloques de (datos, ceros)
    uint32_t blocks = (info_size - 8) / 8;
    if (blocks == 0) return false;
    uint64_t total = 0;
    for (uint32_t i = 0; i < blocks; i++) total += uint64_t(Be32(fmt + 8 + i * 8)) + Be32(fmt + 12 + i * 8);
    if (total > kMaxImage) return false;
    image->assign(total, 0);
    uint8_t iv[16] = {0};
    size_t src = 0, dst = 0;
    for (uint32_t i = 0; i < blocks; i++) {
      uint32_t ds = Be32(fmt + 8 + i * 8), zs = Be32(fmt + 12 + i * 8);
      if (src + ds > data.size() || dst + ds > image->size()) return false;
      if (enc == 1) AesCbcDecrypt(session, &data[src], ds, &(*image)[dst], iv);
      else memcpy(&(*image)[dst], &data[src], ds);
      src += ds; dst += size_t(ds) + zs;
    }
    return true;
  }
  if (comp == 2) {  // normal: bloques con SHA1 + trozos LZX
    if (info_size < 36) return false;  // ventana + primer bloque (tamaño + SHA1)
    decrypt_all();
    uint32_t window = Be32(fmt + 8);
    uint32_t cur_size = Be32(fmt + 12);  // tamaño del primer bloque
    std::vector<uint8_t> lz;
    lz.reserve(data.size());
    size_t p = 0;
    while (cur_size) {
      if (p + cur_size > data.size() || cur_size < 24) return false;
      uint32_t next_size = Be32(&data[p]);
      size_t q = p + 24;
      while (true) {
        if (q + 2 > data.size()) return false;
        size_t chunk = Be16(&data[q]);
        q += 2;
        if (!chunk) break;
        if (q + chunk > data.size()) return false;
        lz.insert(lz.end(), data.begin() + q, data.begin() + q + chunk);
        q += chunk;
      }
      p += cur_size;
      cur_size = next_size;
    }
    uint32_t image_size = Be32(sec + 4);
    if (image_size == 0 || image_size > kMaxImage) return false;
    image->assign(image_size, 0);
    return LzxDecompress(lz.data(), lz.size(), image->data(), image->size(), window);
  }
  return false;  // 3 = delta (parches), no soportado
}

// ---- XDBF ----
bool ParseXdbf(const uint8_t* d, size_t n, XexMeta* out) {
  if (n < 24 || memcmp(d, "XDBF", 4) != 0) return false;
  uint32_t entry_count = Be32(d + 8), entry_used = Be32(d + 12), free_count = Be32(d + 16);
  size_t content = 24 + size_t(entry_count) * 18 + size_t(free_count) * 8;
  if (content > n) return false;
  auto find = [&](uint16_t section, uint64_t id, const uint8_t** p, size_t* sz) {
    for (uint32_t i = 0; i < entry_used && i < entry_count; i++) {
      const uint8_t* e = d + 24 + i * 18;
      if (Be16(e) == section && Be64(e + 2) == id) {
        size_t off = content + Be32(e + 10), size = Be32(e + 14);
        if (off > n || size > n - off) return false;
        *p = d + off; *sz = size;
        return true;
      }
    }
    return false;
  };
  const uint8_t* p; size_t sz;
  uint32_t lang = 1;  // inglés por defecto
  if (find(1, 0x58535443, &p, &sz) && sz >= 16 && memcmp(p, "XSTC", 4) == 0) lang = Be32(p + 12);
  for (uint32_t l : {lang, 1u}) {  // idioma por defecto, o inglés
    if (!find(3, l, &p, &sz) || sz < 14 || memcmp(p, "XSTR", 4) != 0) continue;
    uint16_t count = Be16(p + 12);
    size_t q = 14;
    for (uint16_t i = 0; i < count && q + 4 <= sz; i++) {
      uint16_t id = Be16(p + q), len = Be16(p + q + 2);
      q += 4;
      if (q + len > sz) break;
      if (id == 0x8000) { out->title.assign(reinterpret_cast<const char*>(p + q), len); break; }
      q += len;
    }
    if (!out->title.empty()) break;
  }
  if (find(2, 0x8000, &p, &sz)) out->icon.assign(p, p + sz);
  return true;
}

}  // namespace

bool ReadXexMeta(int fd, XexMeta* out) {
  uint8_t top[0x18];
  if (!ReadAt(fd, 0, top, sizeof(top)) || memcmp(top, "XEX2", 4) != 0) return false;
  Header h;
  h.header_size = Be32(top + 8);
  uint32_t sec_off = Be32(top + 0x10);
  if (h.header_size < 0x200 || h.header_size > kMaxHeader || sec_off + 0x184 > h.header_size) return false;
  h.raw.resize(h.header_size);
  if (!ReadAt(fd, 0, h.raw.data(), h.header_size)) return false;
  const uint8_t* sec = &h.raw[sec_off];

  if (const uint8_t* exec = h.OptHeader(0x00040006, 0x10)) out->title_id = Be32(exec + 0xC);

  const uint8_t* fmt = h.OptHeader(0x000003FF, 8);
  const uint8_t* res = h.OptHeader(0x000002FF, 4);
  if (!fmt || !res) return true;  // sin recursos: solo ID
  // La estructura de formato debe caber entera en la cabecera.
  uint32_t fmt_size = Be32(fmt);
  size_t fmt_avail = h.raw.data() + h.raw.size() - fmt;
  if (fmt_size < 8 || fmt_size > fmt_avail) return true;
  size_t res_avail = h.raw.data() + h.raw.size() - res;

  off_t file_size = lseek(fd, 0, SEEK_END);
  if (file_size <= off_t(h.header_size) || size_t(file_size - h.header_size) > kMaxImage) return true;
  std::vector<uint8_t> payload(file_size - h.header_size);
  if (!ReadAt(fd, h.header_size, payload.data(), payload.size())) return true;

  uint32_t base = Be32(sec + 0x110);  // load_address
  if (const uint8_t* b = h.OptHeader(0x00010201, 4)) base = Be32(b);

  for (const uint8_t* key : {kRetailKey, kDevkitKey}) {
    std::vector<uint8_t> image;
    if (!BuildImage(h, fmt, sec, payload, key, &image)) continue;
    if (image.size() < 2 || image[0] != 'M' || image[1] != 'Z') continue;  // clave equivocada
    uint32_t count = Be32(res) >= 4 ? (Be32(res) - 4) / 16 : 0;
    count = uint32_t(std::min<size_t>(count, (res_avail - 4) / 16));
    for (uint32_t i = 0; i < count; i++) {
      const uint8_t* r = res + 4 + i * 16;
      uint32_t addr = Be32(r + 8), size = Be32(r + 12);
      if (addr < base || size_t(addr - base) + size > image.size()) continue;
      if (ParseXdbf(&image[addr - base], size, out)) return true;
    }
    return true;
  }
  return true;
}
