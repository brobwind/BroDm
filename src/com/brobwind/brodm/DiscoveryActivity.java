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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;

import static com.brobwind.brodm.DeviceActivity.STR_IP;
import static com.brobwind.brodm.DeviceActivity.STR_PORT;
import static com.brobwind.brodm.DeviceActivity.STR_PREF;


public class DiscoveryActivity extends Activity {
	private final static String TAG = "Discovery";

	private final static String SERVICE_TYPE = "_privet._tcp.";

	private Context mContext;

	private NsdManager mNsd;
	private NsdManager.DiscoveryListener mDiscoveryListener;

	private Handler H;
	private Resources mRes;

	private TextView mStatus;
	private ProgressBar mProgressBar;
	private ListView mList;
	private TextView mHost;
	private TextView mPort;

	private Object mLock = new Object();
	private ArrayList<NsdServiceInfo> mInfoList = new ArrayList<>();

	private ProgressDialog mProgressDialog;

	private class MyResolveListener implements NsdManager.ResolveListener {
		private boolean mIsLost;

		public MyResolveListener(boolean isLost) {
			mIsLost = isLost;
		}

		@Override
		public void onServiceResolved(final NsdServiceInfo serviceInfo) {
			H.post(new Runnable() {
				@Override
				public void run() {
					if (mIsLost) {
						final InetAddress address = serviceInfo.getHost();
						final int port = serviceInfo.getPort();
						for (NsdServiceInfo item : mInfoList) {
							if (address.equals(item.getHost()) && port == item.getPort()) {
								mInfoList.remove(item);
								break;
							}
						}
					} else {
						mInfoList.add(serviceInfo);
					}

					((BaseAdapter)mList.getAdapter()).notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onResolveFailed(final NsdServiceInfo serviceInfo, int errCode) {
			Log.v(TAG, " -> service resolved failed: " + serviceInfo + ", code=" + errCode);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = DiscoveryActivity.this;

		mNsd = (NsdManager)getSystemService(NSD_SERVICE);
		mDiscoveryListener = new NsdManager.DiscoveryListener() {
			@Override
			public void onDiscoveryStarted(final String regType) {
				H.post(new Runnable() {
					@Override
					public void run() {
						String prefix = mRes.getString(R.string.nsd_stat_discovery_start);
						mStatus.setText(prefix + regType);
						mProgressBar.setIndeterminate(true);
					}
				});
			}

			@Override
			public void onServiceFound(final NsdServiceInfo service) {
				MyResolveListener resolver = new MyResolveListener(false);
				mNsd.resolveService(service, resolver);
			}

			@Override
			public void onServiceLost(final NsdServiceInfo service) {
				MyResolveListener resolver = new MyResolveListener(true);
				mNsd.resolveService(service, resolver);
			}

			@Override
			public void onDiscoveryStopped(final String serviceType) {
				H.post(new Runnable() {
					@Override
					public void run() {
						String prefix = mRes.getString(R.string.nsd_stat_discovery_stop);
						mStatus.setText(prefix + serviceType);
						mProgressBar.setIndeterminate(false);
					}
				});
			}

			@Override
			public void onStartDiscoveryFailed(final String serviceType, final int errorCode) {
				H.post(new Runnable() {
					@Override
					public void run() {
						String prefix = mRes.getString(R.string.nsd_stat_start_failed);
						mStatus.setText(prefix + " type=" + serviceType + ", code=" + errorCode);
					}
				});
			}

			@Override
			public void onStopDiscoveryFailed(final String serviceType, final int errorCode) {
				H.post(new Runnable() {
					@Override
					public void run() {
						String prefix = mRes.getString(R.string.nsd_stat_stop_failed);
						mStatus.setText(prefix + " type=" + serviceType + ", code=" + errorCode);
					}
				});
			}
		};

		H = new Handler();
		mRes = getResources();

		setContentView(R.layout.main);

		mStatus = (TextView)findViewById(R.id.status);
		mProgressBar = (ProgressBar)findViewById(R.id.progress);

		mList = (ListView)findViewById(R.id.list);
		mList.setAdapter(new MyAdapter());

		mList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final NsdServiceInfo info = (NsdServiceInfo)view.getTag();

				Intent intent = new Intent(mContext, DeviceActivity.class);
				intent.putExtra(STR_IP, info.getHost().getHostAddress());
				intent.putExtra(STR_PORT, info.getPort());

				mContext.startActivity(intent);
			}
		});

		mHost = (EditText)findViewById(R.id.host);
		mPort = (EditText)findViewById(R.id.port);

		Button startButton = (Button)findViewById(R.id.start);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String strHost = ((CharSequence)mHost.getText()).toString();
				final String strPort = ((CharSequence)mPort.getText()).toString();

				if (strHost.length() == 0) {
					Toast.makeText(mContext, "Host name can not be empty!", Toast.LENGTH_SHORT).show();
					return;
				}

				if (strPort.length() == 0) {
					Toast.makeText(mContext, "Port number must be specified!", Toast.LENGTH_SHORT).show();
					return;
				}

				new AsyncTask<String, Void, String>() {
					@Override
					protected void onPreExecute() {
						if (mProgressDialog == null) {
							mProgressDialog = new ProgressDialog(mContext, ProgressDialog.STYLE_SPINNER); 
							mProgressDialog.setCancelable(false);
							mProgressDialog.setCanceledOnTouchOutside(false);
						}
						mProgressDialog.show();
					}
					@Override
					protected String doInBackground(String ...hosts) {
						try {
							return InetAddress.getByName(hosts[0]).getHostAddress();
						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
					}

					@Override
					protected void onPostExecute(String ip) {
						mProgressDialog.dismiss();
						Log.v(TAG, " -> ip=" + ip);
						if (ip == null) {
							Toast.makeText(mContext, "Can't resolve host: " + strHost, Toast.LENGTH_SHORT).show();
							return;
						}

						Intent intent = new Intent(mContext, DeviceActivity.class);
						intent.putExtra(STR_IP, ip);
						intent.putExtra(STR_PORT, Integer.parseInt(strPort));

						mContext.startActivity(intent);
					}
				}.execute(strHost);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences pref = getSharedPreferences(STR_PREF, MODE_PRIVATE);
		String strIp = pref.getString(STR_IP, null);
		int strPort = pref.getInt(STR_PORT, -1);
		if (strIp != null) {
			mHost.setText(strIp);
			mPort.setText("" + strPort);
		}

		mInfoList.clear();
		mNsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
	}

	@Override
	public void onPause() {
		super.onPause();

		mNsd.stopServiceDiscovery(mDiscoveryListener);
	}

	private class MyAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mInfoList.size();
		}

		@Override
		public Object getItem(int position) {
			return mInfoList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mInfoList.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.list_item, container, false);
			}

			NsdServiceInfo info = mInfoList.get(position);

			InetAddress hostAddress = info.getHost();
			TextView view = (TextView)convertView.findViewById(R.id.info_host);
			view.setText((hostAddress == null ? "Unknown" : hostAddress.toString()));

			view = (TextView)convertView.findViewById(R.id.info_port);
			view.setText("" + info.getPort());

			view = (TextView)convertView.findViewById(R.id.info_name);
			view.setText(info.getServiceName());

			view = (TextView)convertView.findViewById(R.id.info_type);
			view.setText(info.getServiceType());

			view = (TextView)convertView.findViewById(R.id.info_detail);
			view.setText(info.toString());

			convertView.setTag(info);

			return convertView;
		}
	}
}
