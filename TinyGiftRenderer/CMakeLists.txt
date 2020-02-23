cmake_minimum_required(VERSION 3.4.1)
set(lib-path ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})
message(${lib-path})

include_directories(${CMAKE_SOURCE_DIR}/../include)
include_directories(src/main/cpp/include)

aux_source_directory(src/main/cpp cpp_src)
set(all_src ${cpp_src})
add_library(giftRendererJni SHARED ${all_src})
find_library(android-lib android)
find_library(log-lib log)
target_link_libraries(giftRendererJni ${log-lib} ${android-lib}
    ${lib-path}/liblightgift.so ${lib-path}/)
