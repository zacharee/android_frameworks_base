package com.android.server;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import com.android.internal.R;
import com.android.internal.policy.DecorView;
import com.android.internal.policy.PhoneWindow;

import java.util.ArrayList;
import java.util.List;

public class SignBoardService extends ISignBoardService.Stub {
	private static final String TAG = "SignBoardService";
	private static final ArrayList<ComponentName> DEFAULT_PAGES = new ArrayList<ComponentName>() {{
	    add(new ComponentName("com.zacharee1.aospsignboard", "com.zacharee1.aospsignboard.widgets.QuickToggles"));
        add(new ComponentName("com.zacharee1.aospsignboard", "com.zacharee1.aospsignboard.widgets.Music"));
    }};

	private Handler mainThreadHandler;
	private ServiceThread signBoardWorker;
	private SignBoardWorkerHandler signBoardHandler;
	private Context context;
	private LockedLinearLayout linearLayout;
	private LockedViewPager viewPager;
	private SignBoardPagerAdapter signBoardPagerAdapter;
	private CustomHost host;
	private Observer observer;
	private WindowManagerImpl windowManager;

	public SignBoardService(Context context) {
		super();
		this.context = context;
		host = new CustomHost(context);
		mainThreadHandler = new Handler(Looper.getMainLooper());
		windowManager = new WindowManagerImpl(context).createLocalWindowManager(new SignBoardWindow(context));
		signBoardPagerAdapter = new SignBoardPagerAdapter();
		viewPager = new LockedViewPager(context);
		viewPager.setAdapter(signBoardPagerAdapter);
		WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
		windowManagerParams.type = WindowManager.LayoutParams.TYPE_SIGNBOARD_NORMAL;
		windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		windowManagerParams.privateFlags = WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
		windowManagerParams.setTitle("SignBoard");
		linearLayout = new LockedLinearLayout(context);
		linearLayout.setBackgroundColor(Color.BLACK);
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setGravity(Gravity.RIGHT);
		linearLayout.addView(viewPager);
		windowManager.addView(linearLayout, windowManagerParams);
		signBoardWorker = new ServiceThread(TAG, Process.THREAD_PRIORITY_FOREGROUND, false);
		signBoardWorker.start();
		signBoardHandler = new SignBoardWorkerHandler(signBoardWorker.getLooper());
		observer = new Observer();
		observer.onCreate();
	}

    private void parseAndAddPages() {
		host.startListening();
		int category = context.getResources().getInteger(R.integer.config_signBoardCategory);
		List<AppWidgetProviderInfo> infos = AppWidgetManager.getInstance(context).getInstalledProviders(category);
		ArrayList<ComponentName> enabled = enabledComponents();

        ArrayList<AppWidgetProviderInfo> newSet = new ArrayList<>();
        for (ComponentName e : enabled) {
            for (AppWidgetProviderInfo info : infos) {
                if (info.provider.equals(e)) newSet.add(info);
            }
        }

		mainThreadHandler.post(() -> signBoardPagerAdapter.updateViews(new ArrayList<>(newSet)));
	}

	private ArrayList<ComponentName> enabledComponents() {
	    ArrayList<ComponentName> ret = new ArrayList<>();
	    String saved = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_SIGNBOARD_COMPONENTS);
	    if (saved == null || saved.isEmpty()) return DEFAULT_PAGES;

	    String[] splitSaved = saved.split(";");
	    for (String comp : splitSaved) {
	        String[] splitComp = comp.split("/");
	        ret.add(new ComponentName(splitComp[0], splitComp[1]));
        }

