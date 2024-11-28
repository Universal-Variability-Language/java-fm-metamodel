package de.vill.util;

import de.vill.conversion.IConversionStrategy;
import de.vill.model.*;
import de.vill.model.constraint.*;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LiteralExpression;

import javax.smartcardio.Card;
import java.util.*;

public class ConvertFeatureCardinalityForOPB {

    public void convertFeatureModel(FeatureModel featureModel) {
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
        Group newChildren = new Group(Group.GroupType.GROUP_CARDINALITY);
        newChildren.setCardinality(new Cardinality(min == 0 ? min + 1 : min, min == 0 ? max + 1 : max));

        feature.setCardinality(null);

        for (int i = min; i <= max; i++) {
            Feature subTreeClone = feature.clone();
            addPrefixToNamesRecursively(subTreeClone, "_" + i, featureModel);
            newChildren.getFeatures().add(subTreeClone);
            subTreeClone.setParentGroup(newChildren);

            if (i == 0) {
                subTreeClone.getChildren().clear();
            }else{
                Map<String, Feature> constraintReplacementMap = new HashMap<>();
                createFeatureReplacementMap(feature, subTreeClone, constraintReplacementMap);
                constraintReplacementMap.remove(feature.getFeatureName());
                for (Constraint constraint : constraintsToClone) {
                    Constraint newConstraint = constraint.clone();
                    adaptConstraint(subTreeClone, newConstraint, constraintReplacementMap);
                    LiteralConstraint subTreeRootConstraint = new LiteralConstraint(subTreeClone);
                    newConstraint = new ImplicationConstraint(subTreeRootConstraint, new ParenthesisConstraint(newConstraint));
                    featureModel.getOwnConstraints().add(newConstraint);

                }
            }

        }
        for (int i = min; i < max; i++) {
            Constraint lastTakenInGroupCardinality = new LiteralConstraint(newChildren.getFeatures().get(i - min));
            List<LiteralConstraint> notToTakeInGroupCarrdinality = new LinkedList<>();
            for (int k=i+1;k<=max;k++){
                notToTakeInGroupCarrdinality.add(new LiteralConstraint(newChildren.getFeatures().get(k - min)));
            }
            Constraint groupCardinalityOrderConstraint = new ImplicationConstraint(new NotConstraint(lastTakenInGroupCardinality), new NotConstraint(createDisjunction(notToTakeInGroupCarrdinality)));
            featureModel.getOwnConstraints().add(groupCardinalityOrderConstraint);
        }

        /*
        new constraint with all cloned features ored
        Set<String> allFeatureNamesInSubTree = new HashSet<>();
        getAllSubFeatureNamesRecursively(feature, allFeatureNamesInSubTree);
        for (Constraint constraint : constraintsToClone) {
            Constraint newConstraint = constraint.clone();
            orAdaptedConstraint(newConstraint, allFeatureNamesInSubTree, min, max, featureModel);
            featureModel.getOwnConstraints().add(newConstraint);
        }

         */

        feature.getChildren().removeAll(feature.getChildren());
        feature.getChildren().add(newChildren);
        newChildren.setParentFeature(feature);
    }

    private void orAdaptedConstraint(Constraint constraint, Set<String> featuresToReplace, int min, int max, FeatureModel featureModel) {
        for (Constraint subPart : constraint.getConstraintSubParts()) {
            if (subPart instanceof LiteralConstraint) {
                String toReplace = ((LiteralConstraint) subPart).getReference().getIdentifier();
                if (featuresToReplace.contains(toReplace)){
                    Feature f = featureModel.getFeatureMap().get(toReplace + "_" + min);
                    Constraint newOr = new LiteralConstraint(f);
                    for (int i = min + 1; i <= max; i++) {
                        newOr = new OrConstraint(newOr, new LiteralConstraint(featureModel.getFeatureMap().get(toReplace + "_" + i)));
                    }
                    constraint.replaceConstraintSubPart(subPart, new ParenthesisConstraint(newOr));
                }
            }else {
                orAdaptedConstraint(subPart, featuresToReplace, min, max, featureModel);
            }
        }
    }

    private void getAllSubFeatureNamesRecursively(Feature feature, Set<String> names) {
        names.add(feature.getFeatureName());
        for (Group child : feature.getChildren()) {
            for(Feature childFeature : child.getFeatures()){
                getAllSubFeatureNamesRecursively(childFeature, names);
            }
        }
    }

    private Constraint createDisjunction(List<LiteralConstraint> literals) {
        if (literals.size() == 1) {
            return literals.get(0);
        }
        LiteralConstraint literalConstraint = literals.get(0);
        literals.remove(0);
        return new OrConstraint(literalConstraint, createDisjunction(literals));
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
        if (constraint instanceof LiteralConstraint && ((LiteralConstraint) constraint).getReference() instanceof Feature) {
            Feature feature = (Feature) ((LiteralConstraint) constraint).getReference();
            if (subTreeFeatures.contains(feature)) {
                return true;
            }
        }else if (constraint instanceof  ExpressionConstraint) {
            Expression left = ((ExpressionConstraint) constraint).getLeft();
            Expression right = ((ExpressionConstraint) constraint).getRight();
            return expressionContains(left,subTreeFeatures) || expressionContains(right,subTreeFeatures);
        }

        List<Constraint> subParts = constraint.getConstraintSubParts();
        for (Constraint subPart : subParts) {
            if (subPart instanceof LiteralConstraint && ((LiteralConstraint) subPart).getReference() instanceof Feature) {
                Feature feature = (Feature) ((LiteralConstraint) subPart).getReference();
                if (subTreeFeatures.contains(feature)) {
                    return true;
                }
            } else {
                if (constraintContains(subPart, subTreeFeatures)) {
                    return true;
                }

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
            }else if(expressionContains(subExpression, subTreeFeatures)){
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
        }else{
            List<Constraint> subParts = constraint.getConstraintSubParts();
            for (Constraint subPart : subParts) {
                if (subPart instanceof LiteralConstraint) {
                    String toReplace = ((LiteralConstraint) subPart).getReference().getIdentifier();
                    if (featureReplacementMap.containsKey(toReplace)) {
                        LiteralConstraint subTreeRootConstraint = new LiteralConstraint(subTreeRoot);
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

        }else{
            for (Expression subExpression : expression.getExpressionSubParts()) {
                adaptExpression(subExpression, featureReplacementMap);
            }
        }
    }
}
