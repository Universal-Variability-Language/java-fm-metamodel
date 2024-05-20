package de.vill.model;

/**
 * Simple object for specifying a cardinality
 */
public class Cardinality {

    int lower;
    int upper;

    public Cardinality(int lower, int upper) {
        this.lower = lower;
        this.upper = upper;
        assert lower <= upper;
    }

    public static Cardinality getStandardCardinality() {
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