        return ret;
    }

	@Override
    public void removeAllViews() {
	    Message msg = Message.obtain();
	    msg.what = SignBoardWorkerHandler.REMOVE_ALL_VIEWS;
	    signBoardHandler.sendMessage(msg);
    }

	@Override
	public void initViews() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.INIT;
		signBoardHandler.sendMessage(msg);
	}

	@Override
	public void refreshViews() {
		Message msg = Message.obtain();
		msg.what = SignBoardWorkerHandler.REFRESH;
		signBoardHandler.sendMessage(msg);
	}

	public int addView(AppWidgetHostView view) {
		return signBoardPagerAdapter.addView(view);
	}

	public int removeView(AppWidgetHostView view) {
		return signBoardPagerAdapter.removeView(view);
	}

	public int removeView(int position) {
		return signBoardPagerAdapter.removeView(position);
	}

	private static class LockedLinearLayout extends LinearLayout {
		public LockedLinearLayout(Context context) {
			super(context);
		}

        @Override
		public void dispatchConfigurationChanged(Configuration config) {
			config.orientation = Configuration.ORIENTATION_PORTRAIT;
			super.dispatchConfigurationChanged(config);
		}
    }

    private static class LockedViewPager extends ViewPager {
	    public LockedViewPager(Context context) {
	        super(context);
        }

        @Override
        public void dispatchConfigurationChanged(Configuration newConfig) {
            newConfig.orientation = Configuration.ORIENTATION_PORTRAIT;
            super.dispatchConfigurationChanged(newConfig);
        }
    }

	private class SignBoardWorkerHandler extends Handler {
		private static final int REMOVE_ALL_VIEWS = 1000;
        private static final int INIT = 1001;
        private static final int REFRESH = 1002;

        public SignBoardWorkerHandler() {
            super();
        }

        public SignBoardWorkerHandler(Looper looper) {
            super(looper);
        }

		@Override
		public void handleMessage(Message msg) {
			try {
				switch(msg.what) {
					case REMOVE_ALL_VIEWS:
						mainThreadHandler.post(() -> {
							signBoardPagerAdapter.removeAllViews();
							host.stopListening();
						});
						break;
                    case INIT:
                        parseAndAddPages();
                        mainThreadHandler.post(() -> host.startListening());
                        break;
					case REFRESH:
						mainThreadHandler.post(() -> signBoardPagerAdapter.removeAllViews());
						parseAndAddPages();
						break;
				}
			} catch(Exception e) {
				Log.e(TAG, "Exception in SignBoardWorkerHandler.handleMessage:", e);
			}
		}
	}

	public class SignBoardPagerAdapter extends PagerAdapter {
		private ArrayList<AppWidgetHostView> views = new ArrayList<>();

		@Override
		public int getItemPosition(Object page) {
			int index = views.indexOf(page);
			if (index == -1) {
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
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return views.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		public boolean hasWidget(ComponentName provider) {
            return views.stream().anyMatch(h -> h.getAppWidgetInfo().provider.equals(provider));
        }

		public void updateViews(ArrayList<AppWidgetProviderInfo> newSet) {
            views.clear();
            newSet.forEach(i -> views.add(makeView(i)));

            notifyDataSetChanged();
        }

		public int addView(AppWidgetHostView view) {
			return addView(view, views.size());
		}

		public int addView(AppWidgetHostView view, int position) {
		    if (view == null) return -1;
			views.add(position, view);
			notifyDataSetChanged();
			return position;
		}

		public int removeView(AppWidgetHostView view) {
			return removeView(views.indexOf(view));
		}

		public int removeView(int position) {
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

        AppWidgetHostView makeView(AppWidgetProviderInfo info) {
            int id = host.allocateAppWidgetId();
            AppWidgetHostView view = host.createView(context, id, info);
            view.setAppWidget(id, info);

            View.OnLongClickListener listener = v -> {
                if (info.configure != null) {
                    Intent configure = new Intent(Intent.ACTION_VIEW);
                    configure.setComponent(info.configure);
                    configure.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(configure);
                    return true;
                }
                return false;
            };

            view.setOnLongClickListener(listener);

            for (int i = 0; i < view.getChildCount(); i++) {
                view.getChildAt(i).setOnLongClickListener(listener);
            }

            AppWidgetManager.getInstance(context).bindAppWidgetId(id, info.provider);

            return view;
        }
	}

    private static class CustomHost extends AppWidgetHost {
        public CustomHost(Context context) {
            super(context, 1001);
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
            return new FixedHostView(context);
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

            @Override
            public void dispatchConfigurationChanged(Configuration newConfig) {
                newConfig.orientation = Configuration.ORIENTATION_PORTRAIT;
                super.dispatchConfigurationChanged(newConfig);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof AppWidgetHostView && getAppWidgetInfo().provider.equals(((AppWidgetHostView) obj).getAppWidgetInfo().provider);
            }
        }
    }

    private class Observer extends ContentObserver {
	    public Observer() {
	        super(signBoardHandler);
        }

        public void onCreate() {
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_SIGNBOARD_COMPONENTS),
                    true, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Secure.getUriFor(Settings.Secure.ENABLED_SIGNBOARD_COMPONENTS))) {
                Message msg = Message.obtain();
                msg.what = SignBoardWorkerHandler.INIT;
                signBoardHandler.sendMessage(msg);
            }
        }
    }

    public static class SignBoardWindow extends PhoneWindow {
	    public SignBoardWindow(Context context) {
	        super(context);
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
	        newConfig.orientation = Configuration.ORIENTATION_PORTRAIT;
            super.onConfigurationChanged(newConfig);
        }

        @Override
        protected DecorView generateDecor(int featureId) {
            return new SignBoardDecorView(getContext(), -1, this, getAttributes());
        }

        public static class SignBoardDecorView extends DecorView {
	        public SignBoardDecorView(Context context, int featureId, PhoneWindow window, WindowManager.LayoutParams attrs) {
	            super(context, featureId, window, attrs);
            }

            @Override
            public void dispatchConfigurationChanged(Configuration newConfig) {
                newConfig.orientation = Configuration.ORIENTATION_PORTRAIT;
                super.dispatchConfigurationChanged(newConfig);
            }
        }
    }
}
