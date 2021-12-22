// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.app.distribution;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Activity opened during installation in {@link UpdateApkClient} after APK download is finished.
 */
public class InstallActivity extends AppCompatActivity {
  private static final String TAG = "InstallActivity: ";
  private boolean unknownSourceEnablementInProgress = false;
  private boolean installInProgress = false;
  private static final String PACKAGE_INSTALLED_ACTION = "com.google.firebase.app.distribution.PACKAGE_INSTALLED_ACTION";

  @Override
  public void onResume() {
    super.onResume();
    LogWrapper.getInstance().i(TAG + "onResume() called");
    // Since we kick-off installation with FLAG_ACTIVITY_NEW_TASK (in a new task), we won't be able
    // to figure out if installation failed or was cancelled.
    // If we re-enter InstallActivity after install is already kicked off, we can assume that either
    // installation failure or user cancelled the install.
    if (installInProgress) {
      LogWrapper.getInstance()
          .e(
              TAG
                  + "Activity resumed when installation already in progress. Installation was either cancelled or failed");
      finish();
      return;
    }

    if (!isUnknownSourcesEnabled()) {
      // See comment about install progress above. Same applies to unknown sources UI.
      if (unknownSourceEnablementInProgress) {
        LogWrapper.getInstance()
            .e(
                TAG
                    + "Unknown sources enablement is already in progress. It was either cancelled or failed");
        finish();
        return;
      }

      unknownSourceEnablementInProgress = true;
      showUnknownSourcesUi();
      return;
    }

    startInstallActivity();
  }

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    LogWrapper.getInstance().e(TAG + "onNewIntent() called! " + intent);
    Bundle extras = intent.getExtras();
    if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction())) {
      int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
      String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
      LogWrapper.getInstance().e(TAG + "Status: " + status);
      LogWrapper.getInstance().e(TAG + "Message: " + message);

      switch (status) {
        case PackageInstaller.STATUS_PENDING_USER_ACTION:
          // This test app isn't privileged, so the user has to confirm the install.
          Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
          confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(confirmIntent);
          break;

        case PackageInstaller.STATUS_SUCCESS:
          LogWrapper.getInstance().e(TAG + "Install succeeded!" + intent);
          break;

        case PackageInstaller.STATUS_FAILURE:
        case PackageInstaller.STATUS_FAILURE_ABORTED:
        case PackageInstaller.STATUS_FAILURE_BLOCKED:
        case PackageInstaller.STATUS_FAILURE_CONFLICT:
        case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
        case PackageInstaller.STATUS_FAILURE_INVALID:
        case PackageInstaller.STATUS_FAILURE_STORAGE:
          LogWrapper.getInstance().e(TAG + "Install failed! " + status + ", " + message);
          break;
        default:
          LogWrapper.getInstance().e(TAG + "Unrecognized status received from installer: " + status);
      }
    } else {
      LogWrapper.getInstance().e(TAG + "Unexpected intent with action: " + intent.getAction());
    }
  }

  private boolean isUnknownSourcesEnabled() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return this.getPackageManager().canRequestPackageInstalls();
    } else {
      try {
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS)
            == 1;
      } catch (Settings.SettingNotFoundException e) {
        LogWrapper.getInstance().e(TAG + "Unable to determine if unknown sources is enabled.", e);
        return true;
      }
    }
  }

  private void showUnknownSourcesUi() {
    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    alertDialog.setTitle(getString(R.string.unknown_sources_dialog_title));
    alertDialog.setMessage(getString(R.string.unknown_sources_dialog_description));
    alertDialog.setButton(
        AlertDialog.BUTTON_POSITIVE,
        getString(R.string.unknown_sources_yes_button),
        (dialogInterface, i) -> startActivity(getUnknownSourcesIntent()));
    alertDialog.setButton(
        AlertDialog.BUTTON_NEGATIVE,
        getString(R.string.update_no_button),
        (dialogInterface, i) -> dismissUnknownSourcesDialogCallback());
    alertDialog.setOnCancelListener(dialogInterface -> dismissUnknownSourcesDialogCallback());

    alertDialog.show();
  }

  private void dismissUnknownSourcesDialogCallback() {
    LogWrapper.getInstance().v(TAG + "Unknown sources dialog cancelled");
    finish();
  }

  private Intent getUnknownSourcesIntent() {
    Intent intent;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
      intent.setData(Uri.parse("package:" + getPackageName()));
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      LogWrapper.getInstance().v(TAG + "Starting unknown sources in new task");
    } else {
      intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    return intent;
  }

  private void startInstallActivity_v0() {
    installInProgress = true;
    Intent originalIntent = getIntent();
    String path = originalIntent.getStringExtra("INSTALL_PATH");
    Intent intent = new Intent(Intent.ACTION_VIEW);
    File apkFile = new File(path);
    String APK_MIME_TYPE = "application/vnd.android.package-archive";

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      Uri apkUri =
          FileProvider.getUriForFile(
              getApplicationContext(),
              getApplicationContext().getPackageName() + ".FirebaseAppDistributionFileProvider",
              apkFile);
      intent.setDataAndType(apkUri, APK_MIME_TYPE);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
      LogWrapper.getInstance().d("Requesting a vanilla URI");
      intent.setDataAndType(Uri.fromFile(apkFile), APK_MIME_TYPE);
    }

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    // intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    LogWrapper.getInstance().v("Kicking off install as new activity");
    startActivity(intent);
  }

  private void startInstallActivity_v1() {
    installInProgress = true;
    Intent originalIntent = getIntent();
    String path = originalIntent.getStringExtra("INSTALL_PATH");
    File apkFile = new File(path);
    String APK_MIME_TYPE = "application/vnd.android.package-archive";

    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      Uri apkUri =
          FileProvider.getUriForFile(
              getApplicationContext(),
              getApplicationContext().getPackageName() + ".FirebaseAppDistributionFileProvider",
              apkFile);
      intent.setDataAndType(apkUri, APK_MIME_TYPE);
      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
      LogWrapper.getInstance().d("Requesting a vanilla URI");
      intent.setDataAndType(Uri.fromFile(apkFile), APK_MIME_TYPE);
    }
    // intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
    // intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
    // intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getApplicationContext().getPackageName());
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  private void startInstallActivity() {
    installInProgress = true;
    Intent originalIntent = getIntent();
    String path = originalIntent.getStringExtra("INSTALL_PATH");
    File apkFile = new File(path);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      PackageInstaller.Session session = null;
      try {
        PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sessionId = packageInstaller.createSession(params);
        session = packageInstaller.openSession(sessionId);

        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite("package", 0, -1);
            InputStream is = new FileInputStream(apkFile)) {
          byte[] buffer = new byte[16384];
          int n;
          while ((n = is.read(buffer)) >= 0) {
            packageInSession.write(buffer, 0, n);
          }
        }

        // Create an install status receiver.
        Context context = InstallActivity.this;
        Intent intent = new Intent(context, InstallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(PACKAGE_INSTALLED_ACTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        IntentSender statusReceiver = pendingIntent.getIntentSender();

        // Commit the session (this will start the installation workflow).
        session.commit(statusReceiver);
      } catch (IOException e) {
        throw new RuntimeException("Couldn't install package", e);
      } catch (RuntimeException e) {
        if (session != null) {
          session.abandon();
        }
        throw e;
      }
    } else {
      LogWrapper.getInstance().d("Requesting a vanilla URI");
      Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
      String APK_MIME_TYPE = "application/vnd.android.package-archive";
      intent.setDataAndType(Uri.fromFile(apkFile), APK_MIME_TYPE);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }
  }

  @VisibleForTesting
  FirebaseAppDistribution getFirebaseAppDistributionInstance() {
    return FirebaseAppDistribution.getInstance();
  }
}
