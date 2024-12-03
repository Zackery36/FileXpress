package com.filexpress.managers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.filexpress.R;
import com.filexpress.ui.FileBrowserActivity;
import com.filexpress.utils.NetworkUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class FileOperations {

    private final Activity activity; // Use Activity for context-specific actions
    private View rootView;
    private ProgressDialog progressDialog;
    private Queue<Uri> fileQueue = new LinkedList<>();
    private boolean isProcessing = false;
    public FileOperations(Activity activity, View rootView) {
        this.activity = activity;
        this.rootView = rootView;
    }

    // Launch the file picker
    public void selectFile(int requestCode) {
        // Check for permissions first
        if (hasRequiredPermissions()) {
            // Launch the FileBrowserActivity if permissions are granted
            Intent intent = new Intent(activity, FileBrowserActivity.class);
            intent.putExtra("requestCode", requestCode);
            activity.startActivityForResult(intent, requestCode);
        } else {
            // Request permissions if not granted
            requestMediaPermissions();
        }
    }

    // Helper method to check if permissions are granted
    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // Permissions are not required for older versions
        }
    }

    // Request permissions if not granted
    private void requestMediaPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, 1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }



    // This method processes multiple files at once
    public void handleMultipleFileSelection(List<Uri> fileUris) {
        fileQueue.addAll(fileUris);  // Add all selected files to the queue
        processNextFile();           // Start processing
    }

    private void processNextFile() {
        if (isProcessing || fileQueue.isEmpty()) return;

        isProcessing = true;
        Uri currentFileUri = fileQueue.poll();

        if (currentFileUri != null) {
            Log.d("FileOperations", "Processing file: " + currentFileUri.toString());

            // Simulate file handling and uploading logic
            saveFileToExternalStorage(currentFileUri, new FileProcessCallback() {
                @Override
                public void onComplete() {
                    isProcessing = false;
                    processNextFile();  // Process the next file in the queue
                }

                @Override
                public void onError(Exception e) {
                    isProcessing = false;
                    Log.e("FileOperations", "Error processing file: " + e.getMessage());
                    processNextFile();  // Continue with the next file even if an error occurs
                }
            });
        }
    }
    public interface FileProcessCallback {
        void onComplete();
        void onError(Exception e);
    }




    // Save the selected file to external storage in the appropriate folder
    // Save the selected file to external storage in the appropriate folder
    private void saveFileToExternalStorage(Uri fileUri, FileProcessCallback callback) {
        new Thread(() -> {
            try {
                String fileName = getFileName(fileUri);
                InputStream inputStream = activity.getContentResolver().openInputStream(fileUri);

                // Determine the folder based on the file type
                String fileType = getFileType(fileName);
                File targetDir = new File(activity.getExternalFilesDir(null), fileType);
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    throw new Exception("Failed to create directory: " + targetDir.getAbsolutePath());
                }

                File externalFile = new File(targetDir, fileName);
                OutputStream outputStream = new FileOutputStream(externalFile);

                // Set up the progress dialog
                activity.runOnUiThread(() -> showProgressDialog(fileName));

                // Copy file contents with progress tracking
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytes = 0;
                long fileSize = inputStream.available();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    // Update the progress dialog
                    int progress = (int) (100 * totalBytes / fileSize);
                    activity.runOnUiThread(() -> updateProgressDialog(progress));
                }

                outputStream.close();
                inputStream.close();

                // Dismiss the progress dialog and update the UI
                activity.runOnUiThread(() -> {
                    dismissProgressDialog();
                    Toast.makeText(activity, "File saved successfully", Toast.LENGTH_SHORT).show();
                    callback.onComplete();

                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    dismissProgressDialog();
                    Toast.makeText(activity, "Error saving file", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Show progress dialog
    private void showProgressDialog(String fileName) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading File");
        progressDialog.setMessage("Uploading " + fileName);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100); // Maximum progress value
        progressDialog.show();
    }

    // Update progress dialog
    private void updateProgressDialog(int progress) {
        if (progressDialog != null) {
            progressDialog.setProgress(progress);
        }
    }

    // Dismiss progress dialog
    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }


    // Get the file name from the Uri
    private String getFileName(Uri fileUri) {
        String fileName = "unknown_file";

        if ("content".equals(fileUri.getScheme())) {
            Cursor cursor = activity.getContentResolver().query(fileUri, null, null, null, null);
            if (cursor != null) {
                try {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(columnIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        } else if ("file".equals(fileUri.getScheme())) {
            fileName = new File(fileUri.getPath()).getName();
        }

        return fileName;
    }


    // Get the folder name based on file type
    private String getFileType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (isImage(extension)) {
            return "images";
        } else if (isVideo(extension)) {
            return "videos";
        } else if (isApplication(extension)) {
            return "applications";
        } else if (isDocument(extension)) {
            return "documents";
        } else {
            return "others"; // For any other file type
        }
    }

    // Helper methods to determine file type
    private boolean isImage(String extension) {
        return extension.equals("jpg") || extension.equals("png") || extension.equals("gif") || extension.equals("jpeg");
    }

    private boolean isVideo(String extension) {
        return extension.equals("mp4") || extension.equals("avi") || extension.equals("mkv");
    }

    private boolean isApplication(String extension) {
        return extension.equals("apk");
    }

    private boolean isDocument(String extension) {
        return extension.equals("pdf") || extension.equals("docx") || extension.equals("txt");
    }

    // Display all existing files in the uploaded files layout
    public void displayExistingFiles() {
        // Get the directory where files are stored
        File directory = activity.getExternalFilesDir(null);
        if (directory != null && directory.exists()) {
            File[] fileTypesDirs = directory.listFiles(); // Get all directories (e.g., images, videos, etc.)

            if (fileTypesDirs != null) {
                // Find the layouts in your fragment (not in MainActivity)
                LinearLayout imagesLayout = rootView.findViewById(R.id.imagesLayout);
                LinearLayout videosLayout = rootView.findViewById(R.id.videosLayout);
                LinearLayout documentsLayout = rootView.findViewById(R.id.documentsLayout);
                LinearLayout applicationsLayout = rootView.findViewById(R.id.applicationsLayout);
                LinearLayout othersLayout = rootView.findViewById(R.id.othersLayout);
                imagesLayout.removeAllViews();  // Clear any existing file views
                videosLayout.removeAllViews();  // Clear any existing file views
                documentsLayout.removeAllViews();  // Clear any existing file views
                applicationsLayout.removeAllViews();  // Clear any existing file views
                othersLayout.removeAllViews();  // Clear any existing file views

                // Iterate through each directory and categorize the files
                for (File typeDir : fileTypesDirs) {
                    if (typeDir.isDirectory()) {
                        File[] filesInDir = typeDir.listFiles(); // Get files inside the directory
                        if (filesInDir != null) {
                            for (File file : filesInDir) {
                                if (file.isFile()) {
                                    // Determine the file type (image, video, etc.)
                                    String fileType = getFileType(file.getName());

                                    // Add file to corresponding layout based on its type
                                    switch (fileType) {
                                        case "images":
                                            addFileToSection(file, imagesLayout);
                                            break;
                                        case "videos":
                                            addFileToSection(file, videosLayout);
                                            break;
                                        case "documents":
                                            addFileToSection(file, documentsLayout);
                                            break;
                                        case "applications":
                                            addFileToSection(file, applicationsLayout);
                                            break;
                                        default:
                                            addFileToSection(file, othersLayout);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    // Add a file to the UI layout
    private void addFileToSection(File file, LinearLayout layout) {
        // Create a horizontal LinearLayout to hold icon and text
        LinearLayout fileItemLayout = new LinearLayout(activity);
        fileItemLayout.setOrientation(LinearLayout.HORIZONTAL);
        fileItemLayout.setPadding(20, 20, 20, 20);
        fileItemLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Get the background color from the current theme
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.fileItemBackgroundColor, typedValue, true);
        int backgroundColor = typedValue.data;

        // Apply the background color
        fileItemLayout.setBackgroundColor(backgroundColor);

        // Create an ImageView for the file icon
        ImageView iconView = new ImageView(activity);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(48, 48)); // Set icon size
        iconView.setImageResource(getFileIcon(getFileType(file.getName()))); // Set appropriate icon

        // Create a TextView for the file name
        TextView textView = new TextView(activity);
        textView.setText(file.getName());
        textView.setTextSize(16);
        textView.setPadding(20, 0, 0, 0); // Padding between icon and text
        textView.setGravity(Gravity.CENTER_VERTICAL);

        // Add the ImageView and TextView to the horizontal layout
        fileItemLayout.addView(iconView);
        fileItemLayout.addView(textView);


        // Set OnClickListener to open the file when clicked
        fileItemLayout.setOnClickListener(view -> openFile(file));

        // Set OnLongClickListener to show options (Show File, Delete File)
        fileItemLayout.setOnLongClickListener(view -> {
            showFileOptionsDialog(file);
            return true; // return true to indicate that the long press is handled
        });
        // Add the horizontal layout to the main category layout
        layout.addView(fileItemLayout);
    }

    private int getFileIcon(String fileType) {
        switch (fileType) {
            case "images":
                return R.drawable.ic_image; // Replace with your actual drawable resource
            case "videos":
                return R.drawable.ic_video; // Replace with your actual drawable resource
            case "documents":
                return R.drawable.ic_document; // Replace with your actual drawable resource
            case "applications":
                return R.drawable.ic_android; // Replace with your actual drawable resource
            default:
                return R.drawable.ic_file; // Default icon for unknown types
        }
    }

    // Show the options dialog for file actions
    private void showFileOptionsDialog(File file) {
        // Create the dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("File Options")
                .setItems(new String[]{"Show File", "Delete File", "Download File"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Show File
                            openFile(file); // Open the file
                            break;
                        case 1: // Delete File
                            deleteFile(file); // Delete the file
                            break;
                        case 2:
                            showQrCode(file);
                            break;
                    }
                });

        // Create the dialog first
        android.app.AlertDialog dialog = builder.create();

        // Retrieve custom background color attribute
        TypedValue dialogBackgroundColorValue = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.dialogBackgroundColor, dialogBackgroundColorValue, true);
        int dialogBackgroundColor = dialogBackgroundColorValue.data;

        // Set the dialog's background color
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(dialogBackgroundColor));

        // Show the dialog
        dialog.show();
    }


    // Delete the selected file
    private void deleteFile(File file) {
        // Check if the file exists before attempting to delete it
        if (file.exists()) {
            if (file.delete()) {
                Toast.makeText(activity, "File deleted successfully", Toast.LENGTH_SHORT).show();
                // Refresh the UI or remove the file from the layout
                activity.runOnUiThread(() -> displayExistingFiles()); // This is assuming you have this method to refresh the UI
            } else {
                Toast.makeText(activity, "Error deleting file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, "File does not exist", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQrCode(File file) {
        try {
            String deviceIp = NetworkUtils.getDeviceIp(activity);
            String fileType = getFileType(file.getName());
            // Retrieve the saved port from SharedPreferences
            SharedPreferences sharedPreferences = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            int savedPort = sharedPreferences.getInt("selected_port", 8080); // Default port is 8080

            // Encode file name to handle spaces and special characters
            String encodedFileName = URLEncoder.encode(file.getName(), "UTF-8").replace("+", "%20");
            String downloadUrl = "http://" + deviceIp + ":"+savedPort +"/" + fileType + "/" + encodedFileName;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Generate larger BitMatrix
            int qrCodeSize = 1024; // Increased QR code size for better readability
            BitMatrix bitMatrix = qrCodeWriter.encode(downloadUrl, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);

            // Convert BitMatrix to Bitmap
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Retrieve custom attributes for QR code and dialog background
            TypedValue qrCodeColorValue = new TypedValue();
            TypedValue qrBackgroundColorValue = new TypedValue();
            TypedValue dialogBackgroundColorValue = new TypedValue();

            activity.getTheme().resolveAttribute(R.attr.qrCodeColor, qrCodeColorValue, true);
            activity.getTheme().resolveAttribute(R.attr.qrBackgroundColor, qrBackgroundColorValue, true);
            activity.getTheme().resolveAttribute(R.attr.dialogBackgroundColor, dialogBackgroundColorValue, true);

            int qrCodeColor = qrCodeColorValue.data;
            int qrBackgroundColor = qrBackgroundColorValue.data;
            int dialogBackgroundColor = dialogBackgroundColorValue.data;

            // Customize the QR code colors
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? qrCodeColor : qrBackgroundColor);
                }
            }

            // Display the QR code in a dialog
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
            builder.setTitle("QR Code");
            builder.setMessage("Scan the QR code to download the file.");

            android.widget.ImageView qrImageView = new android.widget.ImageView(activity);
            qrImageView.setImageBitmap(qrCodeBitmap);
            builder.setView(qrImageView);

            // Create the dialog first
            android.app.AlertDialog dialog = builder.create();

            // Now set the dialog background color
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(dialogBackgroundColor));


            // Set the positive button and show the dialog
            builder.setPositiveButton("Close", null);
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(activity, "Error generating QR code", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }





    // Open a file using an external app
    private void openFile(File file) {
        Uri uri = FileProvider.getUriForFile(activity, "com.example.rfileshare.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, activity.getContentResolver().getType(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }

}