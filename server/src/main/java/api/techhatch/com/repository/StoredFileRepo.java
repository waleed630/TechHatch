package api.techhatch.com.repository;

import api.techhatch.com.model.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoredFileRepo extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findByOwnerIdAndFileType(Long ownerId, StoredFile.FileType fileType);
}
