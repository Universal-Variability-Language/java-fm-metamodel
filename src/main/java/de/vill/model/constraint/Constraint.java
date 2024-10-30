package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.pbc.Literal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class Constraint {
    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    private int lineNumber;

    @Override
    public String toString() {
        return toString(true, "");
    }

    public abstract String toString(boolean withSubmodels, String currentAlias);

    public abstract List<Constraint> getConstraintSubParts();

    public abstract void replaceConstraintSubPart(Constraint oldSubConstraint, Constraint newSubConstraint);

    @Override
    public abstract Constraint clone();

    @Override
    public int hashCode() {
        return hashCode(1);
    }

    public abstract int hashCode(int level);

    @Override
    public abstract boolean equals(Object obj);

    public abstract List<VariableReference> getReferences();

    public int extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping, int n, int counter) {
        return 0;
    };

    public StringBuilder toSMT2string(){
        return null;
    }

    public List<ExpressionConstraint> collectExpressions(){
        List<ExpressionConstraint> expressions = new LinkedList<>();
        for (Constraint subConstraint : getConstraintSubParts()) {
            expressions.addAll(subConstraint.collectExpressions());
        }
        return expressions;
    }
}

