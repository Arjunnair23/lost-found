package validation;

import model.Report;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportValidationTest {
    @Test
    void requiresContactAndAcceptsEmailOrIndianMobileNumbers() {
        Map<String, String> values = validValues("LOST");
        Map<String, String> missing = validValues("LOST");
        missing.remove("contactInfo");
        assertEquals("contactInfo", validationError(missing, "LOST", null).getField());
        values.put("contactInfo", " \t ");
        assertEquals("contactInfo", assertThrows(ReportValidationException.class,
                () -> ReportValidation.create(values, "LOST", null)).getField());

        for (String valid : new String[]{
                "person@example.edu",
                "9876543210",
                "+91 98765-43210",
                "9 8 7 6 5 4 3 2 1 0"
        }) {
            values.put("contactInfo", valid);
            assertDoesNotThrow(() -> ReportValidation.create(values, "LOST", null), valid);
        }

        for (String invalid : new String[]{"abc", "1234567890", "+91 1234567890", "a@b", "a@@b.com"}) {
            values.put("contactInfo", invalid);
            assertEquals("contactInfo", assertThrows(ReportValidationException.class,
                    () -> ReportValidation.create(values, "LOST", null)).getField());
        }
    }

    @Test
    void omittedEditContactPreservesSavedValueAndSuppliedContactIsValidated() {
        Report saved = ReportValidation.create(validValues("FOUND"), "FOUND", null);
        saved.setStatus("RETURNED");
        Map<String, String> values = validValues("FOUND");
        values.remove("contactInfo");
        assertEquals(saved.getPrivateContact(),
                ReportValidation.create(values, "FOUND", saved).getPrivateContact());
        assertEquals("RETURNED",
                ReportValidation.create(values, "FOUND", saved).getStatus());

        Report legacyWithoutContact = new Report();
        assertNull(ReportValidation.create(values, "FOUND", legacyWithoutContact).getPrivateContact());

        values.put("contactInfo", " \t ");
        assertEquals("contactInfo", assertThrows(ReportValidationException.class,
                () -> ReportValidation.create(values, "FOUND", saved)).getField());
        values.put("contactInfo", "not a contact");
        assertThrows(ReportValidationException.class,
                () -> ReportValidation.create(values, "FOUND", saved));
        values.put("contactInfo", "new@example.edu");
        assertEquals("new@example.edu",
                ReportValidation.create(values, "FOUND", saved).getPrivateContact());
    }

    @Test
    void acceptsTodayAndPastDatesAndRejectsFutureAndImpossibleDatesForBothTypes() {
        for (String type : new String[]{"LOST", "FOUND"}) {
            Map<String, String> values = validValues(type);
            String dateField = type.equals("LOST") ? "lostDate" : "foundDate";
            values.put(dateField, LocalDate.now().toString());
            assertDoesNotThrow(() -> ReportValidation.create(values, type, null));
            values.put(dateField, LocalDate.now().minusDays(1).toString());
            assertDoesNotThrow(() -> ReportValidation.create(values, type, null));

            values.put(dateField, LocalDate.now().plusDays(1).toString());
            assertEquals(dateField, assertThrows(ReportValidationException.class,
                    () -> ReportValidation.create(values, type, null)).getField());
            values.put(dateField, "2024-02-30");
            assertTrue(assertThrows(ReportValidationException.class,
                    () -> ReportValidation.create(values, type, null)).getMessage().contains("real date"));
            values.put(dateField, "2024-2-03");
            assertTrue(assertThrows(ReportValidationException.class,
                    () -> ReportValidation.create(values, type, null)).getMessage().contains("YYYY-MM-DD"));
        }
    }

    @Test
    void validatesRequiredWhitespaceAndLengthBoundaries() {
        Map<String, String> values = validValues("LOST");
        values.put("itemName", " ".repeat(150));
        assertEquals("itemName", validationError(values, "LOST", null).getField());

        values = validValues("LOST");
        values.put("itemName", "x".repeat(150));
        values.put("lostLocation", "y".repeat(255));
        values.put("description", "z".repeat(2000));
        assertValid(values);
        values.put("itemName", "x".repeat(151));
        assertTrue(validationError(values, "LOST", null).getMessage().contains("150"));

        values = validValues("LOST");
        values.put("lostLocation", "y".repeat(256));
        assertTrue(validationError(values, "LOST", null).getMessage().contains("255"));
        values = validValues("LOST");
        values.put("description", "z".repeat(2001));
        assertTrue(validationError(values, "LOST", null).getMessage().contains("2000"));

        values = validValues("LOST");
        values.put("contactInfo", "6" + " ".repeat(140) + "123456789");
        assertEquals(150, values.get("contactInfo").length());
        assertValid(values);
        values.put("contactInfo", values.get("contactInfo") + " ");
        assertTrue(validationError(values, "LOST", null).getMessage().contains("150"));
    }

    @Test
    void rejectsWhitespaceOnlyRequiredReportFields() {
        for (String field : new String[]{"itemName", "category", "lostLocation", "lostDate", "contactInfo"}) {
            Map<String, String> values = validValues("LOST");
            values.put(field, " \t ");
            assertEquals(field, validationError(values, "LOST", null).getField());
        }
    }

    @Test
    void acceptsOnlyStandardCategoriesAndRequiresCategory() {
        Map<String, String> values = validValues("LOST");
        for (String category : ReportValidation.CATEGORIES) {
            values.put("category", category);
            assertDoesNotThrow(() -> ReportValidation.create(values, "LOST", null));
        }
        values.put("category", "Old stored category");
        assertEquals("category", assertThrows(ReportValidationException.class,
                () -> ReportValidation.create(values, "LOST", null)).getField());
        values.put("category", " ".repeat(5));
        assertEquals("category", assertThrows(ReportValidationException.class,
                () -> ReportValidation.create(values, "LOST", null)).getField());
        values.put("category", "x".repeat(101));
        assertTrue(validationError(values, "LOST", null).getMessage().contains("100"));
        values.put("category", "x".repeat(100));
        assertEquals("Select an allowed category.",
                validationError(values, "LOST", null).getMessage());
    }

    private Map<String, String> validValues(String type) {
        Map<String, String> values = new HashMap<>();
        values.put("itemName", "Lost keys");
        values.put("category", "Keys");
        values.put("description", "");
        values.put(type.equals("LOST") ? "lostLocation" : "foundLocation", "Library");
        values.put(type.equals("LOST") ? "lostDate" : "foundDate", LocalDate.now().toString());
        values.put("contactInfo", "student@example.edu");
        return values;
    }

    private void assertValid(Map<String, String> values) {
        assertDoesNotThrow(() -> ReportValidation.create(values, "LOST", null));
    }

    private ReportValidationException validationError(Map<String, String> values, String type, Report existing) {
        return assertThrows(ReportValidationException.class,
                () -> ReportValidation.create(values, type, existing));
    }
}
