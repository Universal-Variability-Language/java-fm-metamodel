package de.vill.model.constraint;

import de.vill.model.building.VariableReference;
import de.vill.model.pbc.PBCLiteralConstraint;

import java.util.*;

public class NotConstraint extends Constraint {
    private Constraint content;

    public NotConstraint(Constraint content) {
        this.content = content;
    }

    public Constraint getContent() {
        return content;
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        StringBuilder result = new StringBuilder();
        result.append("!");
        if (content instanceof VariableReference || content instanceof ParenthesisConstraint) {
            result.append(content.toString(withSubmodels, currentAlias));
        } else {
            result.append("(");
            result.append(content.toString(withSubmodels, currentAlias));
            result.append(")");
        }
        return result.toString();
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
        return new NotConstraint(content.clone());
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
        NotConstraint other = (NotConstraint) obj;
        return Objects.equals(content, other.content);
    }

    @Override
    public List<VariableReference> getReferences() {
        return content.getReferences();
    }

    public PBCLiteralConstraint extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping) {
        PBCLiteralConstraint subContent = content.extractTseitinSubConstraints(substitutionMapping);
        //int substitutionIndex = SubstitutionVariableIndex.getInstance().getIndex();
        //substitutionMapping.put(substitutionIndex, new NotConstraint(subContent));
/*
        PBCLiteralConstraint result = new PBCLiteralConstraint(
            new LiteralConstraint(new VariableReference() {
                @Override
                public String getIdentifier() {
                    return "x_" + substitutionIndex;
                }
            })
        );
        result.toggleSign();

 */
        subContent.toggleSign();
        return subContent;
    }

    @Override
    public StringBuilder toSMT2string() {
        StringBuilder builder = new StringBuilder();
        builder.append("(not\n");
        builder.append(content.toSMT2string());
        builder.append("\n");
        builder.append(")");
        return builder;
    }
}
