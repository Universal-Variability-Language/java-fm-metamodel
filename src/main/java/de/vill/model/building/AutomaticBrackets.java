package de.vill.model.building;

import de.vill.model.constraint.*;
import de.vill.model.expression.*;

import java.util.HashMap;
import java.util.Map;

/**
 * This class automatically creates brackets for nested constraints to ensure semantic equivalence when printing out the UVL model.
 * Precedences are ordered according to <a href="https://www.cs.fsu.edu/~cop3014p/lectures/ch4/index.html">...</a> and <a href="http://intrologic.stanford.edu/dictionary/operator_precedence.html">...</a>
 * A higher score indicates a higher precedence (i.e., a stronger binding)
 * @author Chico Sundermann
 */
public class AutomaticBrackets {

    private AutomaticBrackets() {}

    static Map<Class, Integer> constraintprecedenceLookup;

    static Map<Class, Integer> expressionPrecedenceLookup;

    static {
        constraintprecedenceLookup = new HashMap<>();

        // n-ary
        constraintprecedenceLookup.put(EquivalenceConstraint.class, 0);
        constraintprecedenceLookup.put(ImplicationConstraint.class, 1);
        constraintprecedenceLookup.put(OrConstraint.class, 2);
        constraintprecedenceLookup.put(MultiOrConstraint.class, 2);
        constraintprecedenceLookup.put(AndConstraint.class, 3);
        constraintprecedenceLookup.put(GreaterEquationConstraint.class, 4);
        constraintprecedenceLookup.put(LowerEquationConstraint.class, 4);
        constraintprecedenceLookup.put(GreaterEqualsEquationConstraint.class, 4);
        constraintprecedenceLookup.put(LowerEqualsEquationConstraint.class, 4);
        constraintprecedenceLookup.put(NotEqualsEquationConstraint.class, 5);
        constraintprecedenceLookup.put(EqualEquationConstraint.class, 5);

        // Unary
        constraintprecedenceLookup.put(LiteralConstraint.class, 6);
        constraintprecedenceLookup.put(NotConstraint.class, 6);
        constraintprecedenceLookup.put(ParenthesisConstraint.class, 6);

        expressionPrecedenceLookup = new HashMap<>();

        // n-ary
        expressionPrecedenceLookup.put(AddExpression.class, 0);
        expressionPrecedenceLookup.put(SubExpression.class, 0);
        expressionPrecedenceLookup.put(MulExpression.class, 1);
        expressionPrecedenceLookup.put(DivExpression.class, 1);

        // unary
        expressionPrecedenceLookup.put(AvgAggregateFunctionExpression.class, 2);
        expressionPrecedenceLookup.put(MaxAggregateFunctionExpression.class, 2);
        expressionPrecedenceLookup.put(MinAggregateFunctionExpression.class, 2);
        expressionPrecedenceLookup.put(SumAggregateFunctionExpression.class, 2);
        expressionPrecedenceLookup.put(ParenthesisExpression.class, 2);

        expressionPrecedenceLookup.put(LiteralExpression.class, 2);
        expressionPrecedenceLookup.put(NumberExpression.class, 2);
        expressionPrecedenceLookup.put(StringExpression.class, 2);
    }

    /**
     *
     * @param parent
     * @param child
     * @return
     */
    private static boolean requiresBracketsInConstraint(Constraint parent, Constraint child) {
        return constraintprecedenceLookup.get(parent.getClass()) >= constraintprecedenceLookup.get(child.getClass());
    }

    private static boolean requiresBracketsInExpression(Expression parent, Expression child) {
        return expressionPrecedenceLookup.get(parent.getClass()) >= expressionPrecedenceLookup.get(child.getClass());
    }


    public static String enforceConstraintBracketsIfNecessary(Constraint parent, Constraint child, boolean withSubmodels, String currentAlias) {
        if (requiresBracketsInConstraint(parent, child)) {
            return "(" + child.toString(withSubmodels, currentAlias) + ")";
        } else {
            return child.toString(withSubmodels, currentAlias);
        }
    }

    public static String enforceExpressionBracketsIfNecessary(Expression parent, Expression child, boolean withSubmodels, String currentAlias) {
        if (requiresBracketsInExpression(parent, child)) {
            return "(" + child.toString(withSubmodels, currentAlias) + ")";
        } else {
            return child.toString(withSubmodels, currentAlias);
        }
    }



}
