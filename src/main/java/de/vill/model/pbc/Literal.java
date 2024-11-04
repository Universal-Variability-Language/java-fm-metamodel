package de.vill.model.pbc;

public class Literal implements Cloneable{
    public String name;
    public double factor;
    public Literal(){}
    public Literal(double factor, String name) {
        this.name = name;
        this.factor = factor;
    }

    @Override
    public Literal clone() {
        Literal literal = null;
        try {
            literal = (Literal) super.clone();
        } catch (CloneNotSupportedException e) {
            literal = new Literal();
            literal.name = this.name;
            literal.factor = this.factor;
        }
        return literal;
    }
}
