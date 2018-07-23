package com.android.server;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.ISignBoardService;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.OrientationListener;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
//import androidx.viewpager.widget.ViewPager;
//import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

public class SignBoardService extends ISignBoardService.Stub {
	private static final String TAG = "SignBoardService";
	private Handler mainThreadHandler;
	private SignBoardWorkerThread signBoardWorker;
	private SignBoardWorkerHandler signBoardHandler;
	private Context context;
	private WindowManager windowManager;
	private LockedLinearLayout linearLayout;
	private ViewPager viewPager;
	private SignBoardPagerAdapter signBoardPagerAdapter;
	private OrientationListener orientationListener;

	public SignBoardService(Context context) {
		super();
		this.context = context;
		mainThreadHandler = new Handler(Looper.getMainLooper());
		orientationListener = new OrientationListener(context);
		orientationListener.enable();
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		signBoardPagerAdapter = new SignBoardPagerAdapter();
		viewPager = new ViewPager(context);
		viewPager.setAdapter(signBoardPagerAdapter);
		WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
		windowManagerParams.type = WindowManager.LayoutParams.TYPE_SIGNBOARD_NORMAL;
		windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		windowManagerParams.privateFlags = WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
		windowManagerParams.setTitle("SignBoard");
		linearLayout = new LockedLinearLayout(context);
		linearLayout.setPadding(400, 0, 0, 0);
		linearLayout.setBackgroundColor(Color.BLACK);
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setGravity(Gravity.RIGHT);
		linearLayout.addView(viewPager);
		windowManager.addView(linearLayout, windowManagerParams);
		signBoardWorker = new SignBoardWorkerThread("SignBoardServiceWorker");
		signBoardWorker.start();
	}

