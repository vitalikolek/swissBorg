package me.yukitale.cryptoexchange.utils;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.nio.file.*;

import org.springframework.web.multipart.MultipartFile;

@UtilityClass
public class FileUploadUtil {

    public void saveFile(String uploadDir, String fileName, MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IOException("Could not save image file: " + fileName, ex);
        }
    }
}
