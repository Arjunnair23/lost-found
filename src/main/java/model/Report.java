package model;
import java.time.*;
public class Report {
 private String id, ownerId, type, title, description, category, location, status, imagePath, privateContact;
 private LocalDate reportDate; private Instant createdAt, updatedAt;
 public Report(){}
 public String getId(){return id;} public void setId(String v){id=v;} public String getOwnerId(){return ownerId;} public void setOwnerId(String v){ownerId=v;}
 public String getType(){return type;} public void setType(String v){type=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;}
 public String getLocation(){return location;} public void setLocation(String v){location=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getImagePath(){return imagePath;} public void setImagePath(String v){imagePath=v;} public String getPrivateContact(){return privateContact;} public void setPrivateContact(String v){privateContact=v;}
 public LocalDate getReportDate(){return reportDate;} public void setReportDate(LocalDate v){reportDate=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
 public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}