package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.pbc.PBCLiteralConstraint;
import de.vill.util.SubstitutionVariableIndex;
import org.prop4j.And;
import org.prop4j.Node;

import java.util.*;

import static de.vill.util.Util.getMaxAndConstraint;
import static de.vill.util.Util.isJustAnd;

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

    public void setLeft(Constraint left) {
        this.left = left;
    }

    public void setRight(Constraint right){
        this.right = right;
    }

    @Override
    public Node getNode() {
        var node = new And();
        node.setChildren(left.getNode(), right.getNode());
        return node;
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

    public PBCLiteralConstraint extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping) {
        Constraint leftSub = getMaxAndConstraint(left, substitutionMapping);
        Constraint rightSub = getMaxAndConstraint(right, substitutionMapping);
        int substitutionIndex = SubstitutionVariableIndex.getInstance().getIndex();
        substitutionMapping.put(substitutionIndex, new AndConstraint(leftSub, rightSub));

        return new PBCLiteralConstraint(
                new LiteralConstraint(new VariableReference() {
                    @Override
                    public String getIdentifier() {
                        return "x_" + substitutionIndex;
                    }
                })
        );
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
