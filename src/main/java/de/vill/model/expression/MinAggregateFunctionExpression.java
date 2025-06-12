package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MinAggregateFunctionExpression extends AggregateFunctionExpression {

    /**
     * Evaluates the minimum of the values of the selected features.
     *
     * @param selectedFeatures The set of selected features.
     * @return The minimum of the values of the selected features.
     */
    @Override
    public double evaluate(Set<Feature> selectedFeatures) {
        double min = Double.POSITIVE_INFINITY;
        for (Feature feature : selectedFeatures) {
            Attribute<?> attribute = feature.getAttributes().get(getAttribute().getIdentifier());
            if (attribute != null && attribute.getValue() instanceof Number) {
                min = Math.min(min, ((Number) attribute.getValue()).doubleValue());
            }
        }
        return min == Double.POSITIVE_INFINITY ? 0 : min;
    }

    public MinAggregateFunctionExpression(GlobalAttribute attribute) {
        super(attribute);
    }

    public MinAggregateFunctionExpression(GlobalAttribute attribute, Feature rootFeature) {
        super(attribute, rootFeature);
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return super.toString(withSubmodels, "min", currentAlias);
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Arrays.asList();
    }
}
