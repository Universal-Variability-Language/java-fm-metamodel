package de.vill.model;

import de.vill.model.building.VariableReference;

public class ImportedAttributePlaceholder implements VariableReference {

    public final String featureName;
    public final String attributeName;
    public final Import relatedImport;

    public ImportedAttributePlaceholder(String featureName, String attributeName, Import relatedImport) {
        this.featureName = featureName;
        this.attributeName = attributeName;
        this.relatedImport = relatedImport;
    }

    @Override
    public String getIdentifier() {
        return "";
    }
}
