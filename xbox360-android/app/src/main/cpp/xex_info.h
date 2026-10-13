#pragma once
#include <cstdint>
#include <string>
#include <vector>

// Metadatos de un título leídos de un ejecutable XEX2.
struct XexMeta {
  uint32_t title_id = 0;
  std::string title;          // UTF-8, vacío si el XEX no trae recurso XDBF
  std::vector<uint8_t> icon;  // PNG del icono del título, vacío si no hay
};

// Lee título, ID e icono de un XEX abierto en `fd`. Devuelve false si no es un
// XEX2 válido. Un XEX sin recursos válido devuelve true con title/icon vacíos.
bool ReadXexMeta(int fd, XexMeta* out);
