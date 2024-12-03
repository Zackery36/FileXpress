package com.filexpress.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import fi.iki.elonen.NanoHTTPD;


public class SimpleWebServer extends NanoHTTPD {

    private final File rootDir;
    private boolean isRunning = false;

    public SimpleWebServer(int port, File rootDir) {
        super(port);
        this.rootDir = rootDir;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // Serve index.html dynamically if the URI is empty or "/"
        if (uri.equals("/") || uri.isEmpty()) {
            String htmlContent = generateHtmlContent();
            return newFixedLengthResponse(Response.Status.OK, "text/html", htmlContent);
        }

        // Handle file requests
        File requestedFile = new File(rootDir, decodeUrl(uri));  // Decode URL before accessing file
        if (requestedFile.exists() && requestedFile.isFile()) {
            try {
                String mimeType = NanoHTTPD.getMimeTypeForFile(requestedFile.getName());
                FileInputStream fileInputStream = new FileInputStream(requestedFile);

                // Create a fixed-length response with the file size
                Response response = newFixedLengthResponse(
                        Response.Status.OK,
                        mimeType,
                        fileInputStream,
                        requestedFile.length()
                );

                // Add headers to force download
                response.addHeader("Content-Disposition", "attachment; filename=\"" + requestedFile.getName() + "\"");
                response.addHeader("Content-Length", String.valueOf(requestedFile.length()));
                return response;
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error reading file");
            }
        }

        // File not found
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "File not found");
    }

    // Helper method to decode URL
    private String decodeUrl(String uri) {
        try {
            return URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return uri;  // If decoding fails, return the original URI
        }
    }


    // Dynamically generate the HTML content for the file list
    private String generateHtmlContent() {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>");
        htmlContent.append("<html lang=\"en\"><head>");
        htmlContent.append("<meta charset=\"UTF-8\">");
        htmlContent.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        htmlContent.append("<title>File Server</title>");
        htmlContent.append("<style>");
        htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; } ");
        htmlContent.append("a { text-decoration: none; color: blue; } ");
        htmlContent.append("a:hover { text-decoration: underline; } ");
        htmlContent.append(".section { margin-bottom: 20px; } ");
        htmlContent.append(".section h2 { cursor: pointer; color: #333; background-color: #f2f2f2; padding: 10px; border: 1px solid #ddd; display: flex; align-items: center; justify-content: space-between; } ");
        htmlContent.append(".files { display: none; margin-top: 10px; list-style: none; padding-left: 20px; } ");
        htmlContent.append(".files li { margin-bottom: 5px; } ");
        htmlContent.append("</style>");
        htmlContent.append("<script>");
        htmlContent.append("function toggleSection(id, button) {");
        htmlContent.append("  const section = document.getElementById(id);");
        htmlContent.append("  if (section.style.display === 'none' || section.style.display === '') {");
        htmlContent.append("    section.style.display = 'block';");
        htmlContent.append("    button.textContent = '-';");
        htmlContent.append("  } else {");
        htmlContent.append("    section.style.display = 'none';");
        htmlContent.append("    button.textContent = '+';");
        htmlContent.append("  }");
        htmlContent.append("}");
        htmlContent.append("</script>");
        htmlContent.append("</head><body>");
        htmlContent.append("<h1>File Server</h1>");

        // Define folder sections
        String[] folderSections = {
                "images", // Folder for Images
                "videos", // Folder for Videos
                "documents", // Folder for Documents
                "applications", // Folder for Applications
                "others" // Folder for Miscellaneous Files
        };

        // Generate sections based on folder contents
        for (int i = 0; i < folderSections.length; i++) {
            File sectionFolder = new File(rootDir, folderSections[i]);
            String sectionId = "section" + i;

            // Check if the folder exists and contains files
            if (sectionFolder.exists() && sectionFolder.isDirectory()) {
                File[] files = sectionFolder.listFiles();
                if (files != null && files.length > 0) {
                    htmlContent.append("<div class=\"section\">");
                    htmlContent.append("<h2 onclick=\"toggleSection('" + sectionId + "', this.children[1])\">")
                            .append("<span style=\"flex: 1; text-align: left;\">")
                            .append(capitalizeFirstLetter(folderSections[i]))
                            .append("</span>")
                            .append("<span>+</span>")
                            .append("</h2>");
                    htmlContent.append("<ul id=\"" + sectionId + "\" class=\"files\">");

                    // List files in this folder
                    for (File file : files) {
                        if (file.isFile()) {
                            htmlContent.append("<li><a href=\"")
                                    .append(folderSections[i])
                                    .append("/")
                                    .append(file.getName())
                                    .append("\" download=\"")
                                    .append(file.getName())
                                    .append("\">")
                                    .append(file.getName())
                                    .append("</a></li>");
                        }
                    }

                    htmlContent.append("</ul>");
                    htmlContent.append("</div>");
                }
            }
        }

        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }


    // Helper method to capitalize the first letter of a string
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    // Helper method to get file extension
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1) ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }


    public void startServer() throws IOException {
        if (!isRunning) {
            super.start();
            isRunning = true;
        }
    }

    public void stopServer() {
        if (isRunning) {
            super.stop();
            isRunning = false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
