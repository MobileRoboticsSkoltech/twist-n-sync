cmake_minimum_required(VERSION 3.14)

project(twist-n-sync)

include(FetchContent)

FetchContent_Declare(
        cmodule
        URL "https://github.com/scapix-com/cmodule/archive/v1.0.29.tar.gz"
        URL_HASH SHA256=b49019b355423aebacd927e99541b146c900ef416ae1da6a8343a2a274dd4876
)

FetchContent_MakeAvailable(cmodule)

set(SCAPIX_BRIDGE "java" CACHE STRING "cpp, java, objc, python, js, cs")
set(SCAPIX_PLATFORM "generic" CACHE STRING "one of the folders inside 'source/<project>/platform': ios, android, macos, windows, linux, etc.")
set(SCAPIX_JAVA_API "android-28" CACHE STRING "one of the folders inside 'scapix/java_api': jdk-11.0.2, android-28, etc.")

set(SOURCE_LIST twist-n-sync-cpp/TimeSync.cpp twist-n-sync-cpp/util/CubicSpline.cpp twist-n-sync-cpp/util/TSUtil.cpp)
add_library(bridge SHARED ${SOURCE_LIST})

find_package(Scapix REQUIRED)
scapix_bridge_headers(bridge "com.googleresearch.capturesync.softwaresync" twist-n-sync-cpp/TimeSync.h)

set(GENERATED_DIR generated/bridge/java)

include_directories(
        ${GENERATED_DIR}
        twist-n-sync-cpp
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

FetchContent_GetProperties(eigen)
if(NOT eigen_POPULATED)
    FetchContent_Populate(eigen)
    set(eigen_EIGEN_BUILD_DOC OFF)
    set(eigen_BUILD_TESTS OFF)
endif()

FetchContent_GetProperties(spline)
if(NOT spline_POPULATED)
    FetchContent_Populate(spline)
endif()

include_directories(bridge PUBLIC ${eigen_SOURCE_DIR})
include_directories(bridge PUBLIC ${spline_SOURCE_DIR}/src)