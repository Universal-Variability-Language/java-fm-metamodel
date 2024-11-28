package de.vill.model.expression;

import de.vill.model.Feature;
import de.vill.model.building.VariableReference;
import de.vill.model.pbc.Literal;
import de.vill.model.pbc.PBConstraint;
import de.vill.util.Constants;

import java.util.*;

public class StringExpression extends Expression {
    public String getString() {
        return this.value;
    }

    public void setString(final String value) {
        this.value = value;
    }

    private String value;

    public StringExpression(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.toString(true, "");
    }

    @Override
    public String toString(final boolean withSubmodels, final String currentAlias) {
        return "'" + this.value + "'" ;
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Collections.emptyList();
    }

    @Override
    public void replaceExpressionSubPart(final Expression oldSubExpression, final Expression newSubExpression) {
        if (oldSubExpression instanceof StringExpression && ((StringExpression) oldSubExpression).getString().equals(this.value) &&
            newSubExpression instanceof StringExpression) {
            this.value = ((StringExpression) newSubExpression).value;
        }
    }

    @Override
    public double evaluate(final Set<Feature> selectedFeatures) {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(this.value);
        return result;
    }

    @Override
    public int hashCode(final int level) {
        return 31 * level + Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final StringExpression other = (StringExpression) obj;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public List<VariableReference> getReferences() {
        return new ArrayList<>();
    }

    @Override
    public List<Literal> getAsSum(List<PBConstraint> additionalSubstitution) {
        throw new UnsupportedOperationException("All AggregateFunctions must be removed before method is called.");
    }

    @Override
    public String getReturnType() {
        return Constants.STRING;
    }

    @Override
    public Expression clone(){
        return new StringExpression(getString());
    }
}
