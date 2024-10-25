package de.vill.model.expression;

import com.google.common.collect.Sets;
import de.vill.model.Feature;
import de.vill.model.building.VariableReference;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.pbc.Literal;
import de.vill.model.pbc.PBConstraint;
import de.vill.util.Constants;
import de.vill.util.SubstitutionVariableIndex;

import java.util.*;

import static de.vill.util.Util.substitutionConstraint;

public class DivExpression extends BinaryExpression {
    private Expression left;
    private Expression right;

    public DivExpression(Expression left, Expression right) {
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
            " / " +
            right.toString(withSubmodels, currentAlias);
    }

    @Override
    public String getReturnType() {
        return Constants.NUMBER;
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
        return left.evaluate(selectedFeatures) / (right.evaluate(selectedFeatures));
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
        DivExpression other = (DivExpression) obj;
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
    public List<Literal> getAsSum(List<PBConstraint> additionalConstraints) {
        List<Literal> result = new LinkedList<>();
        List<Literal> numeratorSum = getLeft().getAsSum(additionalConstraints);
        List<Literal> denominatorSum = getRight().getAsSum(additionalConstraints);
        SubstitutionVariableIndex substitutionVariableIndex = SubstitutionVariableIndex.getInstance();
        for (Literal l : numeratorSum) {
            Set<Set<Literal>> literalCombinations = getLiteralCombinations(new HashSet<Literal>(denominatorSum));
            for (Set<Literal> combination : literalCombinations) {
                Literal newSummand = new Literal();
                newSummand.factor = l.factor;
                double denominatorFactorSum = 0.0;
                for (Literal denominatorLiteral : combination) {
                    denominatorFactorSum += denominatorLiteral.factor;
                }
                newSummand.factor /= denominatorFactorSum;
                newSummand.name = substitutionVariableIndex.getIndex();
                result.add(newSummand);
                PBConstraint denominatorConstraint = featureCombinationToPBConstraint(combination, denominatorSum);
                denominatorConstraint.literalList.add(new Literal(1, l.name));
                denominatorConstraint.k += 1;
                additionalConstraints.addAll(substitutionConstraint(denominatorConstraint, newSummand.name));
                //TODO add x <=> l & combination (with positive and negative literals)
            }
        }
        return result;
    }

    private Set<Set<Literal>> getLiteralCombinations(Set<Literal> literals) {
        Set<Set<Literal>> literalCombinations = new HashSet<>();
        for (int i = 1; i <= literals.size(); i++) {
            literalCombinations.addAll(Sets.combinations(literals, i));
        }
        return literalCombinations;
    }

    private PBConstraint featureCombinationToPBConstraint(Set<Literal> takenLiterals, List<Literal> allLiterals) {
        PBConstraint pbConstraint = new PBConstraint();
        pbConstraint.literalList = new LinkedList<>();
        pbConstraint.k = allLiterals.size();
        for (Literal literal : allLiterals) {
            if (takenLiterals.contains(literal)) {
                //literal positive in result
                Literal newLiteral = new Literal();
                newLiteral.name = literal.name;
                newLiteral.factor = 1;
                pbConstraint.literalList.add(newLiteral);
            }else {
                //literal negative in result
                Literal negatedLiteral = new Literal();
                negatedLiteral.name = literal.name;
                negatedLiteral.factor = -1;
                pbConstraint.k -= 1;
                pbConstraint.literalList.add(negatedLiteral);
            }

        }
        return pbConstraint;
    }
}
