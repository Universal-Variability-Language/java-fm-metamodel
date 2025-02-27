package de.vill.model;

import de.vill.model.building.VariableReference;
import de.vill.util.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Attribute reference that is not tied to any features. Supposed for usage in aggregate functions
 */
public class GlobalAttribute implements VariableReference {

    public enum AttributeType {
        STRING(Constants.STRING),
        NUMBER(Constants.NUMBER),
        BOOLEAN(Constants.BOOLEAN);

        private final String name;

        AttributeType(String name) {
            this.name = name;
        }

        public static AttributeType fromString(String name) {
            for (AttributeType type : AttributeType.values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    private String identifier;
    private AttributeType type;
    private final Set<Feature> featuresContainingAttribute;

    public GlobalAttribute(String identifier, FeatureModel featureModel) {
        this.identifier = identifier;
        this.featuresContainingAttribute = new HashSet<>();
        for (Feature feature : featureModel.getFeatureMap().values()) {
            if (feature.getAttributes().containsKey(identifier)) {
                this.type = AttributeType.fromString(feature.getAttributes().get(identifier).getType());
            }
        }

    }

    public GlobalAttribute(String identifier, AttributeType type) {
        this.identifier = identifier;
        this.type = type;
        featuresContainingAttribute = new HashSet<>();
    }

    public void renameGlobalAttribute(String newIdentifier) { this.identifier = newIdentifier;}

    public void addFeature(Feature feature) {
        featuresContainingAttribute.add(feature);
    }

    public boolean removeFeature(Feature feature) {
        return featuresContainingAttribute.remove(feature);
    }

    public AttributeType getType() {return type;}

    public void updateSetAccordingToFm(FeatureModel featureModel) {
        // TODO
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
