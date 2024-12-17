package de.vill.util;

import de.vill.config.Configuration;
import de.vill.conversion.ConvertAggregateFunction;
import de.vill.model.building.VariableReference;
import de.vill.model.constraint.*;
import de.vill.model.expression.*;
import de.vill.model.pbc.*;

import java.io.IOException;
import java.io.Writer;
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
            } else if(part.equals("false")) {
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
        SubstitutionVariableIndex substitutionVariableIndex = SubstitutionVariableIndex.getInstance();
        for (Map.Entry<Integer, List<PBConstraint>> entry : implicationMap.entrySet()) {
            int index = entry.getKey();
            if (entry.getValue().size() > 1) {
                PBConstraint pbConstraint = new PBConstraint();
                pbConstraint.k = entry.getValue().size();

                for(PBConstraint constraint : entry.getValue()){
                    String subName = substitutionVariableIndex.getSubName();
                    pbConstraint.literalList.add(new Literal(
                            1, subName, true
                    ));
                    resultList.addAll(substitutionConstraint(constraint, subName));
                }
                resultList.addAll(substitutionConstraint(pbConstraint, "x_" + index));
            }else {
                for(PBConstraint constraint : entry.getValue()){
                    resultList.addAll(substitutionConstraint(constraint, "x_" + index));
                }
            }


        }
        return resultList;
    }

    public static List<PBConstraint> substitutionConstraint(PBConstraint constraint, String substitutionName) {
        //System.out.println(substitutionName + " <=> " + constraint.toString());
        List<PBConstraint> resultList = new LinkedList<>();
        // x <=> constraint is the same as -x v constraint AND x v -constraint
        // -x v constraint
        resultList.addAll(constraint.orWithLiteral(substitutionName, false));
        // x v -constraint
        resultList.addAll(constraint.negatedConstraint().orWithLiteral(substitutionName, true));
        return resultList;
    }

    public static HashMap<Integer, List<PBConstraint>> transformSubFormulas(HashMap<Integer, Constraint> subformulas, List<PBConstraint> additionalSubstitution){
        HashMap<Integer, List<PBConstraint>> resultMap = new HashMap<>();
        for (Map.Entry<Integer, Constraint> entry : subformulas.entrySet()) {
            resultMap.put(entry.getKey(), transformSubFormula(entry.getValue(), additionalSubstitution));
        }
        return resultMap;
    }

    public static List<PBConstraint> transformSubFormula(Constraint constraint, List<PBConstraint> additionalSubstitution){
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
            resultList.addAll(transformExpression(expressionConstraint, additionalSubstitution));
        }return resultList;
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

    public static List<PBConstraint> transformExpression(ExpressionConstraint constraint, List<PBConstraint> additionalSubstitution) {
        List<PBConstraint> additionalConstraints = new LinkedList<>();
        List<Expression> allDenominators = new LinkedList<>();
        collectDenominators(constraint.getLeft(), allDenominators);
        collectDenominators(constraint.getRight(), allDenominators);
        additionalConstraints.addAll(getConstraintsToForbidZeroDivision(allDenominators));

        //transform everything to a sum
        var leftSum = constraint.getLeft().getAsSum(additionalSubstitution);
        var rightSum = constraint.getRight().getAsSum(additionalSubstitution);
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

        additionalConstraints.add(pbConstraint);

        return additionalConstraints;
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

    static int counter = 0;
    public static void constraintDistributiveToOPB(Constraint constraint, OPBResult result, Writer writer) throws IOException {
        constraint = substituteExpressions(constraint, result);
        constraint = removeParenthesisConstraint(constraint);
        constraint = removeBiimplication(constraint);
        constraint = removeImplication(constraint);
        constraint = pushDownNegation(constraint);
        System.out.println(constraint.toString());
        System.out.println(counter);
        if (counter == 74){
            System.out.println("test");
        }

        constraint = distributeOrOverAnd(constraint, 0);
        //cnfToOpb(constraint,writer);
        constraint = null;
        counter++;
    }

    private static void cnfToOpb(Constraint constraint, Writer writer) throws IOException {
        if (constraint instanceof OrConstraint || constraint instanceof NotConstraint || constraint instanceof LiteralConstraint) {
            int negatives = clauseToOpb(constraint, writer);
            writer.write(" >= ");
            writer.write(String.valueOf(1 - negatives));
            writer.write(";\n");
        }else {
            var and = (AndConstraint) constraint;
            if (and.getLeft() instanceof AndConstraint){
                cnfToOpb(and.getLeft(), writer);
            }else{
                int negatives = clauseToOpb(and.getLeft(), writer);
                writer.write(" >= ");
                writer.write(String.valueOf(1 - negatives));
                writer.write(";\n");
            }

            if (and.getRight() instanceof AndConstraint){
                cnfToOpb(and.getRight(), writer);
            }else{
                int negatives = clauseToOpb(and.getRight(), writer);
                writer.write(" >= ");
                writer.write(String.valueOf(1 - negatives));
                writer.write(";\n");
            }
        }
    }

    private static int clauseToOpb(Constraint constraint, Writer writer) throws IOException {
        int negatives = 0;
        if (constraint instanceof NotConstraint) {
            writer.write(" -1 * ");
            var literal = (LiteralConstraint)((NotConstraint) constraint).getContent();
            writer.write('"');
            writer.write(literal.getReference().getIdentifier());
            writer.write('"');
            negatives++;
        }else if (constraint instanceof LiteralConstraint){
            writer.append(" +1 * ");
            var literal = (LiteralConstraint)constraint;
            writer.write('"');
            writer.write(literal.getReference().getIdentifier());
            writer.write('"');
        }else {
            var or = (OrConstraint)constraint;
            negatives += clauseToOpb(or.getLeft(), writer);
            negatives += clauseToOpb(or.getRight(), writer);
        }
        return negatives;
    }

    private static Constraint removeParenthesisConstraint(Constraint constraint){
        if (constraint instanceof ParenthesisConstraint){
            return removeParenthesisConstraint(((ParenthesisConstraint) constraint).getContent());
        }else{
            for (Constraint subConstraint : constraint.getConstraintSubParts()){
                constraint.replaceConstraintSubPart(subConstraint, removeParenthesisConstraint(subConstraint));
            }
        }
        return constraint;
    }

    public static Constraint substituteExpressions(Constraint constraint, OPBResult result){
        if (constraint instanceof ExpressionConstraint){
            SubstitutionVariableIndex substitutionVariableIndex = SubstitutionVariableIndex.getInstance();
            int subIndex = substitutionVariableIndex.getIndex();
            String subName = "x_" + subIndex;
            LiteralConstraint subLiteral = new LiteralConstraint(new VariableReference() {
                @Override
                public String getIdentifier() {
                    return subName;
                }
            });
            HashMap<Integer, List<PBConstraint>> subMap = new HashMap<>();
            List<PBConstraint> additionalSubs = new LinkedList<>();
            List<PBConstraint> expressionEncoding = transformExpression((ExpressionConstraint) constraint, additionalSubs);
            subMap.put(subIndex, expressionEncoding);
            List<PBConstraint> resultList = transformImplicationMap(subMap);
            resultList.addAll(additionalSubs);
            for (PBConstraint pbConstraint : resultList){
                pbConstraint.toOPBString(result);
            }
            return subLiteral;
        }else{
            for (Constraint subConstraint : constraint.getConstraintSubParts()){
                constraint.replaceConstraintSubPart(subConstraint, substituteExpressions(subConstraint, result));
            }
        }
        return constraint;
    }

    private static Constraint removeImplication(Constraint constraint){
        if (constraint instanceof ImplicationConstraint) {
            return new OrConstraint(
                    new NotConstraint(
                            removeImplication(((ImplicationConstraint) constraint).getLeft())),
                    removeImplication(((ImplicationConstraint) constraint).getRight())
            );
        }else{
            for (Constraint subConstraint : constraint.getConstraintSubParts()){
                constraint.replaceConstraintSubPart(subConstraint, removeImplication(subConstraint));
            }
        }
        return constraint;
    }

    private static Constraint removeBiimplication(Constraint constraint){
        if (constraint instanceof EquivalenceConstraint) {
            return new AndConstraint(
                    new OrConstraint(
                            new NotConstraint(
                                removeBiimplication(((EquivalenceConstraint) constraint).getLeft())
                            ),
                            removeBiimplication(((EquivalenceConstraint) constraint).getRight())),
                    new OrConstraint(
                            new NotConstraint(
                                    removeBiimplication(((EquivalenceConstraint) constraint).getRight())),
                            removeBiimplication(((EquivalenceConstraint) constraint).getLeft()))
            );
        }else{
            for (Constraint subConstraint : constraint.getConstraintSubParts()){
                constraint.replaceConstraintSubPart(subConstraint, removeBiimplication(subConstraint));
            }
        }
        return constraint;
    }

    private static Constraint pushDownNegation(Constraint constraint){
        if (constraint instanceof NotConstraint) {
            var notConstraint = (NotConstraint) constraint;
            if (notConstraint.getContent() instanceof AndConstraint){
                return pushDownNegation(
                        new OrConstraint(
                            new NotConstraint(((AndConstraint) notConstraint.getContent()).getLeft()),
                            new NotConstraint(((AndConstraint) notConstraint.getContent()).getRight())
                        )
                );
            }else if (notConstraint.getContent() instanceof OrConstraint){
                return pushDownNegation(
                        new AndConstraint(
                                new NotConstraint(((OrConstraint) notConstraint.getContent()).getLeft()),
                                new NotConstraint(((OrConstraint) notConstraint.getContent()).getRight())
                        )
                );
            }else if (notConstraint.getContent() instanceof NotConstraint){
                return pushDownNegation(
                        ((NotConstraint) notConstraint.getContent()).getContent()
                );
            }else {
                return notConstraint;
            }
        }else {
            for (Constraint subConstraint : constraint.getConstraintSubParts()){
               constraint.replaceConstraintSubPart(subConstraint, pushDownNegation(subConstraint));
            }
        }
        return constraint;
    }

    public static void encodeConstraintTseitinStyle(Constraint constraint, OPBResult result){
        if (constraint instanceof LiteralConstraint){
            PBConstraint pbConstraint = new PBConstraint();
            pbConstraint.literalList = new LinkedList<>();
            pbConstraint.k = 1;
            Literal literal = new Literal(1, ((LiteralConstraint) constraint).getReference().getIdentifier(), true);
            pbConstraint.literalList.add(literal);
            result.numberVariables++;
            pbConstraint.toOPBString(result);
            return;
        }
        HashMap<Integer, Constraint> subMap = new HashMap<>();
        constraint.extractTseitinSubConstraints(subMap);

        if (subMap.isEmpty()) {
            if (constraint instanceof LiteralConstraint){
                PBConstraint pbConstraint = new PBConstraint();
                pbConstraint.literalList = new LinkedList<>();
                pbConstraint.k = 1;
                Literal literal = new Literal(1, ((LiteralConstraint) constraint).getReference().getIdentifier(), true);
                pbConstraint.literalList.add(literal);
                result.numberVariables++;
                pbConstraint.toOPBString(result);
                return;
            }else if(constraint instanceof NotConstraint){
                PBConstraint pbConstraint = new PBConstraint();
                pbConstraint.literalList = new LinkedList<>();
                pbConstraint.k = 0;
                Literal literal = new Literal(-1, ((LiteralConstraint)((NotConstraint)constraint).getContent()).getReference().getIdentifier(), true);
                pbConstraint.literalList.add(literal);
                result.numberVariables++;
                pbConstraint.toOPBString(result);
                return;
            }
        }

        boolean sign = !(constraint instanceof NotConstraint);
        Literal literal = new Literal(1, "x_" + SubstitutionVariableIndex.getInstance().peekIndex(), sign);

        List<PBConstraint> additionalSubstitutionConstraints = new LinkedList<>();
        var map = transformSubFormulas(subMap, additionalSubstitutionConstraints);
        List<PBConstraint> pbcList = transformImplicationMap(map);
        PBConstraint pbConstraint = new PBConstraint();
        pbConstraint.literalList = new LinkedList<>();
        pbConstraint.k = 1;

        pbConstraint.literalList.add(literal);
        pbcList.add(pbConstraint);
        pbcList.addAll(additionalSubstitutionConstraints);
        for(PBConstraint pBConstraint : pbcList){
            result.numberVariables++;
            pBConstraint.toOPBString(result);
        }
    }

    private static Constraint distributeOrOverAnd(Constraint constraint, int d) {
        if (counter == 74 && d >= 20) {
            System.out.println("test");
        }
        d++;
        if (constraint instanceof OrConstraint) {
            OrConstraint or = (OrConstraint) constraint;
            Constraint left = distributeOrOverAnd(or.getLeft(), d);
            Constraint right = distributeOrOverAnd(or.getRight(), d);

            if (left instanceof AndConstraint) {
                AndConstraint leftAnd = (AndConstraint) left;
                leftAnd.setLeft(distributeOrOverAnd(new OrConstraint(leftAnd.getLeft(), right),d));
                leftAnd.setRight(distributeOrOverAnd(new OrConstraint(leftAnd.getRight(), right),d));
                return leftAnd;
                /*
                return new AndConstraint(
                        distributeOrOverAnd(new OrConstraint(leftAnd.getLeft(), right)),
                        distributeOrOverAnd(new OrConstraint(leftAnd.getRight(), right))
                );

                 */
            } else if (right instanceof AndConstraint) {
                AndConstraint rightAnd = (AndConstraint) right;
                rightAnd.setLeft(distributeOrOverAnd(new OrConstraint(left, rightAnd.getLeft()),d));
                rightAnd.setRight(distributeOrOverAnd(new OrConstraint(left, rightAnd.getRight()),d));
                return rightAnd;
                /*
                return new AndConstraint(
                        distributeOrOverAnd(new OrConstraint(left, rightAnd.getLeft())),
                        distributeOrOverAnd(new OrConstraint(left, rightAnd.getRight()))
                );

                 */
            }
            //
            or.setLeft(left);
            or.setRight(right);
            return constraint;
            //return new OrConstraint(left, right);
        } else if (constraint instanceof AndConstraint) {
            //
            AndConstraint and = (AndConstraint) constraint;
            Constraint left = distributeOrOverAnd(and.getLeft(),d);
            Constraint right = distributeOrOverAnd(and.getRight(),d);
            and.setLeft(left);
            and.setRight(right);
            return  and;
            /*
            return new AndConstraint(
                    distributeOrOverAnd(and.getLeft()),
                    distributeOrOverAnd(and.getRight())
            );

             */
        }

        return constraint;
    }

}
