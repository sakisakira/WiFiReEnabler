package jp.sakira.wifireenabler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SleepWatcher extends Service {
  public static final String RequestType = "RequestType";
  public static final int RequestStartCommandStay = 1;
  public static final int RequestReConnect = 2;

  static final int NOTIFICATION_ID = 1;
  static final String keyCommand = "Command_";
  static final String keyStay = "Stay_";
  
  static final int CMD_None = 0;
  static final int CMD_ReAssociate = 1;
  static final int CMD_DisconnectThenConnect = 2;
  
  static final int STAY_Always = 0;
  static final int STAY_Notify = 1;
  static final int STAY_Never = 2;
  
  private int command = CMD_None;
  private int stay = STAY_Notify;

  static private WREBroadcastReceiver broadcastReceiver = null;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);

    if (intent != null) {
      final int req = intent.getIntExtra(RequestType, -1);
      if (req == RequestStartCommandStay) {
        startCommandStay();
        return START_STICKY;
      } else if (req == RequestReConnect) {
        startReConnect();
        return START_STICKY;
      } else {
        return START_NOT_STICKY;
      }
    }
    return START_NOT_STICKY;
  }

  public void startCommandStay() {
    if (stay == STAY_Always) {
      String msg = "stay in status bar";
      switch (command) {
        case CMD_None:
          msg = "None mode"; break;
        case CMD_ReAssociate:
          msg = "Auto Re-Associate mode"; break;
        case CMD_DisconnectThenConnect:
          msg = "Disconnect then Re-Connect mode"; break;
      }
      showNotification(msg);
    }
  }

  private void startReConnect() {
    final Context context = getApplicationContext();
    restoreCommandStay();

    if (command == SleepWatcher.CMD_None) return;
    Log.i("wifireenabler", "startReConnect");

    final ConnectivityManager conn_man = (ConnectivityManager)
      context.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo info =
      conn_man.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    if (false) {
    if (info != null && info.isConnected()) {
      showNotification("WiFi connection alive");
      Log.i("wifireenabler", "wifi connection alive");
      clearNotificationAfter(10000);
      return;
    }
    }

    final WifiManager manager = (WifiManager)
      context.getSystemService(Context.WIFI_SERVICE);
    switch (command) {
      case SleepWatcher.CMD_ReAssociate:
        manager.reassociate();
        showNotification("WiFi ReAssociate");
        Log.i("wifireenabler", "wifi reassociated");
        break;
      case SleepWatcher.CMD_DisconnectThenConnect:
        manager.setWifiEnabled(false);
        showNotification("WiFi OFF");
        Log.i("wifireenabler", "wifi diabled");
        try {
          Thread.sleep(300);
        } catch (InterruptedException e) {
          Log.i("wifireenabler", "sleep cancelled:" + e);
        }
        manager.setWifiEnabled(true);
        showNotification("WiFi ON");
        Log.i("wifireenabler", "wifi enabled");
        break;
    }

    clearNotificationAfter(10000);
  }

  private void showNotification(final String msg) {
    if (stay == STAY_Never) return;

    final Context context = getApplicationContext();
    if (context == null) return;

    final Intent notInent = new Intent(context, WiFiReEnabler.class);
    final PendingIntent pintent =
        PendingIntent.getActivity(context, 0, notInent, 0);
    final Notification noti =
        new NotificationCompat.Builder(context)
            .setContentTitle("WiFiReEnabler")
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_menu_notify)
            .setContentIntent(pintent)
            .build();
    final NotificationManager notMan = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);
    notMan.notify(NOTIFICATION_ID, noti);
  }
  
  private void clearNotificationAfter(final int msec) {
    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      public void run() {
        clearNotification();
      }}, msec);
  }
  
  private void clearNotification() {
    if (stay != STAY_Always) {
      final Context context = getApplicationContext();
      final NotificationManager notMan = (NotificationManager)
      context.getSystemService(Context.NOTIFICATION_SERVICE);
      notMan.cancel(SleepWatcher.NOTIFICATION_ID);
    }
  }
  
  public void setCommand(final int cmd) {
    final SharedPreferences pref =
      PreferenceManager.getDefaultSharedPreferences(this);
    final SharedPreferences.Editor editor = pref.edit();
    editor.putInt(keyCommand, cmd);
    editor.commit();
    command = cmd;
  }

  public void setStay(final int sty) {
    final SharedPreferences pref =
      PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = pref.edit();
    editor.putInt(keyStay, sty);
    editor.commit();
    stay = sty;
  }

  private void restoreCommandStay() {
    final SharedPreferences pref =
      PreferenceManager.getDefaultSharedPreferences(this);
    int cmd = pref.getInt(keyCommand, -1);
    if (cmd >= 0)
      command = cmd;
    stay = pref.getInt(keyStay, stay);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    if (broadcastReceiver == null)
      broadcastReceiver = new WREBroadcastReceiver();
    final Context context = getApplicationContext();
    final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    context.registerReceiver(broadcastReceiver, filter);
  }

  private final IBinder binder = new LocalBinder();
  public class LocalBinder extends Binder {
    SleepWatcher getService() {
      return SleepWatcher.this;
    }
  };

  @Override
  public IBinder onBind(Intent intent) {
      return binder;
  }

}
