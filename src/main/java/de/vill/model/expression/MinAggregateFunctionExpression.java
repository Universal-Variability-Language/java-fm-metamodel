package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;
import de.vill.model.building.VariableReference;
import org.w3c.dom.Attr;

import java.util.Arrays;
import java.util.List;

public class MinAggregateFunctionExpression extends AggregateFunctionExpression {

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
