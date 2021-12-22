package com.google.firebase.remoteconfig.internal;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import io.grpc.ConnectivityState;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.*;
import proto.generated.OpenFetchInvalidationStreamRequest;
import proto.generated.OpenFetchInvalidationStreamResponse;
import proto.generated.RealTimeRCServiceGrpc;

public class RealTimeConfigStream {

    private final String HOST_NAME = "10.0.2.2";
    private final int PORT_NUMBER = 50051;
    private ManagedChannel managedChannel;
    private RealTimeRCServiceGrpc.RealTimeRCServiceStub asyncStub;
    private Context.CancellableContext cancellableContext;
    private final ConfigFetchHandler fetchHandler;
    private static final Logger logger = Logger.getLogger("Real_Time_RC");
    private final Map<String, RealTimeEventListener> eventListeners;
    private final double backoffMultiplier;

    public RealTimeConfigStream(
            ConfigFetchHandler fetchHandler
    ) {
        this.backoffMultiplier = new Random(10).nextDouble() + 1D;
        this.managedChannel = getManagedChannel();
        this.asyncStub = RealTimeRCServiceGrpc.newStub(this.managedChannel);
        this.fetchHandler = fetchHandler;
        this.cancellableContext = null;
        this.eventListeners = new HashMap<>();
    }

    private ManagedChannel getManagedChannel() {
        return ManagedChannelBuilder
                .forAddress(this.HOST_NAME, this.PORT_NUMBER)
                .usePlaintext()
                .enableRetry()
                .defaultServiceConfig(makeServiceConfig())
                .keepAliveWithoutCalls(true)
                .keepAliveTime(3, TimeUnit.HOURS)
                .build();
    }

    // Create service config for stream.
    private Map<String, Object> makeServiceConfig() {
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", 5D);
        retryPolicy.put("maxBackoff", "400s");
        retryPolicy.put("backoffMultiplier", this.backoffMultiplier);
        retryPolicy.put("initialBackoff", "30s");
        retryPolicy.put("retryableStatusCodes", Arrays.<Object>asList("UNAVAILABLE"));

        Map<String, Object> name = new HashMap();
        name.put("service", "remoteconfig.RealTimeRCService");

        Map<String, Object> methodConfig = new HashMap<>();
        methodConfig.put("retryPolicy", retryPolicy);
        methodConfig.put("name", Collections.<Object>singletonList(name));

        HashMap<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("methodConfig", Arrays.<Object>asList(methodConfig));
        return serviceConfig;
    }

    // Starts async stream and configures stream observer that will handle actions on stream.
    public void startStream(@Nullable long fetchVersion) throws RealTimeConfigStreamException {
        logger.log(Level.INFO, "Real Time stream is being started");

        // Only start stream if there are listeners
        if (this.eventListeners.isEmpty()) {
            return;
        }
        // Check if context or channel have been closed and issue new resources if closed.
        if (this.cancellableContext == null) {
            this.cancellableContext = Context.current().withCancellation();
        }
        if (this.managedChannel.isShutdown() || this.managedChannel.isTerminated()) {
            this.managedChannel = getManagedChannel();
            this.asyncStub = RealTimeRCServiceGrpc.newStub(this.managedChannel);
        }

        // Create request.
        OpenFetchInvalidationStreamRequest request
                = OpenFetchInvalidationStreamRequest.newBuilder()
                .setLastKnownVersionNumber(1L)
                .build();
        try {
            // Wrap gRPC stream request in context to allow for graceful closing.
            this.cancellableContext.run(
                    new Runnable() {
                        @Override
                        public void run() {
                            // Call to open async gRPC stream.
                            asyncStub.openFetchInvalidationStream(request, getResponseStreamObserver());
                        }
                    }
            );
        } catch (Exception ex) {
            throw new RealTimeConfigStreamException("Can't start Real Time Stream.", ex);
        }
    }

