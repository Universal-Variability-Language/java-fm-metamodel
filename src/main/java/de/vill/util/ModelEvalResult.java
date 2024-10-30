package de.vill.util;

public class ModelEvalResult {
    public final String MODEL_NAME;
    public final String TIME_TO_COMPUTE_D4;
    public final String TIME_TO_COMPUTE_P2D;
    public final String MODEL_COUNT_D4;
    public final String MODEL_COUNT_P2D;
    public final boolean SAME_RESULT;
    public final long TO_DIMACS_TIME;
    public final long TO_OPB_TIME;

    public ModelEvalResult(String modelName, String timeToComputeD4, String timeToComputeP2D, String modelCountD4, String modelCountP2D, boolean sameResult, long toDimacsTime, long toOpbTime) {
        MODEL_NAME = modelName;
        TIME_TO_COMPUTE_D4 = timeToComputeD4;
        TIME_TO_COMPUTE_P2D = timeToComputeP2D;
        MODEL_COUNT_D4 = modelCountD4;
        MODEL_COUNT_P2D = modelCountP2D;
        SAME_RESULT = sameResult;
        TO_DIMACS_TIME = toDimacsTime;
        TO_OPB_TIME = toOpbTime;
    }

    public String toCSVString() {
        return MODEL_NAME + ";" + TIME_TO_COMPUTE_D4 + ";" + TIME_TO_COMPUTE_P2D + ";" + TO_DIMACS_TIME + ";" + TO_OPB_TIME + ";" + SAME_RESULT + ";" + Double.valueOf(TIME_TO_COMPUTE_P2D) / Double.valueOf(TIME_TO_COMPUTE_D4) + ";" + Double.valueOf(TO_OPB_TIME) / Double.valueOf(TO_DIMACS_TIME) + ";" + (Double.valueOf(TIME_TO_COMPUTE_P2D) + Double.valueOf(TO_OPB_TIME)) / (Double.valueOf(TIME_TO_COMPUTE_D4) + Double.valueOf(TO_DIMACS_TIME));
    }
}
