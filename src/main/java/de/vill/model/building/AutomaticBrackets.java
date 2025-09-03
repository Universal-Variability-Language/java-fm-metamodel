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

    public final static int IFF_PRECEDENCE = 0;
    public final static int IMPLY_PRECEDENCE = 1;
    public final static int OR_PRECEDENCE = 2;
    public final static int AND_PRECEDENCE = 3;
    public final static int GEQ_LEQ_PRECEDENCE = 4;
    public final static int EQUATION_PRECEDENCE = 5;
    public final static int CONSTRAINT_UNARY_PRECEDENCE = 6;

    public final static int ADD_SUB_PRECEDENCE = 0;
    public final static int MULT_DIV_PRECEDENCE = 1;
    public final static int UNARY_EXPRESSION_PRECEDENCE = 2;


    static {
        constraintprecedenceLookup = new HashMap<>();

        // n-ary
        constraintprecedenceLookup.put(EquivalenceConstraint.class, IFF_PRECEDENCE);
        constraintprecedenceLookup.put(ImplicationConstraint.class, IMPLY_PRECEDENCE);
        constraintprecedenceLookup.put(OrConstraint.class, OR_PRECEDENCE);
        constraintprecedenceLookup.put(MultiOrConstraint.class, OR_PRECEDENCE);
        constraintprecedenceLookup.put(AndConstraint.class, AND_PRECEDENCE);
        constraintprecedenceLookup.put(GreaterEquationConstraint.class, GEQ_LEQ_PRECEDENCE);
        constraintprecedenceLookup.put(LowerEquationConstraint.class, GEQ_LEQ_PRECEDENCE);
        constraintprecedenceLookup.put(GreaterEqualsEquationConstraint.class, GEQ_LEQ_PRECEDENCE);
        constraintprecedenceLookup.put(LowerEqualsEquationConstraint.class, GEQ_LEQ_PRECEDENCE);
        constraintprecedenceLookup.put(NotEqualsEquationConstraint.class, EQUATION_PRECEDENCE);
        constraintprecedenceLookup.put(EqualEquationConstraint.class, EQUATION_PRECEDENCE);

        // Unary
        constraintprecedenceLookup.put(LiteralConstraint.class, CONSTRAINT_UNARY_PRECEDENCE);
        constraintprecedenceLookup.put(NotConstraint.class, CONSTRAINT_UNARY_PRECEDENCE);
        constraintprecedenceLookup.put(ParenthesisConstraint.class, CONSTRAINT_UNARY_PRECEDENCE);

        expressionPrecedenceLookup = new HashMap<>();

        // n-ary
        expressionPrecedenceLookup.put(AddExpression.class, ADD_SUB_PRECEDENCE);
        expressionPrecedenceLookup.put(SubExpression.class, ADD_SUB_PRECEDENCE);
        expressionPrecedenceLookup.put(MulExpression.class, MULT_DIV_PRECEDENCE);
        expressionPrecedenceLookup.put(DivExpression.class, MULT_DIV_PRECEDENCE);

        // unary
        expressionPrecedenceLookup.put(AvgAggregateFunctionExpression.class, UNARY_EXPRESSION_PRECEDENCE);
        expressionPrecedenceLookup.put(MaxAggregateFunctionExpression.class, UNARY_EXPRESSION_PRECEDENCE);
        expressionPrecedenceLookup.put(MinAggregateFunctionExpression.class, UNARY_EXPRESSION_PRECEDENCE);
        expressionPrecedenceLookup.put(SumAggregateFunctionExpression.class, UNARY_EXPRESSION_PRECEDENCE);
        expressionPrecedenceLookup.put(ParenthesisExpression.class, UNARY_EXPRESSION_PRECEDENCE);

        expressionPrecedenceLookup.put(LiteralExpression.class, UNARY_EXPRESSION_PRECEDENCE);
        expressionPrecedenceLookup.put(NumberExpression.class, UNARY_EXPRESSION_PRECEDENCE);
        expressionPrecedenceLookup.put(StringExpression.class, UNARY_EXPRESSION_PRECEDENCE);
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

    /**
     * Method can be used so that the printed versions of constraints are semantically equivalent to the internal object representation
     * @see de.vill.model.building.AutomaticBrackets for the precendences 
     * @param parent
     * @param child
     * @param withSubmodels Consider imported submodels
     * @param currentAlias Check alias of submodel at hand
     * @return
     */
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
