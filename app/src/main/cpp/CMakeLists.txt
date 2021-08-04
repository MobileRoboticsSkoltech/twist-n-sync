cmake_minimum_required(VERSION 3.14)

project(twist-n-sync)

include(FetchContent)
FetchContent_Declare(
        cmodule
        URL "https://github.com/scapix-com/cmodule/archive/v1.0.29.tar.gz"
        URL_HASH SHA256=b49019b355423aebacd927e99541b146c900ef416ae1da6a8343a2a274dd4876
)
FetchContent_MakeAvailable(cmodule)

set(SCAPIX_BRIDGE "java" CACHE STRING "java")
set(SCAPIX_PLATFORM "windows" CACHE STRING "one of the folders inside 'source/chat/platform': android, windows")
set(SCAPIX_JAVA_API "android-28" CACHE STRING "one of the folders inside 'scapix/java_api': jdk-11.0.2, android-28, etc.")
set(GENERATED_DIR generated/bridge/java)
set(SOURCE_LIST source/TimeSync.cpp source/util/CubicSpline.cpp source/util/TSUtil.cpp)

add_library(bridge SHARED ${SOURCE_LIST})

find_package(Scapix REQUIRED)
scapix_bridge_headers(bridge "com.googleresearch.capturesync.softwaresync" source/TimeSync.h)

include_directories(
        ${GENERATED_DIR}
        source
)

target_include_directories(bridge PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lib/eigen")
target_include_directories(bridge PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lib/spline/src")