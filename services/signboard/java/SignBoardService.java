package com.android.server;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
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
import java.util.List;

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
		viewPager.setAdapter(new InfinitePagerAdapter(signBoardPagerAdapter));
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

		parseAndAddPages();
	}

    public void parseAndAddPages() {
        CustomHost host = new CustomHost(context);
        int category = context.getResources().getInteger(com.android.internal.R.integer.signboard_widget_category);
        List<AppWidgetProviderInfo> infos = AppWidgetManager.getInstance(context).getInstalledProviders(category);

        for (AppWidgetProviderInfo info : infos) {
            int id = host.allocateAppWidgetId();
            AppWidgetHostView view = host.createView(context, id, info);
            view.setAppWidget(id, info);

            Message msg = Message.obtain();
            msg.what = SignBoardWorkerHandler.ADD_VIEW;
            msg.obj = view;
            signBoardHandler.sendMessage(msg);

            AppWidgetManager.getInstance(context).bindAppWidgetId(id, info.provider);
        }
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
		private static final int ADD_VIEW = 1;

		@Override
		public void handleMessage(Message msg) {
			try {
				switch(msg.what) {
					case REMOVE_ALL_VIEWS:
						Log.i(TAG, "Removing All Views");
						mainThreadHandler.post(() -> signBoardPagerAdapter.removeAllViews());
						break;
					case ADD_VIEW:
						Log.i(TAG, "Adding View to SignBoard: " + msg.obj);
						mainThreadHandler.post(() -> {
                            signBoardPagerAdapter.addView((View) msg.obj);
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

	/*
	 * Copyright (C) 2016 The CyanogenMod Project
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

	/**
	 * A PagerAdapter that wraps around another PagerAdapter to handle paging wrap-around.
	 */
	public class InfinitePagerAdapter extends PagerAdapter {

		private static final String TAG = "InfinitePagerAdapter";
		private static final boolean DEBUG = false;

		private PagerAdapter adapter;

		public InfinitePagerAdapter(PagerAdapter adapter) {
			this.adapter = adapter;
		}

		@Override
		public int getCount() {
			// warning: scrolling to very high values (1,000,000+) results in
			// strange drawing behaviour
			return Integer.MAX_VALUE;
		}

		/**
		 * @return the {@link #getCount()} result of the wrapped adapter
		 */
		public int getRealCount() {
			return adapter.getCount();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			int virtualPosition = position % getRealCount();
			debug("instantiateItem: real position: " + position);
			debug("instantiateItem: virtual position: " + virtualPosition);

			// only expose virtual position to the inner adapter
			return adapter.instantiateItem(container, virtualPosition);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			int virtualPosition = position % getRealCount();
			debug("destroyItem: real position: " + position);
			debug("destroyItem: virtual position: " + virtualPosition);

			// only expose virtual position to the inner adapter
			adapter.destroyItem(container, virtualPosition, object);
		}

		/*
		 * Delegate rest of methods directly to the inner adapter.
		 */

		@Override
		public void finishUpdate(ViewGroup container) {
			adapter.finishUpdate(container);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return adapter.isViewFromObject(view, object);
		}

		@Override
		public void restoreState(Parcelable bundle, ClassLoader classLoader) {
			adapter.restoreState(bundle, classLoader);
		}

		@Override
		public Parcelable saveState() {
			return adapter.saveState();
		}

		@Override
		public void startUpdate(ViewGroup container) {
			adapter.startUpdate(container);
		}

		/*
		 * End delegation
		 */

		private void debug(String message) {
			if (DEBUG) {
				Log.d(TAG, message);
			}
		}
	}

    public static class CustomHost extends AppWidgetHost {
        public CustomHost(Context context) {
            super(context, 1001);
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
            return new com.zacharee1.aospsignboard.CustomHost.FixedHostView(context);
        }

        public static class FixedHostView extends AppWidgetHostView {
            public FixedHostView(Context context) {
                super(context);
            }

            @Override
            public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info) {
                super.setAppWidget(appWidgetId, info);

                if (info != null) {
                    setPadding(0, 0, 0, 0);
                }
            }
        }
    }

}
