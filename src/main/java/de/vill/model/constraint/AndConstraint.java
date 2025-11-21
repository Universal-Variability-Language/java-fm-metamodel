package de.vill.model.constraint;

import de.vill.model.building.VariableReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AndConstraint extends Constraint {

    //List of children
    private final List<Constraint> children = new ArrayList<>();

    public AndConstraint(Constraint... constraints) {
        for (Constraint c : constraints) {
            if (c != null) {
                children.add(c);
            }
        }
    }

    public AndConstraint(Constraint left, Constraint right) {
        this.children.add(left);
        this.children.add(right);
    }

    public Constraint getLeft() {
        if (children.isEmpty()){
            return null;
        }
        else{
            return children.get(0);
        }
    }

    public Constraint getRight() {
        if (children.isEmpty() || children.size() < 2){
            return null;
        }
        else{
            return children.get(children.size() - 1);
        }
    }

    public List<Constraint> getChildren() {
        return children;
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return  
                // Constraint-Stream - jeder constraint in einen String umgewandelt und mit einem & verknüpft
                children.stream()
                .map(c -> c.toString(withSubmodels, currentAlias))
                .collect(Collectors.joining(" & "));
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return new ArrayList<>(children);
    }

    @Override
    public void replaceConstraintSubPart(Constraint oldSubConstraint, Constraint newSubConstraint) {
        for (int i = 0; i< children.size(); i++) {
            if (children.get(i) == oldSubConstraint){
                children.set(i, newSubConstraint);
            }
        }
    }

    @Override
    public Constraint clone() {
        AndConstraint clone = new AndConstraint();
        for (Constraint c : children) {
            clone.addChild(c.clone());
        }
        return clone;
    }

    public void addChild(Constraint constraint){
        if (constraint != null) {
            children.add(constraint);
        }
    }

    @Override
    public int hashCode(int level) {
        final int prime = 31;
        int result = prime * level;
        for(Constraint c: children) {
            result = prime * result + (c == null ? 0 : c.hashCode(1 + level));
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
        AndConstraint other = (AndConstraint) obj;
        return Objects.equals(children, other.children);
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        for(Constraint c: children){
            references.addAll(c.getReferences());
        }
        return references;
    }
}
