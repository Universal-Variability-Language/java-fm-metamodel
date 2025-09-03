package de.vill.conversion;

import com.google.common.collect.Sets;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.LanguageLevel;
import de.vill.model.constraint.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConvertGroupCardinality implements IConversionStrategy {
    @Override
    public Set<LanguageLevel> getLevelsToBeRemoved() {
        return new HashSet<>(Arrays.asList(LanguageLevel.GROUP_CARDINALITY));
    }

    @Override
    public Set<LanguageLevel> getTargetLevelsOfConversion() {
        return new HashSet<>(Arrays.asList(LanguageLevel.BOOLEAN_LEVEL));
    }

    @Override
    public void convertFeatureModel(FeatureModel rootFeatureModel, FeatureModel featureModel) {
        searchGroupCardinalities(featureModel.getRootFeature(), featureModel);
    }

    private void searchGroupCardinalities(Feature feature, FeatureModel featureModel) {
        for (Group group : feature.getChildren()) {
            if (group.GROUPTYPE.equals(Group.GroupType.GROUP_CARDINALITY)) {
                removeGroupCardinality(group, featureModel);
            }
            for (Feature subFeature : group.getFeatures()) {
                if (!subFeature.isSubmodelRoot()) {
                    searchGroupCardinalities(subFeature, featureModel);
                }
            }
        }
    }

    private void removeGroupCardinality(Group group, FeatureModel featureModel) {

        group.GROUPTYPE = Group.GroupType.OPTIONAL;

        Set<Feature> groupMembers = new HashSet<>(group.getFeatures());

        int lowerBound = group.getCardinality().lower;
        int upperBound = Math.min(group.getCardinality().upper, groupMembers.size());
        Set<Set<Feature>> featureCombinations = new HashSet<>();
        for (int i = lowerBound; i <= upperBound; i++) {
            featureCombinations.addAll(Sets.combinations(groupMembers, i));
        }
        Set<Constraint> disjunction = new HashSet<>();
        for (Set<Feature> configuration : featureCombinations) {
            disjunction.add(createConjunction(configuration, new HashSet<>(groupMembers)));
        }

        featureModel.getOwnConstraints().add(new ImplicationConstraint(new LiteralConstraint(group.getParentFeature()), new ParenthesisConstraint(createDisjunction(disjunction))));
    }

    private Constraint createConjunction(Set<Feature> selectedFeatures, Set<Feature> allFeatures) {
        Constraint constraint;
        Feature feature = null;
        if (!allFeatures.isEmpty()) {
            feature = allFeatures.iterator().next();
            allFeatures.remove(feature);
        }
        Constraint literalConstraint = new LiteralConstraint(feature);
        if (!selectedFeatures.contains(feature)) {
            literalConstraint = new NotConstraint(literalConstraint);
        }
        if (allFeatures.size() == 0) {
            constraint = literalConstraint;
        } else {
            constraint = new AndConstraint(literalConstraint, createConjunction(selectedFeatures, allFeatures));
        }

        return constraint;
    }

    private Constraint createDisjunction(Set<Constraint> constraints) {
        MultiOrConstraint orConstraint = new MultiOrConstraint();
        for (Constraint constraint : constraints) {
            orConstraint.add_sub_part(constraint);
        }
        return orConstraint;
    }
}
