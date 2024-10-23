package de.vill.model.pbc;

import java.util.List;

public class PBConstraint {
    public List<Literal> literalList;
    public int k;
    public PBConstraintType type;

    public PBConstraint() {
        type = PBConstraintType.GEQ;
    }

    @Override
    public String toString() {
        String res = "";
        for(Literal l : literalList){
            if(l.factor < 0){
                res += " " + l.factor;
            }else{
                res += " +" + l.factor;
            }
            res += " " + l.name;
        }
        res += " " + type + " " + k;
        return res;
    }
}

