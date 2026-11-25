package com.resumevault;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ResumeRepository extends JpaRepository<Resume,String> {
  List<Resume> findAllByOrderByUploadedAtDesc();
  long countBySource(String source);
}
