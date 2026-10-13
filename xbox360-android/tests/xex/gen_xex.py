import struct, os, sys
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
RETAIL=bytes([0x20,0xB1,0x85,0xA5,0x9D,0x28,0xFD,0xC3,0x40,0x58,0x3F,0xBB,0x08,0x96,0xBF,0x91])
BASE=0x82000000
ICON=b'\x89PNG\r\n\x1a\n'+os.urandom(300)
TITLE="Juego de Prueba ñ".encode()
def xdbf():
    # string table (Spanish=5 default, English=1 too)
    def xstr(t): 
        body=struct.pack('>HH',0x8000,len(t))+t
        return b'XSTR'+struct.pack('>IIH',1,len(body)+2,1)+body
    xstc=b'XSTC'+struct.pack('>III',1,16,5)
    blobs=[(1,0x58535443,xstc),(3,5,xstr(TITLE)),(3,1,xstr(b'Test Game')),(2,0x8000,ICON)]
    hdr=b'XDBF'+struct.pack('>IIIII',0x10000,len(blobs),len(blobs),1,0)
    ents=b'';content=b'';
    for s,i,b in blobs:
        ents+=struct.pack('>HQII',s,i,len(content),len(b)); content+=b
    ents+=b'\0'*(18*0)  # entry_count==used
    files=b'\0'*8       # free_count=1
    return hdr+ents+files+content
def image():
    x=xdbf(); img=bytearray(b'MZ'+os.urandom(0xFFE)); img+=x; img+=b'\0'*(0x4000-len(img)%0x4000)
    return bytes(img), 0x1000
def aes_cbc_enc(key,data):
    e=Cipher(algorithms.AES(key),modes.CBC(b'\0'*16)).encryptor(); return e.update(data)+e.finalize()
def lzx_uncompressed(img):
    n=len(img)
    bits=(0<<27)|(3<<24)|n  # 1+3+24 = 28 bits
    bits<<=4
    w=struct.pack('<HH',(bits>>16)&0xFFFF,bits&0xFFFF)
    out=w+struct.pack('<III',1,1,1)+img
    if n&1: out+=b'\0'
    return out
def build(path,comp,enc,key=RETAIL):
    img,xoff=image(); sess=os.urandom(16)
    aes_key_enc=Cipher(algorithms.AES(key),modes.ECB()).encryptor().update(sess)
    pad=lambda b:b+b'\0'*(-len(b)%16)
    if comp==0: payload=pad(img); fmt_extra=b''
    elif comp==1:
        # dos bloques de datos con ceros entre medias
        d1=img[:0x2000]; d2=img[0x2000:]; payload=d1+d2
        fmt_extra=struct.pack('>IIII',len(d1),0x100,len(d2),0x200)
        total=len(img)+0x300
    else:
        lz=lzx_uncompressed(img)
        chunks=b''
        for i in range(0,len(lz),0x7000):
            c=lz[i:i+0x7000]; chunks+=struct.pack('>H',len(c))+c
        chunks+=struct.pack('>H',0)
        block=struct.pack('>I',0)+b'\0'*20+chunks   # siguiente bloque = 0
        block=block+b'\0'*(-len(block)%16)
        payload=block
        fmt_extra=struct.pack('>I',0x20000)+struct.pack('>I',len(block))+b'\0'*20
    if enc:
        e=Cipher(algorithms.AES(sess),modes.CBC(b'\0'*16)).encryptor()
        if comp==1:
            payload=e.update(payload)+e.finalize() if len(payload)%16==0 else None
        else: payload=aes_cbc_enc(sess,pad(payload))
    # cabecera
    fmt=struct.pack('>IHH',8+len(fmt_extra),enc,comp)+fmt_extra
    base_img=BASE
    res=struct.pack('>I',4+16)+b'12345678'+struct.pack('>II',BASE+xoff,len(xdbf()))
    exe=struct.pack('>IIIIBBBBI',0,0,0,0x4D5307E6,0,0,1,1,0)
    sec=bytearray(0x184+0x10)
    struct.pack_into('>I',sec,4,len(img)); struct.pack_into('>I',sec,0x110,BASE); sec[0x150:0x160]=aes_key_enc
    keys=[(0x2FF,res),(0x3FF,fmt),(0x40006,exe)]
    cnt=len(keys)+1
    off=0x18+8*cnt; hdr_size=0x1000
    sec_off=0x400
    body=bytearray(hdr_size)
    body[0:4]=b'XEX2'; struct.pack_into('>IIIII',body,4,0,hdr_size,0,sec_off,cnt)
    p=0x18; cur=0x800
    for k,v in keys:
        struct.pack_into('>II',body,p,k,cur); body[cur:cur+len(v)]=v; cur+=len(v)+8; p+=8
    struct.pack_into('>II',body,p,0x10201,BASE)  # base inline
    body[sec_off:sec_off+len(sec)]=sec
    open(path,'wb').write(bytes(body)+payload)
for name,comp,enc in [('none_plain',0,0),('none_enc',0,1),('basic_plain',1,0),('basic_enc',1,1),('lzx_plain',2,0),('lzx_enc',2,1)]:
    build(name+'.xex',comp,enc)
open('expected_icon.bin','wb').write(ICON)
open('expected_title.txt','wb').write(TITLE)
