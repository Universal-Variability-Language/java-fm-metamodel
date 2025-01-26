package de.vill.conversion;

import de.vill.main.Example;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.LanguageLevel;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ExpressionConstraint;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LengthAggregateFunctionExpression;
import de.vill.model.expression.StringExpression;
import de.vill.util.Constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DropTypeLevel implements IConversionStrategy {
    @Override
    public Set<LanguageLevel> getLevelsToBeRemoved() {
        return new HashSet<>(Collections.singletonList(LanguageLevel.TYPE_LEVEL));
    }

    @Override
    public Set<LanguageLevel> getTargetLevelsOfConversion() {
        return new HashSet<>();
    }

    @Override
    public void convertFeatureModel(final FeatureModel rootFeatureModel, final FeatureModel featureModel) {
        this.traverseFeatures(featureModel.getRootFeature());
        traverseConstraints(featureModel);
    }

    private void traverseFeatures(final Feature feature) {
        feature.setFeatureType(null);
        feature.getAttributes().remove(Constants.TYPE_LEVEL_VALUE);
        feature.getAttributes().remove(Constants.TYPE_LEVEL_LENGTH);

        for (final Group group : feature.getChildren()) {
            for (final Feature subFeature : group.getFeatures()) {
                this.traverseFeatures(subFeature);
            }
        }
    }

    private void traverseConstraints(FeatureModel featureModel) {
        for(Constraint constraint : featureModel.getConstraints()){
            if (containsTypeConcept(constraint)){
                featureModel.getOwnConstraints().remove(constraint);
            }
        }
    }

    private boolean containsTypeConcept(Constraint constraint) {
        if (constraint instanceof ExpressionConstraint){
            for(Expression subExpression : ((ExpressionConstraint) constraint).getExpressionSubParts()){
                if (containsTypeConcept(subExpression)){
                    return true;
                }
            }

        }
        for(Constraint subConstraints : constraint.getConstraintSubParts()){
            if (containsTypeConcept(subConstraints)){
                return true;
            }
        }
        return false;
    }

    private boolean containsTypeConcept(Expression expression) {
        if (expression instanceof LengthAggregateFunctionExpression){
            return true;
        }
        if (expression instanceof StringExpression){
            return true;
        }
        for(Expression subExpressions : expression.getExpressionSubParts()){
            if (containsTypeConcept(subExpressions)){
                return true;
            }
        }
        return false;
    }
}
