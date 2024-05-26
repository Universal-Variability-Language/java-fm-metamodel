package de.vill.model.building;

import de.vill.model.Import;

public class ReferenceBundle {
    public String featureName;
    public Import relatedImport;
    public String attributeName;

    public ReferenceBundle() {}

    public ReferenceBundle(String featureName, Import relatedImport, String attributeName) {
        this.featureName = featureName;
        this.relatedImport = relatedImport;
        this.attributeName = attributeName;
    }

}
