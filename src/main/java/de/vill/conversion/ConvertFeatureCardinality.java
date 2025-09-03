package de.vill.conversion;

import de.vill.model.*;
import de.vill.model.constraint.*;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LiteralExpression;

import java.util.*;

public class ConvertFeatureCardinality implements IConversionStrategy {
    @Override
    public Set<LanguageLevel> getLevelsToBeRemoved() {
        return new HashSet<>(Arrays.asList(LanguageLevel.FEATURE_CARDINALITY));
    }

    @Override
    public Set<LanguageLevel> getTargetLevelsOfConversion() {
        return new HashSet<>(Arrays.asList(LanguageLevel.ARITHMETIC_LEVEL));
    }

    @Override
    public void convertFeatureModel(FeatureModel rootFeatureModel, FeatureModel featureModel) {
        traverseFeatures(featureModel.getRootFeature(), featureModel);
    }

    private void traverseFeatures(Feature feature, FeatureModel featureModel) {
        if (!feature.isSubmodelRoot()) {
            if (feature.getCardinality() != null) {
                List<Feature> subTreeFeatures = new LinkedList<>();
                for (Group group : feature.getChildren()) {
                    subTreeFeatures.addAll(getFeatureFromSubTree(group));
                }
                List<Constraint> constraintsToClone = getConstraintsOnSubTree(featureModel, subTreeFeatures);

                removeFeatureCardinality(feature, featureModel, constraintsToClone);
            }
            for (Group children : feature.getChildren()) {
                for (Feature subFeature : children.getFeatures()) {
                    traverseFeatures(subFeature, featureModel);
                }
            }
        }
    }

    private void removeFeatureCardinality(Feature feature, FeatureModel featureModel, List<Constraint> constraintsToClone) {
        int min = feature.getCardinality().lower;
        int max = feature.getCardinality().upper;
        Group newChildren = new Group(Group.GroupType.ALTERNATIVE);

        feature.setCardinality(null);

        for (int i = min; i <= max; i++) {
            Feature newChild = new Feature(feature.getFeatureName() + "-" + i);
            featureModel.getFeatureMap().put(newChild.getFeatureName(), newChild);
            newChild.getAttributes().put("abstract", new Attribute<Boolean>("abstract", true, feature));
            newChildren.getFeatures().add(newChild);
            newChild.setParentGroup(newChildren);
            Group mandatoryGroup = new Group(Group.GroupType.MANDATORY);
            if (i > 0) {
                newChild.getChildren().add(mandatoryGroup);
                mandatoryGroup.setParentFeature(newChild);
            }
            for (int j = 1; j <= i; j++) {
                Feature subTreeClone = feature.clone();
                subTreeClone.getAttributes().clear();
                subTreeClone.getAttributes().put("abstract", new Attribute<Boolean>("abstract", true, subTreeClone));
                addPrefixToNamesRecursively(subTreeClone, "-" + i + "-" + j, featureModel);
                mandatoryGroup.getFeatures().add(subTreeClone);
                subTreeClone.setParentGroup(mandatoryGroup);

                Map<String, Feature> constraintReplacementMap = new HashMap<>();
                createFeatureReplacementMap(feature, subTreeClone, constraintReplacementMap);
                constraintReplacementMap.remove(feature.getFeatureName());
                for (Constraint constraint : constraintsToClone) {
                    Constraint newConstraint = constraint.clone();
                    if (newConstraint instanceof LiteralConstraint) {
                        String toReplace = ((LiteralConstraint) newConstraint).getReference().getIdentifier();
                        if (constraintReplacementMap.containsKey(toReplace)) {
                            LiteralConstraint newLiteral = new LiteralConstraint(constraintReplacementMap.get(toReplace));
                            LiteralConstraint subTreeRootConstraint = new LiteralConstraint(newChild);
                            newConstraint = new ImplicationConstraint(subTreeRootConstraint, new ParenthesisConstraint(newLiteral));
                        }
                    } else {
                        adaptConstraint(subTreeClone, newConstraint, constraintReplacementMap);
                        LiteralConstraint subTreeRootConstraint = new LiteralConstraint(newChild);
                        newConstraint = new ImplicationConstraint(subTreeRootConstraint, new ParenthesisConstraint(newConstraint));
                    }
                    featureModel.getOwnConstraints().add(newConstraint);
                }
            }
        }

        feature.getChildren().removeAll(feature.getChildren());
        feature.getChildren().add(newChildren);
        newChildren.setParentFeature(feature);
    }

    private void addPrefixToNamesRecursively(Feature feature, String prefix, FeatureModel featureModel) {
        feature.setFeatureName(feature.getFeatureName() + prefix);
        featureModel.getFeatureMap().put(feature.getFeatureName(), feature);
        if (!feature.isSubmodelRoot()) {
            for (Group group : feature.getChildren()) {
                for (Feature subFeature : group.getFeatures()) {
                    addPrefixToNamesRecursively(subFeature, prefix, featureModel);
                }
            }
        }
    }

    private List<Constraint> getConstraintsOnSubTree(FeatureModel featureModel, List<Feature> subTreeFeatures) {
        List<Constraint> constraints = new LinkedList<>();
        for (Constraint constraint : featureModel.getConstraints()) {
            if (constraintContains(constraint, subTreeFeatures)) {
                constraints.add(constraint);
                featureModel.getOwnConstraints().remove(constraint);
            }
        }
        return constraints;
    }

