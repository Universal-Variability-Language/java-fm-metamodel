package de.vill.model.constraint;

import de.vill.model.expression.Expression;
import java.util.Collections;
import java.util.List;

public class NotEqualsEquationConstraint extends ExpressionConstraint {
    public NotEqualsEquationConstraint(final Expression left, final Expression right) {
        super(left, right, "!=");
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return Collections.emptyList();
    }

    @Override
    public Constraint clone() {
        return new NotEqualsEquationConstraint(getLeft().clone(), getRight().clone());
    }
}
