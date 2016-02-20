/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "Kit"

#include <jni.h>
#include <JNIHelp.h>
#include <ScopedUtfChars.h>

#include <utils/Log.h>

#include "libs/kit.h"

/*
 * Callback methods
 */
static jmethodID updateMethod;

static jboolean com_brobwind_brodm_Kit_nCreate(JNIEnv* env, jclass /*clazz*/, jobject thiz, jstring passwd, jstring id, jstring devCmt) {
	ScopedUtfChars _passwd(env, passwd);
	ScopedUtfChars _id(env, id);
	ScopedUtfChars _devCmt(env, devCmt);

	std::string cliCmt, authCode;

	create_auth_code(std::string{_passwd.c_str()}, std::string{_id.c_str()},
			std::string{_devCmt.c_str()}, cliCmt, authCode);

	jstring _cliCmt = env->NewStringUTF(cliCmt.c_str());
	jstring _authCode = env->NewStringUTF(authCode.c_str());

	env->CallVoidMethod(thiz, updateMethod, _cliCmt, _authCode);

	env->DeleteLocalRef(_cliCmt);
	env->DeleteLocalRef(_authCode);

	return JNI_TRUE;
}

static JNINativeMethod gMethods[] = {
	{ "nCreate", "(Lcom/brobwind/brodm/Kit;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void*)com_brobwind_brodm_Kit_nCreate }
};

int register_com_brobwind_brodm_Kit(JNIEnv* env) {
	jclass clazz = env->FindClass("com/brobwind/brodm/Kit");
	if (clazz == NULL) {
		ALOGE("Can't find com/brobwind/brodm/Kit");
		return -1;
	}

	updateMethod = env->GetMethodID(clazz, "update", "(Ljava/lang/String;Ljava/lang/String;)V");
	if (updateMethod == NULL) {
		ALOGE("Can't find update method");
		return -1;
	}

	return jniRegisterNativeMethods(env, "com/brobwind/brodm/Kit", gMethods, NELEM(gMethods));
}

extern "C" jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
	JNIEnv *env;
	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		ALOGE("ERROR: GetEnv failed");
		return -1;
	}

	register_com_brobwind_brodm_Kit(env);

	return JNI_VERSION_1_6;
}