    private List<Feature> getFeatureFromSubTree(Group group) {
        List<Feature> features = new LinkedList<>();
        features.addAll(group.getFeatures());
        for (Feature subFeatures : group.getFeatures()) {
            if (!subFeatures.isSubmodelRoot()) {
                for (Group subGroup : subFeatures.getChildren()) {
                    features.addAll(getFeatureFromSubTree(subGroup));
                }
            }
        }
        return features;
    }

    private boolean constraintContains(Constraint constraint, List<Feature> subTreeFeatures) {
        List<Constraint> subParts = constraint.getConstraintSubParts();
        if (constraint instanceof LiteralConstraint && ((LiteralConstraint) constraint).getReference() instanceof Feature) {
            Feature feature = (Feature) ((LiteralConstraint) constraint).getReference();
            if (subTreeFeatures.contains(feature)) {
                return true;
            }
        } else if (constraint instanceof ExpressionConstraint) {
            Expression left = ((ExpressionConstraint) constraint).getLeft();
            Expression right = ((ExpressionConstraint) constraint).getRight();
            return expressionContains(left, subTreeFeatures) || expressionContains(right, subTreeFeatures);
        }

        for (Constraint subPart : subParts) {
            if (subPart instanceof LiteralConstraint && ((LiteralConstraint) subPart).getReference() instanceof Feature) {
                Feature feature = (Feature) ((LiteralConstraint) subPart).getReference();
                if (subTreeFeatures.contains(feature)) {
                    return true;
                }
            } else if (constraintContains(subPart, subTreeFeatures)) {
                return true;
            }
        }
        return false;
    }

    private boolean expressionContains(Expression expression, List<Feature> subTreeFeatures) {
        if (expression instanceof LiteralExpression) {
            Feature feature = (Feature) ((Attribute<?>) ((LiteralExpression) expression).getContent()).getFeature();
            if (subTreeFeatures.contains(feature)) {
                return true;
            }
        }

        for (Expression subExpression : expression.getExpressionSubParts()) {
            if (expression instanceof LiteralExpression) {
                Feature feature = (Feature) ((LiteralExpression) expression).getContent();
                if (subTreeFeatures.contains(feature)) {
                    return true;
                }
            } else if (expressionContains(subExpression, subTreeFeatures)) {
                return true;
            }
        }
        return false;
    }


    private void createFeatureReplacementMap(Feature oldSubTree, Feature newSubTree, Map<String, Feature> featureFeatureMap) {
        featureFeatureMap.put(oldSubTree.getFeatureName(), newSubTree);
        if (!oldSubTree.isSubmodelRoot()) {
            for (int i = 0; i < oldSubTree.getChildren().size(); i++) {
                for (int j = 0; j < oldSubTree.getChildren().get(i).getFeatures().size(); j++) {
                    createFeatureReplacementMap(oldSubTree.getChildren().get(i).getFeatures().get(j), newSubTree.getChildren().get(i).getFeatures().get(j), featureFeatureMap);
                }
            }
        }
    }

    private void adaptConstraint(Feature subTreeRoot, Constraint constraint, Map<String, Feature> featureReplacementMap) {
        if (constraint instanceof ExpressionConstraint) {
            adaptExpression(((ExpressionConstraint) constraint).getLeft(), featureReplacementMap);
            adaptExpression(((ExpressionConstraint) constraint).getRight(), featureReplacementMap);
        } else {
            List<Constraint> subParts = constraint.getConstraintSubParts();
            for (Constraint subPart : subParts) {
                if (subPart instanceof LiteralConstraint) {
                    String toReplace = ((LiteralConstraint) subPart).getReference().getIdentifier();
                    if (featureReplacementMap.containsKey(toReplace)) {
                        LiteralConstraint newLiteral = new LiteralConstraint(featureReplacementMap.get(toReplace));
                        constraint.replaceConstraintSubPart(subPart, newLiteral);
                    }
                } else {
                    adaptConstraint(subTreeRoot, subPart, featureReplacementMap);
                }
            }
        }
    }

    private void adaptExpression(Expression expression, Map<String, Feature> featureReplacementMap) {
        if (expression instanceof LiteralExpression) {
            LiteralExpression literalExpression = (LiteralExpression) expression;
            Attribute<?> attribute = (Attribute<?>) literalExpression.getContent();
            if (featureReplacementMap.containsKey(attribute.getFeature().getFeatureName())) {
                var newAttribute = attribute.clone();
                newAttribute.setFeature(featureReplacementMap.get(attribute.getFeature().getFeatureName()));
                literalExpression.setContent(newAttribute);
            }

        } else {
            for (Expression subExpression : expression.getExpressionSubParts()) {
                adaptExpression(subExpression, featureReplacementMap);
            }
        }
    }

    private void updateFeatureMap(FeatureModel featureModel, Feature oldSubTree, Feature newSubTree) {
        for (Group group : oldSubTree.getChildren()) {
            for (Feature subFeature : group.getFeatures()) {
                featureModel.getFeatureMap().put(subFeature.getFullReference(), subFeature);
            }
        }
    }
}
