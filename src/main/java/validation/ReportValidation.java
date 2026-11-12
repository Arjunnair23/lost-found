package validation;

import model.Report;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ReportValidation {
    public static final Set<String> CATEGORIES = Set.of(
            "Electronics", "Books", "ID Card", "Wallet or Purse",
            "Clothing", "Keys", "Other"
    );

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );
    private static final Pattern INDIAN_MOBILE =
            Pattern.compile("(?:\\+91)?[6-9][0-9]{9}");

    private ReportValidation() {
    }

    public static Report create(Map<String, String> values, String type, Report existing) {
        String title = required(values.get("itemName"), "itemName", "Item name", 150);
        String category = required(values.get("category"), "category", "Category", 100);
        if (!CATEGORIES.contains(category)) {
            throw new ReportValidationException("category", "Select an allowed category.");
        }

        String description = optional(values.get("description"), "description", "Description", 2000);
        String locationField = "LOST".equals(type) ? "lostLocation" : "foundLocation";
        String dateField = "LOST".equals(type) ? "lostDate" : "foundDate";
        String location = required(values.get(locationField), locationField, "Location", 255);
        LocalDate reportDate = reportDate(values.get(dateField), dateField);

        String contact = values.get("contactInfo");
        if (contact == null && existing != null) {
            contact = existing.getPrivateContact();
        } else {
            contact = required(contact, "contactInfo", "Contact information", 150);
            validateContact(contact);
        }

        Report report = new Report();
        report.setType(type);
        report.setTitle(title);
        report.setCategory(category);
        report.setDescription(description);
        report.setLocation(location);
        report.setPrivateContact(contact);
        report.setReportDate(reportDate);
        report.setStatus(existing == null ? type : existing.getStatus());
        return report;
    }

    private static String required(String value, String field, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ReportValidationException(field, label + " is required.");
        }
        if (value.length() > maxLength) {
            throw new ReportValidationException(field, label + " must be no more than " + maxLength + " characters.");
        }
        return value.trim();
    }

    private static String optional(String value, String field, String label, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() > maxLength) {
            throw new ReportValidationException(field, label + " must be no more than " + maxLength + " characters.");
        }
        return value.trim();
    }

    private static LocalDate reportDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ReportValidationException(field, "Report date is required.");
        }
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new ReportValidationException(field, "Enter a valid date in YYYY-MM-DD format.");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ReportValidationException(field, "Enter a real date in YYYY-MM-DD format.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ReportValidationException(field, "Report date cannot be after today.");
        }
        return date;
    }

    private static void validateContact(String contact) {
        String trimmed = contact.trim();
        String compactPhone = trimmed.replace(" ", "").replace("-", "");
        String localPart = trimmed.contains("@") ? trimmed.substring(0, trimmed.indexOf('@')) : "";
        boolean validEmail = EMAIL.matcher(trimmed).matches()
                && localPart.length() <= 64
                && !localPart.startsWith(".")
                && !localPart.endsWith(".")
                && !localPart.contains("..");
        if (!validEmail && !INDIAN_MOBILE.matcher(compactPhone).matches()) {
            throw new ReportValidationException(
                    "contactInfo",
                    "Enter a valid email address or Indian 10-digit mobile number."
            );
        }
    }
}
