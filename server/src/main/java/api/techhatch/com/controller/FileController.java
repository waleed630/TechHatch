package api.techhatch.com.controller;

import api.techhatch.com.dto.response.FileUploadResponse;
import api.techhatch.com.model.StoredFile;
import api.techhatch.com.model.UserPrinciple;
import api.techhatch.com.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileService;

    /**
     * Upload resume (Candidate only)
     */
    @PostMapping("/resume")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<FileUploadResponse> uploadResume(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(fileService.uploadResume(userPrinciple.getUsername(), file));
    }

    /**
     * Upload profile picture (Candidate only)
     */
    @PostMapping("/profile-picture")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<FileUploadResponse> uploadProfilePicture(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(fileService.uploadProfilePicture(userPrinciple.getUsername(), file));
    }

    /**
     * Upload company logo (Recruiter only)
     */
    @PostMapping("/company-logo")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<FileUploadResponse> uploadCompanyLogo(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(fileService.uploadCompanyLogo(userPrinciple.getUsername(), file));
    }

    /**
     * Get a file. Pictures and logos are public, resumes need a login
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFile(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @PathVariable Long id) {

        String viewerEmail = userPrinciple != null ? userPrinciple.getUsername() : null;
        StoredFile file = fileService.getFile(id, viewerEmail);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getFileName()).build().toString())
                .body(file.getData());
    }
}
