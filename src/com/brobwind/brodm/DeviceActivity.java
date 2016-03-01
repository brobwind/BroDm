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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.security.cert.X509Certificate;

import org.json.JSONException;
import org.json.JSONObject;


public class DeviceActivity extends Activity {
	private final static String TAG = "Device";

	public static String STR_PREF = "device";
	public static String STR_IP = "ip";
	public static String STR_PORT = "port";
	private String mIpAddr;
	private int mPort;
	private String mAccessToken;

	private Button mTryInfo;
	private TextView mInfo;
	private Button mTryCert;
	private TextView mCert;
	private Button mTryPairingStart;
	private TextView mPairingStart;
	private Button mTryPairingConfirm;
	private TextView mPairingConfirm;
	private Button mTryPairingCancel;
	private TextView mPairingCancel;
	private Button mTryAuth;
	private TextView mAuth;

	private Button mTryTraits;
	private TextView mTraits;
	private Button mTryComponents;
	private TextView mComponents;

	private Button mTryState;
	private Button mTryCmdDefs;
	private Button mTryCmdStatus;
	private Button mTryCmdList;
	private TextView mCmdInfo;

	private Button mTryCmdExec;
	private EditText mCmd;
	private TextView mCmdExec;

	private Kit mKit;

	private Button mFuncAll[];
	private Button mFuncInfo[];
	private Button mFuncPairing[];

	private static void enableFuncs(Button enableList[], Button disableList[]) {
		if (enableList != null) for (Button func : enableList) {
			func.setEnabled(true);
		}
		if (disableList != null) for (Button func : disableList) {
			func.setEnabled(false);
		}
	}

