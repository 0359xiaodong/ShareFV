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
	private String path = "";//����·��
	//	private String path = "/sdcard/MIDH-vision-V3.mp4";
	//  private String path = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8 ";
	private VideoView mVideoView;//VideoView
	private MediaController mMediaController;//���ſ�����
	private View mVolumeBrightnessLayout;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;
	private AudioManager mAudioManager;
	private int mMaxVolume;//�������
	private int mVolume = -1;//��ǰ����
	private float mBrightness = -1f;//��ǰ����
	private int mLayout = VideoView.VIDEO_LAYOUT_ZOOM;//��ǰ����ģʽ
	private GestureDetector mGestureDetector;
	private View mLoadingView;
	//������ӿؼ���������ʾ��ʱ����ʾ�������ٶ���ʾ��
	private RelativeLayout videoview_RelativeLayout_1;//������������ͼ
	private TextView TextView_time,TextView_battery,TextView_download;
	private ImageButton ImageButton_back;
	private boolean isExit;//�Ƿ��˳�
	private String download_string;
	private TextView file_Name;//��ʾ�ļ���
	private long Position;//��ǰ����λ��
	//���첿��
	private ImageButton pop_btn;
	private PopupWindow pop;
	private EditText historyEdit;  
	private EditText messageEdit;  

	private Button sendButton;  

	/** 
	 * �����ַ�����name�洢�û��� chat_txt�洢������Ϣ chat_in�洢�ӷ��������յ�����Ϣ 
	 ****/  

	private String username, ip, chat_txt, chat_in;  

	// �����׽��ֶ���   
	Socket socket;  

	// �����̶߳���   
	Thread thread;  

	// �����ͻ������������������   
	DataInputStream dataInputStream;  
	DataOutputStream dataOutputStream;  
	// �Ƿ��¼�ı��   
	boolean flag = false;  

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		//���Vitamio�Ƿ��ѹ�����
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this)){
			//Vitamio�����δ������ѹ
			Toast.makeText(this, "�������ѹʧ�ܣ�", Toast.LENGTH_SHORT).show();  
			return;
		}

		Bundle extras = getIntent().getExtras();//����button��������ֵ
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

		//�󶨲�����ͼ
		setContentView(R.layout.videoview);

		LayoutInflater inflater = LayoutInflater.from(this);  
		// ���봰�������ļ�  
		View view = inflater.inflate(R.layout.popupwindow, null);  
		// ����PopupWindow����  
		pop = new PopupWindow(view, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);  
		pop_btn = (ImageButton) findViewById(R.id.popupwindow);  
		// ��Ҫ����һ�´˲����������߿���ʧ  
		pop.setBackgroundDrawable(new BitmapDrawable());  
		//���õ��������ߴ�����ʧ  
		pop.setOutsideTouchable(true);  
		// ���ô˲�����ý��㣬�����޷����  
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
					// ���ش��ڣ���������˵��������Сʱ������Ҫ�˷�ʽ����  
					pop.dismiss();  
				} else {  
					// ��ʾ����  
					if(flag==false){
						//��û�е�¼
						final EditText editText = new EditText(PlayVideo.this);//���ڻ�ȡ�û�������û���
						new AlertDialog.Builder(PlayVideo.this).setTitle("�����ǳƼ���������").setIcon(
								android.R.drawable.ic_dialog_info).setView(
										editText).setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog,
													int which) {
												// "����������"��ť�Ĵ���   
												if (flag == true) {  
													Toast.makeText(PlayVideo.this, "�ף����Ѿ���½����������",  
															Toast.LENGTH_LONG).show();  
													return;  
												}  
												// ��ȡ�û���   
												username = editText.getText().toString();
												// �ж��û����Ƿ���Ч��ip�Ƿ�Ϊ��   
												if (username != "" && username != null && username != "�û�������"  
														&& ip != null) { 
													SocketThread socketThread=new SocketThread();
													Thread sThread=new Thread(socketThread);
													sThread.start();
													thread = new Thread(PlayVideo.this);  
													// �����̣߳��������������Ƿ�����Ϣ   
													thread.start();  
													// ˵���Ѿ���¼�ɹ�   
													flag = true;  
													pop.showAtLocation(v,Gravity.CENTER,0,0);
													pop_btn.setVisibility(View.GONE);
												}  

											}
										})
										.setNegativeButton("ȡ��", null).show();

					}else{
						pop.showAtLocation(v,Gravity.CENTER,0,0);
						pop_btn.setVisibility(View.GONE);
					}


				}  

			}  
		});  

		// ʵ�������   
		historyEdit = (EditText) view.findViewById(R.id.history);  
		historyEdit.setKeyListener(null);

		messageEdit = (EditText) view.findViewById(R.id.message);  


		sendButton = (Button)view.findViewById(R.id.SendButton);  

		// Ϊ���Ͱ�ťע�������   
		sendButton.setOnClickListener(listener);  

		//�󶨿ؼ�
		mVideoView = (VideoView) findViewById(R.id.surface_view);
		mVolumeBrightnessLayout = findViewById(R.id.operation_volume_brightness);
		mOperationBg = (ImageView) findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) findViewById(R.id.operation_percent);
		mLoadingView = findViewById(R.id.video_loading);

		videoview_RelativeLayout_1 = (RelativeLayout) findViewById(R.id.videoview_RelativeLayout_1);
		//��������������Ϊ���ɼ�
		videoview_RelativeLayout_1.setVisibility(View.GONE);
		TextView_time = (TextView) findViewById(R.id.time);
		myThread timeThread = new myThread();
		new Thread(timeThread).start();//�������̣߳�����ˢ��ʱ��
		TextView_battery = (TextView) findViewById(R.id.battery);
		batteryLevel();
		TextView_download = (TextView) findViewById(R.id.download);
		//���ذ���(mediacontroller_back)������¼�����
		ImageButton_back = (ImageButton) findViewById(R.id.mediacontroller_back);
		ImageButton_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isExit=true;
				exit();
			}
		});
		//�ļ�����ʾ�ؼ�
		file_Name = (TextView) findViewById(R.id.file_name);
		//file_Name.setText("����ӰƬ.rmvb");

		//����ӰƬ·��
		mVideoView.setVideoPath(path);
		//�󶨼����¼�
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnInfoListener(this);
		//����ӰƬ����Ϊ��
		mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		//��ʼ��������
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
		//�󶨿�����
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

		// �������ƽ���
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			endGesture();
			break;
		}
		return super.onTouchEvent(event);

	}

	/** ���ƽ��� */
	private void endGesture() {
		mVolume = -1;
		mBrightness = -1f;
		// ����
		mHandler.sendEmptyMessageDelayed(3, 1000);
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		/** ˫�� */
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

		/** ���� */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();
			Display disp = getWindowManager().getDefaultDisplay();
			int windowWidth = disp.getWidth();
			int windowHeight = disp.getHeight();

			if (mOldX > windowWidth * 4.0 / 5){
				// �ұ߻���
				onVolumeSlide((mOldY - y) / windowHeight);
				Log.d("onVolumeSlide", Float.toString((mOldY - y) / windowHeight));
			}
			else if (mOldX < windowWidth / 5.0)// ��߻���
				onBrightnessSlide((mOldY - y) / windowHeight);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}


	/**
	 * �����ı�������С
	 * 
	 * @param percent
	 */
	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;

			// ��ʾ
			mOperationBg.setImageResource(R.drawable.video_volumn_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		// �������
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

		// ���������
		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = findViewById(R.id.operation_full).getLayoutParams().width * index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	/**
	 * �����ı�����
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

			// ��ʾ
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

	/** �Ƿ���Ҫ�Զ��ָ����ţ������Զ���ͣ���ָ����� */
	private boolean needResume;

	@Override
	public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
		switch (arg1) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			//��ʼ���棬��ͣ����
			if (isPlaying()) {
				stopPlayer();
				needResume = true;
			}
			mLoadingView.setVisibility(View.VISIBLE);
			mMediaController.setEnabled(false);
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			//������ɣ���������
			if (needResume)
				startPlayer();
			mLoadingView.setVisibility(View.GONE);
			mMediaController.setEnabled(true);
			break;
		case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
			//��ʾ �����ٶ�
			download_string =Integer.toString(arg2)+"KB/S";
			Message msg = mHandler.obtainMessage();  
			msg.what = 1;  
			msg.sendToTarget();  
			break;
		}
		return true;
	}

	//���������仯
	private void batteryLevel() {
		BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				context.unregisterReceiver(this);
				int rawlevel = intent.getIntExtra("level", -1);//��õ�ǰ����
				int scale = intent.getIntExtra("scale", -1);
				//����ܵ���
				int level = -1;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				//����ͼ�ꡢ������ɫ�任
				if(level>=50){
					TextView_battery.setBackgroundResource(R.drawable.battery_green);
					TextView_battery.setTextColor(Color.rgb(0, 255, 0));//��ɫ
				}
				if(level>20 && level<50){
					TextView_battery.setBackgroundResource(R.drawable.battery_yellow);
					TextView_battery.setTextColor(Color.rgb(255, 255, 0));//��ɫ
				}
				if(level<=20){
					TextView_battery.setBackgroundResource(R.drawable.battery_red);
					TextView_battery.setTextColor(Color.rgb(255, 0, 0));//��ɫ
				}
				TextView_battery.setText(Integer.toString(level));
			}
		};
		IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		this.registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}

	//����ˢ��ʱ����߳�
	class myThread implements Runnable {   
		public void run() {  
			try {
				while(true){
					SimpleDateFormat sdf=new SimpleDateFormat("yyyy��MM��dd��   HH:mm:ss");
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
		//�˳�����
		if (!isExit) {  
			isExit = true;  
			Toast.makeText(this, "�ٰ�һ���˳�����", Toast.LENGTH_SHORT).show();  
			mHandler.sendEmptyMessageDelayed(0, 2000);  
		} else {  
			onDestroy();//�ͷ���Դ�����˳����� 
			this.finish();

		}  
	}  

	//ˢ��״̬
	Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			if(msg.what == 0){
				//ˢ���˳�״̬
				isExit = false;
			}
			if(msg.what == 1){  
				//ˢ�������ٶ�
				TextView_download.setText(download_string);
			}
			if(msg.what == 2){
				//ˢ�µ�ǰʱ��
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
			// "����"��ť�Ĵ���   
			case R.id.SendButton:  
				if (flag == false) {  
					Toast.makeText(PlayVideo.this, "�ף��㻹û��¼�����ȵ�¼��",  
							Toast.LENGTH_LONG).show();  
					return;  
				}  
				// ��ȡ�ͻ�������ķ�������   
				chat_txt = messageEdit.getText().toString();  
				System.out.println(chat_txt.length());
				if ((chat_txt != "")&&(chat_txt.length()>0)) {  
					// �õ���ǰʱ��   
					Date now = new Date(System.currentTimeMillis());  
					SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");  
					String nowStr = format.format(now);  

					// ���ԣ�����������ͷ��Ե���Ϣ   
					try {  
						dataOutputStream.writeUTF("�û�:" + username  
								+ "\b\b\b" + nowStr + " ˵:" + chat_txt);  
					} catch (IOException e2) {  
					}  
				}
				messageEdit.setText("");
				break;  
			}  
		}  
	};  

	// �ͻ����߳�������Ķ���   
	@Override  
	public void run() {  
		// ѭ��ִ�У�������һֱ�������������Ƿ�����Ϣ   
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while (true) {  
			try {  
				// ��ȡ��������������������Ϣ   
				chat_in = dataInputStream.readUTF();  
				chat_in = chat_in + "\n";  
				// ����һ����Ϣ��Ҫ��ˢ�½���   
				mHandler1.sendMessage(mHandler1.obtainMessage());  
			} catch (IOException e) {  
			}  
		}  
	}  

	Handler mHandler1 = new Handler() {  
		public void handleMessage(Message msg) {  
			// ����Ϣ����ʾ�ڿͻ��˵ĶԻ�������   
			historyEdit.append(chat_in);  

			//��������������
			historyEdit.setMovementMethod(ScrollingMovementMethod.getInstance());
			historyEdit.setSelection(historyEdit.getText().length(), historyEdit.getText().length());

			// ˢ��   
			super.handleMessage(msg);  

		}  
	};  
	public class SocketThread implements Runnable { //ʵ��Runnable�ӿ�
		public void run() {                                  // ʵ��run����
			try {  
				// ����Socket����   
				socket = new Socket(ip, ServerThread.PORT);  
				// �����ͻ����������룯����������ڶԷ������˷��ͻ��������   
				dataInputStream = new DataInputStream(socket  
						.getInputStream());  
				dataOutputStream = new DataOutputStream(socket  
						.getOutputStream());  

				// �õ�ϵͳ��ʱ��   
				Date now = new Date(System.currentTimeMillis());  
				SimpleDateFormat format = new SimpleDateFormat(  
						"hh:mm:ss");  
				String nowStr = format.format(now);  

				// ���ĳĳ������   
				dataOutputStream.writeUTF("�û�" + username  
						+ "\b" + nowStr + " ��������");  
			} catch (IOException e1) {  
				System.out.println("��Ǹ���Ӳ��ɹ�������");  
			}  
		}
	}  

}
