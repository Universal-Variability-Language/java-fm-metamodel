package de.vill.model;

/**
 * Simple object for specifying a cardinality
 */
public class Cardinality {

    public int lower;
    public int upper;

    public Cardinality(int lower, int upper) {
        this.lower = lower;
        this.upper = upper;
        assert lower <= upper;
    }

    /**
     * Can be used to model cardinality [n..*], here upper bound is set to integer max
     * @param lower
     */
    public Cardinality(int lower) {
        this(lower, Integer.MAX_VALUE);
    }
    /**
     * Get a [1..1] cardinality which is the default behavior for features
     * @return
     */
    public static Cardinality getStandardFeatureCardinality() {
        return new Cardinality(1, 1);
    }

    @Override
    public String toString() {
        return String.format("[%d..%d]", lower, upper);
    }

    @Override
    public Cardinality clone() {
        return new Cardinality(lower, upper);
    }

    public boolean equals(Cardinality o) {
        return lower == o.lower && upper == o.upper;
    }
}