	private boolean notifyError(final TextView view, String msg, String error) {
		if (msg == null) {
			if (error != null) {
				Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					view.setText(null);
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device);

		startService(new Intent(this, NetworkService.class));
		mKit = new Kit();

		Intent intent = getIntent();
		mIpAddr = intent.getStringExtra(STR_IP);
		mPort = intent.getIntExtra(STR_PORT, -1);

		SharedPreferences.Editor editor = getSharedPreferences(STR_PREF, MODE_PRIVATE).edit();
		editor.putString(STR_IP, mIpAddr);
		editor.putInt(STR_PORT, mPort);
		editor.commit();

		TextView statusTv = (TextView)findViewById(R.id.status);
		statusTv.setText(mIpAddr + ":" + mPort);

		// /privet/info
		mTryInfo = (Button)findViewById(R.id.try_info);
		mTryInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.deviceInfo(mIpAddr, mPort, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mInfo, msg, error)) return;

						int securePort = -1;
						try {
							JSONObject target = new JSONObject(msg);
							JSONObject endpoints  = target.getJSONObject("endpoints");
							securePort = endpoints.getInt("httpsPort");
						} catch (JSONException e) {
							e.printStackTrace();
						}

						mNetService.init(mIpAddr, mPort, securePort, new NetworkUtils.Callback() {
							@Override
							public void onCertAvailable(X509Certificate chains[], String authType) {
								final StringBuilder sb = new StringBuilder();
								for (X509Certificate chain : chains) {
									sb.append(chain.toString());
								}
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										mCert.setText(sb.toString());
									}
								});
							}
						});

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								enableFuncs(mFuncInfo, null /*mFuncPairing*/);
								mInfo.setText(msg);
							}
						});
					}
				});
			}
		});
		mInfo = (TextView)findViewById(R.id.info);

		// CERT: /privet/info
		mTryCert = (Button)findViewById(R.id.try_cert);
		mTryCert.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.fetchCert("/privet/info", new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						Toast.makeText(DeviceActivity.this, "Web server certificate fetched!", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		mCert = (TextView)findViewById(R.id.cert);

		// /privet/v3/pairing/start
		mTryPairingStart = (Button)findViewById(R.id.try_pairing_start);
		mTryPairingStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.pairingStart("/privet/v3/pairing/start", new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mPairingStart, msg, error)) return;

						try {
							JSONObject target = new JSONObject(msg);
							String id = target.getString("sessionId");
							String devCmt = target.getString("deviceCommitment");

							mKit.create("hello", id, devCmt);
						} catch (JSONException e) {
							e.printStackTrace();
						}

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
//								enableFuncs(mFuncPairing, new Button[] { mTryAuth} );
								mPairingStart.setText(msg);
							}
						});
					}
				});
			}
		});
		mPairingStart = (TextView)findViewById(R.id.pairing_start);

		// /privet/v3/pairing/confirm
		mTryPairingConfirm = (Button)findViewById(R.id.try_pairing_confirm);
		mTryPairingConfirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.pairingConfirm("/privet/v3/pairing/confirm", mKit.getId(),
						mKit.getCliCmt(), new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mPairingConfirm, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
//								enableFuncs(new Button[] { mTryAuth }, null);
								mPairingConfirm.setText(msg);
							}
						});
					}
				});
			}
		});
		mPairingConfirm = (TextView)findViewById(R.id.pairing_confirm);

		// /privet/v3/pairing/cancel
		mTryPairingCancel = (Button)findViewById(R.id.try_pairing_cancel);
		mTryPairingCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.pairingCancel("/privet/v3/pairing/cancel", mKit.getId(), new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mPairingStart, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mPairingCancel.setText(msg);
							}
						});
					}
				});
			}
		});
		mPairingCancel = (TextView)findViewById(R.id.pairing_cancel);

		// /privet/v3/auth
		mTryAuth = (Button)findViewById(R.id.try_auth);
		mTryAuth.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.startAuth("/privet/v3/auth", mKit.getId(), mKit.getAuthCode(), new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mPairingStart, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mAuth.setText(msg);
							}
						});

						try {
							JSONObject target = new JSONObject(msg);
							mAccessToken = target.getString("accessToken");
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		mAuth = (TextView)findViewById(R.id.auth);

		// /privet/v3/traits
		mTryTraits = (Button)findViewById(R.id.try_traits);
		mTryTraits.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.showTraits("/privet/v3/traits", mAccessToken, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mTraits, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mTraits.setText(msg);
							}
						});
					}
				});
			}
		});
		mTraits= (TextView)findViewById(R.id.traits);

		// /privet/v3/components
		mTryComponents = (Button)findViewById(R.id.try_components);
		mTryComponents.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mNetService.showComponents("/privet/v3/components", mAccessToken, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mComponents, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mComponents.setText(msg);
							}
						});
					}
				});
			}
		});
		mComponents  = (TextView)findViewById(R.id.components);

		// /privet/v3/state
		mTryState = (Button)findViewById(R.id.try_state);
		mTryState.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String cmd = "{}";
				mNetService.showCommand("/privet/v3/state", mAccessToken, cmd, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mCmdInfo, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mCmdInfo.setText(msg);
							}
						});
					}
				});
			}
		});
		// /privet/v3/commandDefs
		mTryCmdDefs = (Button)findViewById(R.id.try_cmd_defs);
		mTryCmdDefs.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String cmd = "{}";
				mNetService.showCommand("/privet/v3/commandDefs", mAccessToken, cmd, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mCmdInfo, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mCmdInfo.setText(msg);
							}
						});
					}
				});
			}
		});
		// /privet/v3/command/status
		mTryCmdStatus = (Button)findViewById(R.id.try_cmd_status);
		mTryCmdStatus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String cmd = "{}";
				mNetService.showCommand("/privet/v3/commands/status", mAccessToken, cmd, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mCmdInfo, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mCmdInfo.setText(msg);
							}
						});
					}
				});
			}
		});
		// /privet/v3/command/list
		mTryCmdList = (Button)findViewById(R.id.try_cmd_list);
		mTryCmdList.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String cmd = "{}";
				mNetService.showCommand("/privet/v3/commands/list", mAccessToken, cmd, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mCmdInfo, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mCmdInfo.setText(msg);
							}
						});
					}
				});
			}
		});
		mCmdInfo = (TextView)findViewById(R.id.cmd_info);

		// /privet/v3/command/exectue
		mTryCmdExec = (Button)findViewById(R.id.try_cmd_exec);
		mTryCmdExec.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String cmd = ((CharSequence)mCmd.getText()).toString();
				mNetService.showCommand("/privet/v3/commands/execute", mAccessToken, cmd, new NetworkUtils.OnMessage() {
					@Override
					public void onMessage(final String msg, String error) {
						if (notifyError(mCmdExec, msg, error)) return;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mCmdExec.setText(msg);
							}
						});
					}
				});
			}
		});
		mCmd = (EditText)findViewById(R.id.cmd);
		mCmd.setText(/*
"{\n" +
"	\"component\": \"_ledflasher\",\n" +
"	\"name\": \"_ledflasher.set\",\n" +
"	\"parameters\": {\n" +
"		\"led\": 2,\n" +
"		\"on\": true\n" +
"	}\n" +
"}" */
"{\n" +
"	\"name\": \"_ledflasher.animate\",\n" +
"	\"parameters\": {\n" +
"		\"duration\": 0.2,\n" +
"		\"type\": \"marquee_left\"\n" +
"	}\n" +
"}"
);
		mCmdExec = (TextView)findViewById(R.id.cmd_exec);

		mFuncAll = new Button[] {
			mTryInfo, mTryCert, mTryPairingStart, mTryPairingConfirm,
			mTryPairingCancel, mTryAuth, mTryTraits, mTryComponents,
			mTryState, mTryCmdDefs, mTryCmdStatus, mTryCmdList, mTryCmdExec,
		};
		mFuncInfo = new Button[] {
			mTryCert, mTryPairingStart, mTryPairingConfirm,
			mTryPairingCancel, mTryAuth, mTryTraits, mTryComponents,
			mTryState, mTryCmdDefs, mTryCmdStatus, mTryCmdList, mTryCmdExec,
		};
		mFuncPairing = new Button[] {
			mTryPairingConfirm,
			mTryPairingCancel, mTryAuth
		};
	}

	@Override
	public void onResume() {
		super.onResume();
		enableFuncs(null, mFuncAll);
		bindService(new Intent(this, NetworkService.class), mConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onPause() {
		super.onPause();
		unbindService(mConnection);
	}

	// --------------------------------------------------------------------------

	private NetworkService mNetService;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mNetService = ((NetworkService.NetBinder)service).getService();
			enableFuncs(mFuncAll, mFuncInfo);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mNetService = null;
		}
	};
}
