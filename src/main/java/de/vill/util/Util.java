package de.vill.util;

import de.vill.config.Configuration;
import de.vill.model.constraint.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Util {
    public static String indentEachLine(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split(Configuration.getNewlineSymbol());
        for (String line : lines) {
            result.append(Configuration.getTabulatorSymbol());
            result.append(line);
            result.append(Configuration.getNewlineSymbol());
        }
        return result.toString();
    }

    public static String addNecessaryQuotes(String reference) {
        String[] parts = reference.split("\\.");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
                result.append("\"");
                result.append(part);
                result.append("\"");
            } else {
                result.append(part);
            }
            result.append(".");
        }
        if (result.length() > 0) {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    public static String readFileContent(Path file) {
        try {
            return new String(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isJustAnd(Constraint constraint){
        if(constraint instanceof ParenthesisConstraint){
            return isJustAnd(((ParenthesisConstraint) constraint).getContent());
        }
        if(constraint instanceof LiteralConstraint){
            return true;
        }
        if(constraint instanceof NotConstraint){
            return isJustAnd(((NotConstraint) constraint).getContent());
        }
        if(constraint instanceof AndConstraint && isJustAnd(((AndConstraint) constraint).getLeft()) && isJustAnd(((AndConstraint) constraint).getRight())){
            return true;
        }
        return false;
    }

    public static boolean isJustOr(Constraint constraint){
        if(constraint instanceof ParenthesisConstraint){
            return isJustOr(((ParenthesisConstraint) constraint).getContent());
        }
        if(constraint instanceof LiteralConstraint){
            return true;
        }
        if(constraint instanceof NotConstraint){
            return isJustOr(((NotConstraint) constraint).getContent());
        }
        if(constraint instanceof OrConstraint && isJustOr(((OrConstraint) constraint).getLeft()) && isJustOr(((OrConstraint) constraint).getRight())){
            return true;
        }
        return false;
    }
}
