LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := PacketCaptureTool
LOCAL_SRC_FILES := PacketCaptureTool.cpp

LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := cap_tool
LOCAL_SRC_FILES := PacketCaptureTool.cpp

LOCAL_LDLIBS    := -llog

include $(BUILD_EXECUTABLE)
