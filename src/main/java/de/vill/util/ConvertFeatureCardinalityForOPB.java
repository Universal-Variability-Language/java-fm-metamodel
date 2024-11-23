package de.vill.util;

import de.vill.conversion.IConversionStrategy;
import de.vill.model.*;
import de.vill.model.constraint.*;

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
        newChildren.setCardinality(new Cardinality(min, max));

        feature.setCardinality(null);

        for (int i = Math.max(min,1); i <= max; i++) {
            Feature subTreeClone = feature.clone();
            addPrefixToNamesRecursively(subTreeClone, "_" + i);
            newChildren.getFeatures().add(subTreeClone);
            subTreeClone.setParentGroup(newChildren);

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
        for (int i = Math.max(min,1); i < max; i++) {
            Constraint lastTakenInGroupCardinality = new LiteralConstraint(newChildren.getFeatures().get(i - min));
            List<LiteralConstraint> notToTakeInGroupCarrdinality = new LinkedList<>();
            for (int k=i+1;k<=max;k++){
                notToTakeInGroupCarrdinality.add(new LiteralConstraint(newChildren.getFeatures().get(k - min)));
            }
            Constraint groupCardinalityOrderConstraint = new ImplicationConstraint(new NotConstraint(lastTakenInGroupCardinality), new NotConstraint(createDisjunction(notToTakeInGroupCarrdinality)));
            featureModel.getOwnConstraints().add(groupCardinalityOrderConstraint);
        }
        feature.getChildren().removeAll(feature.getChildren());
        feature.getChildren().add(newChildren);
        newChildren.setParentFeature(feature);
    }

    private Constraint createDisjunction(List<LiteralConstraint> literals) {
        if (literals.size() == 1) {
            return literals.get(0);
        }
        LiteralConstraint literalConstraint = literals.get(0);
        literals.remove(0);
        return new OrConstraint(literalConstraint, createDisjunction(literals));
    }

    private void addPrefixToNamesRecursively(Feature feature, String prefix) {
        feature.setFeatureName(feature.getFeatureName() + prefix);
        if (!feature.isSubmodelRoot()) {
            for (Group group : feature.getChildren()) {
                for (Feature subFeature : group.getFeatures()) {
                    addPrefixToNamesRecursively(subFeature, prefix);
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
