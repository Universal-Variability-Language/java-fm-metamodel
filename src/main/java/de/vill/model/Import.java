package de.vill.model;

import java.util.Objects;

/**
 * This class represents an import of a feature model in another feature model and
 * is used when uvl feature models are decomposed.
 */
public class Import {

	private final String namespace;
    private final String alias;
    private FeatureModel featureModel;
    private boolean isReferenced = false;
    private int lineNumber;

    /**
     * The constructor of the Import class that sets its name and its optional
     * alias. The alias is used to reference the same submodel in different parts.
     *
     * @param namespace The namespace of the feature model imported (must be set)
     * @param alias     The alias of the import (may be null)
     */
    public Import(String namespace, String alias) {
    	this.namespace = Objects.requireNonNull(namespace);
    	this.alias = alias;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the namespace of the imported feature model
     *
     * @return namespace of the import.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the alias of the import or if it is not set (null or empty string)
     * the namespace. Therefore the getAlias method can always be called and returns
     * the string to reference the submodel.
     *
     * @return alias or namespace if no alias is set.
     */
    public String getAlias() {
        if (alias == null || "".equals(alias)) {
            return namespace;
        }
        return alias;
    }

    /**
     * Returns the featuremodel that is imported by this import. This may be null if
     * the composition is not done. The parser leaves this as null.
     *
     * @return The imported feature model or null if it is not set yet.
     */
    public FeatureModel getFeatureModel() {
        return featureModel;
    }

    /**
     * Set the imported feature model.
     *
     * @param featureModel The feature model which is imported.
     */
    public void setFeatureModel(FeatureModel featureModel) {
        this.featureModel = featureModel;
    }

    /**
     * Boolean depending on if the import is acutally used. Meaning if the root
     * feature of this submodel is referenced anywhere.
     *
     * @return True if the root feature of this import is referenced in the main
     * feature model
     */
    public boolean isReferenced() {
        return isReferenced;
    }

    /**
     * Set boolean if this submodel is used (referenced) any where.
     *
     * @param referenced True if the root feature of this submodel is referenced.
     */
    public void setReferenced(boolean referenced) {
        isReferenced = referenced;
    }

	@Override
	public int hashCode() {
		return Objects.hash(alias, featureModel, isReferenced, lineNumber, namespace);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Import other = (Import) obj;
		return isReferenced == other.isReferenced && lineNumber == other.lineNumber
				&& Objects.equals(alias, other.alias)
				&& Objects.equals(namespace, other.namespace)
				&& Objects.equals(featureModel, other.featureModel);
	}
}
