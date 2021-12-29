package com.google.firebase.remoteconfig.internal;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.net.URI;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
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
    private WebSocketContainer webSocketContainer;

    public ConfigRealtimeWebsocketClient(ConfigFetchHandler configFetchHandler) {
        eventListeners = new HashMap<>();
        this.configFetchHandler = configFetchHandler;
        this.webSocketContainer = ContainerProvider.getWebSocketContainer();
        this.clientSession = null;
    }

    public void startRealtime() {
        if (this.clientSession == null) {
            try {
                this.clientSession = this.webSocketContainer.connectToServer(this, new URI(this.ENDPOINT));
            } catch (Exception ex) {

            }
        }
    }

    public void stopRealtime() {
        if (this.clientSession != null) {
            try {
                this.clientSession.close();
            } catch (Exception ex) {

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
    }

    @OnError
    private void onError() {

    }

    @OnMessage
    private void onMessage(String message) {
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
