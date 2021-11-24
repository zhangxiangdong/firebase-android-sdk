package com.google.firebase.remoteconfig.internal;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.*;
import proto.generated.OpenFetchInvalidationStreamRequest;
import proto.generated.OpenFetchInvalidationStreamResponse;
import proto.generated.RealTimeRCServiceGrpc;

public class RealTimeConfigStream {

    private static final String HOST_NAME = "HOST_NAME";
    private static final int PORT_NUMBER = 123;
    private final ManagedChannel managedChannel;
    private RealTimeRCServiceGrpc.RealTimeRCServiceStub asyncStub;
    private final ConfigFetchHandler fetchHandler;
    private long fetchVersion;
    private static final Logger logger = Logger.getLogger("Real_Time_RC");


    public RealTimeConfigStream(
            ConfigFetchHandler fetchHandler,
            long fetchVersion
    ) {
        this.managedChannel
                = ManagedChannelBuilder.forAddress(HOST_NAME, PORT_NUMBER)
                .defaultServiceConfig(makeServiceConfig())
                .build();
        this.asyncStub = RealTimeRCServiceGrpc.newStub(this.managedChannel);
        this.fetchHandler = fetchHandler;
        this.fetchVersion = fetchVersion;
    }

    // Create service config for stream.
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

    // Starts async stream and configures stream observer that will handle actions on stream.
    public void startStream() {
        OpenFetchInvalidationStreamRequest request
                = OpenFetchInvalidationStreamRequest.newBuilder()
                .setLastKnownVersionNumber(this.fetchVersion)
                .build();
        this.asyncStub.openFetchInvalidationStream(request, getResponseStreamObserver());
    }

    private StreamObserver<OpenFetchInvalidationStreamResponse> getResponseStreamObserver() {
        return new StreamObserver<OpenFetchInvalidationStreamResponse>() {

            @Override
            public void onNext(OpenFetchInvalidationStreamResponse openFetchInvalidationStreamResponse) {
                // Fetch and cache response for future usage by developer.
                Task<ConfigFetchHandler.FetchResponse> fetchTask = fetchHandler.fetchIfNotThrottled();
                fetchTask.onSuccessTask((unusedFetchResponse) -> Tasks.forResult(null));
                // Update fetch version based on response
            }

            @Override
            public void onError(Throwable throwable) {
                // Log Exception being thrown
                logger.log(Level.WARNING, "Real Time Stream has failed. Regular RC still functional." +
                        "Please restart app to restart stream. Status: {0}", throwable.getCause());
            }

            @Override
            public void onCompleted() {
                // Gently close stream if closed from server side
                logger.log(Level.INFO, "Real Time stream has closed from the server side");
                managedChannel.shutdown();
            }
        };
    }

    // Immediately end all streams.
    public void endStream() throws RealTimeConfigStreamException {
        try {
            this.managedChannel.shutdownNow();
        } catch (Exception e) {
            throw new RealTimeConfigStreamException("Can't close stream.", e);
        }
    }

    public ConnectivityState getStreamState() {
        return this.managedChannel.getState(false);
    }
}
