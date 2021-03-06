package io.szp.soundlocalization;

public class SignalProcessing {
    private static final int[] FACTORS = new int[] { 2, 3, 5, 7 };

    public static float[] chirp(float f0, float f1, float[] t) {
        return chirp(f0, f1, t, 0);
    }

    public static float[] chirp(float f0, float f1, float[] t, float angle) {
        float beta = (f1 - f0) / t[t.length - 1];
        float[] value = new float[t.length];
        for (int i = 0; i < t.length; ++i) {
            value[i] = (float) Math.cos(2 * Math.PI *
                    (beta / 2 * (t[i] * t[i]) + f0 * t[i]) + angle);
        }
        return value;
    }

    public static float[] xcorr(float[] x, float[] y) {
        int m = Math.max(x.length, y.length), mxl = m - 1, m2 = 2 * m;
        while (true) {
            int r = m2;
            for (int p: FACTORS) {
                while (r > 1 && r % p == 0)
                    r /= p;
            }
            if (r == 1)
                break;
            ++m2;
        }
        float[] xReal = new float[m2], yReal = new float[m2];
        float[] xImag = new float[m2], yImag = new float[m2];
        System.arraycopy(x, 0, xReal, 0, x.length);
        System.arraycopy(y, 0, yReal, 0, y.length);
        FFT.fft(xReal, xImag);
        FFT.fft(yReal, yImag);
        for (int i = 0; i < m2; ++i) {
            float temp = xImag[i] * yReal[i] - xReal[i] * yImag[i];
            xReal[i] = xReal[i] * yReal[i] + xImag[i] * yImag[i];
            xImag[i] = temp;
        }
        FFT.ifft(xReal, xImag);
        int length = 2 * mxl + 1;
        float[] cor = new float[length];
        System.arraycopy(xReal, m2 - mxl, cor, 0, mxl);
        System.arraycopy(xReal, 0, cor, mxl, mxl + 1);
        return cor;
    }
}
