package de.vill.model.pbc;

import java.util.List;

public class PBConstraint {
    public List<Literal> literalList;
    public double k;
    public PBConstraintType type;

    public PBConstraint() {
        type = PBConstraintType.GEQ;
    }

    @Override
    public String toString() {
        int maxDecimalPlaces = getMaxDecimalPlaces();
        String res = "";
        for(Literal l : literalList){
            if(l.factor < 0){
                res += " " + (long) (l.factor * Math.pow(10,maxDecimalPlaces));
            }else{
                res += " +" + (long) (l.factor * Math.pow(10,maxDecimalPlaces));
            }
            res += " " + l.name;
        }
        res += " " + type + " " + (long) (k * Math.pow(10,maxDecimalPlaces));
        return res;
    }

    private int getMaxDecimalPlaces() {
        int maxDecimalPlaces = 0;
        int kDecimalPlaces = countDecimalPlaces(k);
        if (kDecimalPlaces > maxDecimalPlaces) {
            maxDecimalPlaces = kDecimalPlaces;
        }
        for (Literal l : literalList) {
            int lDecimalPlaces = countDecimalPlaces(l.factor);
            if (lDecimalPlaces > maxDecimalPlaces) {
                maxDecimalPlaces = lDecimalPlaces;
            }
        }
        return maxDecimalPlaces;
    }

    private int countDecimalPlaces(double value) {
        String text = String.valueOf(value);

        if (text.contains(".")) {
            return text.length() - text.indexOf('.') - 1;
        } else {
            return 0;
        }
    }
}