	public void addTestView1() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.ADD_TEST_VIEW1;
		signBoardHandler.sendMessage(msg);
	}

	public void addTestView2() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.ADD_TEST_VIEW2;
		signBoardHandler.sendMessage(msg);
	}

	public void addTestView3() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.ADD_TEST_VIEW3;
		signBoardHandler.sendMessage(msg);
	}

	public void addTestView4() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.ADD_TEST_VIEW4;
		signBoardHandler.sendMessage(msg);
	}

	public void removeAllViews() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.REMOVE_ALL_VIEWS;
		signBoardHandler.sendMessage(msg);
	}

	private class SignBoardWorkerThread extends Thread {
		public SignBoardWorkerThread(String name) {
			super(name);
		}
		public void run() {
			Looper.prepare();
			signBoardHandler = new SignBoardWorkerHandler();
			Looper.loop();
		}
	}

	private class LockedLinearLayout extends LinearLayout {
		public LockedLinearLayout(Context context) {
			super(context);
		}

		@Override
		public void dispatchConfigurationChanged(Configuration config) {
			config.orientation = Configuration.ORIENTATION_PORTRAIT;
			super.dispatchConfigurationChanged(config);
		}
	}

	private class SignBoardWorkerHandler extends Handler {
		private static final int REMOVE_ALL_VIEWS = 0;
		private static final int ADD_TEST_VIEW1 = 1;
		private static final int ADD_TEST_VIEW2 = 2;
		private static final int ADD_TEST_VIEW3 = 3;
		private static final int ADD_TEST_VIEW4 = 4;

		@Override
		public void handleMessage(Message msg) {
			try {
				switch(msg.what) {
					case REMOVE_ALL_VIEWS:
						Log.i(TAG, "Removing All Views");
						mainThreadHandler.post(new Runnable() {
							@Override
							public void run() {
								signBoardPagerAdapter.removeAllViews();
							}
						});
						break;
					case ADD_TEST_VIEW1:
						Log.i(TAG, "Adding View1");
						mainThreadHandler.post(new Runnable() {
							@Override
							public void run() {
								TextClock view1 = new TextClock(context);
								view1.setTextColor(Color.WHITE);
								view1.setBackgroundColor(Color.parseColor("#222222"));
								view1.setGravity(Gravity.CENTER);
								view1.setTextSize(20);
								view1.setFormat12Hour("h:mm:ss a");
								int index1 = signBoardPagerAdapter.addView(view1);
								viewPager.setCurrentItem(index1, true);
							}
						});
						break;
					case ADD_TEST_VIEW2:
						Log.i(TAG, "Adding View2");
						mainThreadHandler.post(new Runnable() {
							@Override
							public void run() {
								ImageView view2 = new ImageView(context);
								view2.setBackgroundColor(Color.parseColor("#667788"));
								view2.setImageResource(android.R.drawable.ic_dialog_email);
								int index2 = signBoardPagerAdapter.addView(view2);
								viewPager.setCurrentItem(index2, true);
							}
						});
						break;
					case ADD_TEST_VIEW3:
						Log.i(TAG, "Adding View3");
						mainThreadHandler.post(new Runnable() {
							@Override
							public void run() {
								ImageView view3 = new ImageView(context);
								view3.setBackgroundColor(Color.parseColor("#778866"));
								view3.setImageResource(android.R.drawable.ic_menu_call);
								int index3 = signBoardPagerAdapter.addView(view3);
								viewPager.setCurrentItem(index3, true);
							}
						});
						break;
					case ADD_TEST_VIEW4:
						Log.i(TAG, "Adding View4");
						mainThreadHandler.post(new Runnable() {
							@Override
							public void run() {
								ImageView view4 = new ImageView(context);
								view4.setBackgroundColor(Color.parseColor("#886677"));
								view4.setImageResource(android.R.drawable.ic_menu_camera);
								int index4 = signBoardPagerAdapter.addView(view4);
								viewPager.setCurrentItem(index4, true);
							}
						});
						break;
				}
			} catch(Exception e) {
				Log.e(TAG, "Exception in SignBoardWorkerHandler.handleMessage:", e);
			}
		}
	}

	public class SignBoardPagerAdapter extends PagerAdapter {
		private ArrayList<View> views = new ArrayList<View>();

		@Override
		public int getItemPosition(Object page) {
			int index = views.indexOf(page);
			if(index == -1) {
				return POSITION_NONE;
			} else{
				return index;
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View view = views.get(position);
			container.addView(view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView(views.get(position));
		}

		@Override
		public int getCount() {
			return views.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		public int addView(View view) {
			return addView(view, views.size());
		}

		public int addView(View view, int position) {
			views.add(position, view);
			signBoardPagerAdapter.notifyDataSetChanged();
			return position;
		}

		public int removeView(ViewPager pager, View view) {
			return removeView(pager, views.indexOf(view));
		}

		public int removeView(ViewPager pager, int position) {
			viewPager.setAdapter(null);
			views.remove(position);
			viewPager.setAdapter(this);
			return position;
		}

		public void removeAllViews() {
			viewPager.setAdapter(null);
			views.clear();
			viewPager.setAdapter(this);
		}

		public View getView(int position) {
			return views.get(position);
		}

		public int getViewCount() {
			return views.size();
		}
	}

	private class OrientationListener extends OrientationEventListener{
		final int ROTATION_O = 1;
		final int ROTATION_90 = 2;
		final int ROTATION_180 = 3;
		final int ROTATION_270 = 4;
		private int rotation = 0;

		public OrientationListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if((orientation < 35 || orientation > 325) && rotation!= ROTATION_O){
				rotation = ROTATION_O;
				linearLayout.setOrientation(LinearLayout.HORIZONTAL);
				linearLayout.setGravity(Gravity.RIGHT);
				linearLayout.setPadding(400, 0, 0, 0);
			} else if(orientation > 145 && orientation < 215 && rotation!=ROTATION_180){
				rotation = ROTATION_180;
				linearLayout.setOrientation(LinearLayout.HORIZONTAL);
				linearLayout.setGravity(Gravity.LEFT);
				linearLayout.setPadding(0, 0, 400, 0);
			} else if(orientation > 55 && orientation < 125 && rotation!=ROTATION_270){
				rotation = ROTATION_270;
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.BOTTOM);
				linearLayout.setPadding(0, 400, 0, 0);
			} else if(orientation > 235 && orientation < 305 && rotation!=ROTATION_90){
				rotation = ROTATION_90;
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.TOP);
				linearLayout.setPadding(0, 0, 0, 400);
			}
		}
	}

}
