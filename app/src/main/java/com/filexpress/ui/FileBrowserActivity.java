package com.filexpress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filexpress.R;
import com.filexpress.adapter.FileAdapter;
import com.filexpress.managers.FileOperations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.view.inputmethod.InputMethodManager;

public class FileBrowserActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView selectedFilesText;
    private LinearLayout selectedFilesLayout;
    private LinearLayout selectedFilesLayoutG;
    private LinearLayout categoryLayout;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private List<File> fileList = new ArrayList<>();
    private List<File> searchResults = new ArrayList<>();
    private FileOperations fileOperations;
    private EditText searchField;
    private ImageView searchButton;
    private boolean isSearching = false;
    private boolean isSearchMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySelectedTheme();
        setContentView(R.layout.activity_file_browser);

        // Initialize fileOperations
        fileOperations = new FileOperations(this, findViewById(android.R.id.content));

        recyclerView = findViewById(R.id.recyclerView);
        selectedFilesText = findViewById(R.id.selectedFilesText);
        selectedFilesLayout = findViewById(R.id.selectedFilesLayout);
        selectedFilesLayoutG = findViewById(R.id.selectedFilesLayoutG);
        searchField = findViewById(R.id.searchField);
        searchButton = findViewById(R.id.searchIcon);
        categoryLayout = findViewById(R.id.categoryLayout);

        SelectedFilesManager.initialize(this, selectedFilesLayout);

        selectedFilesText.setOnClickListener(v -> {
            SelectedFilesManager.getInstance().toggleSelectedFilesLayout();
            toggleSelectedFilesLayoutVisibility();
        });

        findViewById(R.id.mainLayout).setOnClickListener(v -> {
            if (selectedFilesLayoutG.getVisibility() == View.VISIBLE) {
                selectedFilesLayoutG.setVisibility(View.GONE);
            }
        });

        selectedFilesLayout.setOnClickListener(v -> {
            // Prevent layout from hiding if clicked inside
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter(fileList, this::onFileSelected, this);
        recyclerView.setAdapter(fileAdapter);

        currentDirectory = new File("/storage/emulated/0/");
        loadFiles(currentDirectory);

        // Add listener for the search field to start searching when "Enter" is pressed
        searchButton.setOnClickListener(v -> {
            onSearchClicked();
        });
        searchField.setOnEditorActionListener((v, actionId, event) -> {
            String searchQuery = searchField.getText().toString();
            if (!searchQuery.isEmpty()) {
                searchForFile(searchQuery);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reinitialize everything when returning to this activity
        selectedFilesLayout = findViewById(R.id.selectedFilesLayout);
        SelectedFilesManager.getInstance().reinitializeSelectedFiles();
        SelectedFilesManager.initialize(this, selectedFilesLayout);
        updateSelectedFilesText();
        updateSelectedFilesLayoutVisibility();
        SelectedFilesManager.getInstance().updateSelectedFilesLayout();
    }

    private void toggleSelectedFilesLayoutVisibility() {
        selectedFilesLayoutG.setVisibility(selectedFilesLayoutG.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void updateSelectedFilesLayoutVisibility() {
        if (SelectedFilesManager.getInstance().getSelectedFiles().size() > 0) {
            selectedFilesLayoutG.setVisibility(View.VISIBLE);
        } else {
            selectedFilesLayoutG.setVisibility(View.GONE);
        }
    }

    private void loadFiles(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Toast.makeText(this, "Invalid directory", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = directory.listFiles();
        fileList.clear();
        if (files != null) {
            for (File file : files) {
                fileList.add(file);
            }
        }

        fileAdapter.updateFiles(fileList);
        updatePathIndicator(directory);
    }

    private void updatePathIndicator(File directory) {
        TextView pathIndicator = findViewById(R.id.pathIndicator);
        pathIndicator.setText("Current Directory: " + directory.getAbsolutePath());
    }

    private void onFileSelected(File file) {
        if (file.isDirectory()) {
            loadFiles(file);
        } else {
            SelectedFilesManager manager = SelectedFilesManager.getInstance();
            if (manager.getSelectedFiles().contains(file)) {
                manager.unselectFile(file);
            } else {
                manager.addSelectedFile(file);
            }
            updateSelectedFilesText();
        }
    }

    public void updateSelectedFilesText() {
        int selectedCount = SelectedFilesManager.getInstance().getSelectedFiles().size();
        selectedFilesText.setText("Selected files: " + selectedCount);
    }

    public void onCategorySelected(View view) {
        TextView pathIndicator = findViewById(R.id.pathIndicator);

        if (view.getId() == R.id.filesCategory) {
            pathIndicator.setVisibility(View.VISIBLE);
            loadFiles(new File("/storage/emulated/0/"));
        } else {
            pathIndicator.setVisibility(View.GONE);
            if (view.getId() == R.id.videosCategory) {
                loadFilteredFiles(new String[]{".mp4", ".mkv", ".avi", ".mov", ".flv"});
            } else if (view.getId() == R.id.imagesCategory) {
                loadFilteredFiles(new String[]{".jpg", ".jpeg", ".png", ".gif", ".bmp"});
            }
        }
    }

    private void loadFilteredFiles(String[] extensions) {
        File storageDir = new File("/storage/emulated/0/");
        fileList.clear();

        new Thread(() -> {
            List<File> filteredFiles = new ArrayList<>();
            searchFiles(storageDir, extensions, filteredFiles);

            filteredFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            runOnUiThread(() -> {
                fileList.addAll(filteredFiles);
                fileAdapter.updateFiles(fileList);
            });
        }).start();
    }

    private void searchFiles(File directory, String[] extensions, List<File> filteredFiles) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFiles(file, extensions, filteredFiles);
                } else {
                    for (String ext : extensions) {
                        if (file.getName().toLowerCase().endsWith(ext)) {
                            filteredFiles.add(file);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void searchForFile(String query) {
        isSearchMode = true;
        File storageDir = new File("/storage/emulated/0/");
        searchResults.clear();

        new Thread(() -> {
            searchFileRecursive(storageDir, query);
            runOnUiThread(() -> {
                fileList.clear();
                fileList.addAll(searchResults);
                fileAdapter.updateFiles(fileList);
                isSearchMode = false;
            });
        }).start();
    }

    private void searchFileRecursive(File directory, String query) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFileRecursive(file, query);
                } else {
                    if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                        searchResults.add(file);
                    }
                }
            }
        }
    }
    private void onSearchClicked() {
        TextView pathIndicator = findViewById(R.id.pathIndicator);
        pathIndicator.setVisibility(View.GONE);
        categoryLayout.setVisibility(View.GONE);
        // Make the search field visible and request focus
        searchField.setVisibility(View.VISIBLE);
        searchField.requestFocus(); // This will automatically show the keyboard
        // Show the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
        }
        isSearching = true;
    }


    public void onBackClicked(View view) {
        if (isSearching) {
            // If the user is in search mode, hide the search field and return to the "Files" category
            categoryLayout.setVisibility(View.VISIBLE);
            searchField.setVisibility(View.GONE);
            isSearching = false;
            loadFiles(new File("/storage/emulated/0/"));
            return;
        }

        if (currentDirectory.getAbsolutePath().equals("/storage/emulated/0/")) {
            // If in the root directory, exit the app
            finish();
        } else {
            // Navigate to the parent
            currentDirectory = new File("/storage/emulated/0/"); // Update currentDirectory
            loadFiles(currentDirectory);

        }
    }


    public void onUploadClicked(View view) {
        // Get the selected files
        List<File> selectedFiles = SelectedFilesManager.getInstance().getSelectedFiles();

        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "No files selected for upload", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert File objects to Uri objects
        List<Uri> fileUris = new ArrayList<>();
        for (File file : selectedFiles) {
            Uri fileUri = Uri.fromFile(file);
            fileUris.add(fileUri);
        }

        // Now, call the handleMultipleFileSelection method with the list of Uri objects
        fileOperations.handleMultipleFileSelection(fileUris);

        // Clear the selected files after handling
        SelectedFilesManager.getInstance().getSelectedFiles().clear();
        updateSelectedFilesText();
    }




private void applySelectedTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        int themeIndex = sharedPreferences.getInt("theme_index", 0);

        switch (themeIndex) {
            case 1:
                setTheme(R.style.AppTheme_OceanBreeze);
                break;
            case 2:
                setTheme(R.style.AppTheme_ForestGlade);
                break;
            default:
                setTheme(R.style.AppTheme_Akatsuki);
                break;
        }
    }

}
