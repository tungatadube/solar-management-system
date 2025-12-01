package com.solar.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    public String storeFile(MultipartFile file, String subdirectory) {
        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, subdirectory);
            Files.createDirectories(uploadPath);
            
            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFilename.substring(dotIndex);
            }
            
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = uploadPath.resolve(uniqueFilename);
            
            // Copy file to target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            String filePath = subdirectory + "/" + uniqueFilename;
            log.info("File stored successfully: {}", filePath);
            
            return filePath;
            
        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            throw new RuntimeException("Failed to store file", ex);
        }
    }
    
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath);
            Files.deleteIfExists(path);
            log.info("File deleted successfully: {}", filePath);
        } catch (IOException ex) {
            log.error("Failed to delete file: {}", filePath, ex);
            throw new RuntimeException("Failed to delete file", ex);
        }
    }
    
    public byte[] loadFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath);
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            log.error("Failed to load file: {}", filePath, ex);
            throw new RuntimeException("Failed to load file", ex);
        }
    }
}
