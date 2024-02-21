package de.vill.model;

/**
 * An enum that represents the possible types of the Feature (valid of TYPE-level)
 */
public enum FeatureType {

    STRING("String"),
    INT("Integer"),
    BOOL("Boolean"),
    REAL("Real");
	
    private final String name;

    private FeatureType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FeatureType fromString(final String name) {
        for (final FeatureType featureType : FeatureType.values()) {
            if (featureType.name.equalsIgnoreCase(name)) {
                return featureType;
            }
        }
        return null;
    }

    public static Object getDefaultValue(FeatureType type) {
        if (type == BOOL) {
            return Boolean.TRUE;
        } else if (type == INT) {
            return Integer.valueOf(0);
        } else if (type == REAL) {
            return Double.valueOf(0);
        } else  {
            return "";
        }
    }
}
