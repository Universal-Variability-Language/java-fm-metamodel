package de.vill.model.building;

import de.vill.model.*;

import java.util.List;

public final class ParsingUtilities {

    private ParsingUtilities() {} // Only for static use

    public static Import parseImport(String namespace, String alias) {
        return new Import(namespace.replace("\"", ""), alias != null ? alias.replace("\"", "") : null);
    }

    /**
     *
     * @param cardinalityString in format [n..m]
     * @return
     */
    public static Cardinality parseCardinality(String cardinalityString) {
        int lowerBound;
        int upperBound;
        if (cardinalityString.contains("..")) {
            lowerBound = Integer.parseInt(cardinalityString.substring(1, cardinalityString.indexOf("..")));
            upperBound = Integer.parseInt(cardinalityString.substring(cardinalityString.indexOf("..") + 2, cardinalityString.length() - 1));
        } else {
            lowerBound = Integer.parseInt(cardinalityString.substring(1, cardinalityString.length() - 1));
            upperBound = lowerBound;
        }
        return new Cardinality(lowerBound, upperBound);
    }

    /**
     * Creates feature with the following information: feature name, namespace
     * @param identifier
     * @param availableImports
     * @return
     */
    public static Feature parseFeatureInitialization(String identifier, List<Import> availableImports) {
        String featureName = null;
        Import relatedImport = null;
        if (identifier.contains(".")) {
            for (Import subModelImport : availableImports) {
                String prefix = subModelImport.getAlias() + ".";
                if (identifier.startsWith(prefix)) {
                    featureName = identifier.substring(prefix.length());
                    relatedImport = subModelImport;
                }
            }
            if (featureName == null) return null;
        } else {
            featureName = identifier;
        }

        Feature feature = new Feature(featureName);
        if (relatedImport != null) {
            feature.setRelatedImport(relatedImport);
            feature.setSubmodelRoot(true);
        }

        return feature;
    }

    public static VariableReference resolveFeatureDeclarationIdentifier(String identifier, FeatureModel fmInConstruction) {
        ReferenceBundle bundle = getReferenceSplit(identifier, fmInConstruction);
        Feature feature = new Feature(bundle.featureName);
        feature.setRelatedImport(bundle.relatedImport);
        return feature;
    }

    public static VariableReference resolveReference(String reference, FeatureModel fmInConstruction) {
        ReferenceBundle bundle = getReferenceSplit(reference, fmInConstruction);

        if (bundle.relatedImport != null) {
            if (bundle.attributeName != null) {
                return new ImportedAttributePlaceholder(bundle.featureName, bundle.attributeName, bundle.relatedImport);
            } else {
                return new ImportedFeaturePlaceholder(bundle.featureName, bundle.relatedImport);
            }
        }
        Feature relevantFeature = fmInConstruction.getFeatureMap().get(bundle.featureName);
        if (bundle.attributeName != null) {
            if (relevantFeature.getAttributes().containsKey(bundle.attributeName)) {
                return relevantFeature.getAttributes().get(bundle.attributeName);
            }
        }
        return relevantFeature;
    }

    public static ReferenceBundle getReferenceSplit(String reference, FeatureModel fmInConstruction) {
        reference = reference.replace("\"", ""); // Remove "" if available
        ReferenceBundle bundle = new ReferenceBundle();
        List<Import> availableImports = fmInConstruction.getImports();
        String suffix = reference;
        for (Import subModelImport : availableImports) {
            String prefix = subModelImport.getAlias() + ".";
            if (reference.startsWith(prefix)) {
                suffix = reference.substring(prefix.length());
                bundle.relatedImport = subModelImport;
            }
        }
        if (suffix.contains(".")) {
            bundle.featureName = suffix.substring(0, suffix.indexOf('.'));
            bundle.attributeName = suffix.substring(suffix.indexOf('.') + 1);
        } else {
            bundle.featureName = suffix;
        }

        return bundle;
    }

    public static GlobalAttribute getGlobalAttribute(String reference, FeatureModel featureModel) {
        reference = reference.replace("\"", ""); // Remove "" if available
        return new GlobalAttribute(reference, featureModel);
    }

}
