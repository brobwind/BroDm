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

package com.brobwind.brodm;

import android.util.Log;


public class Kit {
	private final static String TAG = "Kit";

	private String mId;
	private String mCliCmt;
	private String mAuthCode;

	static {
		System.loadLibrary("brodm-jni");
	}

	public boolean create(String passwd, String id, String devCmt) {
		mId = id;
		return nCreate(this, passwd, id, devCmt);
	}

	public String getId() { return mId; }
	public String getCliCmt() { return mCliCmt; }
	public String getAuthCode() { return mAuthCode; }

	// Native callback
	private void update(String cliCmt, String authCode) {
		mCliCmt = cliCmt;
		mAuthCode = authCode;
	}

	private static native boolean nCreate(Kit thiz, String passwd, String id, String devCmt);
}
