cmake_minimum_required(VERSION 3.22.1)
project(wavesynch)

# -------------------------------------------------------------------
# 1) Build Opus using Opus' own CMake build (no globs, no hacks)
# -------------------------------------------------------------------
set(OPUS_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/opus)

# If Opus' CMakeLists.txt is not at opus/CMakeLists.txt but in opus/cmake,
# we point CMake to that and set the source dir accordingly.
# Most Opus trees have CMakeLists.txt at the root; some have it under cmake/.
if (EXISTS "${OPUS_SRC_DIR}/CMakeLists.txt")
    add_subdirectory(${OPUS_SRC_DIR} ${CMAKE_CURRENT_BINARY_DIR}/opus_build)
elseif (EXISTS "${OPUS_SRC_DIR}/cmake/CMakeLists.txt")
    add_subdirectory(${OPUS_SRC_DIR}/cmake ${CMAKE_CURRENT_BINARY_DIR}/opus_build)
else()
    message(FATAL_ERROR "Could not find Opus CMakeLists.txt in opus/ or opus/cmake/")
endif()

# Opus CMake typically creates a target named "opus"
# If yours uses a different target name, we’ll adjust after the first build error.
# -------------------------------------------------------------------
# 2) Build your JNI library and link Opus into it
# -------------------------------------------------------------------
add_library(wavesynch SHARED
        opus_encoder.cpp
)

# Include Opus public headers for your JNI code
target_include_directories(wavesynch PRIVATE
        ${OPUS_SRC_DIR}/include
)

find_library(log-lib log)

# Link against opus target produced by add_subdirectory
target_link_libraries(wavesynch PRIVATE
        opus
        ${log-lib}
)
