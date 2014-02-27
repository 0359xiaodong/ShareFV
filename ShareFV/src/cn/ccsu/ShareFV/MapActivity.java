package cn.ccsu.ShareFV;


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;



import cn.ccsu.ShareFV.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import cn.ccsu.netdiscovery.Receive;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class MapActivity extends Activity implements Runnable{
	
	BMapManager mBMapMan = null;  
	MapView mMapView = null; 
	Receive receive;
	Thread receiveThread;
	Vector ipVector;	
	String[] ip;
	String[] name;
	int ipIndex=0;
	GeoPoint[] point;
	List<OverlayItem> overlayItemList=new ArrayList<OverlayItem>();
	private ProgressDialog progressDialog = null;
	private MapController mMapController;
	private OverlayItem mCurItem = null;
	private PopupOverlay   pop  = null;
	private TextView  popupText = null;
	private View viewCache = null;
	private View popupInfo = null;
	private View popupLeft = null;
	private View popupRight = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);  
		mBMapMan=new BMapManager(getApplication());  
		mBMapMan.init("69EEEACEB9CCF6300612CD97EA35A18D3E1542D7", null);    
		//注意：请在试用setContentView前初始化BMapManager对象，否则会报错  
		setContentView(R.layout.map);  
		mMapView=(MapView)findViewById(R.id.bmapsView);  
		mMapView.setBuiltInZoomControls(true);  
		mMapController = mMapView.getController();  
		// 得到mMapView的控制权,可以用它控制和驱动平移和缩放  
		mMapController.setZoom(18);//设置地图zoom级别
		
		progressDialog = ProgressDialog.show(MapActivity.this, "请稍等...", "获取数据中...", true);
		
		receive=new Receive();
		receiveThread=new Thread(receive);
		receiveThread.start(); 
		
		new Thread(this).start();

	}
	@Override  
	protected void onDestroy(){  
	        mMapView.destroy();  
	        if(mBMapMan!=null){  
	                mBMapMan.destroy();  
	                mBMapMan=null;  
	        }
	        
	        super.onDestroy();  
	}  
	@Override  
	protected void onPause(){  
	        mMapView.onPause();  
	        if(mBMapMan!=null){  
	               mBMapMan.stop();  
	        }  
	        super.onPause();  
	}  
	@Override  
	protected void onResume(){  
	        mMapView.onResume();  
	        if(mBMapMan!=null){  
	                mBMapMan.start();  
	        }  
	       super.onResume();  
	}  
	//解析Recive中ipVector的数据
	public void initdata()
	{		
		ipVector=receive.ipVector;
		Drawable mark= getResources().getDrawable(R.drawable.points);
		OverlayLocation itemOverlay = new OverlayLocation(mark, mMapView);  
		mMapView.getOverlays().clear();  
		mMapView.getOverlays().add(itemOverlay); 
		
		point=new GeoPoint[ipVector.size()];
		ip=new String[ipVector.size()];
		name=new String[ipVector.size()];
		if(ipVector.size()>0)
		{
			GeoPoint p =new GeoPoint((int)(Double.parseDouble(ipVector.get(0).toString().split("and")[1])* 1E6),(int)(Double.parseDouble(ipVector.get(0).toString().split("and")[2]) * 1E6));  
			//用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)  
			mMapController.setCenter(p);//设置地图中心点  
			  
			for(int i=0;i<ipVector.size();i++)
			{
				point[i]=new GeoPoint((int) (Double.parseDouble(ipVector.get(i).toString().split("and")[1])* 1E6), (int) (Double.parseDouble(ipVector.get(i).toString().split("and")[2]) * 1E6)); 	
				OverlayItem item1 = new OverlayItem(point[i],ipVector.get(i).toString().split("and")[3],"");
				ip[i]=ipVector.get(i).toString().split("and")[0];
				name[i]=ipVector.get(i).toString().split("and")[3];
				overlayItemList.add(item1);
			}
			itemOverlay.addItem(overlayItemList);  
			mMapView.refresh();  
		}
		else
		{
//			Toast.makeText(this, "没有扫描到数据",Toast.LENGTH_LONG).show();
		}

		
        viewCache = getLayoutInflater().inflate(R.layout.custom_text_view, null);
        popupInfo = (View) viewCache.findViewById(R.id.popinfo);
        popupText =(TextView) viewCache.findViewById(R.id.textcache);
		 /**
         * 创建一个popupoverlay
         */
        PopupClickListener popListener = new PopupClickListener(){
			@Override
			public void onClickedPopup(int index) {
				if ( index == 0){
			        Intent intent = new Intent(MapActivity.this, PlayVideo.class);
		   			intent.putExtra("hostip", ip[ipIndex]);
		   			startActivity(intent);
				}
			}
        };
        pop = new PopupOverlay(mMapView,popListener);
	}
	class OverlayLocation extends ItemizedOverlay<OverlayItem> {  
	    //用MapView构造ItemizedOverlay  
	    public OverlayLocation(Drawable mark,MapView mapView){  
	            super(mark,mapView);  
	    }  
	    protected boolean onTap(int index) {  
	        //在此处理item点击事件  
	    	OverlayItem item = getItem(index);
			mCurItem = item ;
			ipIndex=index;
			popupText.setText(getItem(index).getTitle());
			   Bitmap[] bitMaps={		
				   getBitmapFromView(popupInfo), 				
			    };
			    pop.showPopup(bitMaps,item.getPoint(),32);
			    
	        return true;  
	    }  
        public boolean onTap(GeoPoint pt, MapView mapView){  
                //在此处理MapView的点击事件，当返回 true时  
        	
        	if (pop != null){
                pop.hidePop();
			}
                super.onTap(pt,mapView);  
                return false;  
        }  
	}
	public  Bitmap getBitmapFromView(View view) {
        view.destroyDrawingCache();
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = view.getDrawingCache(true);
        return bitmap;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
	
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			initdata();

		progressDialog.dismiss();
	}
	
}
