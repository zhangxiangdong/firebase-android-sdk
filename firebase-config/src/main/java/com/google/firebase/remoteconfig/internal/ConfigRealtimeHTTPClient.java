package com.google.firebase.remoteconfig.internal;

import android.os.AsyncTask;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class ConfigRealtimeHTTPClient extends AsyncTask<String, Void, Void> {

    private static final String REALTIME_URL_STRING = "localhost:1023";
    private final Map<String, RealTimeEventListener> eventListeners;

    private static final Logger logger = Logger.getLogger("Real_Time_RC");
    private final ConfigFetchHandler configFetchHandler;
    private URL realtimeURL;
    private HttpURLConnection httpURLConnection;

    // Retry parameters
    private final Timer timer;
    private final int ORIGINAL_RETRIES = 7;
    private final long RETRY_TIME = 10000;
    private long RETRY_MULTIPLIER;
    private int RETRIES_REMAINING;
    private final Random random;

    public ConfigRealtimeHTTPClient(ConfigFetchHandler configFetchHandler) {
        this.configFetchHandler = configFetchHandler;
        this.realtimeURL = null;
        this.httpURLConnection = null;
        this.eventListeners = new HashMap<>();
        this.timer = new Timer();
        this.random = new Random();
        this.RETRIES_REMAINING = this.ORIGINAL_RETRIES;

        try {
            this.realtimeURL = new URL(this.REALTIME_URL_STRING);
        } catch (MalformedURLException ex) {
            logger.info("Bad URL.");
        }
    }

    public void startHTTPConnection() {
        if (this.httpURLConnection == null) {
            try {
                this.httpURLConnection = (HttpURLConnection) this.realtimeURL.openConnection();
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setFixedLengthStreamingMode(1024);

                this.RETRIES_REMAINING = this.ORIGINAL_RETRIES;
                this.RETRY_MULTIPLIER = this.random.nextInt(10) + 1;
                logger.info("Realtime started");
            } catch (IOException ex) {
                logger.info("Could not open connection.");
                this.retryHTTPConnection();
            }
        }
    }

    public void stopHttpConnection() {
        if (this.httpURLConnection != null) {
            this.httpURLConnection.disconnect();
            logger.info("Realtime stopped");
        }
    }

    @Override
    protected Void doInBackground(String... strings) {
        if (this.httpURLConnection != null) {
            try {
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == 200) {
                    InputStream inputStream = httpURLConnection.getInputStream();
                    handleMessages(inputStream);
                } else {
                    logger.info("Can't open Realtime stream");
                    this.stopHttpConnection();
                    retryHTTPConnection();
                }

            } catch (IOException ex) {
                logger.info("Error handling messages.");
            }
        }
        return null;
    }

    private void handleMessages(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader((new InputStreamReader(inputStream)));
        while (reader.readLine() != null) {
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
        reader.close();
    }

    private void retryHTTPConnection() {
        if (this.RETRIES_REMAINING > 0) {
            RETRIES_REMAINING--;
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.info("Retrying Realtime connection.");
                    startHTTPConnection();
                }
            }, (this.RETRY_TIME * this.RETRY_MULTIPLIER));
        } else {
            logger.info("No retries remaining. Restart app.");
        }
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
