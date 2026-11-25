package com.resumevault;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface ResumeRepository extends MongoRepository<Resume,String> {
  List<Resume> findAllByOrderByUploadedAtDesc();
  long countBySource(String source);
  java.util.Optional<Resume> findBySha256Hash(String sha256Hash);
}
