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
	//记录ip地址
	public static String hostip;
	//记录视频名
	public static String VideoName;
	//记录自己的地址
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
	//按钮和文字
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
				//将assets目录下的jetty复制到webapps目录中
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
		//如果程序没有正常退出那么视频可能没有还原到共享前的目录所以开始共享前先将上次共享的视频还原
		restore();

		//定位相关
		mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
		mLocationClient.registerLocationListener( myListener );    //注册监听函数
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setCoorType("bd09ll");//返回的定位结果是百度经纬度,默认值gcj02
		mLocationClient.setLocOption(option);

		//得到ip地址
		hostip=getLocalIpAddress();

		//广播自己的地址使局域网其他设备能够发现
		broad=new BroadCastWord();
		publicShareThread=new Thread(broad);

		setContentView(R.layout.jetty_controller);


		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		//开启分享服务按钮
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

		//开启公开分享按钮
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

		//播放按钮
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

		//选择共享视频按钮
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

		//大家都在看什么按钮
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

		//分享到社交网络按钮
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
					//分享给所有人可用
					jetty_controller_LinearLayout_publics.setEnabled(true);
					publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_01);
					jetty_controller_publicshare_TextView.setTextColor(Color.rgb(255,255,255));
					//选择视频不可用
					jetty_controller_LinearLayout_explorer.setEnabled(false);	
					explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_03);
					jetty_controller_explorer_TextView.setTextColor(Color.rgb(204,204,204));

					if (AndroidInfo.isOnEmulator(IJetty.this))
					{}
				} else if (__STOP_ACTION.equalsIgnoreCase(intent.getAction())) {
					//选择视频可用
					jetty_controller_LinearLayout_explorer.setEnabled(true);	
					explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_01);
					jetty_controller_explorer_TextView.setTextColor(Color.rgb(255,255,255));
					//分享给所有人不可用
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
			//分享给所有人可用
			jetty_controller_LinearLayout_publics.setEnabled(true);
			publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_01);
			jetty_controller_publicshare_TextView.setTextColor(Color.rgb(255,255,255));
			//选择视频不可用
			jetty_controller_LinearLayout_explorer.setEnabled(false);	
			explorerButton.setBackgroundResource(R.drawable.jetty_controller_explorer_btn_03);
			jetty_controller_explorer_TextView.setTextColor(Color.rgb(204,204,204));
		} else {
			//分享给所有人不可用
			jetty_controller_LinearLayout_publics.setEnabled(false);
			publics.setBackgroundResource(R.drawable.jetty_controller_publics_btn_03);
			jetty_controller_publicshare_TextView.setTextColor(Color.rgb(204,204,204));
			//选择视频可用
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

	//将assets文件中的文件拷贝到sd卡中
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
		intent.putExtra(Intent.EXTRA_SUBJECT, "好友推荐");  
		intent.putExtra(Intent.EXTRA_TEXT, "嗨，我正在使用ShareFV用手机在局域网分享了一个视频！下载该软件在播放界面输入我的ip地址("+hostip+")就可以和我一起观看视频而且还可以一起聊天.");  
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		startActivity(Intent.createChooser(intent, getTitle()));  
	}
	public  void player(View v)
	{
		//如果共享的服务开启就播放自己共享的视频
		if(IJettyService.isRunning()){
			Intent intent = new Intent(IJetty.this, PlayVideo.class);
			intent.putExtra("hostip", hostip);
			startActivity(intent);
		}
		//如果没有开启服务就播放别人共享的视频
		else
		{
			final EditText inputServer = new EditText(this);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("请输入ip地址").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
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
	//跳转到地图扫描界面
	public void search(View v)
	{
		Intent intent = new Intent(IJetty.this, MapActivity.class);
		startActivity(intent);
	}
	//开启和关闭服务
	public void startservice(View v)
	{
		if(!IJettyService.isRunning())
		{
			SharedPreferences sp = IJetty.this.getSharedPreferences("sharepath", MODE_PRIVATE);
			String path=sp.getString("sharePath", "none");
			if("".equals(path))
			{
				Toast.makeText(IJetty.this, "还未共享视频，请先选择分享的视频！", 5000).show();
				//没有共享视频就跳转到选择视频界面
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
				//开启聊天的线程
				try {
					serverThread= new ServerThread();  
					serverThread.start();
				} catch (Exception e) {
					// TODO: handle exception
					Toast.makeText(IJetty.this, "聊天服务开启失败请重新开启服务", 9000).show();
				}
				//通知栏提示信息
				notificationMessage();
				//提示是否开启公开分享
				dialogPublic();
				jetty_controller_start_TextView.setText("关闭分享服务");
			}
			
		}
		else
		{
			stopService(new Intent(IJetty.this, IJettyService.class));	
			//停止服务之后把通知栏的消息清除
			mNotificationManager.cancel(1);
			if(serverThread!=null&&serverThread.isAlive())
			{
				serverThread.finalize();
			}
			jetty_controller_start_TextView.setText("开启分享服务");
		}
	}
	//选择分享的视频之后如果选择了视频就执行
	//把要启动的activity的launchmode设置成singleTask(AndroidManifest.xml)
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == 1) {
			startservice(null);
		}
	}
	//是否开启公开分享
	public void publicshare(View v) throws InterruptedException
	{
		if(! publicShareThread.isAlive()){
			mLocationClient.start();
			publicShareThread.start();
			jetty_controller_publicshare_TextView.setText("关闭公开分享");
		}
		else
		{
			mLocationClient.stop();
			broad.setFlag(false);
			broad=new BroadCastWord();
			publicShareThread=new Thread(broad);
			jetty_controller_publicshare_TextView.setText("开启公开分享");
		} 

		//定位请求
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
		builder.setMessage("确认退出吗？");
		builder.setTitle("提示");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				restore();
				IJetty.this.finish();
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog=builder.create();
		dialog.show();
	}
	//开启分享服务的时候提示用户是否开启公开分享
	protected void dialogPublic() {
		Builder builder = new Builder(IJetty.this);
		builder.setMessage("是否开启公开分享!");
		builder.setTitle("提示");
		builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
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
		builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog=builder.create();
		dialog.show();
	}
	//将共享的视频剪切到共享前的路径
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
	//开启服务之后的通知栏消息发送
	public void notificationMessage()
	{
		//消息通知栏
        //定义NotificationManager
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);
        //定义通知栏展现的内容信息
        int icon = R.drawable.icon;
        CharSequence tickerText = "分享给好友";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
         
        //定义下拉通知栏时要展现的内容信息
        Context context = getApplicationContext();
        CharSequence contentTitle = "分享给好友";
        CharSequence contentText = "你的ip地址（"+hostip+"）点击播放按钮输入ip就可以观看啦！";
        Intent notificationIntent = new Intent(this, IJetty.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);
         
        //用mNotificationManager的notify方法通知用户生成标题栏消息通知
        mNotificationManager.notify(1, notification);
	}
	//得到ip地址
	public String getLocalIpAddress() {  
		//如果wifi没有开启就跳转到设置界面
		boolean isWiFiActive=isWiFiActive();
		if(!isWiFiActive)
		{
			new AlertDialog.Builder(this) 
			.setTitle("错误提示") 
			.setCancelable(false)//设置不能取消
			.setMessage("wifi未开启")
			.setPositiveButton("设置网络", new DialogInterface.OnClickListener() { 
				@Override 
				public void onClick(DialogInterface arg0, int arg1) { 
					if(android.os.Build.VERSION.SDK_INT > 10 ){
						//3.0以上打开设置界面，也可以直接用ACTION_WIRELESS_SETTINGS打开到wifi界面
						startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
					} else {
						startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
					}	
				} 
			})
			.setNegativeButton("退出", new DialogInterface.OnClickListener() { 
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
	//判断wifi是否开启
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
	//百度地图定位监听类
	public class MyLocationListener implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return ;

			//得到定位的地址
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
