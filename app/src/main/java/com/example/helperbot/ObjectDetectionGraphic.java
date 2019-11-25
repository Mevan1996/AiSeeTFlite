package com.example.helperbot;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

public class ObjectDetectionGraphic extends GraphicOverlay.Graphic {
    private static final int TEXT_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;

    private final RectF rectangle;

    ObjectDetectionGraphic(GraphicOverlay overlay, RectF rectangle) {
        super(overlay);

        this.rectangle = rectangle;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

    }

    /**
     * Draws the barcode block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (rectangle == null) {
            throw new IllegalStateException("Attempting to draw a null barcode.");
        }

        // Draws the bounding box around the BarcodeBlock.

        rectangle.left = translateX(rectangle.left);
        rectangle.top = translateY(rectangle.top);
        rectangle.right = translateX(rectangle.right);
        rectangle.bottom = translateY(rectangle.bottom);
        canvas.drawRect(rectangle, rectPaint);

        // Renders the barcode at the bottom of the box.
        //canvas.drawText(barcode.getRawValue(), rect.left, rect.bottom, barcodePaint);
    }
}
