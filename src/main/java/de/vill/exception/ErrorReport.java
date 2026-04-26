package de.vill.exception;

public class ErrorReport {

    private final int line;
    private final int charPosition;
    private final ErrorCategory category;
    private final ErrorField field;
    private final String message;
    private final String reference;
    private final String cause;
    private final String hint;

    private ErrorReport(Builder builder) {
        this.line = builder.line;
        this.charPosition = builder.charPosition;
        this.category = builder.category;
        this.field = builder.field;
        this.message = builder.message;
        this.reference = builder.reference;
        this.cause = builder.cause;
        this.hint = builder.hint;
    }

    public int getLine() {
        return line;
    }

    public int getCharPosition() {
        return charPosition;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public ErrorField getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    public String getReference() {
        return reference;
    }

    public String getCause() {
        return cause;
    }

    public String getHint() {
        return hint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(category).append("] ");
        sb.append(message);
        sb.append(" (at ").append(line).append(":").append(charPosition).append(")");
        if (reference != null) {
            sb.append("\n  Element: ").append(reference);
        }
        if (field != null) {
            sb.append("\n  Field: ").append(field);
        }
        if (cause != null) {
            sb.append("\n  Possible cause: ").append(cause);
        }
        if (hint != null) {
            sb.append("\n  Hint: ").append(hint);
        }
        return sb.toString();
    }

    public static class Builder {
        private int line;
        private int charPosition;
        private ErrorCategory category;
        private ErrorField field;
        private String message;
        private String reference;
        private String cause;
        private String hint;

        public Builder(ErrorCategory category, String message) {
            this.category = category;
            this.message = message;
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder charPosition(int charPosition) {
            this.charPosition = charPosition;
            return this;
        }

        public Builder field(ErrorField field) {
            this.field = field;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder cause(String cause) {
            this.cause = cause;
            return this;
        }

        public Builder hint(String hint) {
            this.hint = hint;
            return this;
        }

        public ErrorReport build() {
            return new ErrorReport(this);
        }
    }
}
