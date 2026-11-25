package com.resumevault;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Resume {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
  private String name;
  private String originalName;
  private String contentType;
  private String filePath;
  private long sizeBytes;
  private String source;
  private String tags;
  private String stage = "Unreviewed";
  private Instant uploadedAt = Instant.now();
  @Column(columnDefinition="CLOB") private String extractedText;

  public String getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
  public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;}
  public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
  public String getFilePath(){return filePath;} public void setFilePath(String v){filePath=v;}
  public long getSizeBytes(){return sizeBytes;} public void setSizeBytes(long v){sizeBytes=v;}
  public String getSource(){return source;} public void setSource(String v){source=v;}
  public String getTags(){return tags;} public void setTags(String v){tags=v;}
  public String getStage(){return stage;} public void setStage(String v){stage=v;}
  public Instant getUploadedAt(){return uploadedAt;} public String getExtractedText(){return extractedText;} public void setExtractedText(String v){extractedText=v;}
}
