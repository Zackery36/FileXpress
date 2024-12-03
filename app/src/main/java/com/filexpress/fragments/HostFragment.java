package com.filexpress.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.filexpress.R;
import com.filexpress.managers.WebServerManager;
import com.filexpress.utils.NetworkUtils;
import com.filexpress.managers.ServerStateManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import android.graphics.Color;
import android.util.TypedValue;

public class HostFragment extends Fragment {

    private static final String TAG = "HostFragment";
    private WebServerManager webServerManager;
    private Button startStopServerButton;
    private TextView deviceIpTextView;
    private ImageView qrCodeImageView;

    public HostFragment() {
        // Default constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host, container, false);

        TextView statusTextView = view.findViewById(R.id.statusTextView);
        deviceIpTextView = view.findViewById(R.id.deviceIpTextView);
        qrCodeImageView = view.findViewById(R.id.qrCodeImageView);
        startStopServerButton = view.findViewById(R.id.startStopServerButton);

        // Initialize the WebServerManager
        webServerManager = new WebServerManager(requireContext(), statusTextView, startStopServerButton);

        // Set up the button to toggle the server state
        startStopServerButton.setOnClickListener(v -> {
            // Toggle server state
            webServerManager.toggleServer();

            // Ensure state updates propagate with a small delay
            new Handler(Looper.getMainLooper()).postDelayed(this::updateUI, 100);
        });

        // Display the device IP and update the UI based on server state
        displayDeviceIp();
        updateUI(); // Force UI update based on current server state

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensure the UI is updated when the fragment is resumed, in case the server state changed
        updateUI();
    }

    private void updateUI() {
        // Get the current server state
        boolean isRunning = ServerStateManager.getInstance().isServerRunning();

        // Update button text based on the server state
        startStopServerButton.setText(isRunning ? "Stop Web Server" : "Start Web Server");
        startStopServerButton.setEnabled(true); // Ensure button is enabled

        // Update QR code if the server is running
        if (isRunning) {
            String deviceIp = NetworkUtils.getDeviceIp(requireContext());
            if (deviceIp == null || deviceIp.equals("No Connection")) {
                Log.e(TAG, "Failed to fetch device IP for QR Code generation.");
                return;
            }
            String serverUrl = "http://" + deviceIp + ":8080";
            Bitmap qrCodeBitmap = generateQrCode(serverUrl);
            qrCodeImageView.setImageBitmap(qrCodeBitmap);
            qrCodeImageView.invalidate(); // Ensure QR code refreshes
        } else {
            qrCodeImageView.setImageBitmap(null); // Clear QR code when server stops
        }
    }

    private void displayDeviceIp() {
        String deviceIp = NetworkUtils.getDeviceIp(requireContext());
        deviceIpTextView.setText("IP Address: " + deviceIp);

        // Only generate QR code if the server is running
        if (ServerStateManager.getInstance().isServerRunning()) {
            String serverUrl = "http://" + deviceIp + ":8080";
            qrCodeImageView.setImageBitmap(generateQrCode(serverUrl));
        }
    }

    public Bitmap generateQrCode(String content) {
        try {
            // Retrieve current theme
            Context context = requireContext();

            TypedValue qrCodeColorValue = new TypedValue();
            TypedValue qrBackgroundColorValue = new TypedValue();

            context.getTheme().resolveAttribute(R.attr.qrCodeColor, qrCodeColorValue, true);
            context.getTheme().resolveAttribute(R.attr.qrBackgroundColor, qrBackgroundColorValue, true);

            int qrCodeColor = qrCodeColorValue.data;
            int qrBackgroundColor = qrBackgroundColorValue.data;

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 300, 300);

            return customizeQrCode(qrBitmap, qrCodeColor, qrBackgroundColor);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR Code: " + e.getMessage());
            return null;
        }
    }

    private Bitmap customizeQrCode(Bitmap qrBitmap, int foregroundColor, int backgroundColor) {
        int width = qrBitmap.getWidth();
        int height = qrBitmap.getHeight();

        Bitmap customBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = qrBitmap.getPixel(x, y);
                if (pixel == Color.BLACK) {
                    customBitmap.setPixel(x, y, foregroundColor);  // Change QR code color
                } else {
                    customBitmap.setPixel(x, y, backgroundColor);  // Transparent background
                }
            }
        }

        return customBitmap;
    }

}
