package repository;

import model.Report;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {
    List<Report> list(String type);
    List<Report> mine(String type, String owner);
    Optional<Report> find(String type, String id);
    void create(Report report);
    boolean update(Report report);
    boolean markReturned(String type, String id, Instant updatedAt);
    boolean delete(String type, String id);
}