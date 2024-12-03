package com.filexpress.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.filexpress.R;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private List<File> files;
    private final OnFileSelectedListener onFileSelectedListener;
    private final Context context;

    public interface OnFileSelectedListener {
        void onFileSelected(File file);
    }

    public FileAdapter(List<File> files, OnFileSelectedListener onFileSelectedListener, Context context) {
        this.files = files;
        this.onFileSelectedListener = onFileSelectedListener;
        this.context = context;
    }

    public void updateFiles(List<File> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = files.get(position);

        // Set file name with truncation
        holder.fileName.setText(file.getName());
        holder.fileName.setEllipsize(TextUtils.TruncateAt.END); // Add ellipsis
        holder.fileName.setMaxLines(1); // Truncate after one line

        // Determine file type and set preview accordingly
        if (isImageFile(file)) {
            Glide.with(context)
                    .load(file)
                    .apply(new RequestOptions().override(100, 100)) // Lower resolution for optimization
                    .placeholder(R.drawable.ic_image)
                    .into(holder.filePreview);
        } else if (isVideoFile(file)) {
            Glide.with(context)
                    .asBitmap()
                    .load(file)
                    .apply(new RequestOptions().override(100, 100)) // Lower resolution for optimization
                    .placeholder(R.drawable.ic_video)
                    .into(holder.filePreview);
        } else if (isDocumentFile(file)) {
            holder.filePreview.setImageResource(R.drawable.ic_document);
        } else if (file.isDirectory()) {
            holder.filePreview.setImageResource(R.drawable.ic_folder);
        } else {
            holder.filePreview.setImageResource(R.drawable.ic_file);
        }

        // Handle checkbox toggle
        holder.fileCheckBox.setOnCheckedChangeListener(null); // Prevent triggering listener on bind
        holder.fileCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onFileSelectedListener.onFileSelected(file);
        });

        // Handle click events
        holder.itemView.setOnClickListener(v -> holder.fileCheckBox.toggle()); // Toggle checkbox manually
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        ImageView filePreview;
        CheckBox fileCheckBox;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            filePreview = itemView.findViewById(R.id.filePreview);
            fileCheckBox = itemView.findViewById(R.id.fileCheckBox);

            // Set circular shape for checkbox
            fileCheckBox.setButtonDrawable(R.drawable.circular_checkbox); // Custom circular checkbox drawable
        }
    }

    private boolean isImageFile(File file) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        return endsWithAny(file.getName().toLowerCase(), imageExtensions);
    }

    private boolean isVideoFile(File file) {
        String[] videoExtensions = {".mp4", ".mkv", ".avi", ".mov", ".flv"};
        return endsWithAny(file.getName().toLowerCase(), videoExtensions);
    }

    private boolean isDocumentFile(File file) {
        String[] documentExtensions = {".pdf", ".doc", ".docx", ".txt", ".xls", ".xlsx", ".ppt", ".pptx"};
        return endsWithAny(file.getName().toLowerCase(), documentExtensions);
    }

    private boolean endsWithAny(String name, String[] extensions) {
        for (String ext : extensions) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }
}
