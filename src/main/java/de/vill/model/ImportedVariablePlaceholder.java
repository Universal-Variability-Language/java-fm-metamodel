package de.vill.model;

import de.vill.model.building.VariableReference;

import java.util.List;

public class ImportedVariablePlaceholder implements VariableReference {

    public final Import mainImport;
    public final List<String> unidentifiedImportParts;

    public ImportedVariablePlaceholder(Import relatedImport, List<String> unidentifiedImportParts) {
        this.mainImport = relatedImport;
        this.unidentifiedImportParts = unidentifiedImportParts;
    }

    @Override
    public String getIdentifier() {
        return String.format("%s.%s", mainImport.getAlias(), String.join(".", unidentifiedImportParts));
    }
}
