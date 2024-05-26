package de.vill.model.expression;

import static de.vill.util.Util.addNecessaryQuotes;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.GlobalAttribute;
import de.vill.model.building.VariableReference;
import de.vill.util.Constants;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AggregateFunctionExpression extends Expression {

    protected GlobalAttribute attribute;
    protected Feature rootFeature;

    public AggregateFunctionExpression(GlobalAttribute attribute) {
        this.attribute = attribute;
    }

    public AggregateFunctionExpression(GlobalAttribute attribute, Feature rootFeature) {
        this(attribute);
        this.rootFeature = rootFeature;
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return toString(withSubmodels, "aggregateFunction", currentAlias);
    }

    @Override
    public String getReturnType() {
        // implement in children
        return Constants.NUMBER;
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Arrays.asList();
    }

    @Override
    public void replaceExpressionSubPart(Expression oldSubExpression, Expression newSubExpression) {

    }

    public Feature getRootFeature() {
        return rootFeature;
    }

    public GlobalAttribute getAttribute() {
        return attribute;
    }



    @Override
    public double evaluate(Set<Feature> selectedFeatures) {
        // TODO not necessary for now
        return 0;
    }

    protected String toString(boolean withSubmodels, String functionName, String currentAlias) {
        final StringBuilder result = new StringBuilder();
        result.append(functionName).append("(");

        if (rootFeature != null) {
            if (withSubmodels) {
                result.append(addNecessaryQuotes(rootFeature.getFullReference()));
            } else {
                result.append(addNecessaryQuotes(rootFeature.getFeatureName()));
            }
        }

        if (attribute != null) {
            if (rootFeature!= null) {
                result.append(", ");
            }
            result.append(addNecessaryQuotes(attribute.getIdentifier()));
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(attribute);
        return result;
    }

    @Override
    public int hashCode(int level) {
        return 31 * level + (attribute == null ? 0 : attribute.getIdentifier().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AggregateFunctionExpression other = (AggregateFunctionExpression) obj;
        return Objects.equals(attribute, other.attribute);
    }

    @Override
    public List<VariableReference> getReferences() {
        return List.of(attribute);
    }
}
