package de.vill.util;

public class CountingResult {
    public final Double TIME_TO_COMPUTE;
    public final String MODEL_COUNT;

    public CountingResult(Double time, String result) {
        this.TIME_TO_COMPUTE = time;
        this.MODEL_COUNT = result;
    }
}
