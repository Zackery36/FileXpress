package com.filexpress.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.filexpress.R;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView introText = view.findViewById(R.id.introText);
        introText.setText("Welcome to RFileShare, your ultimate file sharing app. Navigate through the menu to explore features!");

        TextView featureText = view.findViewById(R.id.featureText);
        featureText.setText("• Quick file sharing\n• Secure transfers\n• Seamless navigation\n• Multi-platform support");

        TextView footerText = view.findViewById(R.id.footerText);
        footerText.setText("Coded by Zackery36 © 2024");

        return view;
    }
}
