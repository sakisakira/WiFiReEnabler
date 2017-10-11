package jp.sakira.wifireenabler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Suzuki on 2013/07/16.
 */
public class WREBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i("wifireenabler", "onReceive:" + intent);

    final SharedPreferences pref =
      PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    final int cmd = pref.getInt(SleepWatcher.keyCommand, -1);

    if (cmd == SleepWatcher.CMD_DisconnectThenConnect ||
      cmd == SleepWatcher.CMD_ReAssociate) {
      final Intent s_intent = new Intent(context, SleepWatcher.class);
      s_intent.putExtra(SleepWatcher.RequestType,
        SleepWatcher.RequestReConnect);
      context.startService(s_intent);
    }
  }

}
