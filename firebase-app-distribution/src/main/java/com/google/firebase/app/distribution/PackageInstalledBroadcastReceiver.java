package com.google.firebase.app.distribution;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

// TODO: Instead of a broadcast receiver, how about an activity that gets started when the post-install intent happens?
public class PackageInstalledBroadcastReceiver extends BroadcastReceiver {
  private static final String TAG = "PackageInstalledBroadcastReceiver: ";
  @Override
  public void onReceive(Context context, Intent intent) {
    // TODO: app is still open in app switcher, behind chrome account switcher.\
    //    If we don't launch the app here, is the app still there? [YES]
    //    Was it like this before? [YES]
    //    Also, what if we opened the account switcher in a webview instead of new tab? [WONTFIX]
    //    Also, before, if we didn't click "open" could we repro the bug? [NO] If so, check if using PackageInstaller.Session fixes the bug
    // TODO: other option, use example code here to launch specific activity, for showing dialog, etc: https://stackoverflow.com/questions/32084898/android-launch-app-from-broadcast-receiver
    // LogWrapper.getInstance().e(TAG + "Received intent: " + intent);
    // LogWrapper.getInstance().e(TAG + "Launching app: " + context.getPackageName());
    // PackageManager pm = context.getPackageManager();
    // Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
    // LogWrapper.getInstance().e(TAG + "Got launch intent: " + launchIntent);
    // // launchIntent.putExtra("some_data", "value");
    // context.startActivity(launchIntent);

    // Intent launch_intent = new Intent("android.intent.action.MAIN");
    // launch_intent.setPackage(context.getPackageName());
    // launch_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    // launch_intent.addCategory(Intent.CATEGORY_LAUNCHER);
    // // launch_intent.putExtra("some_data", "value");
    // context.startActivity(launch_intent);
  }
}
