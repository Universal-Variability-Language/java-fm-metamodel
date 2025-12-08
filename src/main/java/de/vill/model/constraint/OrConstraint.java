package de.vill.model.constraint;

import de.vill.exception.ParseError;
import de.vill.model.building.AutomaticBrackets;
import de.vill.model.building.VariableReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrConstraint extends Constraint {

    private final List<Constraint> children = new ArrayList<>();

    public OrConstraint(Constraint... constraints) {
        for (Constraint c : constraints) {
            if (c != null) {
                children.add(c);
            }
        }
    }

    public OrConstraint(Constraint left, Constraint right) {
        this.children.add(left);
        this.children.add(right);
    }

    public Constraint getLeft() {
        if (children.isEmpty()){
            throw new ParseError("Left child can not be returned because there are no children.");
        }
        else{
            return children.get(0);
        }
    }

    public Constraint getRight() {
        if (children.isEmpty() || children.size() < 2){
            throw new ParseError("RIght child can not be returned because there are less than two children.");
        }
        else{
            return children.get(children.size() - 1);
        }
    }

    public List<Constraint> getChildren() {
        return children;
    }

    public void setLeft(Constraint left) {
        if (children.isEmpty()) {
            children.add(left);
        }
        else {
            children.set(0, left);
        }
    }

    public void setRight(Constraint right){
        if (children.size() < 2) {
            if (children.size() < 1) {
                children.add(null);
            }
            children.add(right);
        }
        else {
            children.set(children.size() - 1, right);
        }
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return children.stream()
        .map(c -> AutomaticBrackets.enforceConstraintBracketsIfNecessary(this, c, withSubmodels, currentAlias))
        .collect(Collectors.joining(" | "));
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
        OrConstraint clone = new OrConstraint();
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
        OrConstraint other = (OrConstraint) obj;
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
