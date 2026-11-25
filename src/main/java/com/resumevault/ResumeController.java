package com.resumevault;

import net.sourceforge.tess4j.Tesseract;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
  private final ResumeRepository repository;
  private final Path storage;
  private final int maxDocuments;
  private final String tessdataPath;

  public ResumeController(ResumeRepository repository,
      @Value("${app.storage-dir}") String storageDir,
      @Value("${app.max-documents:70}") int maxDocuments,
      @Value("${app.tesseract-path:}") String tessdataPath) throws Exception {
    this.repository=repository; this.storage=Paths.get(storageDir).toAbsolutePath().normalize();
    this.maxDocuments=maxDocuments; this.tessdataPath=tessdataPath;
    Files.createDirectories(this.storage);
  }

  @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) String q) {
    String term=q==null?"":q.toLowerCase(Locale.ROOT);
    return repository.findAllByOrderByUploadedAtDesc().stream()
      .filter(r -> term.isBlank() || (r.getName()+" "+Objects.toString(r.getTags(),"")+" "+Objects.toString(r.getExtractedText(),"")).toLowerCase(Locale.ROOT).contains(term))
      .map(this::view).toList();
  }

  @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(@RequestParam("files") List<MultipartFile> files) throws Exception {
    long remaining=maxDocuments-repository.count();
    if(files.size()>remaining) return ResponseEntity.badRequest().body(Map.of("error","Your library can hold "+maxDocuments+" resumes. There is room for "+Math.max(0,remaining)+" more."));
    long pdfs=files.stream().filter(f->Objects.toString(f.getOriginalFilename(),"").toLowerCase(Locale.ROOT).endsWith(".pdf")).count();
    long images=files.size()-pdfs;
    if(repository.countBySource("PDF")+pdfs>50) return ResponseEntity.badRequest().body(Map.of("error","Your library can hold up to 50 PDFs."));
    if(repository.countBySource("Screenshot")+images>20) return ResponseEntity.badRequest().body(Map.of("error","Your library can hold up to 20 screenshots."));
    List<Map<String,Object>> added=new ArrayList<>();
    for(MultipartFile file:files) {
      String original=Objects.requireNonNullElse(file.getOriginalFilename(),"resume");
      String lower=original.toLowerCase(Locale.ROOT);
      if(!(lower.endsWith(".pdf")||lower.endsWith(".png")||lower.endsWith(".jpg")||lower.endsWith(".jpeg"))) continue;
      String safe=UUID.randomUUID()+"-"+Path.of(original).getFileName(); Path target=storage.resolve(safe).normalize();
      if(!target.startsWith(storage)) continue;
      file.transferTo(target);
      Resume r=new Resume(); r.setOriginalName(original); r.setName(stripExtension(original));
      r.setContentType(file.getContentType()); r.setFilePath(target.toString()); r.setSizeBytes(file.getSize());
      r.setSource(lower.endsWith(".pdf")?"PDF":"Screenshot");
      r.setExtractedText(extract(target, lower));
      repository.save(r); added.add(view(r));
    }
    return ResponseEntity.ok(added);
  }

  private String extract(Path file, String name) {
    try {
      if(name.endsWith(".pdf")) return new Tika().parseToString(file);
      Tesseract ocr=new Tesseract(); if(!tessdataPath.isBlank()) ocr.setDatapath(tessdataPath);
      return ocr.doOCR(file.toFile());
    } catch(Exception e) { return ""; }
  }

  @GetMapping("/{id}") public ResponseEntity<?> get(@PathVariable String id) {
    return repository.findById(id).<ResponseEntity<?>>map(r->ResponseEntity.ok(detail(r))).orElseGet(()->ResponseEntity.notFound().build());
  }
  @PatchMapping("/{id}") public ResponseEntity<?> update(@PathVariable String id,@RequestBody Map<String,String> updates) {
    return repository.findById(id).<ResponseEntity<?>>map(r->{
      if(updates.containsKey("stage")) r.setStage(updates.get("stage"));
      if(updates.containsKey("tags")) r.setTags(updates.get("tags"));
      repository.save(r); return ResponseEntity.ok(view(r));
    }).orElseGet(()->ResponseEntity.notFound().build());
  }
  @GetMapping("/{id}/file") public ResponseEntity<?> file(@PathVariable String id) {
    return repository.findById(id).<ResponseEntity<?>>map(r->ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(Objects.requireNonNullElse(r.getContentType(),"application/octet-stream")))
      .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+r.getOriginalName().replace("\"","")+"\"")
      .body(new FileSystemResource(r.getFilePath()))).orElseGet(()->ResponseEntity.notFound().build());
  }
  @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable String id) throws Exception {
    return repository.findById(id).<ResponseEntity<?>>map(r->{try{Files.deleteIfExists(Path.of(r.getFilePath()));}catch(Exception ignored){} repository.delete(r); return ResponseEntity.noContent().build();}).orElseGet(()->ResponseEntity.notFound().build());
  }
  @PostMapping("/compare") public ResponseEntity<?> compare(@RequestBody List<String> ids) {
    if(ids.size()<2||ids.size()>4) return ResponseEntity.badRequest().body(Map.of("error","Choose 2 to 4 resumes to compare."));
    return ResponseEntity.ok(ids.stream().map(repository::findById).flatMap(Optional::stream).map(this::detail).toList());
  }
  private Map<String,Object> detail(Resume r){Map<String,Object> m=view(r);m.put("extractedText",r.getExtractedText());m.put("fileUrl","/api/resumes/"+r.getId()+"/file");return m;}
  private Map<String,Object> view(Resume r){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("name",r.getName());m.put("originalName",r.getOriginalName());m.put("contentType",r.getContentType());m.put("sizeBytes",r.getSizeBytes());m.put("source",r.getSource());m.put("tags",Objects.toString(r.getTags(),""));m.put("stage",r.getStage());m.put("uploadedAt",r.getUploadedAt());m.put("hasText",r.getExtractedText()!=null&&!r.getExtractedText().isBlank());return m;}
  private String stripExtension(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}
}
