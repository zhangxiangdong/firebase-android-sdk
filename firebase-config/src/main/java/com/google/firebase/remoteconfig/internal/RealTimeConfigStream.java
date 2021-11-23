package com.google.firebase.remoteconfig.internal;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.*;

public class RealTimeConfigStream {

    private static final String HOST_NAME = "HOST_NAME";
    private static final int PORT_NUMBER = 123;
    private ManagedChannel managedChannel;


    public RealTimeConfigStream() {
        this.managedChannel
                = ManagedChannelBuilder.forAddress(HOST_NAME, PORT_NUMBER).build();
    }

    public boolean startStream() {
        return false;
    }
}
