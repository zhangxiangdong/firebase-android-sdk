package com.google.firebase.app.distribution;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;

public class PostInstallActivity extends Activity {
  private static final String TAG = "PostInstallActivity: ";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Context context = getApplicationContext();
    LogWrapper.getInstance().e(TAG + "onCreate() called, started by intent: " + getIntent());
    LogWrapper.getInstance().e(TAG + "Launching app: " + context.getPackageName());
    PackageManager pm = context.getPackageManager();
    Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
    LogWrapper.getInstance().e(TAG + "Got launch intent: " + launchIntent);
    // launchIntent.putExtra("some_data", "value");
    context.startActivity(launchIntent);
  }

  @Override
  public void onResume() {
    super.onResume();
    Context context = getApplicationContext();
    LogWrapper.getInstance().e(TAG + "onResume() called, started by intent: " + getIntent());
    LogWrapper.getInstance().e(TAG + "Launching app: " + context.getPackageName());
    PackageManager pm = context.getPackageManager();
    Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
    LogWrapper.getInstance().e(TAG + "Got launch intent: " + launchIntent);
    // launchIntent.putExtra("some_data", "value");
    context.startActivity(launchIntent);
  }
}
