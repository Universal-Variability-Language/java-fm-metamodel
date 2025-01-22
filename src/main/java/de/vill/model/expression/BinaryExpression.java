package de.vill.model.expression;

public abstract class BinaryExpression extends Expression{

    public abstract Expression getLeft();
    public abstract Expression getRight();

}
