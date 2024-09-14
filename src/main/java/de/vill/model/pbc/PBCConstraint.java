package de.vill.model.pbc;

import java.util.List;

public class PBCConstraint {
    public List<Literal> literalList;
    public int k;

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
        res += " >= " + k;
        return res;
    }
}
