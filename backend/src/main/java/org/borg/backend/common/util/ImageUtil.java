package org.borg.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
public class ImageUtil {


    public static String encodeAvatarImageFileToBase64(String subFilePath) {
        String basePath = "backend/src/main/java/org/borg/backend/storage/profile-pics/";

        if (subFilePath == null || subFilePath.isEmpty()) {
            try {
                log.warn("Path is null");
                return loadAndEncodeResource("placeholder_profile_pic/placeholder.jpg");
            } catch (IOException e) {
                //TODO error handling
            }
        }

        try {
            Path path = Paths.get(basePath + subFilePath);
            byte[] imageBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            
            //TODO error handling
            System.err.println("Error encoding image file: " + subFilePath);
            e.printStackTrace();
        }
        return null;
    }
    
    public static String encodeStoryImageToBase64(String subFilePath) {
        try {
            return loadAndEncodeResource("story/" + subFilePath);
        } catch (IOException e) {

            //TODO error handling
            System.err.println("Error encoding image file: " + subFilePath);
            e.printStackTrace();
        }
        return null;
    }

    public static String encodeAchievementImageToBase64(String subFilePath) {
        try {
            return loadAndEncodeResource("achievement/" + subFilePath);
        } catch (IOException e) {

            //TODO error handling
            System.err.println("Error encoding image file: " + subFilePath);
            e.printStackTrace();
        }
        return null;
    }

    private static String loadAndEncodeResource(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        log.warn(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }
}
