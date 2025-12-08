package de.vill.model.building;

import de.vill.exception.ParseError;
import de.vill.model.*;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ExpressionConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.expression.AggregateFunctionExpression;
import de.vill.model.expression.Expression;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FeatureModelBuilder {
    
    private FeatureModel fmInConstruction;

    private AbstractUVLElementFactory elementFactory;

    public FeatureModelBuilder() {
        this(new DefaultUVLElementFactory());
    }

    public FeatureModelBuilder(AbstractUVLElementFactory factory) {
        this.elementFactory = factory;
        this.fmInConstruction = new FeatureModel();
    }

    public FeatureModelBuilder(FeatureModel old) {
        fmInConstruction = old;
    }

    public void addImport(Import importToAdd) {
        fmInConstruction.getImports().add(importToAdd);
    }

    public void setNamespace(String namespace) {
        fmInConstruction.setNamespace(namespace);
    }

    public void addLanguageLevel(LanguageLevel level) {
        fmInConstruction.getUsedLanguageLevels().add(level);
    }

    public boolean setRootFeature(Feature feature) {
        if (!fmInConstruction.getFeatureMap().containsKey(feature.getFeatureName())) {
            return false;
        }
        fmInConstruction.setRootFeature(feature);
        return true;
    }

    public Feature addRootFeature(String featureName, FeatureType featureType, Cardinality cardinality) {
        Feature rootFeature = elementFactory.createFeature(featureName);
        rootFeature.setFeatureType(featureType);
        rootFeature.setCardinality(cardinality);

        fmInConstruction.getFeatureMap().put(featureName, rootFeature);
        fmInConstruction.setRootFeature(rootFeature);
        return rootFeature;
    }

    public Feature addFeature(String featureName, Group group) {
        return addFeature(featureName, group, null);
    }

    public Feature addFeature(String featureName, Group group, Import featureOrigin) {
        return addFeature(featureName, group, featureOrigin, FeatureType.BOOL);
    }
    
    public Feature addFeature(String featureName, Group group, Import featureOrigin,  FeatureType type) {
        return addFeature(featureName, group, featureOrigin, type, Cardinality.getStandardFeatureCardinality());
    }

    public Feature addFeature(String featureName, Group group, Import featureOrigin, FeatureType type, Cardinality cardinality) {
        Feature feature = elementFactory.createFeature(featureName);
        feature.setRelatedImport(featureOrigin);
        feature.setFeatureType(type);
        feature.setCardinality(cardinality);

        fmInConstruction.getFeatureMap().put(featureName, feature);
        group.getFeatures().add(feature);
        return feature;
    }

    public void addFeature(Feature feature, Group group) {
        fmInConstruction.getFeatureMap().put(feature.getIdentifier(), feature);
        group.getFeatures().add(feature);
        feature.setParentGroup(group);
    }

    public Group addGroup(Feature parentFeature, Group.GroupType groupType) {
        Group toAdd = new Group(groupType);
        parentFeature.addChildren(toAdd);
        return toAdd;
    }

    public void addAttribute(Feature feature, Attribute<?> attribute) {
        feature.getAttributes().put(attribute.getName(), attribute);
    }

    /**
     * Renames a feature and propagates the change through the mdoel
     * @param oldName current name of feature to be renamed in the model
     * @param newName target name
     * @return indicator whether oldName successfully changed in the feature model; fails if oldName is not present
     */
    public boolean renameFeature(String oldName, String newName) {
        Map<String, Feature> featureMap = fmInConstruction.getFeatureMap();
        if (!featureMap.containsKey(oldName)) {
            return false;
        }
        Feature featureToUpdate = featureMap.get(oldName);
        // Update feature
        featureToUpdate.setFeatureName(newName);
        // Update feature map
        featureMap.remove(oldName);
        featureMap.put(newName, featureToUpdate);

        // TODO: Does this need an explicit update of constraints, I assume not
        return true;
    }

    public void renameAttribute(Attribute<?> attribute, String newName) {
        String oldName = attribute.getName();
        attribute.setName(newName);
        attribute.getFeature().getAttributes().remove(oldName);
        attribute.getFeature().getAttributes().put(newName, attribute);
    }

    public void renameAttributeGlobally(String oldName, String newName) {
        for (Feature feature : fmInConstruction.getFeatureMap().values()) {
            if (feature.getAttributes().containsKey(oldName)) {
                Attribute<?> attribute = feature.getAttributes().get(oldName);
                renameAttribute(attribute, newName);
            }
        }
        for (Constraint constraint : fmInConstruction.getConstraints()) {
            crawlConstraintsToRenameGlobalAttribute(constraint, oldName, newName);
        }
    }
    private void crawlConstraintsToRenameGlobalAttribute(Constraint constraint, String attributeName, String replace) {
        if (constraint instanceof ExpressionConstraint) {
            for (Expression exp : ((ExpressionConstraint) constraint).getExpressionSubParts()) {
                crawlExpressionsToRenameGlobalAttribute(exp, attributeName, replace);
            }
        } else {
            for (Constraint child : constraint.getConstraintSubParts()) {
                crawlConstraintsToRenameGlobalAttribute(child, attributeName, replace);
            }
        }
    }

    private void crawlExpressionsToRenameGlobalAttribute(Expression expression, String attributeName, String replace) {
        if (expression instanceof AggregateFunctionExpression) {
            AggregateFunctionExpression aggregateFunctionExpression = (AggregateFunctionExpression) expression;
            if (aggregateFunctionExpression.getAttribute().getIdentifier().equals(attributeName)) {
                aggregateFunctionExpression.getAttribute().renameGlobalAttribute(replace);
            };
        } else {
            for (Expression exp : expression.getExpressionSubParts()) {
                crawlExpressionsToRenameGlobalAttribute(exp, attributeName, replace);
            }
        }
    }

    public void addConstraint(Constraint constraint) {
        fmInConstruction.getOwnConstraints().add(0,constraint);
    }

    public void addConstraintAtPosition(Constraint constraint, int position) {
        fmInConstruction.getOwnConstraints().add(position,constraint);
    }

    public boolean doesFeatureModelSatisfyLanguageLevels(Set<LanguageLevel> languageLevelsToSatisfy) {
        return fmInConstruction.isExplicitLanguageLevels() && !fmInConstruction.getUsedLanguageLevels().equals(languageLevelsToSatisfy);
    }

    public FeatureModel getFeatureModel() {
        return fmInConstruction;
    }

    public Set<LanguageLevel> getLanguageLevels() {
        return fmInConstruction.getUsedLanguageLevels();
    }

    public LiteralConstraint createFeatureLiteral(String name) {
        if (fmInConstruction.getFeatureMap().containsKey(name)) {
            return new LiteralConstraint(fmInConstruction.getFeatureMap().get(name));
        } else {
            System.err.println("Tried to reference " + name + " but feature with that name does not exist");
            return null;
        }
    }

    public GlobalAttribute createGlobalAttribute(String name) {
        GlobalAttribute toCreate = elementFactory.createGlobalAttribute(name, fmInConstruction);
        if (toCreate.getType() == null) {
            System.err.println("Tried to reference " + name + " but attribute with that name does not exist");
            return null;
        }
        return toCreate;
    }

}
