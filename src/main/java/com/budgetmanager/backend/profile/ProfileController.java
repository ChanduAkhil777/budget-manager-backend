package com.budgetmanager.backend.profile;

import com.budgetmanager.backend.dto.UpdateProfileRequest; // <-- DTO with all fields
import com.budgetmanager.backend.dto.UserProfileDto;     // <-- DTO with all fields
import com.budgetmanager.backend.file.FileStorageService;
import com.budgetmanager.backend.model.User;
import com.budgetmanager.backend.repository.UserRepository;
import jakarta.validation.Valid;                           // <-- Import validation
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/api/profile") // Base path for profile actions
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // Helper to get current user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // --- Endpoint to GET current user's profile details ---
    @GetMapping("") // Maps to GET /api/profile
    public ResponseEntity<UserProfileDto> getUserProfile() {
        User user = getCurrentUser();

        String photoUrl = null;
        if (user.getProfilePhotoPath() != null && !user.getProfilePhotoPath().isEmpty()) {
            photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/profile/photo/")
                    .path(user.getProfilePhotoPath())
                    .toUriString();
        }

        UserProfileDto profileDto = UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .village(user.getVillage())
                .phoneNumber(user.getPhoneNumber())
                .profilePhotoUrl(photoUrl)
                .build();

        return ResponseEntity.ok(profileDto);
    }

    // --- Endpoint to UPDATE current user's profile details ---
    @PutMapping("") // Maps to PUT /api/profile
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest
    ) {
        User user = getCurrentUser();

        // Update fields if they are provided in the request
        if (updateRequest.getFullName() != null) {
            user.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getEmail() != null) {
            // Optional: Add validation if email already exists for another user
            user.setEmail(updateRequest.getEmail());
        }
        // --- ADDED LOGIC TO UPDATE VILLAGE AND PHONE ---
        if (updateRequest.getVillage() != null) {
            user.setVillage(updateRequest.getVillage());
        }
        if (updateRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        // ---

        User savedUser = userRepository.save(user);

        // Re-construct the DTO to return updated info
        String photoUrl = null;
        if (savedUser.getProfilePhotoPath() != null && !savedUser.getProfilePhotoPath().isEmpty()) {
            photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/profile/photo/")
                    .path(savedUser.getProfilePhotoPath())
                    .toUriString();
        }
        UserProfileDto profileDto = UserProfileDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .village(savedUser.getVillage())         // Ensure these are included
                .phoneNumber(savedUser.getPhoneNumber()) // Ensure these are included
                .profilePhotoUrl(photoUrl)
                .build();

        return ResponseEntity.ok(profileDto);
    }


    // --- Endpoint to UPLOAD profile photo ---
    @PostMapping("/photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        User user = getCurrentUser();

        try {
            if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type or empty file. Only images are allowed."));
            }

            if (user.getProfilePhotoPath() != null && !user.getProfilePhotoPath().isEmpty()) {
                fileStorageService.delete(user.getProfilePhotoPath());
            }

            String filename = fileStorageService.store(file, user.getUsername());
            user.setProfilePhotoPath(filename);
            userRepository.save(user);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/profile/photo/")
                    .path(filename)
                    .toUriString();

            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "filePath", filename,
                    "fileUrl", fileDownloadUri
            ));
        } catch (Exception e) {
            System.err.println("Upload failed for user " + user.getUsername() + ": " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Could not upload the file: " + e.getMessage()));
        }
    }

    // --- Endpoint to GET (download/view) profile photo ---
    @GetMapping("/photo/{username}/{filename:.+}")
    public ResponseEntity<Resource> getProfilePhoto(@PathVariable String username, @PathVariable String filename) {
        try {
            String relativePath = username + "/" + filename;
            Resource file = fileStorageService.loadAsResource(relativePath);
            String contentType = Files.probeContentType(file.getFile().toPath());
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .body(file);
        } catch (Exception e) {
            System.err.println("Could not get file: " + username + "/" + filename + " - " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

     // --- Endpoint to GET User's Photo URL ---
    @GetMapping("/photo-url")
    public ResponseEntity<?> getProfilePhotoUrl() {
        User user = getCurrentUser();
        if (user.getProfilePhotoPath() == null || user.getProfilePhotoPath().isEmpty()) {
            return ResponseEntity.ok(Map.of("photoUrl", (Object) null));
        }

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/profile/photo/")
                .path(user.getProfilePhotoPath())
                .toUriString();

        return ResponseEntity.ok(Map.of("photoUrl", fileDownloadUri));
    }
}