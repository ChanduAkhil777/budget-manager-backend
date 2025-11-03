package com.budgetmanager.backend.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct; // Ensure this import
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.profile-photos.dir}") // Inject the path from application.properties
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct // This method runs after the bean is created
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir);
            Files.createDirectories(rootLocation); // Create the upload directory if it doesn't exist
            System.out.println("Upload directory created/found at: " + rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String store(MultipartFile file, String subfolder) {
        // Normalize file name
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String filename = "";

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file " + originalFilename);
            }
            if (originalFilename.contains("..")) {
                // Security check
                throw new RuntimeException("Cannot store file with relative path outside current directory " + originalFilename);
            }

            // Generate a unique filename using UUID and preserve the original extension
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

            // Define the destination path including the subfolder
            Path destinationFolder = this.rootLocation.resolve(Paths.get(subfolder)).normalize().toAbsolutePath();

             // Ensure subfolder exists
            if (!Files.exists(destinationFolder)) {
                 Files.createDirectories(destinationFolder);
            }

            Path destinationFile = destinationFolder.resolve(uniqueFilename).normalize().toAbsolutePath();

            // Check if the destination is within the intended storage directory (security)
            if (!destinationFile.getParent().equals(destinationFolder)) {
                throw new RuntimeException("Cannot store file outside specified directory.");
            }

            // Save the file
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return the relative path to be saved in the database
            filename = Paths.get(subfolder).resolve(uniqueFilename).toString().replace("\\", "/"); // Use forward slashes for URLs

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        }

        return filename;
    }

    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    // Optional: Add a method to delete files if needed
    public void delete(String filename) {
        try {
            Path file = load(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filename + " - " + e.getMessage());
            // Log this error properly in a real app
        }
    }
}