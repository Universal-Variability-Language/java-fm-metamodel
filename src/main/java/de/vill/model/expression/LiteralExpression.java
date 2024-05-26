package de.vill.model.expression;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureType;
import de.vill.model.building.VariableReference;
import de.vill.util.Constants;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LiteralExpression extends Expression {
    private VariableReference content;
    private Boolean boolValue; // just used for bool constants

    public LiteralExpression(Boolean value) {
        this.boolValue = value;
    }

    public LiteralExpression(VariableReference reference) {
        this.content = reference;
    }

    public VariableReference getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        return this.toString(true, "");
    }

    @Override
    public String toString(final boolean withSubmodels, final String currentAlias) {
        return content.getIdentifier(); // TODO: is ignoring the flag ok?
    }

    @Override
    public String getReturnType() {
        if (content instanceof Feature) {
            Feature feature = (Feature) content;
            if (FeatureType.STRING.equals(feature.getFeatureType())) {
                return Constants.STRING;
            } else if (FeatureType.BOOL.equals(feature.getFeatureType())) {
                return Constants.BOOLEAN;
            } else if (this.boolValue != null) {
                return Constants.BOOLEAN;
            } else {
                return Constants.NUMBER;
            }
        } else if (content instanceof Attribute) {
            return ((Attribute<?>) content).getType();
        }
        return "";
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Collections.emptyList();
    }

    @Override
    public void replaceExpressionSubPart(final Expression oldSubExpression, final Expression newSubExpression) {
        if (oldSubExpression instanceof LiteralExpression
                && ((LiteralExpression) oldSubExpression).getContent().equals(this.content) &&
                newSubExpression instanceof LiteralExpression) {
            this.content = ((LiteralExpression) newSubExpression).content;
        }
    }

    @Override
    public double evaluate(final Set<Feature> selectedFeatures) {
        if (boolValue != null || !(content instanceof Attribute<?>)) {
            return 0d;
        } else {
            Attribute<?> attribute = (Attribute<?>) content;
            final Object attributeValue = attribute.getValue();
            if (attributeValue instanceof Integer) {
                return ((Integer) attributeValue).doubleValue();
            }
            if (attributeValue instanceof Long) {
                return ((Long) attributeValue).doubleValue();
            }
            return (double) attributeValue;
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(content.getIdentifier());
        return result;
    }

    @Override
    public int hashCode(final int level) {
        return 31 * level + (content.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final LiteralExpression other = (LiteralExpression) obj;
        return Objects.equals(this.content.getIdentifier(), other.content.getIdentifier());
    }

    @Override
    public List<VariableReference> getReferences() {
        return List.of(content);
    }

}
