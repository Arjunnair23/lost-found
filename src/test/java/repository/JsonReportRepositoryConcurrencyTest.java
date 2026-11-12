package repository;

import model.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storage.JsonStore;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonReportRepositoryConcurrencyTest {
    @TempDir
    Path tempDirectory;

    @Test
    void concurrentCreatesAcrossRepositoryInstancesKeepEveryReport() throws Exception {
        int reportCount = 32;
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> creates = new ArrayList<>();
            for (int i = 0; i < reportCount; i++) {
                int index = i;
                creates.add(() -> {
                    JsonReportRepository repository =
                            new JsonReportRepository(new JsonStore(tempDirectory));
                    repository.create(report("Concurrent item " + index));
                    return null;
                });
            }

            for (var result : executor.invokeAll(creates)) {
                result.get();
            }

            assertEquals(reportCount,
                    new JsonReportRepository(new JsonStore(tempDirectory)).list("LOST").size());
        } finally {
            executor.shutdownNow();
        }
    }

    private Report report(String title) {
        Report report = new Report();
        report.setId(UUID.randomUUID().toString());
        report.setOwnerId("owner");
        report.setType("LOST");
        report.setTitle(title);
        report.setCategory("Other");
        report.setDescription("");
        report.setLocation("Library");
        report.setReportDate(LocalDate.of(2026, 4, 23));
        report.setStatus("LOST");
        report.setPrivateContact("owner@example.edu");
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        return report;
    }
}
