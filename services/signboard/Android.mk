LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := services.signboard
LOCAL_SRC_FILES += \
	$(call all-java-files-under,java)
LOCAL_JAVA_LIBRARIES := services.core
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)
