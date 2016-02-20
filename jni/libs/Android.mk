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

LOCAL_PATH := $(call my-dir)

BRPRE := libbro

include $(CLEAR_VARS)

LIBCHROME_PATH := external/libchrome

#LOCAL_RTTI_FLAG := -frtti
LOCAL_CPP_EXTENSION := .cc
LOCAL_CFLAGS := \
	-Wall -Werror \
	-Wno-char-subscripts -Wno-missing-field-initializers \
	-Wno-unused-function -Wno-unused-parameter -fvisibility=hidden

LOCAL_CPPFLAGS := \
	-Wno-deprecated-register -Wno-sign-promo \
	-Wno-non-virtual-dtor

LOCAL_C_INCLUDES := \
	external/gmock/include \
	external/gtest/include \
	external/libchrome

LOCAL_MODULE := $(BRPRE)_libchrome

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES

libchrome_LOCAL_SRC_FILES := \
	base/at_exit.cc \
	base/callback_internal.cc \
	base/files/file_util_posix.cc \
	base/lazy_instance.cc \
	base/pickle.cc \
	base/rand_util.cc \
	base/rand_util_posix.cc \
	base/strings/string_number_conversions.cc \
	base/strings/string_util.cc \
	base/synchronization/lock_impl_posix.cc \
	base/threading/platform_thread_posix.cc \
	crypto/p224.cc \
	crypto/p224_spake.cc \
	crypto/random.cc \
	crypto/secure_hash_default.cc \
	crypto/secure_util.cc \
	crypto/sha2.cc \
	crypto/third_party/nss/sha512.cc

libchrome_LOCAL_SRC_FILES += \
	base/command_line.cc \
	base/debug/alias.cc \
	base/debug/debugger.cc \
	base/debug/debugger_posix.cc \
	base/files/file_path.cc \
	base/files/file_path_constants.cc \
	base/logging.cc \
	base/strings/string_piece.cc

$(call local-intermediates-dir)/$(LIBCHROME_PATH)/%.cc: PRIV_INTERMEDIATES_DIR := $(call local-intermediates-dir)
$(call local-intermediates-dir)/$(LIBCHROME_PATH)/%.cc: | \
		$(ACP) $(addprefix $(LIBCHROME_PATH)/,$(libchrome_LOCAL_SRC_FILES))
	$(hide)mkdir -p $(dir $@)
	$(hide)$(ACP) $(patsubst $(PRIV_INTERMEDIATES_DIR)/%,%,$@) $@

LOCAL_GENERATED_SOURCES := \
	$(addprefix $(call local-intermediates-dir)/$(LIBCHROME_PATH)/,$(libchrome_LOCAL_SRC_FILES))

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LIBWEAVE_PATH := external/libweave

#LOCAL_RTTI_FLAG := -frtti
LOCAL_CPP_EXTENSION := .cc
LOCAL_CFLAGS := \
	-Wall -Werror \
	-Wno-char-subscripts -Wno-missing-field-initializers \
	-Wno-unused-function -Wno-unused-parameter -fvisibility=hidden

LOCAL_CPPFLAGS := \
	-Wno-deprecated-register \
	-Wno-sign-compare \
	-Wno-sign-promo \
	-Wno-non-virtual-dtor

LOCAL_C_INCLUDES := \
	external/boringssl/src/include \
	external/libchrome \
	external/libweave \
	external/libweave/third_party/modp_b64/modp_b64 \
	external/libweave/third_party/modp_b64 \
	external/libweave/third_party/libuweave

LOCAL_MODULE := $(BRPRE)_libweave

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES

libweave_LOCAL_SRC_FILES := \
	src/data_encoding.cc \
	src/privet/openssl_utils.cc \
	third_party/modp_b64/modp_b64.cc \
	third_party/libuweave/src/crypto_hmac.c
	
$(call local-intermediates-dir)/$(LIBWEAVE_PATH)/%.cc: PRIV_INTERMEDIATES_DIR := $(call local-intermediates-dir)
$(call local-intermediates-dir)/$(LIBWEAVE_PATH)/%.cc: | \
		$(ACP) $(addprefix $(LIBWEAVE_PATH)/,$(libweave_LOCAL_SRC_FILES))
	$(hide)mkdir -p $(dir $@)
	$(hide)$(ACP) $(patsubst $(PRIV_INTERMEDIATES_DIR)/%,%,$@) $@

$(call local-intermediates-dir)/$(LIBWEAVE_PATH)/%.c: PRIV_INTERMEDIATES_DIR := $(call local-intermediates-dir)
$(call local-intermediates-dir)/$(LIBWEAVE_PATH)/%.c: | \
		$(ACP) $(addprefix $(LIBWEAVE_PATH)/,$(libweave_LOCAL_SRC_FILES))
	$(hide)mkdir -p $(dir $@)
	$(hide)$(ACP) $(patsubst $(PRIV_INTERMEDIATES_DIR)/%,%,$@) $@

LOCAL_GENERATED_SOURCES := \
	$(addprefix $(call local-intermediates-dir)/$(LIBWEAVE_PATH)/,$(libweave_LOCAL_SRC_FILES))

include $(BUILD_STATIC_LIBRARY)

# ----------------------------------------------------------------------------

include $(CLEAR_VARS)

#LOCAL_RTTI_FLAG := -frtti
LOCAL_CPP_EXTENSION := .cc
LOCAL_CFLAGS := \
	-Wall -Werror \
	-Wno-char-subscripts -Wno-missing-field-initializers \
	-Wno-unused-function -Wno-unused-parameter -fvisibility=hidden

LOCAL_CPPFLAGS := \
	-Wno-deprecated-register -Wno-sign-promo \
	-Wno-non-virtual-dtor

LOCAL_C_INCLUDES := \
	external/gmock/include \
	external/gtest/include \
	external/libchrome \
	external/libweave

LOCAL_MODULE := $(BRPRE)kit

LOCAL_WHOLE_STATIC_LIBRARIES := \
	$(BRPRE)_libchrome $(BRPRE)_libweave \
	libcrypto_static

LOCAL_SRC_FILES += \
	kit.cc

include $(BUILD_STATIC_LIBRARY)

my_LOCAL_BUILT_MODULE := $(LOCAL_BUILT_MODULE)

$(LOCAL_PATH)/$(notdir $(my_LOCAL_BUILT_MODULE)): $(LOCAL_BUILT_MODULE)
	$(copy-file-to-target)

# ----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc
LOCAL_CFLAGS := \
	-Wall -Werror \
	-Wno-char-subscripts -Wno-missing-field-initializers \
	-Wno-unused-function -Wno-unused-parameter

LOCAL_CPPFLAGS := \
	-Wno-deprecated-register -Wno-sign-promo \
	-Wno-non-virtual-dtor

LOCAL_C_INCLUDES := \
	external/gmock/include \
	external/gtest/include \
	external/libchrome \
	external/libweave

LOCAL_MODULE := Kit

LOCAL_STATIC_LIBRARIES := \
	$(BRPRE)kit

LOCAL_SHARED_LIBRARIES := \
	liblog

LOCAL_SRC_FILES += \
	main.cc

include $(BUILD_EXECUTABLE)

# ----------------------------------------------------------------------------

$(LOCAL_BUILT_MODULE): $(LOCAL_PATH)/$(notdir $(my_LOCAL_BUILT_MODULE))
