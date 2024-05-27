package de.vill.model;

import de.vill.model.building.VariableReference;

public class ImportedFeaturePlaceholder implements VariableReference {

    public final String featureName;
    public final Import relatedImport;

    public ImportedFeaturePlaceholder(String featureName, Import relatedImport) {
        this.featureName = featureName;
        this.relatedImport = relatedImport;
    }

    @Override
    public String getIdentifier() {
        return String.format("%s.%s", relatedImport.getAlias(), featureName);
    }
}
