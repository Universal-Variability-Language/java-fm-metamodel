package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GreaterEqualsEquationConstraint extends ExpressionConstraint {
    public GreaterEqualsEquationConstraint(final Expression left, final Expression right) {
        super(left, right, ">=");
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return Collections.emptyList();
    }

    @Override
    public Constraint clone() {
        return new GreaterEqualsEquationConstraint(getLeft().clone(), getRight().clone());
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        references.addAll(getLeft().getReferences());
        references.addAll(getRight().getReferences());
        return references;
    }
}
