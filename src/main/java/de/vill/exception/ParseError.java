package de.vill.exception;

public class ParseError extends RuntimeException {

	private static final long serialVersionUID = 1L;

    private int line = 0;
    private int charPositionInLine = 0;
    private ErrorReport report;

    public ParseError(int line, int charPositionInLine, String errorMessage, Throwable err) {
        super(errorMessage, err);
        this.line = line;
        this.charPositionInLine = charPositionInLine;
    }

    public ParseError(String errorMessage) {
        super(errorMessage);
    }

    public ParseError(String errorMessage, int line) {
        super(errorMessage);
        this.line = line;
    }

    public ParseError(ErrorReport report) {
        super(report.getMessage());
        this.line = report.getLine();
        this.charPositionInLine = report.getCharPosition();
        this.report = report;
    }

	public int getLine() {
        return line;
    }

    public ErrorReport getReport() {
        return report;
    }

    @Override
    public String toString() {
        if (report != null) {
            return report.toString();
        }
        return String.format("%s (at %d:%d)", getMessage(), line, charPositionInLine);
    }
}
