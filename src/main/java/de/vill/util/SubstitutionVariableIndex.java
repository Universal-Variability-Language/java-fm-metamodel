package de.vill.util;

public class SubstitutionVariableIndex {
    private int index;
    private static SubstitutionVariableIndex objectRef;
    private SubstitutionVariableIndex() {
        index = 0;
    }

    public static SubstitutionVariableIndex getInstance() {
        if (objectRef == null) {
            objectRef = new SubstitutionVariableIndex();
        }
        return objectRef;
    }

    public String getSubName() {
        index++;
        return "x_" + index;
    }

    public int getIndex() {
        index++;
        return index;
    }

    public int peekIndex(){
        return index;
    }
}
