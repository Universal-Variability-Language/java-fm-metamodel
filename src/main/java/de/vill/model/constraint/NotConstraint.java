package de.vill.model.constraint;

import de.vill.model.building.VariableReference;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public int extractTseitinSubConstraints(Map<Integer, Constraint> substitutionMapping, int n, int counter) {
        if (content instanceof LiteralConstraint){
            return 0;
        }
        int a1 = content.extractTseitinSubConstraints(substitutionMapping, n, counter);
        int finalA = a1;
        Constraint l1 = new LiteralConstraint(new VariableReference() {
            @Override
            public String getIdentifier() {
                return "x_" + counter + "_" + finalA;
            }
        });
        if(a1 == 0) {
            l1 = content;
        }else{
            n = a1 + 1;
        }

        Constraint newConstraint = new NotConstraint(l1);
        substitutionMapping.put(n, newConstraint);
        return n;
    };

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
