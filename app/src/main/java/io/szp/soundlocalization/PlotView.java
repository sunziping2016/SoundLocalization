package io.szp.soundlocalization;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PlotView extends View {
    private static final float TIME_SCALE = 2000;
    private static final float POSITION_SCALE = 400;
    private static final int POSITION_NUM = 80;
    private static final float TWO_DIMENSION_SCALE = 800;
    private static final float TWO_DIMENSION_NUM = 3000;

    private int width, height;

    private Paint baseLinePaint, timePaint;
    private Paint receiver1Paint, receiver2Paint;
    private Paint positionPaint;

    private float[] timeData;
    boolean receiver1Enabled, receiver2Enabled;
    private List<Float> receiver1Data = new LinkedList<>();
    private List<Float> receiver2Data = new LinkedList<>();
    private List<Float> positionXData = new LinkedList<>();
    private List<Float> positionYData = new LinkedList<>();

    public PlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        baseLinePaint = new Paint();
        baseLinePaint.setAntiAlias(true);
        baseLinePaint.setStyle(Paint.Style.STROKE);
        baseLinePaint.setColor(Color.GRAY);
        baseLinePaint.setStrokeWidth(2);
        timePaint = new Paint();
        timePaint.setAntiAlias(true);
        timePaint.setStyle(Paint.Style.STROKE);
        timePaint.setColor(Color.RED);
        timePaint.setStrokeWidth(2);
        receiver1Paint = new Paint();
        receiver1Paint.setAntiAlias(true);
        receiver1Paint.setStyle(Paint.Style.STROKE);
        receiver1Paint.setColor(Color.BLUE);
        receiver1Paint.setStrokeWidth(2);
        receiver2Paint = new Paint();
        receiver2Paint.setAntiAlias(true);
        receiver2Paint.setStyle(Paint.Style.STROKE);
        receiver2Paint.setColor(Color.GREEN);
        receiver2Paint.setStrokeWidth(2);
        positionPaint = new Paint();
        positionPaint.setAntiAlias(true);
        positionPaint.setStyle(Paint.Style.STROKE);
        positionPaint.setColor(Color.CYAN);
        positionPaint.setStrokeWidth(2);
        for (int i = 0; i < POSITION_NUM; ++i) {
            receiver1Data.add(0.0f);
            receiver2Data.add(0.0f);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        height = MeasureSpec.getSize(heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    synchronized protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float baseHeight = (float) height / 2;
        float baseWidth = (float) width / 2;
        if (timeData != null || receiver1Enabled || receiver2Enabled)
            canvas.drawLine(0, baseHeight, width, baseHeight, baseLinePaint);
        if (timeData != null && timeData.length != 0) {
            float stepWidth = (float) width / timeData.length;
            for (int i = 0; i < timeData.length - 1; ++i) {
                canvas.drawLine(stepWidth * i, baseHeight - TIME_SCALE * timeData[i],
                        stepWidth * (i + 1), baseHeight - TIME_SCALE * timeData[i + 1],
                        timePaint);
            }
        }
        if (receiver1Enabled) {
            float stepWidth = (float) width / receiver1Data.size();
            for (int i = 0; i < receiver1Data.size() - 1; ++i)
                canvas.drawLine(stepWidth * i,
                        baseHeight - POSITION_SCALE * receiver1Data.get(i),
                        stepWidth * (i + 1),
                        baseHeight - POSITION_SCALE * receiver1Data.get(i + 1),
                        receiver1Paint);
        }
        if (receiver2Enabled) {
            float stepWidth = (float) width / receiver2Data.size();
            for (int i = 0; i < receiver2Data.size() - 1; ++i)
                canvas.drawLine(stepWidth * i,
                        baseHeight - POSITION_SCALE * receiver2Data.get(i),
                        stepWidth * (i + 1),
                        baseHeight - POSITION_SCALE * receiver2Data.get(i + 1),
                        receiver2Paint);
        }
        Iterator<Float> xIterator = positionXData.iterator();
        Iterator<Float> yIterator = positionYData.iterator();
        if (xIterator.hasNext()) {
            float prevX = xIterator.next();
            float prevY = yIterator.next();
            while (xIterator.hasNext()) {
                float currX = xIterator.next();
                float currY = yIterator.next();
                canvas.drawLine(
                        baseWidth + TWO_DIMENSION_SCALE * prevX,
                        baseHeight + TWO_DIMENSION_SCALE * prevY,
                        baseWidth + TWO_DIMENSION_SCALE * currX,
                        baseHeight + TWO_DIMENSION_SCALE * currY,
                        positionPaint
                );
                prevX = currX;
                prevY = currY;
            }
        }
    }

    public synchronized void setTimeData(float[] data) {
        timeData = data;
    }

    public synchronized void setReceiver1Enabled(boolean enabled) {
        receiver1Enabled = enabled;
    }

    public synchronized void setReceiver2Enabled(boolean enabled) {
        receiver2Enabled = enabled;
    }

    public synchronized void addReceiver1Data(float data) {
        receiver1Data.remove(0);
        receiver1Data.add(data);
    }

    public synchronized void addReceiver2Data(float data) {
        receiver2Data.remove(0);
        receiver2Data.add(data);
    }

    public synchronized void clearAllData() {
        timeData = null;
        receiver1Data = new LinkedList<>();
        receiver2Data = new LinkedList<>();
        for (int i = 0; i < POSITION_NUM; ++i) {
            receiver1Data.add(0.0f);
            receiver2Data.add(0.0f);
        }
        positionXData = new LinkedList<>();
        positionYData = new LinkedList<>();
    }

    public synchronized void addPositionData(float x, float y) {
        positionXData.add(x);
        positionYData.add(y);
        if (positionXData.size() > TWO_DIMENSION_NUM) {
            positionXData.remove(0);
            positionYData.remove(0);
        }
    }
}
