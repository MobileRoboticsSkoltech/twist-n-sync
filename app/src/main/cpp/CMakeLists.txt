cmake_minimum_required(VERSION 3.14)

project(twist-n-sync)

include(FetchContent)

FetchContent_Declare(
        cmodule
        URL "https://github.com/scapix-com/cmodule/archive/v1.0.29.tar.gz"
        URL_HASH SHA256=b49019b355423aebacd927e99541b146c900ef416ae1da6a8343a2a274dd4876
)

FetchContent_Declare(
        eigen
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG origin/master
)

FetchContent_Declare(
        spline
        GIT_REPOSITORY https://github.com/ttk592/spline.git
        GIT_TAG origin/master
)

FetchContent_MakeAvailable(cmodule)
FetchContent_MakeAvailable(eigen)
FetchContent_MakeAvailable(spline)

set(SCAPIX_BRIDGE "java" CACHE STRING "java")
set(SCAPIX_JAVA_API "android-28" CACHE STRING "one of the folders inside 'scapix/java_api': jdk-11.0.2, android-28, etc.")
set(GENERATED_DIR generated/bridge/java)
set(SOURCE_LIST twist-n-sync-cpp/TimeSync.cpp twist-n-sync-cpp/util/CubicSpline.cpp twist-n-sync-cpp/util/TSUtil.cpp)

add_library(bridge SHARED ${SOURCE_LIST})

find_package(Scapix REQUIRED)
scapix_bridge_headers(bridge "com.googleresearch.capturesync.softwaresync" twist-n-sync-cpp/TimeSync.h)

include_directories(
        ${GENERATED_DIR}
        twist-n-sync-cpp
)

target_include_directories(bridge PUBLIC "${CMAKE_BINARY_DIR}/_deps/eigen-src")
target_include_directories(bridge PUBLIC "${CMAKE_BINARY_DIR}/_deps/spline-src/src")