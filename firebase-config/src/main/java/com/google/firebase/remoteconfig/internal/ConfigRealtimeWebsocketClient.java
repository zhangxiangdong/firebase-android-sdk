package com.google.firebase.remoteconfig.internal;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.net.URI;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

@ClientEndpoint
public class ConfigRealtimeWebsocketClient {
    private Session clientSession;
    private MessageHandler messageHandler;
    private final Map<String, RealTimeEventListener> eventListeners;
    private final ConfigFetchHandler configFetchHandler;
    private static final Logger logger = Logger.getLogger("Real_Time_RC");
    private final String ENDPOINT = "10.0.2.2::50051";
    private final WebSocketContainer webSocketContainer;

    // Retry constants
    private final Timer timer;
    private final int ORIGINAL_RETRIES = 5;
    private final long RETRY_TIME = 10000;
    private long RETRY_MULTIPLIER;
    private int RETRIES_REMAINING;


    public ConfigRealtimeWebsocketClient(ConfigFetchHandler configFetchHandler) {
        eventListeners = new HashMap<>();
        this.configFetchHandler = configFetchHandler;
        this.webSocketContainer = ContainerProvider.getWebSocketContainer();
        this.clientSession = null;
        this.RETRY_MULTIPLIER = new Random(10).nextLong();
        this.RETRIES_REMAINING = this.ORIGINAL_RETRIES;
        this.timer = new Timer();
    }

    public void startRealtime() {
        if (this.clientSession == null) {
            try {
                logger.info("Starting Realtime RC.");
                this.clientSession = this.webSocketContainer.connectToServer(this, new URI(this.ENDPOINT));
                this.RETRIES_REMAINING = this.ORIGINAL_RETRIES;
                this.RETRY_MULTIPLIER = new Random(10).nextLong();
            } catch (Exception ex) {
                logger.info("Failed to start Realtime RC.");
            }
        }
    }

    public void stopRealtime() {
        if (this.clientSession != null) {
            try {
                logger.info("Closing Realtime RC.");
                CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client closed.");
                this.clientSession.close(closeReason);
            } catch (Exception ex) {
                logger.info("Failed to close Realtime RC.");
            }
        }
    }

    @OnOpen
    private void onOpen() {
        logger.info("Realtime stream is open.");
    }

    @OnClose
    private void onClose(Session session, CloseReason closeReason) {
        this.clientSession = null;
        logger.info("Realtime stream is closed.");
    }

    @OnError
    private void onError(CloseReason closeReason) {
        logger.info("Realtime RC failed for reason: " + closeReason.toString());
        if (closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE && RETRIES_REMAINING > 0) {
            RETRIES_REMAINING--;
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.info("Retrying Realtime connection.");
                    startRealtime();
                }
            }, this.RETRY_TIME * this.RETRY_MULTIPLIER);
        }
    }

    @OnMessage
    private void onMessage(String message) {
        logger.info("Notification received from RC.");
        Task<ConfigFetchHandler.FetchResponse> fetchTask = this.configFetchHandler.fetch(0L);
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
