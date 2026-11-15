cmake_minimum_required(VERSION 3.22.1)
project(xenojvm C)

add_library(xenojvm SHARED xeno_jvm.c)

find_library(log-lib log)
target_link_libraries(xenojvm ${log-lib} dl)
