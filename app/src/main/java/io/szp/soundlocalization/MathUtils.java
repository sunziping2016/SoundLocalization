package io.szp.soundlocalization;

import java.util.Deque;

public class MathUtils {
    public static void max(float[] input, Box<Float> max, Box<Integer> index) {
        float floatMax = Float.NEGATIVE_INFINITY;
        int intIndex = 0;
        for (int i = 0; i < input.length; ++i) {
            if (input[i] > floatMax) {
                floatMax = input[i];
                intIndex = i;
            }
        }
        max.value = floatMax;
        index.value = intIndex;
    }

    public static <T extends Number> float mean(Deque<T> input) {
        float sum = 0;
        for (T i: input) {
            sum += i.floatValue();
        }
        return sum / input.size();
    }

    public static float meanAbs(float[] input) {
        float sum = 0;
        for (float i: input)
            sum += Math.abs(i);
        return sum / input.length;
    }

    public static <T extends Number>  float std(Deque<T> input, float mean) {
        float sum = 0;
        for (T i: input) {
            float diff = i.floatValue() - mean;
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum / input.size());
    }
}
