package com.miniflo.femcare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.graphics.PathParser;

public class HexagonProgressBar extends View {

    private Paint progressPaint;
    private Paint backgroundPaint;
    private Path originalPath;
    private Path scaledPath;
    private Path progressPath;
    private PathMeasure pathMeasure;
    private float progressPercentage = 0f;

    // THIS IS YOUR EXACT FIGMA DNA!
    private static final String SVG_PATH_DATA = "M223.276 32.1196L149.277 2.18883C141.562 -0.931798 132.897 -0.707241 125.354 2.80883L43.2498 41.0794C35.6506 44.6215 29.8804 51.182 27.3374 59.1712L1.41353 140.614C-0.937138 147.999 -0.338701 156.007 3.08347 162.961L36.5474 230.956C39.7841 237.533 45.3166 242.697 52.1004 245.473L125.143 275.369C132.823 278.512 141.462 278.334 149.006 274.877L231.62 237.015C239.257 233.515 245.076 226.973 247.663 218.981L274.017 137.566C276.412 130.17 275.841 122.132 272.426 115.148L238.977 46.7512C235.727 40.1048 230.135 34.8938 223.276 32.1196Z";

    public HexagonProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#F5F5F5")); // Light gray background track
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(8f * density);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(10f * density); // Slightly thicker for emphasis
        progressPaint.setAntiAlias(true);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        scaledPath = new Path();
        progressPath = new Path();
        pathMeasure = new PathMeasure();

        originalPath = PathParser.createPathFromPathData(SVG_PATH_DATA);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        RectF bounds = new RectF();
        originalPath.computeBounds(bounds, true);

        float density = getResources().getDisplayMetrics().density;
        float paddingPx = 20f * density;

        float scaleX = (w - (paddingPx * 2)) / bounds.width();
        float scaleY = (h - (paddingPx * 2)) / bounds.height();
        float scale = Math.min(scaleX, scaleY);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-bounds.left, -bounds.top);
        matrix.postScale(scale, scale);

        float scaledWidth = bounds.width() * scale;
        float scaledHeight = bounds.height() * scale;
        float dx = (w - scaledWidth) / 2f;
        float dy = (h - scaledHeight) / 2f;
        matrix.postTranslate(dx, dy);

        originalPath.transform(matrix, scaledPath);
        pathMeasure.setPath(scaledPath, false);

        // Apply Gradient based on actual view size
        Shader gradient = new LinearGradient(0, 0, w, h,
                new int[]{Color.parseColor("#FF4081"), Color.parseColor("#C2185B"), Color.parseColor("#880E4F")},
                null, Shader.TileMode.CLAMP);
        progressPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw the background track (Full hexagon)
        canvas.drawPath(scaledPath, backgroundPaint);

        // 2. Draw the progress segment
        progressPath.reset();
        float pathLength = pathMeasure.getLength();
        pathMeasure.getSegment(0, pathLength * progressPercentage, progressPath, true);

        canvas.drawPath(progressPath, progressPaint);
    }

    public void setProgress(float percentage) {
        this.progressPercentage = Math.max(0f, Math.min(percentage, 1f));
        invalidate();
    }
}
