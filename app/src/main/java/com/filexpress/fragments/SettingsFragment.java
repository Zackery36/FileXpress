package com.filexpress.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.filexpress.R;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private EditText portEditText;
    private Spinner themeSpinner;
    private Switch darkModeSwitch;
    private Button saveButton;

    private static final String[] themes = {"Default", "Blue", "Green"};

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences("app_settings", getContext().MODE_PRIVATE);

        portEditText = rootView.findViewById(R.id.editTextPort);
        themeSpinner = rootView.findViewById(R.id.spinnerTheme);
        darkModeSwitch = rootView.findViewById(R.id.switchDarkMode);
        saveButton = rootView.findViewById(R.id.buttonSave);

        int savedPort = sharedPreferences.getInt("selected_port", 8080);
        portEditText.setText(String.valueOf(savedPort));

        int savedThemeIndex = sharedPreferences.getInt("theme_index", 0);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, themes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);
        themeSpinner.setSelection(savedThemeIndex);

        boolean isDarkModeEnabled = sharedPreferences.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkModeEnabled);

        saveButton.setOnClickListener(v -> {
            try {
                int port = Integer.parseInt(portEditText.getText().toString());
                int selectedThemeIndex = themeSpinner.getSelectedItemPosition();
                boolean darkModeEnabled = darkModeSwitch.isChecked();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("selected_port", port);
                editor.putInt("theme_index", selectedThemeIndex);
                editor.putBoolean("dark_mode", darkModeEnabled);
                editor.apply();

                Toast.makeText(getContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show();

                applyTheme(selectedThemeIndex);
                applyDarkMode(darkModeEnabled);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid port number", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    private void applyTheme(int themeIndex) {
        switch (themeIndex) {
            case 1:
                getActivity().setTheme(R.style.AppTheme_OceanBreeze);
                break;
            case 2:
                getActivity().setTheme(R.style.AppTheme_ForestGlade);
                break;
            default:
                getActivity().setTheme(R.style.AppTheme_Akatsuki);
                break;
        }
        getActivity().recreate();
    }

    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
