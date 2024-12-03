package com.filexpress.managers;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.filexpress.MainActivity;
import com.filexpress.utils.NetworkUtils;

import java.io.File;
import android.content.SharedPreferences;
import java.io.IOException;

public class WebServerManager {

    private final Context context;
    private final TextView statusTextView;
    private final Button startStopButton;
    private SimpleWebServer webServer;

    public WebServerManager(Context context, TextView statusTextView, Button startStopButton) {
        this.context = context;
        this.statusTextView = statusTextView;
        this.startStopButton = startStopButton;

        // Synchronize the button text with the server state
        updateButtonText();
    }

    public void toggleServer() {
        if (ServerStateManager.getInstance().isServerRunning()) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        String deviceIp = NetworkUtils.getDeviceIp(context);
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {

            // Retrieve the saved port from SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            int savedPort = sharedPreferences.getInt("selected_port", 8080); // Default port is 8080

            // Set the server state early to reflect changes immediately
            ServerStateManager.getInstance().setServerRunning(true);

            // Start the web server
            webServer = new SimpleWebServer(savedPort, externalFilesDir);
            new Thread(() -> {
                try {
                    webServer.startServer();
                    updateUI("Server running at http://" + deviceIp + ":" + savedPort, "Web server started", "Stop Web Server");
                } catch (IOException e) {
                    ServerStateManager.getInstance().setServerRunning(false); // Revert state on failure
                    updateUI("Error starting server", "Error starting server", "Start Web Server");
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void stopServer() {
        if (webServer != null) {
            ServerStateManager.getInstance().setServerRunning(false); // Set state early
            webServer.stopServer();
            updateUI("Server stopped.", "Web server stopped", "Start Web Server");
        }
    }

    private void updateUI(String statusMessage, String toastMessage, String buttonText) {
        // Update the UI from the main thread
        ((MainActivity) context).runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText(statusMessage);
            }
            if (startStopButton != null) {
                startStopButton.setText(buttonText);
                startStopButton.setEnabled(true); // Ensure button is enabled
            }
            if (toastMessage != null) {
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateButtonText() {
        // Update the button text based on the server state
        ((MainActivity) context).runOnUiThread(() -> {
            if (startStopButton != null) {
                startStopButton.setText(ServerStateManager.getInstance().isServerRunning() ? "Stop Web Server" : "Start Web Server");
            }
        });
    }
}
