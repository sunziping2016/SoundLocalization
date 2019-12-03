package io.szp.soundlocalization;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_IN_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_OUT_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final float SHORT_MAX = 32768;
    private static final String START_CONTENT_TEXT = "contentText";
    private static final String[] LOG_LEVEL_STRINGS = new String[] {
            "error", "warn", "info", "debug"
    };
    private enum LogLevel { ERROR, WARN, INFO, DEBUG };

    // UI Parameter
    private boolean receiverEnabled;
    private boolean senderEnabled;
    private AtomicInteger logLevel = new AtomicInteger();
    private AtomicBoolean drawTime = new AtomicBoolean();
    private AtomicBoolean drawReceiver1 = new AtomicBoolean();
    private AtomicBoolean drawReceiver2 = new AtomicBoolean();
    private AtomicBoolean drawTwoDimension = new AtomicBoolean();

    // Common Parameter
    private float cycleTime;
    private float startFreq1;
    private float endFreq1;
    private float startFreq2;
    private float endFreq2;
    
    // Receiver Parameter
    private boolean twoDimensionEnabled;
    private float startIntensityThreshold;
    private float startIndexStdLimit;
    private float endIntensityThreshold;
    private float endIndexStdLimit;
    private int bufferLength;
    private int fftLength;

    // Sender Parameter
    private boolean useSecondSender;

    // Computed Parameter
    private int symbolLength;
    private float[] symbol1;
    private float[] symbol2;

    // UI Component
    private TextView contentText;
    private PlotView plotView;
    private ScrollView scrollView;

    // Other Fields
    private boolean permissionToRecordAccepted = false;

    private int receiverBufferSize;
    private int senderBufferSize;
    private AtomicBoolean receiverOn = new AtomicBoolean(false);
    private AtomicBoolean senderOn = new AtomicBoolean(false);
    private AudioRecord receiver;
    private AudioTrack sender;

    private Float recentReceiver1Position;
    private Float recentReceiver2Position;

    // Default value
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
        setContentView(R.layout.activity_main);
        // UI Component
        contentText = findViewById(R.id.contentText);
        if (savedInstanceState != null) {
            contentText.setText(savedInstanceState.getString(START_CONTENT_TEXT, ""));
        }
        plotView = findViewById(R.id.plotView);
        scrollView = findViewById(R.id.scrollView);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        // Settings
        Resources res = getResources();
        logLevel.set(findInStringArray(LOG_LEVEL_STRINGS, preferences.getString(
                getString(R.string.log_level_key), "error")));
        if (logLevel.get() == -1)
            throw new AssertionError("Unknown log level");
        drawTime.set(preferences.getBoolean(getString(R.string.draw_time_enabled_key),
                res.getBoolean(R.bool.draw_time_enabled_default)));
        drawReceiver1.set(preferences.getBoolean(getString(R.string.draw_receiver1_enabled_key),
                res.getBoolean(R.bool.draw_receiver1_enabled_default)));
        drawReceiver2.set(preferences.getBoolean(getString(R.string.draw_receiver2_enabled_key),
                res.getBoolean(R.bool.draw_receiver2_enabled_default)));
        drawTwoDimension.set(preferences.getBoolean(
                getString(R.string.draw_two_dimension_enabled_key),
                res.getBoolean(R.bool.draw_two_dimension_enabled_default)));
        cycleTime = Float.parseFloat(preferences.getString(getString(R.string.cycle_time_key),
                getString(R.string.cycle_time_default)));
        startFreq1 = Float.parseFloat(preferences.getString(getString(R.string.start_freq1_key),
                getString(R.string.start_freq1_default)));
        endFreq1 = Float.parseFloat(preferences.getString(getString(R.string.end_freq1_key),
                getString(R.string.end_freq1_default)));
        startFreq2 = Float.parseFloat(preferences.getString(getString(R.string.start_freq2_key),
                getString(R.string.start_freq2_default)));
        endFreq2 = Float.parseFloat(preferences.getString(getString(R.string.end_freq2_key),
                getString(R.string.end_freq2_default)));
        twoDimensionEnabled = preferences.getBoolean(getString(R.string.two_dimension_enabled_key),
                res.getBoolean(R.bool.two_dimension_enabled_default));
        startIntensityThreshold = Float.parseFloat(preferences.getString(
                getString(R.string.start_intensity_threshold_key),
                getString(R.string.start_intensity_threshold_default)));
        startIndexStdLimit = Float.parseFloat(preferences.getString(
                getString(R.string.start_index_std_limit_key),
                getString(R.string.start_index_std_limit_default)));
        endIntensityThreshold = Float.parseFloat(preferences.getString(
                getString(R.string.end_intensity_threshold_key),
                getString(R.string.end_intensity_threshold_default)));
        endIndexStdLimit = Float.parseFloat(preferences.getString(
                getString(R.string.end_index_std_limit_key),
                getString(R.string.end_index_std_limit_default)));
        bufferLength = Integer.parseInt(preferences.getString(
                getString(R.string.buffer_length_key),
                getString(R.string.buffer_length_default)));
        fftLength = Integer.parseInt(preferences.getString(
                getString(R.string.fft_length_key),
                getString(R.string.fft_length_default)));
        useSecondSender = preferences.getBoolean(getString(R.string.use_second_sender_key),
                res.getBoolean(R.bool.use_second_sender_default));
        updateParameter();
        updateBufferSize();
        setReceiverEnabled(preferences.getBoolean(getString(R.string.receiver_enabled_key),
                res.getBoolean(R.bool.receiver_enabled_default)));
        setSenderEnabled(preferences.getBoolean(getString(R.string.sender_enabled_key),
                res.getBoolean(R.bool.sender_enabled_default)));
    }

    @Override
    protected void onDestroy() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted)
            finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settingsButton) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.clearButton) {
            contentText.setText("");
            plotView.clearAllData();
            plotView.postInvalidate();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    public void log(LogLevel level, String text) {
        if (level.ordinal() <= logLevel.get()) {
            contentText.setText(contentText.getText().toString() + text + '\n');
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(START_CONTENT_TEXT, contentText.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        Resources res = getResources();
        if (key.equals(getString(R.string.receiver_enabled_key))) {
            setReceiverEnabled(preferences.getBoolean(key,
                    res.getBoolean(R.bool.receiver_enabled_default)));
        } else if (key.equals(getString(R.string.sender_enabled_key))) {
            setSenderEnabled(preferences.getBoolean(key,
                    res.getBoolean(R.bool.sender_enabled_default)));
        } else if (key.equals(getString(R.string.log_level_key))) {
            logLevel.set(findInStringArray(LOG_LEVEL_STRINGS, preferences.getString(key,
                    getString(R.string.log_level_default))));
            if (logLevel.get() == -1)
                throw new AssertionError("Unknown log level");
        } else if (key.equals(getString(R.string.draw_time_enabled_key))) {
            drawTime.set(preferences.getBoolean(key,
                    res.getBoolean(R.bool.draw_time_enabled_default)));
        } else if (key.equals(getString(R.string.draw_receiver1_enabled_key))) {
            drawReceiver1.set(preferences.getBoolean(key,
                    res.getBoolean(R.bool.draw_receiver1_enabled_default)));
        } else if (key.equals(getString(R.string.draw_receiver2_enabled_key))) {
            drawReceiver2.set(preferences.getBoolean(key,
                    res.getBoolean(R.bool.draw_receiver2_enabled_default)));
        } else if (key.equals(getString(R.string.draw_two_dimension_enabled_key))) {
            drawTwoDimension.set(preferences.getBoolean(key,
                    res.getBoolean(R.bool.draw_two_dimension_enabled_default)));
        } else if (key.equals(getString(R.string.cycle_time_key))) {
            boolean commitBack = false;
            try {
                cycleTime = Float.parseFloat(preferences.getString(key,
                        getString(R.string.cycle_time_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(cycleTime));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.start_freq1_key))) {
            boolean commitBack = false;
            try {
                startFreq1 = Float.parseFloat(preferences.getString(key,
                        getString(R.string.start_freq1_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(startFreq1));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.end_freq1_key))) {
            boolean commitBack = false;
            try {
                endFreq1 = Float.parseFloat(preferences.getString(key,
                        getString(R.string.end_freq1_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(endFreq1));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.start_freq2_key))) {
            boolean commitBack = false;
            try {
                startFreq2 = Float.parseFloat(preferences.getString(key,
                        getString(R.string.start_freq2_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(startFreq2));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.end_freq2_key))) {
            boolean commitBack = false;
            try {
                endFreq2 = Float.parseFloat(preferences.getString(key,
                        getString(R.string.end_freq2_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(endFreq2));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.two_dimension_enabled_key))) {
            twoDimensionEnabled = preferences.getBoolean(key, 
                    res.getBoolean(R.bool.two_dimension_enabled_default));
        } else if (key.equals(getString(R.string.start_intensity_threshold_key))) {
            boolean commitBack = false;
            try {
                startIntensityThreshold = Float.parseFloat(preferences.getString(key,
                        getString(R.string.start_intensity_threshold_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(startIntensityThreshold));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.start_index_std_limit_key))) {
            boolean commitBack = false;
            try {
                startIndexStdLimit = Float.parseFloat(preferences.getString(key,
                        getString(R.string.start_index_std_limit_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(startIndexStdLimit));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.end_intensity_threshold_key))) {
            boolean commitBack = false;
            try {
                endIntensityThreshold = Float.parseFloat(preferences.getString(key,
                        getString(R.string.end_intensity_threshold_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(endIntensityThreshold));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.end_index_std_limit_key))) {
            boolean commitBack = false;
            try {
                endIndexStdLimit = Float.parseFloat(preferences.getString(key,
                        getString(R.string.end_index_std_limit_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(endIndexStdLimit));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.buffer_length_key))) {
            boolean commitBack = false;
            try {
                int newBufferLength = Integer.parseInt(preferences.getString(key,
                        getString(R.string.buffer_length_default)));
                if (newBufferLength < 1) {
                    bufferLength = 1;
                    commitBack = true;
                } else {
                    bufferLength = newBufferLength;
                }
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(bufferLength));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.fft_length_key))) {
            boolean commitBack = false;
            try {
                fftLength = Integer.parseInt(preferences.getString(key,
                        getString(R.string.fft_length_default)));
            } catch (NumberFormatException e) {
                commitBack = true;
            }
            if (commitBack) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, String.valueOf(fftLength));
                editor.apply();
            }
        } else if (key.equals(getString(R.string.use_second_sender_key))) {
            useSecondSender = preferences.getBoolean(key,
                    res.getBoolean(R.bool.use_second_sender_default));
        }
        updateParameter();
    }

    private synchronized void setRecentReceiver1Position(float position) {
        recentReceiver1Position = position;
        if (recentReceiver2Position != null)
            updateTwoDimensionPosition();
    }

    private synchronized void setRecentReceiver2Position(float position) {
        recentReceiver2Position = position;
        if (recentReceiver1Position != null)
            updateTwoDimensionPosition();
    }

    private void updateTwoDimensionPosition() {
        if (drawTwoDimension.get()) {
            float p1 = recentReceiver1Position, p2 = recentReceiver2Position;
            float d = 0.5f;
            float x = (p2 * p2 - p1 * p1) / (4 * d);
            float base = -(float) Math.pow(p1, 4) - (float) Math.pow(p2, 4)
                    + 8 * d * d * (p1 * p1 + p2 * p2) + 2 * (p1 * p1 * p2 * p2)
                    - 16 * (float) Math.pow(d, 4);
            float y = 0;
            if (base > 0)
                y = (float) Math.sqrt(base) / (4 * d);
            plotView.addPositionData(x, y);
            plotView.postInvalidate();
        }
        recentReceiver1Position = null;
        recentReceiver2Position = null;
    }

    protected void setReceiverEnabled(boolean enabled) {
        if (receiverEnabled == enabled)
            return;
        receiverEnabled = enabled;
        if (enabled) {
            receiver = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                    CHANNEL_IN_CONFIG, AUDIO_IN_FORMAT, receiverBufferSize);
            receiver.startRecording();
            receiverOn.set(true);
            ReceiverProcessor[] processors = new ReceiverProcessor[twoDimensionEnabled ? 2 : 1];
            processors[0] = new ReceiverProcessor(1, symbol1,
                    new ReceiverProcessorResultHandler() {
                        @Override
                        public int handle(int result, int length) {
                            final float position = (float) result * SAMPLING_RATE_IN_HZ / length *
                                    340 * cycleTime / Math.abs(endFreq1 - startFreq1);
                            runOnUiThread(new Runnable() {
                                @SuppressLint("DefaultLocale")
                                @Override
                                public void run() {
                                    MainActivity.this.log(LogLevel.DEBUG,
                                            String.format("D: Position@1: %f", position));
                                }
                            });
                            plotView.addReceiver1Data(position);
                            plotView.postInvalidate();
                            setRecentReceiver1Position(position);
                            return Math.round(position / 340 * SAMPLING_RATE_IN_HZ);
                        }
                    });
            if (twoDimensionEnabled) {
                processors[1] = new ReceiverProcessor(2, symbol2,
                        new ReceiverProcessorResultHandler() {
                            @Override
                            public int handle(int result, int length) {
                                final float position = (float) result * SAMPLING_RATE_IN_HZ /
                                        length * 340 * cycleTime / Math.abs(endFreq2 - startFreq2);
                                runOnUiThread(new Runnable() {
                                    @SuppressLint("DefaultLocale")
                                    @Override
                                    public void run() {
                                        MainActivity.this.log(LogLevel.DEBUG,
                                                String.format("D: Position@2: %f", position));
                                    }
                                });
                                plotView.addReceiver2Data(position);
                                plotView.postInvalidate();
                                setRecentReceiver2Position(position);
                                return Math.round(position / 340 * SAMPLING_RATE_IN_HZ);
                            }
                        });
            }
            Thread receiverThread = new Thread(new ReceiverRunnable(processors), "Receiver Thread");
            receiverThread.start();
        } else {
            receiverOn.set(false);
            receiver.stop();
            receiver.release();
            receiver = null;
        }
    }

    private interface ReceiverProcessorResultHandler {
        int handle(int result, int length);
    }

    private class ReceiverProcessor {
        private float[] prevWindow;
        private Deque<Float> intensitiesBuffer = new ArrayDeque<>();
        private Deque<Integer> indicesBuffer = new ArrayDeque<>();
        private boolean started = false;
        private int paddingOffset = 65;
        private float[] signalBuffer = new float[0];

        private int receiverIndex;
        private float[] symbol;
        private ReceiverProcessorResultHandler handler;

        public ReceiverProcessor(int receiverIndex, float[] symbol,
                          ReceiverProcessorResultHandler handler) {
            this.receiverIndex = receiverIndex;
            this.symbol = symbol;
            this.handler = handler;
        }

        private void logOnUiThread(final LogLevel level, final String content) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.log(level, content);
                }
            });
        }

        @SuppressLint("DefaultLocale")
        private void processWindow(float[] curWindow) {
            if (prevWindow == null) {
                prevWindow = curWindow;
                return;
            }
            float[] window = new float[symbolLength * 2];
            System.arraycopy(prevWindow, 0, window, 0, symbolLength);
            System.arraycopy(curWindow, 0, window, symbolLength, symbolLength);
            float[] cor = SignalProcessing.xcorr(window, symbol);
            float[] clippedCor = new float[symbolLength];
            System.arraycopy(cor, cor.length / 2, clippedCor, 0, symbolLength);
            Box<Float> maxCor = new Box<>();
            Box<Integer> index = new Box<>();
            MathUtils.max(clippedCor, maxCor, index);
            float intensity = maxCor.value / MathUtils.meanAbs(clippedCor);
            intensitiesBuffer.add(intensity);
            indicesBuffer.add(index.value);
            if (intensitiesBuffer.size() > bufferLength) {
                intensitiesBuffer.pop();
            }
            if (indicesBuffer.size() > bufferLength) {
                indicesBuffer.pop();
            }
            if (intensitiesBuffer.size() == bufferLength &&
                    indicesBuffer.size() == bufferLength) {
                float meanIntensitiesBuffer = MathUtils.mean(intensitiesBuffer);
                float meanIndicesBuffer = MathUtils.mean(indicesBuffer);
                float stdIndicesBuffer = MathUtils.std(indicesBuffer, meanIndicesBuffer);
                boolean skipSignalBuffer = false;
                if (!started) {
                    if (meanIntensitiesBuffer > startIntensityThreshold &&
                            stdIndicesBuffer <= startIndexStdLimit) {
                        started = true;
                        logOnUiThread(LogLevel.INFO,
                                String.format("I: Receiver@%d started: int: %f, dev %f",
                                        receiverIndex, meanIntensitiesBuffer, stdIndicesBuffer));
                        int indexOffset = Math.round(meanIndicesBuffer);
                        indexOffset -= paddingOffset;
                        if (indexOffset < 0)
                            indexOffset += symbolLength;
                        signalBuffer = Arrays.copyOfRange(window, indexOffset, symbolLength);
                        skipSignalBuffer = true;
                    }
                } else {
                    if (meanIntensitiesBuffer <= endIntensityThreshold ||
                            stdIndicesBuffer > endIndexStdLimit) {
                        started = false;
                        logOnUiThread(LogLevel.INFO,
                                String.format("I: Receiver@%d stopped: int: %f, dev %f",
                                        receiverIndex, meanIndicesBuffer, stdIndicesBuffer));
                        signalBuffer = new float[0];
                    }
                }
                if (started && !skipSignalBuffer) {
                    float[] newSignalBuffer = new float[signalBuffer.length + symbolLength];
                    System.arraycopy(signalBuffer, 0, newSignalBuffer, 0, signalBuffer.length);
                    System.arraycopy(window, 0, newSignalBuffer, signalBuffer.length, symbolLength);
                    signalBuffer = newSignalBuffer;
                    while (signalBuffer.length > symbolLength) {
                        float[] signal = Arrays.copyOfRange(signalBuffer, 0, symbolLength);
                        signalBuffer = Arrays.copyOfRange(signalBuffer, symbolLength,
                                signalBuffer.length);
                        int length = Math.max(symbolLength, fftLength);
                        float[] realFFFIn = new float[length];
                        for (int i = 0; i < symbolLength; ++i)
                            realFFFIn[i] = signal[i] * symbol[i];
                        float[] imagFFTIn = new float[length];
                        FFT.fft(realFFFIn, imagFFTIn);
                        float[] absFFTOut = new float[length];
                        for (int i = 0; i < length; ++i)
                            absFFTOut[i] = (float) Math.sqrt(realFFFIn[i] * realFFFIn[i] +
                                    imagFFTIn[i] * imagFFTIn[i]);
                        int end = length / 10;
                        float max = Float.NEGATIVE_INFINITY;
                        int maxIndex = 0;
                        for (int i = 0; i < end; ++i) {
                            if (absFFTOut[i] > max) {
                                max = absFFTOut[i];
                                maxIndex = i;
                            }
                        }
                        paddingOffset = handler.handle(maxIndex, length);
                    }
                }
            }
            prevWindow = curWindow;
            if (drawTime.get()) {
                plotView.setTimeData(window);
                plotView.postInvalidate();
            }
        }
    }

    private class ReceiverRunnable implements Runnable {
        private ReceiverProcessor[] processors;

        private ReceiverRunnable(ReceiverProcessor[] processors) {
            this.processors = processors;
        }

        private void logOnUiThread(final LogLevel level, final String content) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.log(level, content);
                }
            });
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            short[] prevBuffer = new short[0], curBuffer = new short[receiverBufferSize];
            while (receiverOn.get()) {
                int read = 0;
                while (receiverOn.get() && read != receiverBufferSize) {
                    int result = receiver.read(curBuffer, read, receiverBufferSize);
                    if (result < 0)
                        throw new RuntimeException("Error when reading audio");
                    read += result;
                }
                if (!receiverOn.get())
                    break;
                short[] totalBuffer = new short[prevBuffer.length + curBuffer.length];
                System.arraycopy(prevBuffer, 0, totalBuffer, 0, prevBuffer.length);
                System.arraycopy(curBuffer, 0, totalBuffer, prevBuffer.length, curBuffer.length);
                int bufferOffset = 0;
                // logOnUiThread(LogLevel.INFO,
                //         String.format("processing: %d", new Date().getTime()));
                while (bufferOffset + symbolLength < totalBuffer.length) {
                    float[] window = new float[symbolLength];
                    for (int i = 0; i < symbolLength; ++i)
                        window[i] = totalBuffer[bufferOffset + i] / SHORT_MAX;
                    for (ReceiverProcessor processor: processors)
                        processor.processWindow(window);
                    bufferOffset += symbolLength;
                }
                // logOnUiThread(LogLevel.INFO,
                //         String.format("processed: %d", new Date().getTime()));
                prevBuffer = Arrays.copyOfRange(totalBuffer, bufferOffset, totalBuffer.length);
            }
        }
    }

    protected void setSenderEnabled(boolean enabled) {
        if (senderEnabled == enabled)
            return;
        senderEnabled = enabled;
        if (enabled) {
            sender =  new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE_IN_HZ,
                    CHANNEL_OUT_CONFIG, AUDIO_OUT_FORMAT, senderBufferSize, AudioTrack.MODE_STREAM);
            sender.play();
            senderOn.set(true);
            Thread senderThread = new Thread(new SenderRunnable(), "Sender Thread");
            senderThread.start();
        } else {
            senderOn.set(false);
            sender.stop();
            sender.release();
            sender = null;
        }
    }

    private class SenderRunnable implements Runnable {
        @Override
        public void run() {
            short[] data = new short[0];
            float[] symbol = useSecondSender ? symbol2 : symbol1;
            while (senderOn.get()) {
                if (data.length < symbolLength) {
                    short[] newData = new short[data.length + symbolLength];
                    System.arraycopy(data, 0, newData, 0, data.length);
                    for (int i = 0; i < symbolLength; ++i)
                        newData[data.length + i] = (short) Math.round(symbol[i] * 0.8 * SHORT_MAX);
                    data = newData;
                }
                int result = sender.write(data, 0, data.length);
                if (result < 0)
                    throw new RuntimeException("Error when writing audio");
                data = Arrays.copyOfRange(data, result, data.length);
            }
        }
    }

    protected void updateParameter() {
        symbolLength = Math.round(cycleTime * SAMPLING_RATE_IN_HZ);
        float[] symbolTime = new float[symbolLength];
        float sampleTime = 1.0f / SAMPLING_RATE_IN_HZ;
        for (int i = 0; i < symbolLength; ++i)
            symbolTime[i] = i * sampleTime;
        symbol1 = SignalProcessing.chirp(startFreq1, endFreq1, symbolTime);
        symbol2 = SignalProcessing.chirp(startFreq2, endFreq2, symbolTime);
        if (!drawTime.get())
            plotView.setTimeData(null);
        plotView.setReceiver1Enabled(drawReceiver1.get());
        plotView.setReceiver2Enabled(drawReceiver2.get());
    }

    protected void updateBufferSize() {
        receiverBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
                CHANNEL_IN_CONFIG, AUDIO_IN_FORMAT);
        senderBufferSize = AudioTrack.getMinBufferSize(SAMPLING_RATE_IN_HZ,
                CHANNEL_OUT_CONFIG, AUDIO_IN_FORMAT);
    }

    @SuppressWarnings("SameParameterValue")
    private static int findInStringArray(String[] array, String value) {
        for (int i = 0; i < array.length; ++i)
            if (array[i].equals(value))
                return i;
        return -1;
    }
}
