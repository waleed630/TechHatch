package api.techhatch.com.service;

import api.techhatch.com.dto.response.FileUploadResponse;
import api.techhatch.com.exception.BadRequestException;
import api.techhatch.com.exception.ResourceNotFoundException;
import api.techhatch.com.exception.UnauthorizedException;
import api.techhatch.com.model.CandidateProfile;
import api.techhatch.com.model.RecruiterProfile;
import api.techhatch.com.model.StoredFile;
import api.techhatch.com.model.StoredFile.FileType;
import api.techhatch.com.model.Users;
import api.techhatch.com.repository.CandidateProfileRepo;
import api.techhatch.com.repository.RecruiterProfileRepo;
import api.techhatch.com.repository.StoredFileRepo;
import api.techhatch.com.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FileStorageService {

    private final StoredFileRepo storedFileRepo;
    private final UserRepo userRepo;
    private final CandidateProfileRepo candidateRepo;
    private final RecruiterProfileRepo recruiterRepo;

    private static final long MAX_RESUME_SIZE = 5 * 1024 * 1024;  //5 MB
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;   //2 MB

    //allowed file extensions with the content type they are served as
    private static final Map<String, String> RESUME_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp");

    private static final Set<FileType> PUBLIC_TYPES = Set.of(FileType.PROFILE_PICTURE, FileType.COMPANY_LOGO);

    /**
     * Upload candidate resume (PDF, DOC or DOCX up to 5 MB)
     */
    public FileUploadResponse uploadResume(String email, MultipartFile file) {

        CandidateProfile profile = candidateRepo.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Please save your profile before uploading a resume"));

        StoredFile saved = store(profile.getUser(), FileType.RESUME, file, RESUME_TYPES, MAX_RESUME_SIZE,
                "Resume must be a PDF, DOC or DOCX file", "Resume must be smaller than 5 MB");

        profile.setResumeUrl(urlOf(saved));
        candidateRepo.save(profile);
        return response(saved, "Resume uploaded successfully");
    }

    /**
     * Upload candidate profile picture (PNG, JPG or WEBP up to 2 MB)
     */
    public FileUploadResponse uploadProfilePicture(String email, MultipartFile file) {

        CandidateProfile profile = candidateRepo.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Please save your profile before uploading a picture"));

        StoredFile saved = store(profile.getUser(), FileType.PROFILE_PICTURE, file, IMAGE_TYPES, MAX_IMAGE_SIZE,
                "Picture must be a PNG, JPG or WEBP image", "Picture must be smaller than 2 MB");

        profile.setProfilePictureUrl(urlOf(saved));
        candidateRepo.save(profile);
        return response(saved, "Profile picture uploaded successfully");
    }

    /**
     * Upload company logo (PNG, JPG or WEBP up to 2 MB)
     */
    public FileUploadResponse uploadCompanyLogo(String email, MultipartFile file) {

        RecruiterProfile profile = recruiterRepo.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Please save your company profile before uploading a logo"));

        StoredFile saved = store(profile.getUser(), FileType.COMPANY_LOGO, file, IMAGE_TYPES, MAX_IMAGE_SIZE,
                "Logo must be a PNG, JPG or WEBP image", "Logo must be smaller than 2 MB");

        profile.setCompanyLogoUrl(urlOf(saved));
        recruiterRepo.save(profile);
        return response(saved, "Company logo uploaded successfully");
    }

    /**
     * Get a stored file. Pictures and logos are public,
     * a resume can only be opened by its owner or by a recruiter.
     * @param viewerEmail email of the logged in user, null when not logged in
     */
    @Transactional(readOnly = true)
    public StoredFile getFile(Long fileId, String viewerEmail) {

        StoredFile file = storedFileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (PUBLIC_TYPES.contains(file.getFileType())) {
            return file;
        }

        if (viewerEmail == null) {
            throw new UnauthorizedException("Please login to view this file");
        }

        Users viewer = userRepo.findUserByEmail(viewerEmail)
                .orElseThrow(() -> new UnauthorizedException("Please login to view this file"));

        boolean isOwner = file.getOwnerId().equals(viewer.getId());
        boolean isRecruiter = viewer.getRole() == Users.Role.RECRUITER;
        if (!isOwner && !isRecruiter) {
            throw new UnauthorizedException("You don't have permission to view this file");
        }
        return file;
    }

    //HELPER METHODS

    private StoredFile store(Users owner, FileType type, MultipartFile file, Map<String, String> allowedTypes,
                             long maxSize, String typeMessage, String sizeMessage) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to upload");
        }
        if (file.getSize() > maxSize) {
            throw new BadRequestException(sizeMessage);
        }

        String fileName = cleanFileName(file.getOriginalFilename());
        String contentType = allowedTypes.get(extensionOf(fileName));
        if (contentType == null) {
            throw new BadRequestException(typeMessage);
        }

        final byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        //a new upload replaces the previous file of the same kind
        storedFileRepo.deleteAll(storedFileRepo.findByOwnerIdAndFileType(owner.getId(), type));

        return storedFileRepo.save(StoredFile.builder()
                .ownerId(owner.getId())
                .fileType(type)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .data(data)
                .build());
    }

    private String urlOf(StoredFile file) {
        return "/api/v1/files/" + file.getId();
    }

    private FileUploadResponse response(StoredFile file, String message) {
        return FileUploadResponse.builder()
                .success(true)
                .url(urlOf(file))
                .fileName(file.getFileName())
                .message(message)
                .build();
    }

    //keeps only the name part and removes characters that are unsafe in a header
    private String cleanFileName(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String name = original.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return name.isEmpty() ? "file" : name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}
