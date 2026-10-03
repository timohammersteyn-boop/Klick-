cmake_minimum_required(VERSION 3.22.1)
project("schwung_audio")

find_package(oboe REQUIRED CONFIG)

add_library(schwung_audio SHARED
    jni_bridge.cpp
    OboeAudioEngine.cpp
)

target_include_directories(schwung_audio PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(schwung_audio
    PRIVATE
    oboe::oboe
    log
    android
    m
)
