package repository;
import model.Report;
import storage.JsonStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonReportRepository implements ReportRepository {
 private static final Object LOCK = new Object();
 private final JsonStore store;

 public JsonReportRepository() {
  this(new JsonStore());
 }

 public JsonReportRepository(JsonStore store) {
  this.store = store;
 }

 private List<Report> data(String type) {
  return "FOUND".equals(type) ? store.found() : store.lost();
 }

 private void save(String type, List<Report> reports) {
  if ("FOUND".equals(type)) {
   store.saveFound(reports);
  } else {
   store.saveLost(reports);
  }
 }

 @Override
 public List<Report> list(String type) {
  synchronized (LOCK) {
   return new ArrayList<>(data(type));
  }
 }

 @Override
 public List<Report> mine(String type, String owner) {
  synchronized (LOCK) {
   return data(type).stream().filter(report -> owner.equals(report.getOwnerId())).toList();
  }
 }

 @Override
 public Optional<Report> find(String type, String id) {
  synchronized (LOCK) {
   return data(type).stream().filter(report -> id.equals(report.getId())).findFirst();
  }
 }

 @Override
 public void create(Report report) {
  synchronized (LOCK) {
   List<Report> reports = data(report.getType());
   reports.add(report);
   save(report.getType(), reports);
  }
 }

 @Override
 public boolean update(Report report) {
  synchronized (LOCK) {
   List<Report> reports = data(report.getType());
   for (int i = 0; i < reports.size(); i++) {
    if (reports.get(i).getId().equals(report.getId())) {
     reports.set(i, report);
     save(report.getType(), reports);
     return true;
    }
   }
   return false;
  }
 }

 @Override
 public boolean markReturned(String type, String id, Instant updatedAt) {
  synchronized (LOCK) {
   List<Report> reports = data(type);
   for (Report report : reports) {
    if (report.getId().equals(id)) {
     if (!type.equals(report.getStatus())) {
      return false;
     }
     report.setStatus("RETURNED");
     report.setUpdatedAt(updatedAt);
     save(type, reports);
     return true;
    }
   }
   return false;
  }
 }

 @Override
 public boolean delete(String type, String id) {
  synchronized (LOCK) {
   List<Report> reports = data(type);
   boolean removed = reports.removeIf(report -> report.getId().equals(id));
   if (removed) {
    save(type, reports);
   }
   return removed;
  }
 }
}