package com.google.firebase.appdistribution;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.TaskCompletionSource;

public class SynchronizedTaskCompletionSource<TResult> extends TaskCompletionSource<TResult> {

  public SynchronizedTaskCompletionSource(CancellationToken token) {
    super(token);
  }

  @Override
  public synchronized void setResult(TResult var1) {
    super.setResult(var1);
  }

  @Override
  public synchronized void setException(@NonNull Exception var1) {
    super.setException(var1);
  }
}
