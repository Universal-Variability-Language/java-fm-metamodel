package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MaxAggregateFunctionExpression extends AggregateFunctionExpression {
    /**
     * Evaluates the maximum of the values of the selected features.
     *
     * @param selectedFeatures The set of selected features.
     * @return The maximum of the values of the selected features.
     */
    @Override
    public double evaluate(Set<Feature> selectedFeatures) {
        double max = Double.NEGATIVE_INFINITY;
        for (Feature feature : selectedFeatures) {
            Attribute<?> attribute = feature.getAttributes().get(getAttribute().getIdentifier());
            if (attribute != null && attribute.getValue() instanceof Number) {
                max = Math.max(max, ((Number) attribute.getValue()).doubleValue());
            }
        }
        return max == Double.NEGATIVE_INFINITY ? 0 : max;
    }
    
    public MaxAggregateFunctionExpression(GlobalAttribute attribute) {
        super(attribute);
    }

    public MaxAggregateFunctionExpression(GlobalAttribute attribute, Feature rootFeature) {
        super(attribute, rootFeature);
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return super.toString(withSubmodels, "max", currentAlias);
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Arrays.asList();
    }
}
