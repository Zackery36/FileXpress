package com.filexpress.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filexpress.R;

import java.io.File;
import java.util.List;

public class SelectedFilesAdapter extends RecyclerView.Adapter<SelectedFilesAdapter.ViewHolder> {

    private final List<File> selectedFiles;
    private final OnFileUnselectListener onFileUnselectListener;


    public SelectedFilesAdapter(List<File> selectedFiles, OnFileUnselectListener listener) {
        this.selectedFiles = selectedFiles;
        this.onFileUnselectListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = selectedFiles.get(position);
        holder.fileNameTextView.setText(file.getName());

        holder.removeImageView.setOnClickListener(v -> onFileUnselectListener.onUnselect(file));
    }

    @Override
    public int getItemCount() {
        return selectedFiles.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        ImageView removeImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            removeImageView = itemView.findViewById(R.id.removeImageView);
        }
    }

    public interface OnFileUnselectListener {
        void onUnselect(File file);
    }
}
