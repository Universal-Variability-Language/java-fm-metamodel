package de.vill.util;

import de.vill.config.Configuration;
import de.vill.model.constraint.*;
import de.vill.model.expression.*;
import de.vill.model.pbc.Literal;
import de.vill.model.pbc.PBCLiteralConstraint;
import de.vill.model.pbc.PBConstraint;
import de.vill.model.pbc.PBConstraintType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static Constraint getMaxAndConstraint(Constraint constraint, Map<Integer, Constraint> substitutionMapping) {
        if (constraint instanceof AndConstraint){
            return new AndConstraint(
                    getMaxAndConstraint(((AndConstraint) constraint).getLeft(), substitutionMapping),
                    getMaxAndConstraint(((AndConstraint) constraint).getRight(), substitutionMapping ));
        }else{
            return constraint.extractTseitinSubConstraints(substitutionMapping);
        }
    }

    public static Constraint getMaxOrConstraint(Constraint constraint, Map<Integer, Constraint> substitutionMapping) {
        if (constraint instanceof OrConstraint){
            return new OrConstraint(
                    getMaxOrConstraint(((OrConstraint) constraint).getLeft(), substitutionMapping),
                    getMaxOrConstraint(((OrConstraint) constraint).getRight(), substitutionMapping ));
        }else{
            return constraint.extractTseitinSubConstraints(substitutionMapping);
        }
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

    public static List<PBConstraint> transformImplicationMap (HashMap<Integer, List<PBConstraint>> implicationMap){
        List<PBConstraint> resultList = new LinkedList<>();
        for (Map.Entry<Integer, List<PBConstraint>> entry : implicationMap.entrySet()) {
            int index = entry.getKey();
            for(PBConstraint constraint : entry.getValue()){
                resultList.addAll(substitutionConstraint(constraint, "x_" + index));
            }

        }
        return resultList;
    }

    public static List<PBConstraint> substitutionConstraint(PBConstraint constraint, String substitutionName) {
        List<PBConstraint> resultList = new LinkedList<>();
        // x <=> constraint is the same as -x v constraint AND x v -constraint
        // -x v constraint
        resultList.addAll(constraint.orWithLiteral(substitutionName, false));
        // x v -constraint
        resultList.addAll(constraint.negatedConstraint().orWithLiteral(substitutionName, true));
        return resultList;
    }

    public static HashMap<Integer, List<PBConstraint>> transformSubFormulas(HashMap<Integer, Constraint> subformulas, List<PBConstraint> additionalConstraints){
        HashMap<Integer, List<PBConstraint>> resultMap = new HashMap<>();
        for (Map.Entry<Integer, Constraint> entry : subformulas.entrySet()) {
            resultMap.put(entry.getKey(), transformSubFormula(entry.getValue(), additionalConstraints));
        }
        return resultMap;
    }

    public static List<PBConstraint> transformSubFormula(Constraint constraint, List<PBConstraint> additionalConstraints){
        List<PBConstraint> resultList = new LinkedList<>();
        if(constraint instanceof NotConstraint){
            System.err.println("error");
            System.exit(1);
            resultList.add(transformNegLiteral((NotConstraint) constraint));
        } else if (constraint instanceof ImplicationConstraint) {
            resultList.add(transformImplication((ImplicationConstraint) constraint));
        } else if (constraint instanceof EquivalenceConstraint) {
            resultList.add(transformBiImplication((EquivalenceConstraint) constraint));
        }
        else if (constraint instanceof AndConstraint) {
            resultList.add(transformAnd((AndConstraint) constraint));
        }
        else if (constraint instanceof OrConstraint) {
            var orConstraint = transformOr((OrConstraint) constraint);
            resultList.add(orConstraint);
        }
        else if (constraint instanceof ExpressionConstraint) {
            ExpressionConstraint expressionConstraint = (ExpressionConstraint) constraint;
            resultList.add(transformExpression(expressionConstraint, additionalConstraints));
        }
        return resultList;
    }

    public static PBConstraint transformNegLiteral(NotConstraint constraint){
        Literal literal = new Literal();
        literal.name = ((LiteralConstraint)constraint.getContent()).getReference().getIdentifier();
        literal.factor = -1;
        PBConstraint PBConstraint = new PBConstraint();
        PBConstraint.k = 0;
        PBConstraint.literalList = new LinkedList<>();
        PBConstraint.literalList.add(literal);
        return PBConstraint;
    }

    public static PBConstraint transformImplication(ImplicationConstraint constraint){
        PBCLiteralConstraint c1 = (PBCLiteralConstraint) constraint.getLeft();
        PBCLiteralConstraint c2 = (PBCLiteralConstraint) constraint.getRight();
        Literal l1 = new Literal(-1, c1.getReference().getIdentifier(), c1.sign);
        Literal l2 = new Literal(1, c2.getReference().getIdentifier(), c2.sign);

        PBConstraint PBConstraint1 = new PBConstraint();
        PBConstraint1.k = 0;
        PBConstraint1.literalList.add(l1);
        PBConstraint1.literalList.add(l2);
        return PBConstraint1;
    }

    public static PBConstraint transformBiImplication(EquivalenceConstraint constraint){
        PBCLiteralConstraint c1 = (PBCLiteralConstraint) constraint.getLeft();
        PBCLiteralConstraint c2 = (PBCLiteralConstraint) constraint.getRight();
        Literal l1 = new Literal(1, c1.getReference().getIdentifier(), c1.sign);
        Literal l2 = new Literal(-1, c2.getReference().getIdentifier(), c2.sign);

        PBConstraint pbConstraint = new PBConstraint();
        pbConstraint.k = 0;
        pbConstraint.type = PBConstraintType.EQ;
        pbConstraint.literalList = new LinkedList<>();
        pbConstraint.literalList.add(l1);
        pbConstraint.literalList.add(l2);

        return pbConstraint;

    }

    public static PBConstraint transformAnd(Constraint constraint){
        if(constraint instanceof AndConstraint){
            PBConstraint PBConstraint1 = transformAnd(((AndConstraint) constraint).getLeft());
            PBConstraint PBConstraint2 = transformAnd(((AndConstraint) constraint).getRight());
            PBConstraint1.k = PBConstraint1.k + PBConstraint2.k;
            PBConstraint1.literalList.addAll(PBConstraint2.literalList);
            return PBConstraint1;
        }else{
            LiteralConstraint c1;
            boolean sign;
            if (constraint instanceof PBCLiteralConstraint) {
                c1 = (LiteralConstraint) constraint;
                sign = ((PBCLiteralConstraint)constraint).sign;
            }else if (constraint instanceof LiteralConstraint) {
                c1 = (LiteralConstraint) constraint;
                sign = true;
            }else {
                c1 = (LiteralConstraint) ((NotConstraint) constraint).getContent();
                sign = false;
            }
            Literal l1 = new Literal();
            PBConstraint PBConstraint = new PBConstraint();

            l1.name = c1.getReference().getIdentifier();
            l1.factor = 1;
            l1.sign = sign;
            PBConstraint.k = 1;

            PBConstraint.literalList = new LinkedList<>();
            PBConstraint.literalList.add(l1);
            return PBConstraint;
        }
    }

    public static PBConstraint transformOr(Constraint constraint){
        if(constraint instanceof OrConstraint){
            PBConstraint PBConstraint1 = transformOr(((OrConstraint) constraint).getLeft());
            PBConstraint PBConstraint2 = transformOr(((OrConstraint) constraint).getRight());
            PBConstraint1.literalList.addAll(PBConstraint2.literalList);
            return PBConstraint1;
        }else {
            LiteralConstraint c1;
            boolean sign;
            if (constraint instanceof PBCLiteralConstraint) {
                c1 = (LiteralConstraint) constraint;
                sign = ((PBCLiteralConstraint)constraint).sign;;
            }else if (constraint instanceof LiteralConstraint) {
                c1 = (LiteralConstraint) constraint;
                sign = true;
            }else {
                c1 = (LiteralConstraint) ((NotConstraint) constraint).getContent();
                sign = false;
            }
            Literal l1 = new Literal();
            PBConstraint PBConstraint = new PBConstraint();

            l1.name = c1.getReference().getIdentifier();
            l1.factor = 1;
            l1.sign = sign;
            PBConstraint.k = 1;

            PBConstraint.literalList = new LinkedList<>();
            PBConstraint.literalList.add(l1);
            return PBConstraint;
        }
    }

    public static PBConstraint transformExpression(ExpressionConstraint constraint, List<PBConstraint> additionalConstraints) {
        List<Expression> allDenominators = new LinkedList<>();
        collectDenominators(constraint.getLeft(), allDenominators);
        collectDenominators(constraint.getRight(), allDenominators);
        additionalConstraints.addAll(getConstraintsToForbidZeroDivision(allDenominators));

        //transform everything to a sum
        var leftSum = constraint.getLeft().getAsSum(additionalConstraints);
        var rightSum = constraint.getRight().getAsSum(additionalConstraints);
        //take all numbers to the right side
        List<Literal> numbersFromLeftToRight = leftSum.stream().filter(x -> x.name == null).collect(Collectors.toList());
        for (Literal l : numbersFromLeftToRight){
            l.factor *= -1;
        }
        rightSum.addAll(numbersFromLeftToRight);
        leftSum = leftSum.stream().filter(x -> x.name != null).collect(Collectors.toList());
        //take all variables with factors to the left side
        List<Literal> numbersFromRightToLeft = rightSum.stream().filter(x -> x.name != null).collect(Collectors.toList());
        for (Literal l : numbersFromRightToLeft){
            l.factor *= -1;
        }
        leftSum.addAll(numbersFromRightToLeft);
        rightSum = rightSum.stream().filter(x -> x.name == null).collect(Collectors.toList());

        //sum app factors with same variable
        HashMap<String, Double> literalMap = new HashMap<>();
        for (Literal l : leftSum) {
            if (literalMap.containsKey(l.name)) {
                literalMap.put(l.name, literalMap.get(l.name) + l.factor);
            }else{
                literalMap.put(l.name, l.factor);
            }
        }

        //create constraint
        PBConstraint pbConstraint = new PBConstraint();
        if ("==".equals(constraint.getExpressionSymbol())) {
            pbConstraint.type = PBConstraintType.EQ;
        } else if ("<".equals(constraint.getExpressionSymbol())) {
            pbConstraint.type = PBConstraintType.LE;
        } else if (">".equals(constraint.getExpressionSymbol())) {
            pbConstraint.type = PBConstraintType.GE;
        } else if (">=".equals(constraint.getExpressionSymbol())) {
            pbConstraint.type = PBConstraintType.GEQ;
        } else if ("<=".equals(constraint.getExpressionSymbol())) {
            pbConstraint.type = PBConstraintType.LEQ;
        } else if ("!=".equals(constraint.getExpressionSymbol())) {
            pbConstraint.type = PBConstraintType.NOTEQ;
        }
        pbConstraint.k = rightSum.stream().map(x -> x.factor).reduce(0.0, Double::sum);
        pbConstraint.literalList = new LinkedList<>();
        for (Map.Entry<String,Double> e : literalMap.entrySet()) {
            pbConstraint.literalList.add(new Literal(e.getValue(), e.getKey(), true));
        }

        return pbConstraint;
    }

    public static void collectDenominators(Expression expression, List<Expression> denominators) {
        if (expression instanceof DivExpression){
            denominators.add(((DivExpression) expression).getRight());
        }
        for (Expression subExpression : expression.getExpressionSubParts()){
            collectDenominators(subExpression, denominators);
        }
    }

    public static Expression removeAllDenominators(Expression expression) {
        if (expression instanceof DivExpression){
            return removeAllDenominators(((DivExpression) expression).getLeft());
        }else {
            HashMap<Expression, Expression> newSubParts = new HashMap<>();
            for (Expression subExpression : expression.getExpressionSubParts()){
                newSubParts.put(subExpression, removeAllDenominators(subExpression));
            }
            for (Map.Entry<Expression, Expression> entry : newSubParts.entrySet()){
                expression.replaceExpressionSubPart(entry.getKey(), entry.getValue());
            }
            return expression;
        }
    }

    public static List<PBConstraint> getConstraintsToForbidZeroDivision(List<Expression> denominators) {
        List<PBConstraint> additionalConstraints = new LinkedList<>();
        for (Expression denominator : denominators){
            removeAllDenominators(denominator);
            PBConstraint pbConstraint = new PBConstraint();
            pbConstraint.k = 0;
            pbConstraint.type = PBConstraintType.NOTEQ;
            pbConstraint.literalList = new LinkedList<>();
            for (Literal l : denominator.getAsSum(additionalConstraints)) {
                pbConstraint.literalList.add(l);
            }
            additionalConstraints.add(pbConstraint);
        }
        return additionalConstraints;
    }
}
