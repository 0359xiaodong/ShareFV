/*
 * Copyright (C) 2012 YIXIA.COM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ccsu.ShareFV;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.ccsu.ShareFV.IJetty;
import cn.ccsu.ShareFV.IJettyService;
import cn.ccsu.chatserver.ServerThread;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.MediaController.OnHiddenListener;
import io.vov.vitamio.widget.MediaController.OnShownListener;
import io.vov.vitamio.widget.VideoView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow.OnDismissListener;

public class PlayVideo extends Activity implements OnCompletionListener, OnInfoListener, Runnable {
	private String path = "";//播放路径
	//	private String path = "/sdcard/MIDH-vision-V3.mp4";
	//  private String path = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8 ";
	private VideoView mVideoView;//VideoView
	private MediaController mMediaController;//播放控制类
	private View mVolumeBrightnessLayout;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;
	private AudioManager mAudioManager;
	private int mMaxVolume;//最大声音
	private int mVolume = -1;//当前声音
	private float mBrightness = -1f;//当前亮度
	private int mLayout = VideoView.VIDEO_LAYOUT_ZOOM;//当前缩放模式
	private GestureDetector mGestureDetector;
	private View mLoadingView;
	//额外添加控件（电量显示，时间显示，缓冲速度显示）
	private RelativeLayout videoview_RelativeLayout_1;//顶部控制条视图
	private TextView TextView_time,TextView_battery,TextView_download;
	private ImageButton ImageButton_back;
	private boolean isExit;//是否退出
	private String download_string;
	private TextView file_Name;//显示文件名
	private long Position;//当前播放位置
	//聊天部分
	private ImageButton pop_btn;
	private PopupWindow pop;
	private EditText historyEdit;  
	private EditText messageEdit;  

	private Button sendButton;  

	/** 
	 * 声明字符串，name存储用户名 chat_txt存储发送信息 chat_in存储从服务器接收到的信息 
	 ****/  

	private String username, ip, chat_txt, chat_in;  

	// 声明套接字对象   
	Socket socket;  

	// 声明线程对象   
	Thread thread;  

	// 声明客户器端数据输入输出流   
	DataInputStream dataInputStream;  
	DataOutputStream dataOutputStream;  
	// 是否登录的标记   
	boolean flag = false;  

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		//检测Vitamio是否解压解码包
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this)){
			//Vitamio解码包未正常解压
			Toast.makeText(this, "解码包解压失败！", Toast.LENGTH_SHORT).show();  
			return;
		}

		Bundle extras = getIntent().getExtras();//接收button传过来的值
		ip=extras.getString("hostip");
		if(IJettyService.isRunning())
		{
			path="/mnt/sdcard/jetty/webapps/jetty/gongxiang.flv";
		}
		else
		{
			path="http://"+ip+":"+IJetty.__PORT_DEFAULT+"/jetty/gongxiang.flv";	
		}
		
		Log.d("path=",path);

		//绑定播放视图
		setContentView(R.layout.videoview);

		LayoutInflater inflater = LayoutInflater.from(this);  
		// 引入窗口配置文件  
		View view = inflater.inflate(R.layout.popupwindow, null);  
		// 创建PopupWindow对象  
		pop = new PopupWindow(view, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);  
		pop_btn = (ImageButton) findViewById(R.id.popupwindow);  
		// 需要设置一下此参数，点击外边可消失  
		pop.setBackgroundDrawable(new BitmapDrawable());  
		//设置点击窗口外边窗口消失  
		pop.setOutsideTouchable(true);  
		// 设置此参数获得焦点，否则无法点击  
		pop.setFocusable(true);  
		pop.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss() {
				pop_btn.setVisibility(View.VISIBLE);

			}
		});
		pop_btn.setOnClickListener(new OnClickListener() {  

			@Override  
			public void onClick(final View v) {  
				if(pop.isShowing()) {  
					// 隐藏窗口，如果设置了点击窗口外小时即不需要此方式隐藏  
					pop.dismiss();  
				} else {  
					// 显示窗口  
					if(flag==false){
						//还没有登录
						final EditText editText = new EditText(PlayVideo.this);//用于获取用户输入的用户名
						new AlertDialog.Builder(PlayVideo.this).setTitle("输入昵称加入聊天室").setIcon(
								android.R.drawable.ic_dialog_info).setView(
										editText).setPositiveButton("确定", new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog,
													int which) {
												// "进入聊天室"按钮的处理   
												if (flag == true) {  
													Toast.makeText(PlayVideo.this, "亲，你已经登陆过啦！！！",  
															Toast.LENGTH_LONG).show();  
													return;  
												}  
												// 获取用户名   
												username = editText.getText().toString();
												// 判断用户名是否有效及ip是否为空   
												if (username != "" && username != null && username != "用户名输入"  
														&& ip != null) { 
													SocketThread socketThread=new SocketThread();
													Thread sThread=new Thread(socketThread);
													sThread.start();
													thread = new Thread(PlayVideo.this);  
													// 开启线程，监听服务器段是否有消息   
													thread.start();  
													// 说明已经登录成功   
													flag = true;  
													pop.showAtLocation(v,Gravity.CENTER,0,0);
													pop_btn.setVisibility(View.GONE);
												}  

											}
										})
										.setNegativeButton("取消", null).show();

					}else{
						pop.showAtLocation(v,Gravity.CENTER,0,0);
						pop_btn.setVisibility(View.GONE);
					}


				}  

			}  
		});  

		// 实例化组件   
		historyEdit = (EditText) view.findViewById(R.id.history);  
		historyEdit.setKeyListener(null);

		messageEdit = (EditText) view.findViewById(R.id.message);  


		sendButton = (Button)view.findViewById(R.id.SendButton);  

		// 为发送按钮注册监听器   
		sendButton.setOnClickListener(listener);  

		//绑定控件
		mVideoView = (VideoView) findViewById(R.id.surface_view);
		mVolumeBrightnessLayout = findViewById(R.id.operation_volume_brightness);
		mOperationBg = (ImageView) findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) findViewById(R.id.operation_percent);
		mLoadingView = findViewById(R.id.video_loading);

		videoview_RelativeLayout_1 = (RelativeLayout) findViewById(R.id.videoview_RelativeLayout_1);
		//将顶部控制条设为不可见
		videoview_RelativeLayout_1.setVisibility(View.GONE);
		TextView_time = (TextView) findViewById(R.id.time);
		myThread timeThread = new myThread();
		new Thread(timeThread).start();//开启新线程，用于刷新时间
		TextView_battery = (TextView) findViewById(R.id.battery);
		batteryLevel();
		TextView_download = (TextView) findViewById(R.id.download);
		//返回按键(mediacontroller_back)及点击事件监听
		ImageButton_back = (ImageButton) findViewById(R.id.mediacontroller_back);
		ImageButton_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isExit=true;
				exit();
			}
		});
		//文件名显示控件
		file_Name = (TextView) findViewById(R.id.file_name);
		//file_Name.setText("测试影片.rmvb");

		//设置影片路径
		mVideoView.setVideoPath(path);
		//绑定监听事件
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnInfoListener(this);
		//设置影片质量为高
		mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		//初始化控制器
		mMediaController = new MediaController(this);
		

		mMediaController.setOnShownListener(new OnShownListener() {
			public void onShown() {
				videoview_RelativeLayout_1.setVisibility(View.VISIBLE);
				pop_btn.setVisibility(View.VISIBLE);

			}
		});

		mMediaController.setOnHiddenListener(new OnHiddenListener() {

			@Override
			public void onHidden() {
				videoview_RelativeLayout_1.setVisibility(View.GONE);
				pop_btn.setVisibility(View.GONE);

			}
		});
		//绑定控制器
		mVideoView.setMediaController(mMediaController);
		mVideoView.requestFocus();

		mGestureDetector = new GestureDetector(this, new MyGestureListener());
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


	}


	@Override
	protected void onResume() {
		Log.d("VideoViewPlayer", "onResume");
		super.onResume();
		if (mVideoView != null)
			mVideoView.resume();
		mVideoView.seekTo(Position);
	}

	@Override
	protected void onPause() {
		Log.d("VideoViewPlayer", "onPause");
		super.onPause();
		if (mVideoView != null)
			mVideoView.pause();
		Position = mVideoView.getCurrentPosition();
	}


	@Override
	protected void onDestroy() {
		Log.d("VideoViewPlayer", "onDestroy");
		super.onDestroy();
		if (mVideoView != null)
			mVideoView.stopPlayback();
	}

	@Override
	protected void onRestart()
	{
		Log.d("VideoViewPlayer", "onRestart");
		super.onRestart();
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			exit();
			return true;
		case KeyEvent.KEYCODE_MENU:
			mMediaController.show(MediaController.sDefaultTimeout);
			return true;
		case KeyEvent.KEYCODE_HOME:
			onPause();
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			onVolumeSlide(0.05f);
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			onVolumeSlide(-0.05f);
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event))
			return true;

		// 处理手势结束
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			endGesture();
			break;
		}
		return super.onTouchEvent(event);

	}

	/** 手势结束 */
	private void endGesture() {
		mVolume = -1;
		mBrightness = -1f;
		// 隐藏
		mHandler.sendEmptyMessageDelayed(3, 1000);
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		/** 双击 */
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mLayout == VideoView.VIDEO_LAYOUT_ZOOM)
				mLayout = VideoView.VIDEO_LAYOUT_ORIGIN;
			else
				mLayout++;
			if (mVideoView != null)
				mVideoView.setVideoLayout(mLayout, 0);
			return true;
		}

		/** 滑动 */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();
			Display disp = getWindowManager().getDefaultDisplay();
			int windowWidth = disp.getWidth();
			int windowHeight = disp.getHeight();

			if (mOldX > windowWidth * 4.0 / 5){
				// 右边滑动
				onVolumeSlide((mOldY - y) / windowHeight);
				Log.d("onVolumeSlide", Float.toString((mOldY - y) / windowHeight));
			}
			else if (mOldX < windowWidth / 5.0)// 左边滑动
				onBrightnessSlide((mOldY - y) / windowHeight);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}


	/**
	 * 滑动改变声音大小
	 * 
	 * @param percent
	 */
	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;

			// 显示
			mOperationBg.setImageResource(R.drawable.video_volumn_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		// 变更声音
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

		// 变更进度条
		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = findViewById(R.id.operation_full).getLayoutParams().width * index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	/**
	 * 滑动改变亮度
	 * 
	 * @param percent
	 */
	private void onBrightnessSlide(float percent) {
		if (mBrightness < 0) {
			mBrightness = getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;

			// 显示
			mOperationBg.setImageResource(R.drawable.video_brightness_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}
		WindowManager.LayoutParams lpa = getWindow().getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		if (lpa.screenBrightness > 1.0f)
			lpa.screenBrightness = 1.0f;
		else if (lpa.screenBrightness < 0.01f)
			lpa.screenBrightness = 0.01f;
		getWindow().setAttributes(lpa);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * lpa.screenBrightness);
		mOperationPercent.setLayoutParams(lp);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mVideoView != null)
			mVideoView.setVideoLayout(mLayout, 0);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		finish();
	}

	private void stopPlayer() {
		if (mVideoView != null)
			mVideoView.pause();
	}

	private void startPlayer() {
		if (mVideoView != null)
			mVideoView.start();
	}

	private boolean isPlaying() {
		return mVideoView != null && mVideoView.isPlaying();
	}

	/** 是否需要自动恢复播放，用于自动暂停，恢复播放 */
	private boolean needResume;

	@Override
	public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
		switch (arg1) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			//开始缓存，暂停播放
			if (isPlaying()) {
				stopPlayer();
				needResume = true;
			}
			mLoadingView.setVisibility(View.VISIBLE);
			mMediaController.setEnabled(false);
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			//缓存完成，继续播放
			if (needResume)
				startPlayer();
			mLoadingView.setVisibility(View.GONE);
			mMediaController.setEnabled(true);
			break;
		case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
			//显示 下载速度
			download_string =Integer.toString(arg2)+"KB/S";
			Message msg = mHandler.obtainMessage();  
			msg.what = 1;  
			msg.sendToTarget();  
			break;
		}
		return true;
	}

	//监听电量变化
	private void batteryLevel() {
		BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				context.unregisterReceiver(this);
				int rawlevel = intent.getIntExtra("level", -1);//获得当前电量
				int scale = intent.getIntExtra("scale", -1);
				//获得总电量
				int level = -1;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				//电量图标、字体颜色变换
				if(level>=50){
					TextView_battery.setBackgroundResource(R.drawable.battery_green);
					TextView_battery.setTextColor(Color.rgb(0, 255, 0));//绿色
				}
				if(level>20 && level<50){
					TextView_battery.setBackgroundResource(R.drawable.battery_yellow);
					TextView_battery.setTextColor(Color.rgb(255, 255, 0));//黄色
				}
				if(level<=20){
					TextView_battery.setBackgroundResource(R.drawable.battery_red);
					TextView_battery.setTextColor(Color.rgb(255, 0, 0));//红色
				}
				TextView_battery.setText(Integer.toString(level));
			}
		};
		IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		this.registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}

	//用于刷新时间的线程
	class myThread implements Runnable {   
		public void run() {  
			try {
				while(true){
					SimpleDateFormat sdf=new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
					String str=sdf.format(new Date());
					mHandler.sendMessage(mHandler.obtainMessage(2,str));
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}   
	}   




	public void exit(){
		//退出方法
		if (!isExit) {  
			isExit = true;  
			Toast.makeText(this, "再按一次退出播放", Toast.LENGTH_SHORT).show();  
			mHandler.sendEmptyMessageDelayed(0, 2000);  
		} else {  
			onDestroy();//释放资源，并退出播放 
			this.finish();

		}  
	}  

	//刷新状态
	Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			if(msg.what == 0){
				//刷新退出状态
				isExit = false;
			}
			if(msg.what == 1){  
				//刷新下载速度
				TextView_download.setText(download_string);
			}
			if(msg.what == 2){
				//刷新当前时间
				TextView_time.setText((String)msg.obj);
			}
			if(msg.what == 3){
				mVolumeBrightnessLayout.setVisibility(View.GONE);
			}
			super.handleMessage(msg);
		}

	};

	View.OnClickListener listener = new View.OnClickListener() {  

		@Override  
		public void onClick(View v) {  
			switch (v.getId()) {  
			// "发送"按钮的处理   
			case R.id.SendButton:  
				if (flag == false) {  
					Toast.makeText(PlayVideo.this, "亲，你还没登录，请先登录！",  
							Toast.LENGTH_LONG).show();  
					return;  
				}  
				// 获取客户端输入的发言内容   
				chat_txt = messageEdit.getText().toString();  
				System.out.println(chat_txt.length());
				if ((chat_txt != "")&&(chat_txt.length()>0)) {  
					// 得到当前时间   
					Date now = new Date(System.currentTimeMillis());  
					SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");  
					String nowStr = format.format(now);  

					// 发言，向服务器发送发言的信息   
					try {  
						dataOutputStream.writeUTF("用户:" + username  
								+ "\b\b\b" + nowStr + " 说:" + chat_txt);  
					} catch (IOException e2) {  
					}  
				}
				messageEdit.setText("");
				break;  
			}  
		}  
	};  

	// 客户端线程启动后的动作   
	@Override  
	public void run() {  
		// 循环执行，作用是一直监听服务器端是否有消息   
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while (true) {  
			try {  
				// 读取服务器发送来的数据信息   
				chat_in = dataInputStream.readUTF();  
				chat_in = chat_in + "\n";  
				// 发送一个消息，要求刷新界面   
				mHandler1.sendMessage(mHandler1.obtainMessage());  
			} catch (IOException e) {  
			}  
		}  
	}  

	Handler mHandler1 = new Handler() {  
		public void handleMessage(Message msg) {  
			// 将消息并显示在客户端的对话窗口中   
			historyEdit.append(chat_in);  

			//滚动到最新内容
			historyEdit.setMovementMethod(ScrollingMovementMethod.getInstance());
			historyEdit.setSelection(historyEdit.getText().length(), historyEdit.getText().length());

			// 刷新   
			super.handleMessage(msg);  

		}  
	};  
	public class SocketThread implements Runnable { //实现Runnable接口
		public void run() {                                  // 实现run方法
			try {  
				// 创建Socket对象   
				socket = new Socket(ip, ServerThread.PORT);  
				// 创建客户端数据输入／输出流，用于对服务器端发送或接收数据   
				dataInputStream = new DataInputStream(socket  
						.getInputStream());  
				dataOutputStream = new DataOutputStream(socket  
						.getOutputStream());  

				// 得到系统的时间   
				Date now = new Date(System.currentTimeMillis());  
				SimpleDateFormat format = new SimpleDateFormat(  
						"hh:mm:ss");  
				String nowStr = format.format(now);  

				// 输出某某上线啦   
				dataOutputStream.writeUTF("用户" + username  
						+ "\b" + nowStr + " 上线啦！");  
			} catch (IOException e1) {  
				System.out.println("抱歉连接不成功！！！");  
			}  
		}
	}  

}
