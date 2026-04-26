package de.vill.main;

import de.vill.exception.ErrorCategory;
import de.vill.exception.ErrorField;
import de.vill.exception.ErrorReport;
import de.vill.exception.ParseError;
import org.antlr.v4.runtime.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.*;

public class UVLErrorListener extends BaseErrorListener {

    private static final Set<String> GROUP_KEYWORDS = new HashSet<>(Arrays.asList( "mandatory", "optional", "alternative", "or"));

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
        
        // Token recognition error
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

        // Missing token
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

        // Extraneous input
        m = Pattern.compile("extraneous input '?(.*?)'? expecting (.*)").matcher(message);
        if (m.find()) {
            String extraString = m.group(1);
            String extra = tokenToReadable(extraString);
            if (Character.isDigit(extraString.charAt(0))){
                return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "Wrong feature name: " + extra)
                    .line(line).charPosition(charPosition)
                    .reference(m.group(1))
                    .cause("Invalid feature name. Feature names can not start with a number.")
                    .hint("Rename the feature.")
                    .build();
            }
            return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "Unexpected input " + extra)
                    .line(line).charPosition(charPosition)
                    .reference(m.group(1))
                    .cause("Found " + extra + " where a valid UVL element was expected.")
                    .hint("Remove the unexpected input or check the indentation.")
                    .build();
        }

        // Mismatched input
        m = Pattern.compile("mismatched input '?(.*?)'? expecting (.*)").matcher(message);
        if (m.find()) {
            String found = tokenToReadable(m.group(1));
            String expected = simplifySet(m.group(2));

            // Case: group keywords without features/children
            if (expected.equals("an indentation") && (found.equals("a new line") || found.contains("'mandatory'") || found.contains("'optional'")|| found.contains("'alternative'") || found.contains("'or'"))) {
                return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                        "Missing features after group type")
                        .line(line).charPosition(charPosition)
                        .field(ErrorField.GROUP)
                        .cause("A group type ('optional', 'or', 'mandatory', 'alternative') must be followed by at least one feature.")
                        .hint("Add at least one child feature below the group type.")
                        .build();
            }

            // Case: More than one root feature
            if (expected.equals("an indentation or a dedentation")) {
                // Check if the offending token is actually a group keyword
                String offendingText = null;
                if (offendingSymbol instanceof Token) {
                    offendingText = ((Token) offendingSymbol).getText();
                }
                if (offendingText != null && GROUP_KEYWORDS.contains(offendingText.toLowerCase())) {
                    return new ErrorReport.Builder(ErrorCategory.SYNTAX,
                            "Group keyword '" + offendingText + "' at wrong indentation level")
                            .line(line).charPosition(charPosition)
                            .field(ErrorField.GROUP)
                            .reference(offendingText)
                            .cause("'" + offendingText + "' is a group type keyword but appears at the wrong indentation level.")
                            .hint("Indent '" + offendingText + "' further so it is nested under a parent feature.")
                            .build();
                }

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
