package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;
import de.vill.model.building.VariableReference;
import de.vill.util.Constants;
import java.util.Collections;
import java.util.List;

public class AvgAggregateFunctionExpression extends AggregateFunctionExpression {
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
}
