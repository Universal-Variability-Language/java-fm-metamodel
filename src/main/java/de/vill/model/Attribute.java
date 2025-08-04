package de.vill.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.management.AttributeList;

import de.vill.model.building.VariableReference;
import de.vill.model.constraint.Constraint;
import de.vill.util.Constants;

/**
 * This class represents an Attribute.
 * There is an separated class and not just Objects in the attributes map to be able to reference a single attribute
 * for example in a constraint.
 *
 * @param <T> The type of the value
 */
public class Attribute<T> implements VariableReference {

    private int line;
    private final String name;
    private final T value;
    private final Feature feature;

    /**
     * The constructor of the attribute class takes an attribute name (does not contain the feature name) and a value of type T
     *
     * @param name  the name of the attribute (must be different from all other attributes of the feature)
     * @param value the value of the attribute
     */
    public Attribute(String name, T value, Feature correspondingFeature) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
        this.feature = correspondingFeature;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    /**
     * Returns the value of the attribute.
     *
     * @return Value of the attribute (never null)
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns the name of the attribute.
     *
     * @return Name of the attribute (never null)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the attribute
     * @return Name of the attribute (never null)
     */
    public String getType() {
        if (value instanceof Boolean) {
            return Constants.BOOLEAN;
        } else if (value instanceof String) {
            return Constants.STRING;   
        } else if (value instanceof AttributeList) {
            return Constants.ATTRIBUTE_LIST;
        } else if (value instanceof Number) {
            return Constants.NUMBER;
        } else {
            return Constants.UNDEF; 
        }
    }

    /**
     * Returns the feature this attribute is attached to
     * @return Feature
     */
    public Feature getFeature() {return feature;}

    /**
     * Returns a uvl representation of the attribute as string (different for the possible types of the value)
     *
     * @return attribute as string
     */
    public String toString(boolean withSubmodels, String currentAlias) {
    	//should never be the case but who knows...
    	if (value == null) {
    		return "";
    	}
        StringBuilder result = new StringBuilder();
        if (value instanceof Map) {
            //attributes map to string
            result.append("{");
            Map<?, ?> map = (Map<?, ?>) value;
			if (!map.isEmpty()) {
				map.forEach((k, v) -> {
					result.append(k);
	                result.append(' ');
	                if (v instanceof Attribute) {
	                    result.append(((Attribute<?>) v).toString(withSubmodels, currentAlias));
	                } else {
	                    result.append(String.valueOf(v));
	                }
	                result.append(',');
	                result.append(' ');
	            });
	            //remove comma after last entry
	            result.setLength(result.length() - 2);
			}
            result.append("}");
        } else if (value instanceof List) {
            //vector (list) of attributes to string
            result.append("[");
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
				for (Object item : list) {
	                if (item instanceof Constraint) {
	                    result.append(((Constraint) item).toString(withSubmodels, currentAlias));
	                } else {
	                    result.append(String.valueOf(item));
	                }
	                result.append(", ");
	            }
	            result.setLength(result.length() - 2);
            }
            result.append("]");
        } else if (value instanceof String) {
            result.append("'");
            result.append((String) value);
            result.append("'");
        } else if (value instanceof Constraint) {
            result.append(((Constraint) value).toString(withSubmodels, currentAlias));
        } else {
            result.append(String.valueOf(value));
        }
        return result.toString();
    }
    
    @Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

    @Override
    public boolean equals(Object obj) {
    	if (this == obj) {
    		return true;
    	}
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Attribute<?> other = (Attribute<?>) obj;
        return Objects.equals(name, other.name)
        		&& Objects.equals(value, other.value);
    }

    @Override
    public String getIdentifier() {
        return feature.getIdentifier() + "." + name;
    }
}
