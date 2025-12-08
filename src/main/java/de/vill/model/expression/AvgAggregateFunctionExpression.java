package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;
import de.vill.util.Constants;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AvgAggregateFunctionExpression extends AggregateFunctionExpression {
    /**
     * Evaluates the average of the values of the selected features.
     *
     * @param selectedFeatures The set of selected features.
     * @return The average of the values of the selected features.
     */
    @Override
    public double evaluate(Set<Feature> selectedFeatures) {
        double sum = 0;
        double count = 0;
        for (Feature feature : selectedFeatures) {
            Attribute<?> attribute = feature.getAttributes().get(getAttribute().getIdentifier());
            if (attribute != null && attribute.getValue() instanceof Number) {
                sum += ((Number) attribute.getValue()).doubleValue();
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }
    
    public AvgAggregateFunctionExpression(GlobalAttribute reference) {
        super(reference);
    }

    public AvgAggregateFunctionExpression(GlobalAttribute reference, Feature rootFeature) {
        super(reference, rootFeature);
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return super.toString(withSubmodels, "avg", currentAlias);
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Collections.emptyList();
    }

    @Override
    public String getReturnType() {
        return Constants.NUMBER;
    }

    @Override
    public Expression clone() {
        return new AvgAggregateFunctionExpression(attribute, rootFeature);
    }
}
