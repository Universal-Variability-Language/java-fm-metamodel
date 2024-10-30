package de.vill.model.pbc;

import java.util.List;

public class PBConstraint {
    public List<Literal> literalList;
    public double k;
    public PBConstraintType type;

    public PBConstraint() {
        type = PBConstraintType.GEQ;
    }

    public void toOPBString(OPBResult result) {
        result.numberConstraints++;
        int maxDecimalPlaces = getMaxDecimalPlaces();
        for(Literal l : literalList){
            if(l.factor < 0){
                result.opbString.append(" ");
                result.opbString.append((long) (l.factor * Math.pow(10,maxDecimalPlaces)));
            }else{
                result.opbString.append(" +");
                result.opbString.append((long) (l.factor * Math.pow(10,maxDecimalPlaces)));
            }
            result.opbString.append(" ");
            result.opbString.append(l.name);
        }
        result.opbString.append(" ");
        result.opbString.append(type);
        result.opbString.append(" ");
        result.opbString.append((long) (k * Math.pow(10,maxDecimalPlaces)));
        result.opbString.append(";\n");
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

