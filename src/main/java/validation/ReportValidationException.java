package validation;

public class ReportValidationException extends IllegalArgumentException {
    private final String field;

    public ReportValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
