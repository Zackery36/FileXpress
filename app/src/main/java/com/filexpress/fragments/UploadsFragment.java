package com.filexpress.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.filexpress.R;
import com.filexpress.managers.FileOperations;

import android.widget.ImageView;

public class UploadsFragment extends Fragment {

    private FileOperations fileOperations;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_uploads, container, false);

        LinearLayout uploadedFilesLayout = view.findViewById(R.id.uploadedFilesLayout);
        Button selectFileButton = view.findViewById(R.id.selectFileButton);

        fileOperations = new FileOperations(requireActivity(), view);

        fileOperations.displayExistingFiles();

        selectFileButton.setOnClickListener(v -> fileOperations.selectFile(1));
        setupCategoryToggle(view, R.id.imagesHeader, R.id.imagesLayout, R.id.imagesArrow);
        setupCategoryToggle(view, R.id.videosHeader, R.id.videosLayout, R.id.videosArrow);
        setupCategoryToggle(view, R.id.documentsHeader, R.id.documentsLayout, R.id.documentsArrow);
        setupCategoryToggle(view, R.id.applicationsHeader, R.id.applicationsLayout, R.id.applicationsArrow);
        setupCategoryToggle(view, R.id.othersHeader, R.id.othersLayout, R.id.othersArrow);

        return view;
    }
    private void setupCategoryToggle(View view, int headerId, int layoutId, int arrowId) {
        LinearLayout header = view.findViewById(headerId);
        LinearLayout layout = view.findViewById(layoutId);
        ImageView arrow = view.findViewById(arrowId);

        header.setOnClickListener(v -> {
            if (layout.getVisibility() == View.GONE) {
                layout.setVisibility(View.VISIBLE);
                arrow.setImageResource(R.drawable.ic_arrow_up);
            } else {
                layout.setVisibility(View.GONE);
                arrow.setImageResource(R.drawable.ic_arrow_down);
            }
        });
    }
}
