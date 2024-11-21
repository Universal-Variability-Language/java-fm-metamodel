package de.vill.model.pbc;

import java.util.LinkedList;
import java.util.List;

public class PBConstraint implements Cloneable{
    public List<Literal> literalList;
    public double k;
    public PBConstraintType type;

    public PBConstraint() {
        type = PBConstraintType.GEQ;
    }

    public PBConstraint negatedConstraint() {
        PBConstraint newConstraint = (PBConstraint)clone();
        switch (type) {
            case GEQ:
                newConstraint.type = PBConstraintType.LE;
                break;
            case GE:
                newConstraint.type = PBConstraintType.LEQ;
                break;
            case LEQ:
                newConstraint.type = PBConstraintType.GE;
                break;
            case LE:
                newConstraint.type = PBConstraintType.GEQ;
                break;
            case EQ:
                newConstraint.type = PBConstraintType.NOTEQ;
                break;
            case NOTEQ:
                newConstraint.type = PBConstraintType.EQ;
                break;
        }
        return newConstraint;
    }

    public List<PBConstraint> orWithLiteral(String literalName, boolean sign) {
        PBConstraint newConstraint = (PBConstraint)clone();
        double f = this.k;
        switch (type) {
            case GEQ:
                for (Literal l : this.literalList){
                    f += Math.abs(l.factor);
                }
                break;
            case GE:
                for (Literal l : this.literalList){
                    f += Math.abs(l.factor);
                }
                f++;
                break;
            case LEQ:
                for (Literal l : this.literalList){
                    f -= Math.abs(l.factor);
                }
                break;
            case LE:
                for (Literal l : this.literalList){
                    f -= Math.abs(l.factor);
                }
                f--;
                break;
            case EQ:
                PBConstraint c1 = clone();
                PBConstraint c2 = clone();
                c1.type = PBConstraintType.LEQ;
                c2.type = PBConstraintType.GEQ;
                List<PBConstraint> resultList = new LinkedList<>();
                resultList.addAll(c1.orWithLiteral(literalName, sign));
                resultList.addAll(c2.orWithLiteral(literalName, sign));
                return resultList;
            case NOTEQ:
                for (Literal l : this.literalList){
                    f += Math.abs(l.factor);
                }
                f++;
                break;
        }
        if (sign) {
            newConstraint.literalList.add(new Literal(f, literalName, true));
        } else {
            newConstraint.k -= f;
            newConstraint.literalList.add(new Literal(-f, literalName, true));
        }

        List<PBConstraint> resultList = new LinkedList<>();
        resultList.add(newConstraint);
        return resultList;
    }

    public void toOPBString(OPBResult result) {
        for (Literal l : literalList) {
            if (!l.sign){
                k -= l.factor;
                l.factor *= -1;
                l.sign = !l.sign;
            }
        }
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
            result.opbString.append("_" + l.name);
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

    @Override
    public PBConstraint clone() {
        PBConstraint pbConstraint = null;
        try {
            pbConstraint = (PBConstraint) super.clone();
        } catch (CloneNotSupportedException e) {
            pbConstraint = new PBConstraint();
            pbConstraint.type = this.type;
            pbConstraint.k = this.k;
        }
        pbConstraint.literalList = new LinkedList<>();
        for (Literal l : this.literalList) {
            pbConstraint.literalList.add(l.clone());
        }

        return pbConstraint;
    }
}

