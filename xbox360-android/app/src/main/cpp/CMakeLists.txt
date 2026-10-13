cmake_minimum_required(VERSION 3.22)
project(x360core C CXX)
set(CMAKE_CXX_STANDARD 20)

set(XENIA ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/xenia)

# Descompresor LZX (mspack) y AES de Xenia, usados por el lector de XEX.
add_library(xex_deps STATIC
    ${XENIA}/third_party/mspack/lzxd.c
    ${XENIA}/third_party/crypto/rijndael-alg-fst.c)
target_include_directories(xex_deps PUBLIC ${XENIA} ${XENIA}/third_party/mspack)
target_compile_definitions(xex_deps PRIVATE HAVE_CONFIG_H)

add_library(x360core SHARED bridge.cpp xex_info.cpp)
target_include_directories(x360core PRIVATE ${XENIA} ${XENIA}/third_party/mspack)
target_link_libraries(x360core xex_deps)
find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(x360core ${log-lib} ${android-lib})

# Integración del núcleo (pendiente): añadir Xenia como subdirectorio y enlazarlo.
#   add_subdirectory(${CMAKE_SOURCE_DIR}/../../../../third_party/xenia xenia)
#   target_link_libraries(x360core xenia-core xenia-cpu-backend-a64 xenia-gpu-vulkan ...)
