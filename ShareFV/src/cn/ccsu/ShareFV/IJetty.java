//========================================================================
//$Id: IJetty.java 474 2012-01-23 03:07:14Z janb.webtide $
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package cn.ccsu.ShareFV;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.eclipse.jetty.util.IO;

import cn.ccsu.ShareFV.R;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.ccsu.chatserver.ServerThread;
import cn.ccsu.log.AndroidLog;
import cn.ccsu.netdiscovery.BroadCastWord;
import cn.ccsu.util.AndroidInfo;
import cn.ccsu.util.IJettyToast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

/**
 * IJetty
 * 
 * Main Jetty activity. Can start other activities: + configure + download
 * 
 * Can start/stop services: + IJettyService
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class IJetty extends Activity {

	private static final String TAG = "Jetty";

	public static final String __START_ACTION = "org.mortbay.ijetty.start";
	public static final String __STOP_ACTION = "org.mortbay.ijetty.stop";

	public static final String __PORT = "org.mortbay.ijetty.port";
	public static final String __NIO = "org.mortbay.ijetty.nio";
	public static final String __SSL = "org.mortbay.ijetty.ssl";

	public static final String __CONSOLE_PWD = "org.mortbay.ijetty.console";
	public static final String __PORT_DEFAULT = "8080";
	public static final boolean __NIO_DEFAULT = true;
	public static final boolean __SSL_DEFAULT = false;

	public static final String __CONSOLE_PWD_DEFAULT = "admin";

	public static final String __WEBAPP_DIR = "webapps";
	public static final String __ETC_DIR = "etc";
	public static final String __CONTEXTS_DIR = "contexts";

	public static final String __TMP_DIR = "tmp";
	public static final String __WORK_DIR = "work";
	public static final int __SETUP_PROGRESS_DIALOG = 0;
	public static final int __SETUP_DONE = 2;
	public static final int __SETUP_RUNNING = 1;
	public static final int __SETUP_NOTDONE = 0;

	public LocationClient mLocationClient = null;
	public BDLocationListener myListener = new MyLocationListener();
	//��¼ip��ַ
	public static String hostip;
	//��¼��Ƶ��
	public static String VideoName;
	//��¼�Լ��ĵ�ַ
	public static  double mLat ;  
	public static 	double mLon ;  
	BroadCastWord broad;
	Thread publicShareThread;
	ServerThread serverThread;  
	NotificationManager mNotificationManager;
	public static final File __JETTY_DIR;
	private ProgressDialog progressDialog;
	private Thread progressThread;
	private Handler handler;
	private BroadcastReceiver bcastReceiver;
	//��ť������
	private ImageButton startButton;
	private ImageButton publics;
	private ImageButton play;
	private ImageButton explorerButton;
	private ImageButton searchButton;
	private ImageButton shareButton;
	private TextView jetty_controller_start_TextView;
	private TextView jetty_controller_publicshare_TextView;
	private TextView jetty_controller_play_TextView;
	private TextView jetty_controller_explorer_TextView;
	private TextView jetty_controller_search_TextView;
	private TextView jetty_controller_share_TextView;
	private LinearLayout jetty_controller_LinearLayout_start;
	private LinearLayout jetty_controller_LinearLayout_publics;
	private LinearLayout jetty_controller_LinearLayout_play;
	private LinearLayout jetty_controller_LinearLayout_explorer;
	private LinearLayout jetty_controller_LinearLayout_search;
	private LinearLayout jetty_controller_LinearLayout_share;
	/**
	 * ProgressThread
	 * 
	 * Handles finishing install tasks for Jetty.
	 */
	class ProgressThread extends Thread {
		private Handler _handler;

		public ProgressThread(Handler h) {
			_handler = h;
		}

		public void sendProgressUpdate(int prog) {
			Message msg = _handler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("prog", prog);
			msg.setData(b);
			_handler.sendMessage(msg);
		}

		public void run() {
			boolean updateNeeded = false;

			// create the jetty dir structure
			File jettyDir = __JETTY_DIR;
			if (!jettyDir.exists()) {
				boolean made = jettyDir.mkdirs();
				Log.i(TAG, "Made " + __JETTY_DIR + ": " + made);
			}

			sendProgressUpdate(10);

			File workDir = new File(jettyDir, __WORK_DIR);
			if (workDir.exists()) {
				Installer.delete(workDir);
				Log.i(TAG, "removed work dir");
			}

			// make jetty/tmp
			File tmpDir = new File(jettyDir, __TMP_DIR);
			if (!tmpDir.exists()) {
				boolean made = tmpDir.mkdirs();
				Log.i(TAG, "Made " + tmpDir + ": " + made);
			} else {
				Log.i(TAG, tmpDir + " exists");
			}

			// make jetty/webapps
			File webappsDir = new File(jettyDir, __WEBAPP_DIR);
			if (!webappsDir.exists()) {
				boolean made = webappsDir.mkdirs();
				Log.i(TAG, "Made " + webappsDir + ": " + made);
				//��assetsĿ¼�µ�jetty���Ƶ�webappsĿ¼��
				CopyAssets("jetty", "/mnt/sdcard/jetty/webapps/jetty");

			} else {
				Log.i(TAG, webappsDir + " exists");
			}

			// make jetty/etc
			File etcDir = new File(jettyDir, __ETC_DIR);
			if (!etcDir.exists()) {
				boolean made = etcDir.mkdirs();
				Log.i(TAG, "Made " + etcDir + ": " + made);
			} else {
				Log.i(TAG, etcDir + " exists");
			}
			sendProgressUpdate(30);

			File webdefaults = new File(etcDir, "webdefault.xml");
			if (!webdefaults.exists() || updateNeeded) {
				// get the webdefaults.xml file out of resources
				try {
					InputStream is = getResources().openRawResource(
							R.raw.webdefault);
					OutputStream os = new FileOutputStream(webdefaults);
					IO.copy(is, os);
					Log.i(TAG, "Loaded webdefault.xml");
				} catch (Exception e) {
					Log.e(TAG, "Error loading webdefault.xml", e);
				}
			}
			sendProgressUpdate(40);

			File realm = new File(etcDir, "realm.properties");
			if (!realm.exists() || updateNeeded) {
				try {
					// get the realm.properties file out resources
					InputStream is = getResources().openRawResource(
							R.raw.realm_properties);
					OutputStream os = new FileOutputStream(realm);
					IO.copy(is, os);
					Log.i(TAG, "Loaded realm.properties");
				} catch (Exception e) {
					Log.e(TAG, "Error loading realm.properties", e);
				}
			}
			sendProgressUpdate(50);

			File keystore = new File(etcDir, "keystore");
			if (!keystore.exists() || updateNeeded) {
				try {
					// get the keystore out of resources
					InputStream is = getResources().openRawResource(
							R.raw.keystore);
					OutputStream os = new FileOutputStream(keystore);
					IO.copy(is, os);
					Log.i(TAG, "Loaded keystore");
				} catch (Exception e) {
					Log.e(TAG, "Error loading keystore", e);
				}
			}
			sendProgressUpdate(60);

			// make jetty/contexts
			File contextsDir = new File(jettyDir, __CONTEXTS_DIR);
			if (!contextsDir.exists()) {
				boolean made = contextsDir.mkdirs();
				Log.i(TAG, "Made " + contextsDir + ": " + made);
			} else {
				Log.i(TAG, contextsDir + " exists");
			}
			sendProgressUpdate(70);

			try {
				PackageInfo pi = getPackageManager().getPackageInfo(
						getPackageName(), 0);
				if (pi != null) {
					setStoredJettyVersion(pi.versionCode);
				}
			} catch (Exception e) {
				Log.w(TAG, "Unable to get PackageInfo for i-jetty");
			}

			// if there was a .update file indicating an update was needed,
			// remove it now we've updated
			File update = new File(__JETTY_DIR, ".update");
			if (update.exists())
				update.delete();

			sendProgressUpdate(100);
		}
	};

	static {
		
		__JETTY_DIR = new File(Environment.getExternalStorageDirectory(),
				"jetty");
		// Ensure parsing is not validating - does not work with android
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating",
				"false");

		// Bridge Jetty logging to Android logging
		System.setProperty("org.eclipse.jetty.util.log.class",
				"org.mortbay.ijetty.AndroidLog");
		org.eclipse.jetty.util.log.Log.setLog(new AndroidLog());
	}

	public IJetty() {
		super();

		handler = new Handler() {
			public void handleMessage(Message msg) {
				int total = msg.getData().getInt("prog");
				progressDialog.setProgress(total);
				if (total >= 100) {
					dismissDialog(__SETUP_PROGRESS_DIALOG);
				}
			}

		};
	}

	public String formatJettyInfoLine(String format, Object... args) {
		String ms = "";
		if (format != null)
			ms = String.format(format, args);
		return ms + "<br/>";
	}


	protected int getStoredJettyVersion() {
		File jettyDir = __JETTY_DIR;
		if (!jettyDir.exists()) {
			return -1;
		}
		File versionFile = new File(jettyDir, "version.code");
		if (!versionFile.exists()) {
			return -1;
		}
		int val = -1;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(versionFile));
			val = ois.readInt();
			return val;
		} catch (Exception e) {
			Log.e(TAG, "Problem reading version.code", e);
			return -1;
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (Exception e) {
					Log.d(TAG, "Error closing version.code input stream", e);
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (bcastReceiver != null)
			unregisterReceiver(bcastReceiver);

		if(IJettyService.isRunning())
		{
			stopService(new Intent(IJetty.this, IJettyService.class));	
		}
		if(serverThread!=null&&serverThread.isAlive())
		{
			serverThread.finalize();
		}

		super.onDestroy();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		//�������û�������˳���ô��Ƶ����û�л�ԭ������ǰ��Ŀ¼���Կ�ʼ����ǰ�Ƚ��ϴι������Ƶ��ԭ
		restore();

		//��λ���
		mLocationClient = new LocationClient(getApplicationContext());     //����LocationClient��
		mLocationClient.registerLocationListener( myListener );    //ע���������
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setCoorType("bd09ll");//���صĶ�λ����ǰٶȾ�γ��,Ĭ��ֵgcj02
		mLocationClient.setLocOption(option);

		//�õ�ip��ַ
		hostip=getLocalIpAddress();

		//�㲥�Լ��ĵ�ַʹ�����������豸�ܹ�����
		broad=new BroadCastWord();
		publicShareThread=new Thread(broad);

		setContentView(R.layout.jetty_controller);


		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		//�����������ť
		startButton = (ImageButton) findViewById(R.id.start);
		jetty_controller_start_TextView = (TextView) findViewById(R.id.jetty_controller_start_TextView);
		jetty_controller_LinearLayout_start = (LinearLayout) findViewById(R.id.jetty_controller_LinearLayout_start);
		jetty_controller_LinearLayout_start.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					startButton.setBackgroundResource(R.drawable.jetty_controller_start_btn_02);
					jetty_controller_start_TextView.setTextColor(Color.rgb(0,0,0));
				}
				else if(event.getAction()==MotionEvent.ACTION_UP)
				{
					startButton.setBackgroundResource(R.drawable.jetty_controller_start_btn_01);
					jetty_controller_start_TextView.setTextColor(Color.rgb(255,255,255));
				}
				return false;
			}
		});

		//������������ť
		publics=(ImageButton) findViewById(R.id.publics);
		jetty_controller_publicshare_TextView = (TextView) findViewById(R.id.jetty_controller_publicshare_TextView);
		jetty_controller_LinearLayout_publics = (LinearLayout) findViewById(R.id.jetty_controller_LinearLayout_publics);
		jetty_controller_LinearLayout_publics.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_02);
					jetty_controller_publicshare_TextView.setTextColor(Color.rgb(0,0,0));
				}
				else if(event.getAction()==MotionEvent.ACTION_UP)
				{
					publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_01);
					jetty_controller_publicshare_TextView.setTextColor(Color.rgb(255,255,255));
				}
				return false;
			}
		});

		//���Ű�ť
		play = (ImageButton) findViewById(R.id.play);
		jetty_controller_play_TextView = (TextView) findViewById(R.id.jetty_controller_play_TextView);
		jetty_controller_LinearLayout_play = (LinearLayout) findViewById(R.id.jetty_controller_LinearLayout_play);
		jetty_controller_LinearLayout_play.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					play.setBackgroundResource(R.drawable.jetty_controller_play_btn_02);
					jetty_controller_play_TextView.setTextColor(Color.rgb(0,0,0));
				}
				else if(event.getAction()==MotionEvent.ACTION_UP)
				{
					play.setBackgroundResource(R.drawable.jetty_controller_play_btn_01);
					jetty_controller_play_TextView.setTextColor(Color.rgb(255,255,255));
				}
				return false;
			}
		});

		//ѡ������Ƶ��ť
		explorerButton = (ImageButton) findViewById(R.id.explorer);
		jetty_controller_explorer_TextView = (TextView) findViewById(R.id.jetty_controller_explorer_TextView);
		jetty_controller_LinearLayout_explorer = (LinearLayout) findViewById(R.id.jetty_controller_LinearLayout_explorer);
		jetty_controller_LinearLayout_explorer.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_02);
					jetty_controller_explorer_TextView.setTextColor(Color.rgb(0,0,0));
				}
				else if(event.getAction()==MotionEvent.ACTION_UP)
				{
					explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_01);
					jetty_controller_explorer_TextView.setTextColor(Color.rgb(255,255,255));
				}
				return false;
			}
		});

		//��Ҷ��ڿ�ʲô��ť
		searchButton = (ImageButton)findViewById(R.id.search);
		jetty_controller_search_TextView = (TextView) findViewById(R.id.jetty_controller_search_TextView);
		jetty_controller_LinearLayout_search = (LinearLayout) findViewById(R.id.jetty_controller_LinearLayout_search);
		jetty_controller_LinearLayout_search.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					searchButton.setBackgroundResource(R.drawable.jetty_controller_search_btn_02);
					jetty_controller_search_TextView.setTextColor(Color.rgb(0,0,0));
				}
				else if(event.getAction()==MotionEvent.ACTION_UP)
				{
					searchButton.setBackgroundResource(R.drawable.jetty_controller_search_btn_01);
					jetty_controller_search_TextView.setTextColor(Color.rgb(255,255,255));
				}
				return false;
			}
		});

		//�����罻���簴ť
		shareButton = (ImageButton) findViewById(R.id.share);
		jetty_controller_share_TextView = (TextView) findViewById(R.id.jetty_controller_share_TextView);
		jetty_controller_LinearLayout_share = (LinearLayout) findViewById(R.id.jetty_controller_LinearLayout_share);
		jetty_controller_LinearLayout_share.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					shareButton.setBackgroundResource(R.drawable.jetty_controller_share_btn_02);
					jetty_controller_share_TextView.setTextColor(Color.rgb(0,0,0));
				}
				else if(event.getAction()==MotionEvent.ACTION_UP)
				{
					shareButton.setBackgroundResource(R.drawable.jetty_controller_share_btn_01);
					jetty_controller_share_TextView.setTextColor(Color.rgb(255,255,255));
				}
				return false;
			}
		});
		
		
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(__START_ACTION);
		filter.addAction(__STOP_ACTION);
		filter.addCategory("default");

		bcastReceiver = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				if (__START_ACTION.equalsIgnoreCase(intent.getAction())) {
					//����������˿���
					jetty_controller_LinearLayout_publics.setEnabled(true);
					publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_01);
					jetty_controller_publicshare_TextView.setTextColor(Color.rgb(255,255,255));
					//ѡ����Ƶ������
					jetty_controller_LinearLayout_explorer.setEnabled(false);	
					explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_03);
					jetty_controller_explorer_TextView.setTextColor(Color.rgb(204,204,204));

					if (AndroidInfo.isOnEmulator(IJetty.this))
					{}
				} else if (__STOP_ACTION.equalsIgnoreCase(intent.getAction())) {
					//ѡ����Ƶ����
					jetty_controller_LinearLayout_explorer.setEnabled(true);	
					explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_01);
					jetty_controller_explorer_TextView.setTextColor(Color.rgb(255,255,255));
					//����������˲�����
					jetty_controller_LinearLayout_publics.setEnabled(false);
					publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_03);
					jetty_controller_publicshare_TextView.setTextColor(Color.rgb(204,204,204));

				}
			}

		};

		registerReceiver(bcastReceiver, filter);




		jetty_controller_LinearLayout_explorer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//IJettyDownloader.show(IJetty.this);
				restore();
				Intent intent = new Intent();
				intent.putExtra("explorer_title",
						getString(R.string.dialog_read_from_dir));
				intent.setDataAndType(Uri.fromFile(new File("/sdcard")), "*/*");
				intent.setClass(IJetty.this, ExDialog.class);
				startActivityForResult(intent, 1);
			}
		});	
	}

	public static void show(Context context) {
		final Intent intent = new Intent(context, IJetty.class);
		context.startActivity(intent);
	}

	@Override
	protected void onResume() {
		if (!SdCardUnavailableActivity.isExternalStorageAvailable()) {
			SdCardUnavailableActivity.show(this);
		} else {
			if (isUpdateNeeded()) {
				setupJetty();
			}
		}

		if (IJettyService.isRunning()) {
			//����������˿���
			jetty_controller_LinearLayout_publics.setEnabled(true);
			publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_01);
			jetty_controller_publicshare_TextView.setTextColor(Color.rgb(255,255,255));
			//ѡ����Ƶ������
			jetty_controller_LinearLayout_explorer.setEnabled(false);	
			explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_03);
			jetty_controller_explorer_TextView.setTextColor(Color.rgb(204,204,204));
		} else {
			//����������˲�����
			jetty_controller_LinearLayout_publics.setEnabled(false);
			publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_03);
			jetty_controller_publicshare_TextView.setTextColor(Color.rgb(204,204,204));
			//ѡ����Ƶ����
			jetty_controller_LinearLayout_explorer.setEnabled(true);	
			explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_01);
			jetty_controller_explorer_TextView.setTextColor(Color.rgb(255,255,255));
		}
		super.onResume();
	}
	/**
	 * We need to an update iff we don't know the current jetty version or it is
	 * different to the last version that was installed.
	 * 
	 * @return
	 */
	public boolean isUpdateNeeded() {
		// if no previous version file, assume update is required
		int storedVersion = getStoredJettyVersion();
		if (storedVersion <= 0)
			return true;

		try {
			// if different previous version, update is required
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			if (pi == null)
				return true;
			if (pi.versionCode != storedVersion)
				return true;

			// if /sdcard/jetty/.update file exists, then update is required
			File alwaysUpdate = new File(__JETTY_DIR, ".update");
			if (alwaysUpdate.exists()) {
				Log.i(TAG, "Always Update tag found " + alwaysUpdate);
				return true;
			}
		} catch (Exception e) {
			// if any of these tests go wrong, best to assume update is true?
			return true;
		}

		return false;
	}

	public void setupJetty() {
		showDialog(__SETUP_PROGRESS_DIALOG);
		progressThread = new ProgressThread(handler);
		progressThread.start();
	};
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case __SETUP_PROGRESS_DIALOG: {
			progressDialog = new ProgressDialog(IJetty.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Finishing initial install ...");

			return progressDialog;
		}
		default:
			return null;
		}
	}


	protected void setStoredJettyVersion(int version) {
		File jettyDir = __JETTY_DIR;
		if (!jettyDir.exists()) {
			return;
		}
		File versionFile = new File(jettyDir, "version.code");
		ObjectOutputStream oos = null;
		try {
			FileOutputStream fos = new FileOutputStream(versionFile);
			oos = new ObjectOutputStream(fos);
			oos.writeInt(version);
			oos.flush();
		} catch (Exception e) {
			Log.e(TAG, "Problem writing jetty version", e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (Exception e) {
					Log.d(TAG, "Error closing version.code output stream", e);
				}
			}
		}
	}

	//��assets�ļ��е��ļ�������sd����
	private void CopyAssets(String assetDir, String dir) {   
		String[] files;   
		try {   
			files = this.getResources().getAssets().list(assetDir);   
		} catch (IOException e1) {   
			return;   
		}   
		File mWorkingPath = new File(dir);   
		// if this directory does not exists, make one.   
		if (!mWorkingPath.exists()) {   
			if (!mWorkingPath.mkdirs()) {   

			}   
		}   
		for (int i = 0; i < files.length; i++) {   
			try {   
				String fileName = files[i];   
				// we make sure file name not contains '.' to be a folder.   
				if (!fileName.contains(".")) {   
					if (0 == assetDir.length()) {   
						CopyAssets(fileName, dir  + fileName);   
						//                    	 CopyAssets(assetDir + "/" + fileName, dir + fileName   
						//                                 + "/"); 
					} else {   
						CopyAssets(assetDir + "/" + fileName, dir  + "/"+ fileName);   

					}   
					continue;   
				}   
				File outFile = new File(mWorkingPath, fileName);   
				if (outFile.exists())   
					outFile.delete();   
				InputStream in = null;   
				if (0 != assetDir.length()) {   
					in = getAssets().open(assetDir + "/" + fileName);   
				} else {   
					in = getAssets().open(fileName);   
				}   
				OutputStream out = new FileOutputStream(outFile);   

				// Transfer bytes from in to out   
				byte[] buf = new byte[1024];   
				int len;   
				while ((len = in.read(buf)) > 0) {   
					out.write(buf, 0, len);   
				}   
			} catch (Exception e) {   
				e.printStackTrace();   
			}   

		}   
	}  
	public void share(View v)
	{
		Intent intent=new Intent(Intent.ACTION_SEND);  
		intent.setType("image/*");//intent.setType("text/plain");  
		intent.putExtra(Intent.EXTRA_SUBJECT, "�����Ƽ�");  
		intent.putExtra(Intent.EXTRA_TEXT, "�ˣ�������ʹ��ShareFV���ֻ��ھ�����������һ����Ƶ�����ظ�����ڲ��Ž��������ҵ�ip��ַ("+hostip+")�Ϳ��Ժ���һ��ۿ���Ƶ���һ�����һ������.");  
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		startActivity(Intent.createChooser(intent, getTitle()));  
	}
	public  void player(View v)
	{
		//�������ķ������Ͳ����Լ��������Ƶ
		if(IJettyService.isRunning()){
			Intent intent = new Intent(IJetty.this, PlayVideo.class);
			intent.putExtra("hostip", hostip);
			startActivity(intent);
		}
		//���û�п�������Ͳ��ű��˹������Ƶ
		else
		{
			final EditText inputServer = new EditText(this);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("������ip��ַ").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
			.setNegativeButton("Cancel", null);
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					String ip= inputServer.getText().toString();
					Intent intent = new Intent(IJetty.this, PlayVideo.class);
					intent.putExtra("hostip", ip);
					startActivity(intent);
				}
			});
			builder.show();
		}
	}
	//��ת����ͼɨ�����
	public void search(View v)
	{
		Intent intent = new Intent(IJetty.this, MapActivity.class);
		startActivity(intent);
	}
	//�����͹رշ���
	public void startservice(View v)
	{
		if(!IJettyService.isRunning())
		{
			SharedPreferences sp = IJetty.this.getSharedPreferences("sharepath", MODE_PRIVATE);
			String path=sp.getString("sharePath", "none");
			if("".equals(path))
			{
				Toast.makeText(IJetty.this, "��δ������Ƶ������ѡ��������Ƶ��", 5000).show();
				//û�й�����Ƶ����ת��ѡ����Ƶ����
				restore();
				Intent intent = new Intent();
				intent.putExtra("explorer_title",
						getString(R.string.dialog_read_from_dir));
				intent.setDataAndType(Uri.fromFile(new File("/sdcard")), "*/*");
				intent.setClass(IJetty.this, ExDialog.class);
				startActivityForResult(intent, 1);
			}			
			else {
				// TODO get these values from editable UI elements
				Intent intent = new Intent(IJetty.this, IJettyService.class);
				intent.putExtra(__PORT, __PORT_DEFAULT);
				intent.putExtra(__NIO, __NIO_DEFAULT);
				intent.putExtra(__SSL, __SSL_DEFAULT);
				intent.putExtra(__CONSOLE_PWD, __CONSOLE_PWD_DEFAULT);
				startService(intent);
				//����������߳�
				try {
					serverThread= new ServerThread();  
					serverThread.start();
				} catch (Exception e) {
					// TODO: handle exception
					Toast.makeText(IJetty.this, "���������ʧ�������¿�������", 9000).show();
				}
				//֪ͨ����ʾ��Ϣ
				notificationMessage();
				//��ʾ�Ƿ�����������
				dialogPublic();
				jetty_controller_start_TextView.setText("�رշ������");
			}
			
		}
		else
		{
			stopService(new Intent(IJetty.this, IJettyService.class));	
			//ֹͣ����֮���֪ͨ������Ϣ���
			mNotificationManager.cancel(1);
			if(serverThread!=null&&serverThread.isAlive())
			{
				serverThread.finalize();
			}
			jetty_controller_start_TextView.setText("�����������");
		}
	}
	//ѡ��������Ƶ֮�����ѡ������Ƶ��ִ��
	//��Ҫ������activity��launchmode���ó�singleTask(AndroidManifest.xml)
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == 1) {
			startservice(null);
		}
	}
	//�Ƿ�����������
	public void publicshare(View v) throws InterruptedException
	{
		if(! publicShareThread.isAlive()){
			mLocationClient.start();
			publicShareThread.start();
			jetty_controller_publicshare_TextView.setText("�رչ�������");
		}
		else
		{
			mLocationClient.stop();
			broad.setFlag(false);
			broad=new BroadCastWord();
			publicShareThread=new Thread(broad);
			jetty_controller_publicshare_TextView.setText("������������");
		} 

		//��λ����
		if (mLocationClient != null && mLocationClient.isStarted())
			mLocationClient.requestLocation();
		else 
			Log.d("LocSDK3", "locClient is null or not started");
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {    
			dialog();
		} 
		return super.onKeyDown(keyCode, event);
	}
	protected void dialog() {
		Builder builder = new Builder(IJetty.this);
		builder.setMessage("ȷ���˳���");
		builder.setTitle("��ʾ");
		builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				restore();
				IJetty.this.finish();
			}
		});
		builder.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog=builder.create();
		dialog.show();
	}
	//������������ʱ����ʾ�û��Ƿ�����������
	protected void dialogPublic() {
		Builder builder = new Builder(IJetty.this);
		builder.setMessage("�Ƿ�����������!");
		builder.setTitle("��ʾ");
		builder.setPositiveButton("��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				try {
					publicshare(null);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		builder.setNegativeButton("��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog=builder.create();
		dialog.show();
	}
	//���������Ƶ���е�����ǰ��·��
	public void restore()
	{
		SharedPreferences sp = IJetty.this.getSharedPreferences("sharepath", MODE_PRIVATE);
		Editor editor = sp.edit();
		String path=sp.getString("sharePath", "none");
		if(!"".equals(path))
		{
			File file=new File("/sdcard/jetty/webapps/jetty/gongxiang.flv");
			file.renameTo(new File(path));
			editor.putString("sharePath","");
		}
		editor.commit();

	}
	//��������֮���֪ͨ����Ϣ����
	public void notificationMessage()
	{
		//��Ϣ֪ͨ��
        //����NotificationManager
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);
        //����֪ͨ��չ�ֵ�������Ϣ
        int icon = R.drawable.icon;
        CharSequence tickerText = "���������";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
         
        //��������֪ͨ��ʱҪչ�ֵ�������Ϣ
        Context context = getApplicationContext();
        CharSequence contentTitle = "���������";
        CharSequence contentText = "���ip��ַ��"+hostip+"��������Ű�ť����ip�Ϳ��Թۿ�����";
        Intent notificationIntent = new Intent(this, IJetty.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);
         
        //��mNotificationManager��notify����֪ͨ�û����ɱ�������Ϣ֪ͨ
        mNotificationManager.notify(1, notification);
	}
	//�õ�ip��ַ
	public String getLocalIpAddress() {  
		//���wifiû�п�������ת�����ý���
		boolean isWiFiActive=isWiFiActive();
		if(!isWiFiActive)
		{
			new AlertDialog.Builder(this) 
			.setTitle("������ʾ") 
			.setCancelable(false)//���ò���ȡ��
			.setMessage("wifiδ����")
			.setPositiveButton("��������", new DialogInterface.OnClickListener() { 
				@Override 
				public void onClick(DialogInterface arg0, int arg1) { 
					if(android.os.Build.VERSION.SDK_INT > 10 ){
						//3.0���ϴ����ý��棬Ҳ����ֱ����ACTION_WIRELESS_SETTINGS�򿪵�wifi����
						startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
					} else {
						startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
					}	
				} 
			})
			.setNegativeButton("�˳�", new DialogInterface.OnClickListener() { 
				@Override 
				public void onClick(DialogInterface arg0, int arg1) { 
					android.os.Process.killProcess(android.os.Process.myPid()); 
					System.exit(0); 
				} 
			}).show(); 

		}
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);     
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();     
		int ipAddress = wifiInfo.getIpAddress();     
		String ip = intToIp(ipAddress);    
		return ip;
	}   
	//�ж�wifi�Ƿ���
	public boolean isWiFiActive() {      
		ConnectivityManager connectivity = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);      
		if (connectivity != null) {      
			NetworkInfo[] infos = connectivity.getAllNetworkInfo();      
			if (infos != null) {      
				for(NetworkInfo ni : infos){  
					if(ni.getTypeName().equals("WIFI") && ni.isConnected()){  
						return true;  
					}  
				}  
			}      
		}      
		return false;      
	}   
	public String intToIp(int i) {      
		return (i & 0xFF ) + "." +     
				((i >> 8 ) & 0xFF) + "." +     
				((i >> 16 ) & 0xFF) + "." +     
				( i >> 24 & 0xFF) ; 
	} 
	//�ٶȵ�ͼ��λ������
	public class MyLocationListener implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return ;

			//�õ���λ�ĵ�ַ
			mLat=location.getLatitude();
			mLon=location.getLongitude();
		}
		public void onReceivePoi(BDLocation poiLocation) {
			if (poiLocation == null){
				return ;
			}			 
		}
	}
}
