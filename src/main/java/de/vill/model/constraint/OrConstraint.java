package de.vill.model.constraint;

import de.vill.model.building.VariableReference;

import java.util.*;

import static de.vill.util.Util.isJustAnd;
import static de.vill.util.Util.isJustOr;

public class OrConstraint extends Constraint {

    private Constraint left;
    private Constraint right;

    public OrConstraint(Constraint left, Constraint right) {
        this.left = left;
        this.right = right;
    }

    public Constraint getLeft() {
        return left;
    }

    public Constraint getRight() {
        return right;
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return left.toString(withSubmodels, currentAlias) +
                " | " +
                right.toString(withSubmodels, currentAlias);
    }

    @Override
    public List<Constraint> getConstraintSubParts() {
        return Arrays.asList(left, right);
    }

    @Override
    public void replaceConstraintSubPart(Constraint oldSubConstraint, Constraint newSubConstraint) {
        if (left == oldSubConstraint) {
            left = newSubConstraint;
        } else if (right == oldSubConstraint) {
            right = newSubConstraint;
        }
    }

    @Override
    public Constraint clone() {
        return new OrConstraint(left.clone(), right.clone());
    }

    @Override
    public int hashCode(int level) {
        final int prime = 31;
        int result = prime * level + (left == null ? 0 : left.hashCode(1 + level));
        result = prime * result + (right == null ? 0 : right.hashCode(1 + level));
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
        return Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        references.addAll(left.getReferences());
        references.addAll(right.getReferences());
        return references;
    }

    public int extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping, int n) {
        int a1 = 0;
        if(!isJustOr(left)){
            a1 = left.extractTseitinSubConstraints(substitutionMapping, n);
        }
        int a2 = 0;
        if(!isJustOr(right)){
            a2 = right.extractTseitinSubConstraints(substitutionMapping, n);
        }
        int finalA = a1;
        Constraint l1 = new LiteralConstraint(new VariableReference() {
            @Override
            public String getIdentifier() {
                return "x_" + finalA;
            }
        });
        int finalA1 = a2;
        Constraint l2 = new LiteralConstraint(new VariableReference() {
            @Override
            public String getIdentifier() {
                return "x_" + finalA1;
            }
        });
        if(a1 == 0) {
            l1 = left;
        }else{
            n = a1 + 1;
        }
        if(a2 == 0) {
            l2 = right;
        }else{
            n = a2 + 1;
        }

        OrConstraint newConstraint = new OrConstraint(l1, l2);
        substitutionMapping.put(n, newConstraint);
        return n;
    };
}
