LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE	:= test
LOCAL_SRC_FILES	:= test.cpp
LOCAL_CPPFLAGS	+= -I/home/jesper/proj/mydroid/frameworks/base/include # todo: fix abs paths...
LOCAL_CPPFLAGS	+= -I/home/jesper/proj/mydroid/system/core/include
LOCAL_LDLIBS	+= -L/home/jesper/proj/mydroid/out/target/product/generic/system/lib
LOCAL_LDLIBS	+= -lmedia -llog

include $(BUILD_SHARED_LIBRARY)
