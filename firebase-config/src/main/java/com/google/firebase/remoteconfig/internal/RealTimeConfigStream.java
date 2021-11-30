package com.google.firebase.remoteconfig.internal;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ConnectivityState;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.*;
import proto.generated.OpenFetchInvalidationStreamRequest;
import proto.generated.OpenFetchInvalidationStreamResponse;
import proto.generated.RealTimeRCServiceGrpc;

public class RealTimeConfigStream {

    private static final String HOST_NAME = "localhost";
    private static final int PORT_NUMBER = 50051;
    private final ManagedChannel managedChannel;
    private RealTimeRCServiceGrpc.RealTimeRCServiceStub asyncStub;
    private Context.CancellableContext cancellableContext;
    private final ConfigFetchHandler fetchHandler;
    private long fetchVersion;
    private static final Logger logger = Logger.getLogger("Real_Time_RC");


    public RealTimeConfigStream(
            ConfigFetchHandler fetchHandler,
            long fetchVersion
    ) {
        this.managedChannel
                = ManagedChannelBuilder.forTarget("10.0.2.2:50051")
                .usePlaintext()
                .defaultServiceConfig(makeServiceConfig())
                .keepAliveWithoutCalls(true)
                .build();
        this.asyncStub = RealTimeRCServiceGrpc.newStub(this.managedChannel);
        this.fetchHandler = fetchHandler;
        this.fetchVersion = fetchVersion;
        this.cancellableContext = null;
    }

    // Create service config for stream.
    private Map<String, Object> makeServiceConfig() {
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", 5.0);
        retryPolicy.put("maxBackoff", "40s");
        retryPolicy.put("backoffMultiplier", 2.0);
        retryPolicy.put("initialBackoff", "30s");
        retryPolicy.put("retryableStatusCodes", Arrays.<Object>asList("UNAVAILABLE"));

        Map<String, Object> name = new HashMap();
        name.put("service", "RealTimeRemoteConfig.RealTimeRCService");

        Map<String, Object> methodConfig = new HashMap<>();
        methodConfig.put("retryPolicy", retryPolicy);

        HashMap<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("methodConfig", Arrays.<Object>asList(methodConfig));
        return serviceConfig;
    }

    // Starts async stream and configures stream observer that will handle actions on stream.
    public void startStream() throws RealTimeConfigStreamException {
        OpenFetchInvalidationStreamRequest request
                = OpenFetchInvalidationStreamRequest.newBuilder()
                .setLastKnownVersionNumber(this.fetchVersion)
                .build();
        logger.log(Level.INFO, "Real Time stream is being started");
        if (this.cancellableContext == null) {
            this.cancellableContext = Context.current().withCancellation();
        }
        try {
            this.cancellableContext.run(
                    new Runnable() {
                        @Override
                        public void run() {
                            asyncStub.openFetchInvalidationStream(request, getResponseStreamObserver());
                        }
                    }
            );
        } catch (Exception ex) {
            throw new RealTimeConfigStreamException("Can't start Real Time Stream.", ex);
        }
    }

    private StreamObserver<OpenFetchInvalidationStreamResponse> getResponseStreamObserver() {
        return new StreamObserver<OpenFetchInvalidationStreamResponse>() {

            @Override
            public void onNext(OpenFetchInvalidationStreamResponse openFetchInvalidationStreamResponse) {
                logger.log(Level.INFO, "Received invalidation signal. Fetching new Config.");
                // Fetch and cache response for future usage by developer.
                Task<ConfigFetchHandler.FetchResponse> fetchTask = fetchHandler.fetchIfNotThrottled();
                fetchTask.onSuccessTask((unusedFetchResponse) -> Tasks.forResult(null));
                logger.info("Finished Fetching new updates.");
                // Update fetch version based on response
            }

            @Override
            public void onError(Throwable throwable) {
                // Log Exception being thrown
                logger.log(Level.WARNING, "Real Time Stream is closing. Regular Remote Config is still functional." +
                        "Please restart app to restart stream. Message: " + throwable.toString(), throwable.getCause());
            }

            @Override
            public void onCompleted() {
                // Gently close stream if closed from server side
                logger.log(Level.INFO, "Real Time stream has closed from the server side");
                cancellableContext.cancel(null);
            }
        };
    }

    // Immediately end stream connection.
    public void endStreamConnection() throws RealTimeConfigStreamException {
        try {
            if (this.cancellableContext != null) {
                this.cancellableContext.cancel(null);
                this.cancellableContext = null;
            }
        } catch (Exception ex) {
            throw new RealTimeConfigStreamException("Can't close stream connection.", ex);
        }
    }

    public void endStreamChannel() throws RealTimeConfigStreamException {
        try {
            if (this.managedChannel != null) {
                this.managedChannel.shutdownNow();
            }
        } catch (Exception ex) {
            throw new RealTimeConfigStreamException("Can't close stream channel.", ex);
        }
    }

    public ConnectivityState getStreamState() {
        if (this.managedChannel != null) {
            return this.managedChannel.getState(false);
        }
        return ConnectivityState.SHUTDOWN;
    }
}
