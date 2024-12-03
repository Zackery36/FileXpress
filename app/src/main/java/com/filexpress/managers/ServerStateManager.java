package com.filexpress.managers;

public class ServerStateManager {
    private static ServerStateManager instance;
    private boolean isServerRunning;

    private ServerStateManager() {
        // Default state
        isServerRunning = false;
    }

    public static synchronized ServerStateManager getInstance() {
        if (instance == null) {
            instance = new ServerStateManager();
        }
        return instance;
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    public void setServerRunning(boolean isRunning) {
        this.isServerRunning = isRunning;
    }
}
