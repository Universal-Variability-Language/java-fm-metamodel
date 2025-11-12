package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.expression.Expression;
import de.vill.util.ConstantSymbols;

import java.util.ArrayList;
import java.util.List;

public class EqualEquationConstraint extends ExpressionConstraint {

    public EqualEquationConstraint(final Expression left, final Expression right) {
        super(left, right, ConstantSymbols.EQUALS);
    }

    @Override
    public Constraint clone() {
        return new EqualEquationConstraint(getLeft().clone(), getRight().clone());
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        references.addAll(getLeft().getReferences());
        references.addAll(getRight().getReferences());
        return references;
    }
}
