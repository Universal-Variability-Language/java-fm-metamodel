package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;
import de.vill.model.building.VariableReference;

import java.util.Arrays;
import java.util.List;

public class MaxAggregateFunctionExpression extends AggregateFunctionExpression {
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
