package org.borg.backend.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class FileStorageService {
    @Value("${file.path.storage}")
    private String storageBasePath;

    public String saveProfilePicture(MultipartFile file, Long playerId) {
        final String playerProfilePicPath = "profile-pics" + File.separator + playerId;
        log.warn("PlayerProfilePicPath: {}", playerProfilePicPath);

        final String fullProfilePicDirectory = storageBasePath + File.separator + playerProfilePicPath;
        log.warn("FullProfilePicDirectory: {}", fullProfilePicDirectory);

        File profilePicDirectory = new File(fullProfilePicDirectory);

        if (!profilePicDirectory.exists() && !profilePicDirectory.mkdirs()) {
            log.error("Failed to create directory: {}", profilePicDirectory.getAbsolutePath());
            return null;
        }
        
        
        final String fileExtension = getFileExtension(file.getOriginalFilename());
        String savedFileName = System.currentTimeMillis() + "." + fileExtension;

        Path targetPath= Paths.get(fullProfilePicDirectory + File.separator + savedFileName);
        
        try {
            Files.write(targetPath, file.getBytes());
        } catch (IOException e) {
            log.error("File was not saved", e);
        }

        return null;

    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "";
        }
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex == - 1) {
            return "";
        }
        return originalFilename.substring(lastDotIndex + 1).toLowerCase();
    }

    private void resizeImages(String profilePicSubPath) {
        
        
    }

    //TODO 300x300 for profile pic
}
