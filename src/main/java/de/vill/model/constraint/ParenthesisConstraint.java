package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.pbc.PBCLiteralConstraint;
import org.prop4j.And;
import org.prop4j.Node;

import java.util.*;

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

    public PBCLiteralConstraint extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping) {
        return content.extractTseitinSubConstraints(substitutionMapping);
    };

    @Override
    public StringBuilder toSMT2string() {
        StringBuilder builder = new StringBuilder();
        builder.append(content.toSMT2string());
        return builder;
    }

    @Override
    public Node getNode() {
        return content.getNode();
    }
}
