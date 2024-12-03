package com.filexpress;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.filexpress.fragments.HomeFragment;
import com.filexpress.fragments.HostFragment;
import com.filexpress.fragments.SettingsFragment;
import com.filexpress.fragments.UploadsFragment;
import com.filexpress.managers.FileOperations;
import com.filexpress.managers.WebServerManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1;
    private WebServerManager webServerManager;
    private FileOperations fileOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply the selected theme
        applySelectedTheme();

        setContentView(R.layout.activity_main);

        // Initialize file operations
        fileOperations = new FileOperations(this, null);

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.menu_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.menu_host) {
                selectedFragment = new HostFragment();
            } else if (item.getItemId() == R.id.menu_uploads) {
                selectedFragment = new UploadsFragment();
            } else if (item.getItemId() == R.id.menu_settings) { // Add this for Settings
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Load the default fragment (Home) on the first launch
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.menu_home);
        }
    }

    private void applySelectedTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        int themeIndex = sharedPreferences.getInt("theme_index", 0);

        switch (themeIndex) {
            case 1: // Ocean Breeze
                setTheme(R.style.AppTheme_OceanBreeze);
                break;
            case 2: // Forest Glade
                setTheme(R.style.AppTheme_ForestGlade);
                break;
            default: // Akatsuki (Default)
                setTheme(R.style.AppTheme_Akatsuki);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle file selection if multiple files are selected
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            // Get a list of selected file URIs (or paths, depending on how the files are returned)
            List<Uri> fileUris = getFileUrisFromIntent(data);

            // Now, pass these file URIs to the file operations
            if (fileUris != null && !fileUris.isEmpty()) {
                fileOperations.handleMultipleFileSelection(fileUris);
            }
        }
    }
    private List<Uri> getFileUrisFromIntent(Intent data) {
        List<Uri> uris = new ArrayList<>();

        // Check if the intent contains multiple file URIs
        if (data != null) {
            ClipData clipData = data.getClipData();

            if (clipData != null) {
                // Handle multiple selected files (ClipData contains multiple URIs)
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri fileUri = clipData.getItemAt(i).getUri();
                    uris.add(fileUri);
                }
            } else {
                // If only one file is selected, it will be in data.getData()
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    uris.add(fileUri);
                }
            }
        }

        return uris;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webServerManager != null) {
            webServerManager.stopServer();
        }
    }
}
