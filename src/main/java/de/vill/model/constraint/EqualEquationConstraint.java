package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.expression.Expression;

import java.util.ArrayList;
import java.util.List;

public class EqualEquationConstraint extends ExpressionConstraint {
    private final Expression left;
    private final Expression right;

    public EqualEquationConstraint(final Expression left, final Expression right) {
        super(left, right, "==");
        this.left = left;
        this.right = right;
    }

    @Override
    public Constraint clone() {
        return new EqualEquationConstraint(this.left, this.right);
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        references.addAll(left.getReferences());
        references.addAll(right.getReferences());
        return references;
    }
}
