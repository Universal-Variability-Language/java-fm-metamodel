package de.vill.model.constraint;

import de.vill.model.building.VariableReference;

import java.util.*;

import static de.vill.util.Util.isJustAnd;
import static de.vill.util.Util.isJustOr;

public class AndConstraint extends Constraint {
    private Constraint left;
    private Constraint right;

    public AndConstraint(Constraint left, Constraint right) {
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
        return  left.toString(withSubmodels, currentAlias) +
                " & " +
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
        return new AndConstraint(left.clone(), right.clone());
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
        AndConstraint other = (AndConstraint) obj;
        return Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        references.addAll(left.getReferences());
        references.addAll(right.getReferences());
        return references;
    }

    public int extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping, int n, int counter) {
        int a1 = 0;
        if(!isJustAnd(left) ){
            a1 = left.extractTseitinSubConstraints(substitutionMapping, n, counter);
            n += a1;
        }
        int a2 = 0;
        if(!isJustAnd(right)){
            a2 = right.extractTseitinSubConstraints(substitutionMapping, n + 1, counter);
            n+= a2;
        }
        int finalA = a1;
        Constraint l1 = new LiteralConstraint(new VariableReference() {
            @Override
            public String getIdentifier() {
                return "x_" + counter + "_" + finalA;
            }
        });
        int finalA1 = a2;
        Constraint l2 = new LiteralConstraint(new VariableReference() {
            @Override
            public String getIdentifier() {
                return "x_" + counter + "_" + finalA1;
            }
        });
        if(a1 == 0) {
            l1 = left;
        }
        if(a2 == 0) {
            l2 = right;
        }
        if (a1 + a2 != 0){
            n++;
        }

        Constraint newConstraint = new AndConstraint(l1, l2);
        substitutionMapping.put(n, newConstraint);
        return n;
    }

    @Override
    public StringBuilder toSMT2string() {
        StringBuilder builder = new StringBuilder();
        builder.append("(and\n");
        builder.append(left.toSMT2string());
        builder.append("\n");
        if (right.toSMT2string().length() == 0) {
            System.out.println("test");
        }
        builder.append(right.toSMT2string());
        builder.append(")");
        return builder;
    }

    ;
}
