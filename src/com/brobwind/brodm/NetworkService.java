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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Binder;

import org.apache.http.client.HttpClient;


public class NetworkService extends Service {
	private final static String TAG = "NetworkService";

	private HttpClient sClient;
	private Handler H;
	private IBinder mBinder = new NetBinder();

	@Override
	public void onCreate() {
		// Create handler thread
		HandlerThread handlerThread = new HandlerThread("net");
		handlerThread.start();
		H = new Handler(handlerThread.getLooper());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int id) {
		return START_NOT_STICKY;
	}

	public class NetBinder extends Binder {
		NetworkService getService() {
			return NetworkService.this;
		}
	}

	// ------------------------------------------------------------

	private String mIpAddr;
	private int mPort;
	private int mSecurePort;

	public void deviceInfo(final String ipAddr, final int port, final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				NetworkUtils.deviceInfo("http://" + ipAddr + ":" + port, callback);
			}
		});
	}

	public void init(String ipAddress, int port, int securePort, NetworkUtils.Callback callback) {
		mIpAddr = ipAddress;
		mPort = port;
		mSecurePort = securePort;
		sClient = NetworkUtils.getHttpClient(port, securePort, callback);
	}

	public void fetchCert(final String path, final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{}";

				NetworkUtils.runCommand(sClient, "anonymous", url, cmd, callback);
			}
		});
	}

	public void pairingStart(final String path, final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{ \"pairing\": \"embeddedCode\", \"crypto\":\"p224_spake2\" }";

				NetworkUtils.runCommand(sClient, "anonymous", url, cmd, callback);
			}
		});
	}

	public void pairingConfirm(final String path, final String id, final String cliCmt,
			final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{ \"sessionId\": \"" + id + "\", \"clientCommitment\":\"" + cliCmt + "\" }";

				NetworkUtils.runCommand(sClient, "anonymous", url, cmd, callback);
			}
		});
	}

	public void pairingCancel(final String path, final String id,
			final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{ \"sessionId\": \"" + id + "\" }";

				NetworkUtils.runCommand(sClient, "anonymous", url, cmd, callback);
			}
		});
	}

	public void startAuth(final String path, final String id, final String authCode,
			final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{ \"mode\": \"pairing\", \"requestedScope\":\"owner\", \"authCode\": \"" + authCode + "\" }";

				NetworkUtils.runCommand(sClient, "anonymous", url, cmd, callback);
			}
		});
	}

	public void showTraits(final String path, final String token,
			final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{}";

				NetworkUtils.runCommand(sClient, token, url, cmd, callback);
			}
		});
	}

	public void showComponents(final String path, final String token,
			final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				String cmd = "{}";

				NetworkUtils.runCommand(sClient, token, url, cmd, callback);
			}
		});
	}

	public void showCommand(final String path, final String token, final String cmd,
			final NetworkUtils.OnMessage callback) {
		H.post(new Runnable() {
			@Override
			public void run() {
				String url = "https://" + mIpAddr + ":" + mSecurePort + path;
				NetworkUtils.runCommand(sClient, token, url, cmd, callback);
			}
		});
	}
}
