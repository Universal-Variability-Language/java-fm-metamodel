package de.vill.model.pbc;

public class Literal {
    public String name;
    public double factor;
    public Literal(){}
    public Literal(double factor, String name) {
        this.name = name;
        this.factor = factor;
    }
}
