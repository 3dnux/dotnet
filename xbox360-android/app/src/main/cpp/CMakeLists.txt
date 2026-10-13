cmake_minimum_required(VERSION 3.22)
project(x360core CXX)
set(CMAKE_CXX_STANDARD 20)

add_library(x360core SHARED bridge.cpp)
find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(x360core ${log-lib} ${android-lib})

# Integración del núcleo (pendiente): añadir Xenia como subdirectorio y enlazarlo.
#   add_subdirectory(${CMAKE_SOURCE_DIR}/../../../../third_party/xenia xenia)
#   target_link_libraries(x360core xenia-core xenia-cpu-backend-a64 xenia-gpu-vulkan ...)
