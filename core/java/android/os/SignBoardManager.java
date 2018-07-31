package android.os;

import android.annotation.BroadcastBehavior;
import android.annotation.SdkConstant;
import android.annotation.SystemService;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

@SystemService(Context.SIGNBOARD_SERVICE)
public class SignBoardManager {
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    @BroadcastBehavior(includeBackground = true)
    public static final String ACTION_UPDATE_QUICKTOGGLES = "com.android.signboard.action.UPDATE_QUICKTOGGLES";

    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    @BroadcastBehavior(includeBackground = true)
    public static final String ACTION_TOGGLE_QUICKTOGGLE = "com.android.signboard.action.TOGGLE_QT";

    /**
     * QuickToggle related constants
     */
    public static final String QT_TOGGLE = "toggle";

    public static final String QT_WIFI = "wifi";
    public static final String QT_BT = "bluetooth";
    public static final String QT_AIRPLANE = "airplane";
    public static final String QT_LOCATION = "location";
    public static final String QT_DATA = "data";
    public static final String QT_VOLUME = "volume";
    public static final String QT_SETTINGS = "settings";
    public static final String QT_CAMERA = "camera";
    public static final String QT_FLASHLIGHT = "flashlight";
    public static final String QT_ROTATION = "rotation";

    public static final String[] QT_ITEMS = new String[] {
            QT_WIFI,
            QT_BT,
            QT_AIRPLANE,
            QT_LOCATION,
            QT_DATA,
            QT_VOLUME,
            QT_SETTINGS,
            QT_CAMERA,
            QT_FLASHLIGHT,
            QT_ROTATION,
    };

    public static final String QT_KEY = "qt_order";

    public static final String SEPARATOR = ";";

    public static final String QT_DEFAULT =
            QT_VOLUME + SEPARATOR
                    + QT_WIFI + SEPARATOR
                    + QT_BT + SEPARATOR
//            + DATA + SEPARATOR
                    + QT_AIRPLANE + SEPARATOR
                    + QT_LOCATION;

    public static SignBoardManager getInstance(Context context) {
        return new SignBoardManager(context, ISignBoardService.Stub.asInterface(ServiceManager.getService(Context.SIGNBOARD_SERVICE)));
    }

    private Context context;
    private ISignBoardService service;

    public SignBoardManager(Context context, ISignBoardService service) {
        this.context = context;
        this.service = service;
    }

    public void removeAllViews() {
        try {
            service.removeAllViews();
        } catch (RemoteException e) {}
    }

    public void initViews() {
        try {
            service.initViews();
        } catch (RemoteException e) {}
    }

    public void refreshViews() {
        try {
            service.refreshViews();
        } catch (RemoteException e) {}
    }

    public void setQuickToolsEnabled(boolean enabled) {
        try {
            service.setQuickToolsEnabled(enabled);
        } catch (RemoteException e) {}
    }

    public void sendQuickToolsAction(String action) {
        try {
            service.sendQuickToolsAction(action);
        } catch (RemoteException e) {}
    }

    public ArrayList<String> getList() {
        return new ArrayList<>(Arrays.asList(getStringList().split(SEPARATOR)));
    }

    public String getStringList() {
        String ret = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.CURRENT_SIGNBOARD_QUICK_TOGGLES);
        if (ret == null || ret.isEmpty()) ret = QT_DEFAULT;
        return ret;
    }

    public void saveList(ArrayList<String> list) {
        if (list == null) saveStringList(null);
        else {
            String joined = TextUtils.join(SEPARATOR, list);
            saveStringList(joined);
        }
    }

    public void saveStringList(String list) {
        Settings.Secure.putString(context.getContentResolver(), Settings.Secure.CURRENT_SIGNBOARD_QUICK_TOGGLES, list);
    }

    public boolean isFlashlightEnabled() {
        try {
            return service.isFlashlightEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setFlashlightEnabled(boolean enabled) {
        try {
            service.setFlashlightEnabled(enabled);
        } catch (RemoteException e) {}
    }
}
