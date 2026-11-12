package service;

public class AccountValidationException extends IllegalArgumentException {
    private final String field;

    public AccountValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
