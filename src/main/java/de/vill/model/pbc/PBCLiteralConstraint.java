package de.vill.model.pbc;

import de.vill.model.building.VariableReference;
import de.vill.model.constraint.LiteralConstraint;

public class PBCLiteralConstraint extends LiteralConstraint {
    public boolean sign = true;
    public PBCLiteralConstraint(VariableReference reference) {
        super(reference);
    }

    public PBCLiteralConstraint(LiteralConstraint literalConstraint){
        super(literalConstraint.getReference());
    }

    public void toggleSign(){
        sign = !sign;
    }
}
