package cn.ccsu.ShareFV;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.BaseAdapter;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.app.ListActivity;
import android.view.WindowManager;
import android.view.Display;
import android.view.WindowManager.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.ccsu.ShareFV.R;

public class ExDialog extends ListActivity {
	private List<Map<String, Object>> mData;
	private String mDir = "/sdcard";
	private String sharePath="";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();
		Bundle bl = intent.getExtras();
		String title = bl.getString("explorer_title");
		Uri uri = intent.getData();
		mDir = uri.getPath();

		setTitle(title);
		mData = getData();
		MyAdapter adapter = new MyAdapter(this);
		setListAdapter(adapter);

		WindowManager m = getWindowManager();
		Display d = m.getDefaultDisplay();
		LayoutParams p = getWindow().getAttributes();
		p.height = (int) (d.getHeight() * 0.8);
		p.width = (int) (d.getWidth() * 0.95);
		getWindow().setAttributes(p);
	}

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		File f = new File(mDir);
		File[] files = f.listFiles();

		if (!mDir.equals("/sdcard")) {
			map = new HashMap<String, Object>();
			map.put("title", "Back to ../");
			map.put("info", f.getParent());
			map.put("img", R.drawable.ex_folder);
			list.add(map);
		}
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				
					map = new HashMap<String, Object>();
					map.put("title", files[i].getName());
					map.put("info", files[i].getPath());
					if (files[i].isDirectory())
					{
						map.put("img", R.drawable.ex_folder);
						list.add(map);
					}
					else
					{
						if(files[i].getName().endsWith(".rmvb")
								||files[i].getName().endsWith(".mp4")
								||files[i].getName().endsWith(".flv")
								||files[i].getName().endsWith(".avi")
								||files[i].getName().endsWith(".mkv")
								||files[i].getName().endsWith(".mpeg")
								||files[i].getName().endsWith(".3gp")
								||files[i].getName().endsWith(".wmv")
								||files[i].getName().endsWith(".mov"))
						{
							map.put("img", R.drawable.ex_doc);
							list.add(map);
						}
					}
					
				}
			
		}
		return list;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d("MyListView4-click", (String) mData.get(position).get("info"));
		if ((Integer) mData.get(position).get("img") == R.drawable.ex_folder) {
			mDir = (String) mData.get(position).get("info");
			mData = getData();
			MyAdapter adapter = new MyAdapter(this);
			setListAdapter(adapter);
		} else {
			sharePath=(String) mData.get(position).get("info");
			dialog();
			
		}
	}

	public final class ViewHolder {
		public ImageView img;
		public TextView title;
		public TextView info;
	}

	public class MyAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public MyAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return mData.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.listview, null);
				holder.img = (ImageView) convertView.findViewById(R.id.img);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.info = (TextView) convertView.findViewById(R.id.info);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.img.setBackgroundResource((Integer) mData.get(position).get(
					"img"));
			holder.title.setText((String) mData.get(position).get("title"));
			holder.info.setText((String) mData.get(position).get("info"));
			return convertView;
		}
	}

	private void finishWithResult() {
		Intent intent = new Intent(this,IJetty.class);
		setResult(1, intent);
		finish();
	}
	
	protected void dialog() {
	       Builder builder = new Builder(ExDialog.this);
	       builder.setMessage("ȷ�Ϲ������Ƶ!");
	       builder.setTitle("��ʾ");
	       builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	         dialog.dismiss();
	         try {
			        File file=new File(sharePath);
			        file.renameTo(new File("/sdcard/jetty/webapps/jetty/gongxiang.flv"));
			        //��������Ƶԭʼ·��������sharepath.xml�ļ���
			        SharedPreferences sp = ExDialog.this.getSharedPreferences("sharepath", MODE_PRIVATE);
			        Editor editor = sp.edit();
			        editor.putString("sharePath",sharePath);
			        IJetty.VideoName=sharePath.split("/")[sharePath.split("/").length-1];
			        editor.commit();
			        finishWithResult();
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("�ƶ�ʧ��");
					Toast.makeText(ExDialog.this, "�ƶ��ļ�ʧ��", 5000);
				}
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
};

