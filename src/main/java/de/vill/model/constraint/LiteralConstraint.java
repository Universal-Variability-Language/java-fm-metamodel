package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class LiteralConstraint extends Constraint {

    private VariableReference reference;

    public LiteralConstraint(final VariableReference reference) {
        this.reference = Objects.requireNonNull(reference);
    }

    public VariableReference getReference() {
        return reference;
    }

    public void setReference(final VariableReference reference) {this.reference = reference;}

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return Util.addNecessaryQuotes(reference.getIdentifier());
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return List.of();
    }

    @Override
    public void replaceConstraintSubPart(Constraint oldSubConstraint, Constraint newSubConstraint) {
        // no sub constraints
    }

    @Override
    public Constraint clone() {
        return new LiteralConstraint(reference);
    }

    @Override
    public int hashCode(int level) {
        return 31 * level + (reference == null ? 0 : reference.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LiteralConstraint other = (LiteralConstraint) obj;
        return Objects.equals(reference, other.reference);
    }

    @Override
    public List<VariableReference> getReferences() {
        return List.of(reference);
    }

    @Override
    public StringBuilder toSMT2string() {
        StringBuilder builder = new StringBuilder();
        builder.append(reference.getIdentifier());
        return builder;
    }
}
