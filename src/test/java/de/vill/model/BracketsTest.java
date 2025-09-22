package de.vill.model;

import de.vill.model.constraint.*;
import de.vill.model.expression.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BracketsTest {


    private static LiteralConstraint A;
    private static LiteralConstraint B;

    private static LiteralExpression C;
    private static LiteralExpression D;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        A = new LiteralConstraint(new Feature("A"));
        B = new LiteralConstraint(new Feature("B"));
        C = new LiteralExpression(new Feature("C"));
        D = new LiteralExpression(new Feature("D"));
    }

    @Test
    void testSimpleBooleanLogic() {
        Constraint simpleOr = new OrConstraint(A,B);
        Constraint simpleAnd = new AndConstraint(A,B);
        Constraint simpleImply = new ImplicationConstraint(A,B);
        Constraint simpleEquivalence = new EquivalenceConstraint(A,B);

        // and-or
        assert new AndConstraint(simpleOr, simpleOr).toString().equals("(A | B) & (A | B)");
        assert new OrConstraint(simpleAnd, simpleAnd).toString().equals("A & B | A & B");

        // and-imply
        assert new AndConstraint(simpleImply, simpleImply).toString().equals("(A => B) & (A => B)");
        assert new ImplicationConstraint(simpleAnd, simpleAnd).toString().equals("A & B => A & B");

        // imply-equiv
        assert new ImplicationConstraint(simpleEquivalence, simpleEquivalence).toString().equals("(A <=> B) => (A <=> B)");
        assert new EquivalenceConstraint(simpleImply, simpleImply).toString().equals("A => B <=> A => B");
    }

    @Test
    void testSimpleExpressions() {
        Expression simpleSub = new SubExpression(C,D);
        Expression simpleAdd = new AddExpression(C,D);
        Expression simpleMult = new MulExpression(C,D);
        Expression simpleDiv = new DivExpression(C,D);

        // add-sub
        assert new AddExpression(simpleSub, simpleSub).toString().equals("(C - D) + (C - D)");
        assert new SubExpression(simpleAdd, simpleAdd).toString().equals("(C + D) - (C + D)");

        // add-mult
        assert new AddExpression(simpleMult, simpleMult).toString().equals("C * D + C * D");
        assert new MulExpression(simpleAdd, simpleAdd).toString().equals("(C + D) * (C + D)");

        // mult-div
        assert new MulExpression(simpleDiv, simpleDiv).toString().equals("(C / D) * (C / D)");
    }

}