    // Returns stream observer that directs how the stream should handle different scenarios.
    private StreamObserver<OpenFetchInvalidationStreamResponse> getResponseStreamObserver() {
        return new StreamObserver<OpenFetchInvalidationStreamResponse>() {

            // What to do on every successful stream response.
            @Override
            public void onNext(OpenFetchInvalidationStreamResponse openFetchInvalidationStreamResponse) {
                logger.log(Level.INFO, "Received invalidation signal. Fetching new Config.");
                // Fetch and cache response for future usage by developer.
                Task<ConfigFetchHandler.FetchResponse> fetchTask = fetchHandler.fetchIfNotThrottled();

                // Possibly add loop so that new config update version is fetched and not an old one
                /**
                 * int curVersion = 1;
                 * while (curVersion == oldVersion) {
                 *  Task<ConfigFetchHandler.FetchResponse> fetchTask = fetchHandler.fetchIfNotThrottled();
                 *  fetchTask.onSuccessTask((unusedFetchResponse) ->
                 *      {
                 *          curVersion = unusedFetchResponse.versionNumber;
                 *          if (curVersion > oldVersion) {
                 *              // do event listener stuff
                 *          }
                 *      }
                 *  );
                 *
                 * }
                 * */
                fetchTask.onSuccessTask((unusedFetchResponse) ->
                        {
                            logger.info("Finished Fetching new updates.");
                            // Execute callbacks for listeners.
                            for (RealTimeEventListener listener : eventListeners.values()) {
                                listener.onEvent();
                            }
                            return Tasks.forResult(null);
                        }
                );
            }

            // What to do on stream errors.
            @Override
            public void onError(Throwable throwable) {
                // Log Exception being thrown
                logger.log(Level.WARNING, "Real Time Stream is closing. Regular Remote Config is still functional." +
                        "Please restart app to open stream again. Message: " + throwable.toString(), throwable.getCause());
            }

            // What to do when stream is closed.
            @Override
            public void onCompleted() {
                // Gently close stream if closed from server side.
                logger.log(Level.INFO, "Real Time stream has closed from the server side");
                cancellableContext.cancel(null);
            }
        };
    }

    // Immediately end stream connection. Allows for easy reopening without creating new resources.
    public void pauseStreamConnection() throws RealTimeConfigStreamException {
        logger.info("Closing stream connections.");
        try {
            if (this.cancellableContext != null) {
                this.cancellableContext.cancel(null);
                this.cancellableContext = null;
            }
        } catch (Exception ex) {
            throw new RealTimeConfigStreamException("Can't close stream connection.", ex);
        }
    }

    // Permanent closing of gRPC stream. Closes channel and will require new channel and stub to reopen stream.
    public void endStreamConnection() throws RealTimeConfigStreamException {
        logger.info("Closing stream channel.");
        try {
            if (this.managedChannel != null) {
                this.managedChannel.shutdownNow();
            }
        } catch (Exception ex) {
            throw new RealTimeConfigStreamException("Can't close stream channel.", ex);
        }
    }

    // Retrieve state of current channel/stream.
    public ConnectivityState getStreamState() {
        if (this.managedChannel != null) {
            return this.managedChannel.getState(false);
        }
        return ConnectivityState.SHUTDOWN;
    }

    // Event Listener interface to be used by developers.
    public interface RealTimeEventListener extends EventListener {
        // Call back for when Real Time signal occurs.
        void onEvent();
    }

    // Add Event listener.
    public void putRealTimeEventListener(String listenerName, RealTimeEventListener realTimeEventListener) {
        this.eventListeners.put(listenerName, realTimeEventListener);
    }

    // Remove Event listener.
    public void removeRealTimeEventListener(String listenerName) {
        this.eventListeners.remove(listenerName);
    }

    // Remove all Event listeners.
    public void clearAllRealTimeEventListeners() {
        this.eventListeners.clear();
    }
}
