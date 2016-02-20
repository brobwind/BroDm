#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_PACKAGE_NAME := BroDm

ifneq ($(ONE_SHOT_MAKEFILE),)
TARGET_BUILD_APPS := BroDm
endif

LOCAL_JNI_SHARED_LIBRARIES := libbrodm-jni
LOCAL_DIST_BUNDLED_BINARIES := true

LOCAL_SRC_FILES := \
	$(call all-java-files-under, src)

LOCAL_PROGUARD_ENABLED := disabled

#LOCAL_SDK_VERSION := 20

# Card view support
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v7-cardview

LOCAL_RESOURCE_DIR := \
    frameworks/support/v7/cardview/res \
    $(LOCAL_PATH)/res

LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages android.support.v7.cardview

#LOCAL_JARJAR_RULES := \
#	$(LOCAL_PATH)/jarjar-rules.txt

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
