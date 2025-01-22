package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;
import de.vill.util.Constants;
import java.util.Collections;
import java.util.List;

public class SumAggregateFunctionExpression extends AggregateFunctionExpression {
    public SumAggregateFunctionExpression(GlobalAttribute attribute) {
        super(attribute);
    }

    public SumAggregateFunctionExpression(GlobalAttribute attribute, Feature rootFeature) {
        super(attribute, rootFeature);
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return super.toString(withSubmodels, "sum", currentAlias);
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
