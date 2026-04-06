package de.vill.main;

import de.vill.exception.ErrorCategory;
import de.vill.exception.ErrorField;
import de.vill.exception.ErrorReport;
import de.vill.exception.ParseError;
import org.antlr.v4.runtime.*;

import java.util.List;
import java.util.regex.*;

public class UVLErrorListener extends BaseErrorListener {

    private final List<ParseError> errorList;

    public UVLErrorListener(List<ParseError> errorList) {
        this.errorList = errorList;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPosition, String message, RecognitionException e) {
        ErrorReport report = translateToReport(message, offendingSymbol, recognizer, line, charPosition);
        errorList.add(new ParseError(report));
    }

    private ErrorReport translateToReport(String message, Object offendingSymbol, Recognizer<?, ?> recognizer, int line, int charPosition) {
        // Token recognition error -> LEXICAL
        Matcher m = Pattern.compile("token recognition error at: '(.*)'").matcher(message);
        if (m.find()) {
            String character = m.group(1);
            return new ErrorReport.Builder(ErrorCategory.LEXICAL,
                    "Unexpected character '" + character + "'")
                    .line(line).charPosition(charPosition)
                    .reference(character)
                    .cause("The character '" + character + "' is not supported in UVL.")
                    .hint("Remove or replace the unsupported character.")
                    .build();
        }

        // Missing token -> SYNTAX
        m = Pattern.compile("missing '?([<>\\w]+)'? at '?(.*?)'?$").matcher(message);
        if (m.find()) {
            String missing = tokenToReadable(m.group(1));
            String found = tokenToReadable(m.group(2));
            return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "Missing " + missing + " before " + found)
                    .line(line).charPosition(charPosition)
                    .cause("Expected " + missing + " but found " + found + " instead.")
                    .hint("Add " + missing + " before " + found + ".")
                    .build();
        }

        // Extraneous input -> SYNTAX
        m = Pattern.compile("extraneous input '?(.*?)'? expecting (.*)").matcher(message);
        if (m.find()) {
            String extra = tokenToReadable(m.group(1));
            return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "Unexpected input " + extra)
                    .line(line).charPosition(charPosition)
                    .reference(m.group(1))
                    .cause("Found " + extra + " where a valid UVL element was expected.")
                    .hint("Remove the unexpected input or check the indentation.")
                    .build();
        }

        // Mismatched input -> SYNTAX
        m = Pattern.compile("mismatched input '?(.*?)'? expecting (.*)").matcher(message);
        if (m.find()) {
            String found = tokenToReadable(m.group(1));
            String expected = simplifySet(m.group(2));

            // Sonderfall: nach Gruppierung ohne Features
            // ANTLR meldet entweder "a new line" oder ein Gruppen-Keyword als found,
            // wenn eine Gruppe leer ist und expected "an indentation" ist
            if (expected.equals("an indentation") && (found.equals("a new line")
                    || found.contains("'mandatory'") || found.contains("'optional'")
                    || found.contains("'alternative'") || found.contains("'or'"))) {
                return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                        "Missing features after group type")
                        .line(line).charPosition(charPosition)
                        .field(ErrorField.GROUP)
                        .cause("A group type ('optional', 'or', 'mandatory', 'alternative') must be followed by at least one feature.")
                        .hint("Add at least one child feature below the group type.")
                        .build();
            }

            // Sonderfall: mehr als ein root Feature
            if (expected.equals("an indentation or a dedentation")) {
                return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                        "More than one root feature detected")
                        .line(line).charPosition(charPosition)
                        .cause("A UVL model can only have one root feature.")
                        .hint("Check if a group type ('mandatory', 'optional', 'or', 'alternative') is missing.")
                        .build();
            }

            return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "Found " + found + " but expected " + expected)
                    .line(line).charPosition(charPosition)
                    .cause("The parser expected " + expected + " at this position.")
                    .hint("Replace " + found + " with " + expected + ".")
                    .build();
        }

        // No viable alternative
        m = Pattern.compile("no viable alternative at input '(.*)'").matcher(message);
        if (m.find()) {
            String input = m.group(1).replace("\n", "").trim();
            return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "No valid interpretation for '" + input + "'")
                    .line(line).charPosition(charPosition)
                    .reference(input)
                    .cause("The input '" + input + "' does not match any valid UVL structure at this position.")
                    .hint("Check if a keyword like 'features', 'constraints', or 'imports' is missing, or if the indentation is correct.")
                    .build();
        }

        // Fallback
        return new ErrorReport.Builder(ErrorCategory.SYNTAX, message)
                .line(line).charPosition(charPosition)
                .cause("An unexpected syntax error occurred.")
                .hint("Check the UVL syntax around this position.")
                .build();
    }

    // Translating ANTLR tokens 
    private String tokenToReadable(String token) {
        switch (token) {
            case "<INDENT>": return "an indentation";
            case "<DEDENT>": return "a dedentation";
            case "<EOF>": case "EOF": return "end of file";
            case "\\n": return "a new line";
            case "features": return "'features' keyword";
            case "constraints": return "'constraints' keyword";
            case "imports": return "'imports' keyword";
            case "mandatory": return "'mandatory' group";
            case "optional": return "'optional' group";
            case "alternative": return "'alternative' group";
            case "or": return "'or' group";
            default: return "'" + token + "'";
        }
    }

    private String simplifySet(String expectedSet) {
        String cleaned = expectedSet.replaceAll("[{}]", "").trim();
        String[] tokens = cleaned.split(",\\s*");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                if (i == tokens.length - 1) {
                    sb.append(" or ");
                } else {
                    sb.append(", ");
                }
            }
            sb.append(tokenToReadable(tokens[i].replace("'", "").trim()));
        }
        return sb.toString();
    }
}
