package jp.sakira.wifireenabler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RadioButton;

public class WiFiReEnabler extends Activity {
  final int[] CmdIDs = {R.id.noneButton,
        R.id.reassociateButton, R.id.disconnectConnectButton};
  final int[] StayIDs = {R.id.stayAlways,
        R.id.stayNotify, R.id.stayNever};

  private SleepWatcher sleepWatcher = null;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  protected void onResume() {
    super.onResume();

    final SharedPreferences pref =
      PreferenceManager.getDefaultSharedPreferences(this);
    final int cmd = pref.getInt(SleepWatcher.keyCommand, -1);
    final int stay = pref.getInt(SleepWatcher.keyStay, -1);

    if (cmd >= 0) {
      RadioButton cmd_btn =
        (RadioButton)findViewById(CmdIDs[cmd]);
      cmd_btn.setChecked(true);
    }
    if (stay >= 0) {
      RadioButton stay_btn =
        (RadioButton)findViewById(StayIDs[stay]);
      stay_btn.setChecked(true);
    }

    final Intent intent = new Intent(getApplicationContext(),
        SleepWatcher.class);
    intent.putExtra(SleepWatcher.RequestType, SleepWatcher.RequestStartCommandStay);
    startService(intent);

    final Intent bintent = new Intent(getApplicationContext(),
        SleepWatcher.class);
    bindService(bintent, serviceConnection, BIND_AUTO_CREATE);
  }

  @Override
  public void onPause() {
    super.onPause();

    unbindService(serviceConnection);
  }
  
  public void noneClicked(View v) {
    sleepWatcher.setCommand(SleepWatcher.CMD_None);
    sleepWatcher.startCommandStay();
  }
  
  public void reassociateClicked(View v) {
    sleepWatcher.setCommand(SleepWatcher.CMD_ReAssociate);
    sleepWatcher.startCommandStay();
  }

  public void disconnectConnectClicked(View v) {
    sleepWatcher.setCommand(SleepWatcher.CMD_DisconnectThenConnect);
    sleepWatcher.startCommandStay();
  }
  
  public void stayButtonClicked(View v) {
    for (int i = 0; i < StayIDs.length; i ++)  {
      RadioButton rb = (RadioButton) findViewById(StayIDs[i]);
      if (rb.isChecked()) {
        sleepWatcher.setStay(i);
        sleepWatcher.startCommandStay();
        return;
      }
    }
  }
  
  public void showAboutAlert(View v) {
    String versionName;
    PackageManager pm = getPackageManager();
    if (pm == null) return;
    try {
        PackageInfo info = null;
        info = pm.getPackageInfo("jp.sakira.wifireenabler", 0);
        versionName = info.versionName;
    } catch (NameNotFoundException e) {
      versionName = "";
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Wifi Auto Re-Enabler");
    builder.setMessage("version " + versionName + "\n\n" +
      "twitter: sakira");
    builder.setPositiveButton("OK", 
          new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {}
    });
  
    builder.show();
  }

  private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      final SleepWatcher.LocalBinder binder = (SleepWatcher.LocalBinder)iBinder;
      sleepWatcher = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      sleepWatcher = null;
    }
  };

}