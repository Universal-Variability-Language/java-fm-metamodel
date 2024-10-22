package de.vill.model.pbc;

public class Literal {
    public String name;
    public int factor;
    public Literal(){}
    public Literal(int factor, String name) {
        this.name = name;
        this.factor = factor;
    }
}
