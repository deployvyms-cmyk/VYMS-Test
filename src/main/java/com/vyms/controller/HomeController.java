package com.vyms.controller;

import com.vyms.entity.UploadedFile;
import com.vyms.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Handles root URL navigation and root-level static asset serving from the database.
 *
 * Keeping this in a dedicated controller makes the entry flow explicit and easy
 * to change later.
 */
@Controller
public class HomeController {

    private final UploadedFileRepository uploadedFileRepository;

    @Autowired
    public HomeController(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    /**
     * Sends users from the base URL to the login page.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    /**
     * Serves uploaded files from the database if they exist.
     * If not present in the database, falls back to the classpath static folder
     * to support backward compatibility with legacy local disk uploads.
     */
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        // 1. Try to serve from database first
        java.util.Optional<UploadedFile> dbFileOpt = uploadedFileRepository.findByFilename(filename);
        if (dbFileOpt.isPresent()) {
            UploadedFile file = dbFileOpt.get();
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(new ByteArrayResource(file.getFileData()));
        }

        // 2. Fallback to classpath static resources for legacy local uploads
        Resource staticFile = new ClassPathResource("static/uploads/" + filename);
        if (staticFile.exists() && staticFile.isReadable()) {
            try {
                // Determine content type dynamically
                String contentType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(staticFile.getURI()));
                if (contentType == null) {
                    contentType = "image/jpeg"; // standard fallback
                }
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(staticFile);
            } catch (IOException e) {
                // fall through to 404
            }
        }

        return ResponseEntity.notFound().build();
    }
}
