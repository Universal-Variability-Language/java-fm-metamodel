package de.vill.model.building;

import de.vill.model.Import;

import java.util.List;

public class ReferenceBundle {
    public String featureName;
    public Import mainImport;
    public List<String> unidentifiedImportParts;
    public String attributeName;

    public ReferenceBundle() {}

    public ReferenceBundle(String featureName, Import relatedImport, String attributeName, List<String> unidentifiedImportParts) {
        this.featureName = featureName;
        this.mainImport = relatedImport;
        this.attributeName = attributeName;
        this.unidentifiedImportParts = unidentifiedImportParts;
    }

}
