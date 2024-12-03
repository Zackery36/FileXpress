package com.filexpress.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.filexpress.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectedFilesManager {

    private static SelectedFilesManager instance;
    private LinearLayout selectedFilesLayout;
    private Context context;
    private List<File> selectedFiles = new ArrayList<>();
    private FileBrowserActivity fileBrowser;

    private SelectedFilesManager(Context context, LinearLayout layout) {
        this.context = context;
        this.selectedFilesLayout = layout;
    }

    public static SelectedFilesManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SelectedFilesManager not initialized!");
        }
        return instance;
    }

    public static void initialize(Context context, LinearLayout layout) {
        if (instance == null) {
            instance = new SelectedFilesManager(context, layout);
        }
    }

    public void toggleSelectedFilesLayout() {
        updateSelectedFilesLayout();
    }

    public void addSelectedFile(File file) {
        if (!selectedFiles.contains(file)) {
            selectedFiles.add(file);
            updateSelectedFilesLayout();
        }
    }

    public void unselectFile(File file) {
        selectedFiles.remove(file);
        updateSelectedFilesLayout();
    }

    public void updateSelectedFilesLayout() {
        selectedFilesLayout.removeAllViews();
        for (File file : selectedFiles) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_selected_file, selectedFilesLayout, false);

            TextView fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            fileNameTextView.setText(file.getName());

            ImageView removeImageView = itemView.findViewById(R.id.removeImageView);
            removeImageView.setOnClickListener(v -> {
                unselectFile(file);
                FileBrowserActivity fileBrowserActivity = (FileBrowserActivity) context;
                fileBrowserActivity.updateSelectedFilesText();
            });

            selectedFilesLayout.addView(itemView);
        }
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    public void reinitializeSelectedFiles() {
        selectedFiles.clear();
        updateSelectedFilesLayout();
    }
}
