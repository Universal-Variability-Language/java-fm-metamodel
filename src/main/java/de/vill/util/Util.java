package de.vill.util;

import de.vill.config.Configuration;
import de.vill.model.constraint.*;
import de.vill.model.pbc.Literal;
import de.vill.model.pbc.PBCConstraint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
            return ((NotConstraint) constraint).getContent() instanceof LiteralConstraint;
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
            return ((NotConstraint) constraint).getContent() instanceof LiteralConstraint;
        }
        if(constraint instanceof OrConstraint && isJustOr(((OrConstraint) constraint).getLeft()) && isJustOr(((OrConstraint) constraint).getRight())){
            return true;
        }
        return false;
    }

    public static List<PBCConstraint> transformImplicationMap (HashMap<Integer, List<PBCConstraint>> implicationMap, int counter){
        List<PBCConstraint> resultList = new LinkedList<>();
        int max = 0;
        for (Map.Entry<Integer, List<PBCConstraint>> entry : implicationMap.entrySet()) {
            int x = entry.getKey();
            if(x > max) {
                max = x;
            }
            for(PBCConstraint constraint : entry.getValue()){
                //x <=> constraint
                PBCConstraint c2 = new PBCConstraint();
                c2.literalList = new LinkedList<>();
                c2.k = constraint.k;
                for(Literal lit : constraint.literalList){
                    Literal l2 = new Literal();
                    l2.factor = lit.factor;
                    l2.name = lit.name;
                    c2.literalList.add(l2);
                }

                //-x v constraint
                int f = constraint.k;
                for(Literal lit : constraint.literalList){
                    f += Math.abs(lit.factor);
                }
                Literal l1 = new Literal();
                l1.name = "x_" + counter + "_" + x;
                l1.factor = -f;
                constraint.k = constraint.k - f;
                constraint.literalList.add(l1);
                resultList.add(constraint);

                //x v -constraint
                for(Literal lit : c2.literalList){
                    lit.factor = -1 * lit.factor;
                }
                c2.k = -1 * c2.k + 1;
                f = c2.k;
                for(Literal lit : c2.literalList){
                    f += Math.abs(lit.factor);
                }
                Literal l2 = new Literal();
                l2.name = "x_" + counter + "_" + x;
                l2.factor = f;
                c2.literalList.add(l2);
                resultList.add(c2);
            }

        }
        PBCConstraint finalPBCConstraint = new PBCConstraint();
        finalPBCConstraint.literalList = new LinkedList<>();
        Literal l = new Literal();
        l.factor = 1;
        l.name = "x_" + counter + "_" + max;
        finalPBCConstraint.literalList.add(l);
        finalPBCConstraint.k = 1;
        resultList.add(finalPBCConstraint);
        return resultList;
    }

    public static HashMap<Integer, List<PBCConstraint>> transformSubFormulas(HashMap<Integer, Constraint> subformulas){
        HashMap<Integer, List<PBCConstraint>> resultMap = new HashMap<>();
        for (Map.Entry<Integer, Constraint> entry : subformulas.entrySet()) {
            resultMap.put(entry.getKey(), transformSubFormula(entry.getValue()));
        }
        return resultMap;
    }

    public static List<PBCConstraint> transformSubFormula(Constraint constraint){
        List<PBCConstraint> resultList = new LinkedList<>();
        if(constraint instanceof NotConstraint){
            resultList.add(transformNegLiteral((NotConstraint) constraint));
        } else if (constraint instanceof ImplicationConstraint) {
            resultList.add(transformImplication((ImplicationConstraint) constraint));
        } else if (constraint instanceof EquivalenceConstraint) {
            resultList.addAll(transformBiImplication((EquivalenceConstraint) constraint));
        }
        else if (constraint instanceof AndConstraint) {
            resultList.add(transformAnd((AndConstraint) constraint));
        }
        else if (constraint instanceof OrConstraint) {
            var orConstraint = transformOr((OrConstraint) constraint);
            orConstraint.k = orConstraint.k + 1;
            resultList.add(orConstraint);
        }
        return resultList;
    }

    public static PBCConstraint transformNegLiteral(NotConstraint constraint){
        Literal literal = new Literal();
        literal.name = ((LiteralConstraint)constraint.getContent()).getReference().getIdentifier();
        literal.factor = -1;
        PBCConstraint pbcConstraint = new PBCConstraint();
        pbcConstraint.k = 0;
        pbcConstraint.literalList = new LinkedList<>();
        pbcConstraint.literalList.add(literal);
        return pbcConstraint;
    }

    public static PBCConstraint transformImplication(ImplicationConstraint constraint){
        Constraint c1 = constraint.getLeft();
        Constraint c2 = constraint.getRight();
        Literal l1 = new Literal();
        Literal l2 = new Literal();
        if(c1 instanceof NotConstraint && c2 instanceof NotConstraint){
            l1.name = ((LiteralConstraint)(((NotConstraint) c1).getContent())).getReference().getIdentifier();
            l2.name = ((LiteralConstraint)(((NotConstraint) c2).getContent())).getReference().getIdentifier();
            l1.factor = 1;
            l2.factor = -1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = 0;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);
            return pbcConstraint1;
        }else if(c1 instanceof LiteralConstraint && c2 instanceof LiteralConstraint){
            l1.name = ((LiteralConstraint)(c1)).getReference().getIdentifier();
            l2.name = ((LiteralConstraint)(c2)).getReference().getIdentifier();
            l1.factor = -1;
            l2.factor = 1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = 0;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);
            return pbcConstraint1;
        }else if (c1 instanceof NotConstraint) {
            l1.name = ((LiteralConstraint) (((NotConstraint) c1).getContent())).getReference().getIdentifier();
            l1.factor = 1;
            l2.name = ((LiteralConstraint) (c2)).getReference().getIdentifier();
            l2.factor = 1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = 1;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);
            return pbcConstraint1;
        }else{
            l1.name = ((LiteralConstraint)(c1)).getReference().getIdentifier();
            l2.name = ((LiteralConstraint) (((NotConstraint) c2).getContent())).getReference().getIdentifier();
            l1.factor = -1;
            l2.factor = -1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = -1;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);
            return pbcConstraint1;
        }
    }

    public static List<PBCConstraint> transformBiImplication(EquivalenceConstraint constraint){
        Constraint c1 = constraint.getLeft();
        Constraint c2 = constraint.getRight();
        Literal l1 = new Literal();
        Literal l2 = new Literal();
        if(c1 instanceof NotConstraint && c2 instanceof NotConstraint){
            l1.name = ((LiteralConstraint)(((NotConstraint) c1).getContent())).getReference().getIdentifier();
            l2.name = ((LiteralConstraint)(((NotConstraint) c2).getContent())).getReference().getIdentifier();
            l1.factor = 1;
            l2.factor = -1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = 0;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);

            Literal l1_1 = new Literal();
            Literal l2_1 = new Literal();
            l1_1.name = ((LiteralConstraint)(((NotConstraint) c1).getContent())).getReference().getIdentifier();
            l2_1.name = ((LiteralConstraint)(((NotConstraint) c2).getContent())).getReference().getIdentifier();
            l1_1.factor = -1;
            l2_1.factor = 1;
            PBCConstraint pbcConstraint2 = new PBCConstraint();
            pbcConstraint2.k = 0;
            pbcConstraint2.literalList = new LinkedList<>();
            pbcConstraint2.literalList.add(l1_1);
            pbcConstraint2.literalList.add(l2_1);

            List<PBCConstraint> constraintList = new LinkedList<>();
            constraintList.add(pbcConstraint1);
            constraintList.add(pbcConstraint2);
            return constraintList;
        }else if(c1 instanceof LiteralConstraint && c2 instanceof LiteralConstraint){
            l1.name = ((LiteralConstraint)(c1)).getReference().getIdentifier();
            l2.name = ((LiteralConstraint)(c2)).getReference().getIdentifier();
            l1.factor = 1;
            l2.factor = -1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = 0;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);

            Literal l1_1 = new Literal();
            Literal l2_1 = new Literal();
            l1_1.name = ((LiteralConstraint)(c1)).getReference().getIdentifier();
            l2_1.name = ((LiteralConstraint)(c2)).getReference().getIdentifier();
            l1_1.factor = -1;
            l2_1.factor = 1;
            PBCConstraint pbcConstraint2 = new PBCConstraint();
            pbcConstraint2.k = 0;
            pbcConstraint2.literalList = new LinkedList<>();
            pbcConstraint2.literalList.add(l1_1);
            pbcConstraint2.literalList.add(l2_1);

            List<PBCConstraint> constraintList = new LinkedList<>();
            constraintList.add(pbcConstraint1);
            constraintList.add(pbcConstraint2);
            return constraintList;
        }else if (c1 instanceof NotConstraint) {
            l1.name = ((LiteralConstraint) (((NotConstraint) c1).getContent())).getReference().getIdentifier();
            l1.factor = -1;
            l2.name = ((LiteralConstraint) (c2)).getReference().getIdentifier();
            l2.factor = -1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = -1;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);

            Literal l1_1 = new Literal();
            l1_1.name = ((LiteralConstraint) (((NotConstraint) c1).getContent())).getReference().getIdentifier();
            l1_1.factor = 1;
            Literal l2_1 = new Literal();
            l2_1.name = ((LiteralConstraint) c2).getReference().getIdentifier();
            l2_1.factor = 1;
            PBCConstraint pbcConstraint2 = new PBCConstraint();
            pbcConstraint2.k = 1;
            pbcConstraint2.literalList = new LinkedList<>();
            pbcConstraint2.literalList.add(l1);
            pbcConstraint2.literalList.add(l2);
            List<PBCConstraint> constraintList = new LinkedList<>();
            constraintList.add(pbcConstraint1);
            constraintList.add(pbcConstraint2);
            return constraintList;
        }else{
            l1.name = ((LiteralConstraint)(c1)).getReference().getIdentifier();
            l2.name = ((LiteralConstraint) (((NotConstraint) c2).getContent())).getReference().getIdentifier();
            l1.factor = 1;
            l2.factor = 1;
            PBCConstraint pbcConstraint1 = new PBCConstraint();
            pbcConstraint1.k = 1;
            pbcConstraint1.literalList = new LinkedList<>();
            pbcConstraint1.literalList.add(l1);
            pbcConstraint1.literalList.add(l2);

            Literal l1_1 = new Literal();
            Literal l2_1 = new Literal();
            l1_1.name = ((LiteralConstraint)(c1)).getReference().getIdentifier();
            l2_1.name = ((LiteralConstraint) (((NotConstraint) c2).getContent())).getReference().getIdentifier();
            l1_1.factor = -1;
            l2_1.factor = -1;
            PBCConstraint pbcConstraint2 = new PBCConstraint();
            pbcConstraint2.k = -1;
            pbcConstraint2.literalList = new LinkedList<>();
            pbcConstraint2.literalList.add(l1_1);
            pbcConstraint2.literalList.add(l2_1);

            List<PBCConstraint> constraintList = new LinkedList<>();
            constraintList.add(pbcConstraint1);
            constraintList.add(pbcConstraint2);
            return constraintList;
        }
    }

    public static PBCConstraint transformAnd(Constraint constraint){
        if(constraint instanceof AndConstraint){
            PBCConstraint pbcConstraint1 = transformAnd(((AndConstraint) constraint).getLeft());
            PBCConstraint pbcConstraint2 = transformAnd(((AndConstraint) constraint).getRight());
            pbcConstraint1.k = pbcConstraint1.k + pbcConstraint2.k;
            pbcConstraint1.literalList.addAll(pbcConstraint2.literalList);
            return pbcConstraint1;
        }else{
            Literal l1 = new Literal();
            PBCConstraint pbcConstraint = new PBCConstraint();
            if(constraint instanceof NotConstraint){
                l1.name = ((LiteralConstraint)((NotConstraint) constraint).getContent()).getReference().getIdentifier();
                l1.factor = -1;
                pbcConstraint.k = 0;
            }else{
                l1.name = ((LiteralConstraint)constraint).getReference().getIdentifier();
                l1.factor = 1;
                pbcConstraint.k = 1;
            }

            pbcConstraint.literalList = new LinkedList<>();
            pbcConstraint.literalList.add(l1);
            return pbcConstraint;
        }
    }

    public static PBCConstraint transformOr(Constraint constraint){
        if(constraint instanceof OrConstraint){
            PBCConstraint pbcConstraint1 = transformOr(((OrConstraint) constraint).getLeft());
            PBCConstraint pbcConstraint2 = transformOr(((OrConstraint) constraint).getRight());
            pbcConstraint1.k = pbcConstraint1.k + pbcConstraint2.k;
            pbcConstraint1.literalList.addAll(pbcConstraint2.literalList);
            return pbcConstraint1;
        }else{
            Literal l1 = new Literal();
            PBCConstraint pbcConstraint = new PBCConstraint();
            if(constraint instanceof NotConstraint){
                l1.name = ((LiteralConstraint)((NotConstraint) constraint).getContent()).getReference().getIdentifier();
                l1.factor = -1;
                pbcConstraint.k = -1;
            }else{
                l1.name = ((LiteralConstraint)constraint).getReference().getIdentifier();
                l1.factor = 1;
                pbcConstraint.k = 0;
            }

            pbcConstraint.literalList = new LinkedList<>();
            pbcConstraint.literalList.add(l1);
            return pbcConstraint;
        }
    }
    
}
