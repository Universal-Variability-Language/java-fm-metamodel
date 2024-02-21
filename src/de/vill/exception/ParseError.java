package de.vill.exception;

public class ParseError extends RuntimeException {

	private static final long serialVersionUID = 1L;

    private int line = 0;
    private int charPositionInLine = 0;

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
    
	public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%s (at %d:%d)", getMessage(), line, charPositionInLine);
    }
}
