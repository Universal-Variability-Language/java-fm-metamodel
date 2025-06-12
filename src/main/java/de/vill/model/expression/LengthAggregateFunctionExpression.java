package de.vill.model.expression;

import de.vill.model.Feature;
import de.vill.model.building.VariableReference;
import de.vill.util.Constants;
import de.vill.util.Util;

import java.util.*;

public class LengthAggregateFunctionExpression extends Expression {

    private VariableReference reference;

    public LengthAggregateFunctionExpression(final VariableReference reference) {
        this.reference = reference;
    }

    @Override
    public String toString(final boolean withSubmodels, final String currentAlias) {
        return String.format("len(%s)", Util.addNecessaryQuotes(reference.getIdentifier()));
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Arrays.asList();
    }

    @Override
    public void replaceExpressionSubPart(Expression oldSubExpression, Expression newSubExpression) {
    }

    public VariableReference getReference() {return reference;}

    public void setReference(VariableReference reference) {this.reference = reference;}

    @Override
    public String getReturnType() {
        return Constants.NUMBER;
    }

    @Override
    public double evaluate(final Set<Feature> selectedFeatures) {
        final Optional<Feature> feature = selectedFeatures.stream()
            .filter(f -> f.getFeatureName().equals(reference.getIdentifier())) // TODO: Is this correct?
            .findFirst();

        if (feature.isPresent()) {
            if (feature.get().getAttributes().containsKey("type_level_value_length")) {
                return Double.parseDouble(feature.get().getAttributes().get("type_level_value_length").getValue().toString());
            } else if (feature.get().getAttributes().containsKey("type_level_value")) {
                return feature.get().getAttributes().get("type_level_value").getValue().toString().length();
            }
        }

        return 0;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(reference);
        return result;
    }

    @Override
    public int hashCode(int level) {
        return 31 * level + (reference == null ? 0 : reference.getIdentifier().hashCode());
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LengthAggregateFunctionExpression other = (LengthAggregateFunctionExpression) obj;
        return Objects.equals(reference, other.reference);
    }

    @Override
    public List<VariableReference> getReferences() {
        return List.of();
    }
}
