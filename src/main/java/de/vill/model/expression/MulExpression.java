package de.vill.model.expression;

import de.vill.model.Feature;
import de.vill.model.building.VariableReference;
import de.vill.model.pbc.Literal;
import de.vill.model.pbc.PBCConstraint;
import de.vill.util.Constants;
import de.vill.util.SubstitutionVariableIndex;

import java.util.*;

public class MulExpression extends BinaryExpression {
    private Expression left;
    private Expression right;

    public MulExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Expression getLeft() {
        return left;
    }

    @Override
    public Expression getRight() {
        return right;
    }

    @Override
    public String toString(boolean withSubmodels, String currentAlias) {
        return left.toString(withSubmodels, currentAlias) +
            " * " +
            right.toString(withSubmodels, currentAlias);
    }

    @Override
    public List<Expression> getExpressionSubParts() {
        return Arrays.asList(left, right);
    }

    @Override
    public void replaceExpressionSubPart(Expression oldSubExpression, Expression newSubExpression) {
        if (left == oldSubExpression) {
            left = newSubExpression;
        } else if (right == oldSubExpression) {
            right = newSubExpression;
        }
    }

    @Override
    public double evaluate(Set<Feature> selectedFeatures) {
        double leftResult;
        if (left instanceof LiteralExpression
                && ((LiteralExpression) left).getContent() instanceof Feature && !selectedFeatures.contains((Feature)((LiteralExpression) left).getContent())) {
            leftResult = 1;
        } else {
            leftResult = left.evaluate(selectedFeatures);
        }
        double rightResult;
        if (right instanceof LiteralExpression
            && ((LiteralExpression) right).getContent() instanceof Feature && !selectedFeatures.contains((Feature)((LiteralExpression) right).getContent())) {
            rightResult = 1;
        } else {
            rightResult = right.evaluate(selectedFeatures);
        }
        return leftResult * rightResult;
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
        MulExpression other = (MulExpression) obj;
        return Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }

    @Override
    public List<VariableReference> getReferences() {
        List<VariableReference> references = new ArrayList<>();
        references.addAll(left.getReferences());
        references.addAll(right.getReferences());
        return references;
    }

    @Override
    public List<Literal> getAsSum(List<PBCConstraint> additionalConstraints) {
        var leftSum = left.getAsSum(additionalConstraints);
        var rightSum = right.getAsSum(additionalConstraints);
        List<Literal> result = new LinkedList<>();
        SubstitutionVariableIndex substitutionVariableIndex = SubstitutionVariableIndex.getInstance();
        for (int i=0;i<leftSum.size();i++){
            for (int j=0;j<rightSum.size();j++){
                Literal l = new Literal();
                l.factor = leftSum.get(i).factor * rightSum.get(j).factor;
                l.name = substitutionVariableIndex.getIndex();
                result.add(l);
                additionalConstraints.addAll(getSubstitutionConstraints(leftSum.get(i).name, rightSum.get(j).name, l.name));
            }
        }
        return result;
    }

    private List<PBCConstraint> getSubstitutionConstraints(String a, String b, String c){
        List<PBCConstraint> result = new LinkedList<>();
        PBCConstraint pbcConstraint1 = new PBCConstraint();
        pbcConstraint1.literalList = new LinkedList<>();
        pbcConstraint1.k = 0;
        pbcConstraint1.literalList.add(new Literal(1, a));
        pbcConstraint1.literalList.add(new Literal(1, b));
        pbcConstraint1.literalList.add(new Literal(-2, c));
        result.add(pbcConstraint1);
        PBCConstraint pbcConstraint2 = new PBCConstraint();
        pbcConstraint2.literalList = new LinkedList<>();
        pbcConstraint2.k = -1;
        pbcConstraint2.literalList.add(new Literal(-1, a));
        pbcConstraint2.literalList.add(new Literal(-1, b));
        pbcConstraint2.literalList.add(new Literal(2, c));
        result.add(pbcConstraint2);
        return result;
    }

    @Override
    public String getReturnType() {
        return Constants.NUMBER;
    }
}
