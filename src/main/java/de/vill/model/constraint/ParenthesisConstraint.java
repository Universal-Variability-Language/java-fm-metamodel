package de.vill.model.constraint;

import de.vill.model.building.VariableReference;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParenthesisConstraint extends Constraint {
    private Constraint content;

    public ParenthesisConstraint(Constraint content) {
        this.content = content;
    }

    public Constraint getContent() {
        return content;
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return "(" +
                content.toString(withSubmodels, currentAlias) +
                ")";
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return Arrays.asList(content);
    }

    @Override
    public void replaceConstraintSubPart(Constraint oldSubConstraint, Constraint newSubConstraint) {
        if (content == oldSubConstraint) {
            content = newSubConstraint;
        }
    }

    @Override
    public Constraint clone() {
        return new ParenthesisConstraint(content.clone());
    }

    @Override
    public int hashCode(int level) {
        return 31 * level + (content == null ? 0 : content.hashCode(1 + level));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ParenthesisConstraint other = (ParenthesisConstraint) obj;
        return Objects.equals(content, other.content);
    }

    @Override
    public List<VariableReference> getReferences() {
        return content.getReferences();
    }

    public int extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping, int n, int counter) {
        return content.extractTseitinSubConstraints(substitutionMapping, n, counter);
    };
}
