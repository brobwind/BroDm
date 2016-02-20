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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class NetworkUtils {
	private final static String TAG = "NetworkUtils";

	public interface Callback {
		void onCertAvailable(X509Certificate chains[], String authType);
	}

	public interface OnMessage {
		void onMessage(String msg, String error);
	}

	// http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	private static class MySSLSocketFactory extends SSLSocketFactory {
		private SSLContext mSslContext;
		private Callback mCallback;

		public MySSLSocketFactory(KeyStore truststore, Callback callback) throws NoSuchAlgorithmException,
				KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			mCallback = callback;

			TrustManager tm = new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chains, String authType)
						throws CertificateException {
					if (chains == null || mCallback == null) {
						return;
					}

					mCallback.onCertAvailable(chains, authType);
				}
			};

			mSslContext = SSLContext.getInstance("TLS");
			mSslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
					throws IOException, UnknownHostException {
			Log.v(TAG, " -> Create socket: socket=" + socket + ", host=" + host + ", port=" + port + ", autoClose=" + autoClose);
			return mSslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return mSslContext.getSocketFactory().createSocket();
		}
	}

	public static synchronized HttpClient getHttpClient(int port, int securePort, Callback callback) {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory factory = new MySSLSocketFactory(trustStore, callback);
			factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
			HttpProtocolParams.setUseExpectContinue(params, true);

			ConnManagerParams.setTimeout(params, 10000);
			HttpConnectionParams.setConnectionTimeout(params, 10000);
			HttpConnectionParams.setSoTimeout(params, 100000);

			SchemeRegistry reg = new SchemeRegistry();
			reg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), port));
			reg.register(new Scheme("https", factory, securePort));

			ClientConnectionManager connManager = new ThreadSafeClientConnManager(params, reg);

			return new DefaultHttpClient(connManager, params);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new DefaultHttpClient();
	}

	// -----------------------------------------------------------------------
	// http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-apache-http-client
	// http://developer.android.com/reference/java/net/HttpURLConnection.html
	public static void deviceInfo(String path, OnMessage callback) {
		try {
			URL url = new URL(path + "/privet/info");
			HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic anonymous");
			urlConnection.setRequestProperty("Content-Type", "application/json");
			urlConnection.connect();
			try {
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				BufferedReader bufReader = new BufferedReader(new InputStreamReader(in));
				StringBuilder sb = new StringBuilder();
				for (String line = bufReader.readLine(); line != null;) {
					sb.append(line).append("\n");
					line = bufReader.readLine();
				}

				callback.onMessage(sb.toString(), null);
				return;
			} finally {
				urlConnection.disconnect();
			}
		} catch (MalformedURLException muex) {
			Log.e(TAG, "Malformed url: " + path);
			callback.onMessage(null, muex.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			callback.onMessage(null, e.getMessage());
		}
	}

	public static void runCommand(HttpClient client, String token, String url, String cmd, OnMessage callback) {
		HttpPost request = new HttpPost(url);
		request.addHeader("Authorization", "Basic " + token);
		request.addHeader("Content-Type", "application/json");

		try {
			StringEntity content = new StringEntity(cmd);
			request.setEntity(content);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			callback.onMessage(null, e.getMessage());
			return;
		}

		try {
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
				Log.v(TAG, " -> ABORT: " + response.getStatusLine());
				request.abort();
				callback.onMessage(null, "ABORT: " + response.getStatusLine());
				return;
			}
			String result = EntityUtils.toString(response.getEntity());
			Log.v(TAG, " -> RESULT: " + result);
			callback.onMessage(result, null);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			callback.onMessage(null, e.getMessage());
		}
	}
}
