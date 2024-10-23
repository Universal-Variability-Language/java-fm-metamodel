package de.vill.model.pbc;

public enum PBConstraintType {
    GEQ(">="),
    GE(">"),
    LEQ("<="),
    LE("<"),
    EQ("="),
    NOTEQ("!=");

    private String value;

    PBConstraintType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
