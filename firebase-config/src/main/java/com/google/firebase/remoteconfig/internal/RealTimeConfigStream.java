package com.google.firebase.remoteconfig.internal;


import com.google.protobuf.RpcChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.*;
import proto.generated.OpenFetchInvalidationStreamRequest;
import proto.generated.OpenFetchInvalidationStreamResponse;
import proto.generated.RealTimeRCServiceGrpc;

public class RealTimeConfigStream {

    private static final String HOST_NAME = "HOST_NAME";
    private static final int PORT_NUMBER = 123;
    private ManagedChannel managedChannel;
    private RealTimeRCServiceGrpc.RealTimeRCServiceStub asyncStub;


    public RealTimeConfigStream() {
        this.managedChannel
                = ManagedChannelBuilder.forAddress(HOST_NAME, PORT_NUMBER)
                .defaultServiceConfig(makeServiceConfig())
                .build();
        this.asyncStub = RealTimeRCServiceGrpc.newStub(this.managedChannel);
    }

    private Map<String, Object> makeServiceConfig() {
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", 5);
        retryPolicy.put("maxBackoff", "40s");
        retryPolicy.put("backoffMultiplier", 2);
        retryPolicy.put("initialBackoff", "30s");
        retryPolicy.put("retryableStatusCodes", Arrays.<Object>asList("UNAVAILABLE"));

        Map<String, Object> name = new HashMap();
        name.put("service", "RealTimeRemoteConfig.RealTimeRCService");

        Map<String, Object> methodConfig = new HashMap<>();
        methodConfig.put("retryPolicy", retryPolicy);

        HashMap<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("methodConfig", methodConfig);
        return serviceConfig;
    }

    public boolean startStream() {
        OpenFetchInvalidationStreamRequest request
                = OpenFetchInvalidationStreamRequest.newBuilder().build();
        this.asyncStub.openFetchInvalidationStream(request, new StreamObserver<OpenFetchInvalidationStreamResponse>() {

            @Override
            public void onNext(OpenFetchInvalidationStreamResponse openFetchInvalidationStreamResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }

        });

        return true;
    }

    public boolean endStream() {
        return true;
    }
}
