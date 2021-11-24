package com.google.firebase.remoteconfig.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.remoteconfig.FirebaseRemoteConfigException;

public class RealTimeConfigStreamException extends FirebaseRemoteConfigException {

    public RealTimeConfigStreamException(@NonNull String detailMessage) {
        super(detailMessage);
    }

    public RealTimeConfigStreamException(
            @NonNull String detailMessage, @Nullable Throwable cause
    ) {
        super(detailMessage, cause);
    }
}
