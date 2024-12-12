package org.borg.backend.util;

import org.springframework.beans.factory.annotation.Value;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageUtil {


    public static String encodeImageFileToBase64(String subFilePath) {
        String basePath = "backend/src/main/java/org/borg/backend/storage/profile-pics/";

        if (subFilePath == null || subFilePath.isEmpty()) {
            try {
                Path path = Paths.get("backend/src/main/java/org/borg/backend/storage/player-pics/placeholder/placeholder.jpg");
                byte[] imageBytes = Files.readAllBytes(path);
                return Base64.getEncoder().encodeToString(imageBytes);
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
}
