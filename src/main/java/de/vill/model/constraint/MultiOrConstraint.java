package de.vill.model.constraint;

import de.vill.model.building.VariableReference;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MultiOrConstraint extends Constraint{

    private List<Constraint> sub_parts;

    public MultiOrConstraint() {
        sub_parts = new LinkedList<>();
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        StringBuilder result = new StringBuilder();
        for (Constraint part : sub_parts) {
            result.append(part.toString(withSubmodels, currentAlias));
            result.append(" | ");
        }
        result.delete(result.length() -3, result.length());
        return result.toString();
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return sub_parts;
    }

    public void add_sub_part(Constraint constraint) {
        sub_parts.add(constraint);
    }

    @Override
    public void replaceConstraintSubPart(Constraint oldSubConstraint, Constraint newSubConstraint) {
        for (int i=0;i<sub_parts.size();i++) {
            if (sub_parts.get(i) == oldSubConstraint) {
                sub_parts.set(i, newSubConstraint);
                break;
            }
        }
    }

    @Override
    public Constraint clone() {
        MultiOrConstraint multiOrConstraint = new MultiOrConstraint();
        for (Constraint part : sub_parts) {
            multiOrConstraint.add_sub_part(part.clone());
        }
        return multiOrConstraint;
    }

    @Override
    public int hashCode(int level) {
        final int prime = 31;
        int result = 1;
        for (Constraint part : sub_parts) {
            result = prime * result + part.hashCode(1 + level);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MultiOrConstraint other = (MultiOrConstraint) obj;

        if (this.sub_parts.size() != other.sub_parts.size()) {
            return false;
        }
        boolean same = true;
        for (int i=0;i<sub_parts.size();i++) {
            if (sub_parts.get(i) != other.sub_parts.get(i)) {
                same = false;
                break;
            }
        }
        return same;
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        for (int i=0;i<sub_parts.size();i++) {
            references.addAll(sub_parts.get(i).getReferences());
        }
        return references;
    }
}
