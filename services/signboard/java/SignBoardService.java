package com.android.server;

import android.Manifest;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.*;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import com.android.internal.R;
import com.android.server.audio.AudioService;

import java.util.ArrayList;
import java.util.List;

public class SignBoardService extends ISignBoardService.Stub {
	private static final String TAG = "SignBoardService";
	private static final ArrayList<ComponentName> DEFAULT_PAGES = new ArrayList<ComponentName>() {{
	    //empty for now
    }};

	private Handler mainThreadHandler;
	private ServiceThread signBoardWorker;
	private SignBoardWorkerHandler signBoardHandler;
	private Context context;
	private WindowManager windowManager;
	private LockedLinearLayout linearLayout;
	private LockedViewPager viewPager;
	private SignBoardPagerAdapter signBoardPagerAdapter;
	private OrientationListener orientationListener;
	private CustomHost host;
	private Observer observer;

    private QuickToolsListener listener = new QuickToolsListener();
	private boolean quickToolsEnabled = false;

	public SignBoardService(Context context) {
		super();
		this.context = context;
		host = new CustomHost(context);
		mainThreadHandler = new Handler(Looper.getMainLooper());
		orientationListener = new OrientationListener(context);
		orientationListener.enable();
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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

	@Override
    public void setQuickToolsEnabled(boolean enabled) {
	    quickToolsEnabled = enabled;
	    if (enabled) listener.onCreate();
	    else listener.onDestroy();
    }

    @Override
    public void sendQuickToolsAction(String action) {
        if (quickToolsEnabled) {
            Message msg = Message.obtain();
            msg.obj = action;
            msg.what = SignBoardWorkerHandler.QT_ACTION;
            signBoardHandler.sendMessage(msg);
        }
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
        private static final int QT_ACTION = 1003;

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
						Log.i(TAG, "Removing All Views");
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
                    case QT_ACTION:
                        switch (msg.obj.toString()) {
                            case SignBoardManager.QT_WIFI:
                                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                                wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
                                break;
                            case SignBoardManager.QT_BT:
                                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                                if (adapter.isEnabled()) adapter.disable();
                                else adapter.enable();
                                break;
                            case SignBoardManager.QT_AIRPLANE:
                                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                                connectivityManager.setAirplaneMode(Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 1);
                                break;
                            case SignBoardManager.QT_LOCATION:
                                boolean enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                                        != Settings.Secure.LOCATION_MODE_OFF;
                                int prev = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_PREVIOUS_MODE, Settings.Secure.LOCATION_MODE_OFF);
                                Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, enabled ? 0 : prev);
                                break;
                            case SignBoardManager.QT_DATA:
                                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                                telephonyManager.setDataEnabled(!telephonyManager.isDataEnabled());
                                break;
                            case SignBoardManager.QT_VOLUME:
                                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                switch (audioManager.getRingerMode()) {
                                    case AudioManager.RINGER_MODE_SILENT:
                                        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                                        break;
                                    case AudioManager.RINGER_MODE_VIBRATE:
                                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                        break;
                                    case AudioManager.RINGER_MODE_NORMAL:
                                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                                        break;
                                }
                                break;
                        }
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

	private class OrientationListener extends OrientationEventListener {
		private int rotation = 0;

		public OrientationListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if (orientation != rotation) {
				rotation = orientation;
				switch (context.getDisplay().getRotation()) {
					case Surface.ROTATION_0:
						linearLayout.setOrientation(LinearLayout.HORIZONTAL);
						linearLayout.setGravity(Gravity.RIGHT);
						break;
					case Surface.ROTATION_90:
						linearLayout.setOrientation(LinearLayout.VERTICAL);
						linearLayout.setGravity(Gravity.TOP);
						break;
					case Surface.ROTATION_180:
						linearLayout.setOrientation(LinearLayout.HORIZONTAL);
						linearLayout.setGravity(Gravity.LEFT);
						break;
					case Surface.ROTATION_270:
						linearLayout.setOrientation(LinearLayout.VERTICAL);
						linearLayout.setGravity(Gravity.BOTTOM);
						break;
				}
			}
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
            public boolean equals(Object obj) {
                return obj instanceof AppWidgetHostView && getAppWidgetInfo().provider.equals(((AppWidgetHostView) obj).getAppWidgetInfo().provider);
            }
        }
    }

    private class Observer extends ContentObserver {
	    public Observer() {
	        super(signBoardHandler);

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

    private class QuickToolsListener extends ContentObserver {
        private BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(SignBoardManager.ACTION_TOGGLE_QUICKTOGGLE)) {
                     if (intent.hasExtra(SignBoardManager.QT_TOGGLE)) {
                         sendQuickToolsAction(intent.getStringExtra(SignBoardManager.QT_TOGGLE));
                     }
                } else {
                    update();
                }
            }
        };

        public QuickToolsListener() {
            super(Handler.getMain());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        public void onCreate() {
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true, this);

            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filter.addAction(SignBoardManager.ACTION_TOGGLE_QUICKTOGGLE);
            filter.addAction(LocationManager.MODE_CHANGED_ACTION);
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

            context.registerReceiver(receiver, filter);
        }

        public void onDestroy() {
            context.getContentResolver().unregisterContentObserver(this);
            context.unregisterReceiver(receiver);
        }

        private void update() {
            Intent update = new Intent(SignBoardManager.ACTION_UPDATE_QUICKTOGGLES);
            update.setComponent(new ComponentName("com.zacharee1.aospsignboard", "com.zacharee1.aospsignboard.widgets.QuickToggles"));
            context.sendBroadcastAsUser(update, Process.myUserHandle(), Manifest.permission.MANAGE_SIGNBOARD);
        }
    }

}
