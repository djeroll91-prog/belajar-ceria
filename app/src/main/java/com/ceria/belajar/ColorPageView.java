package com.ceria.belajar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple colouring page. A bold outline picture is drawn on canvas and the
 * child fills each region by tapping it. Perfect for teaching colours without
 * needing any drawing skill.
 */
public class ColorPageView extends View {

    public interface OnColorListener {
        void onRegionColored(String colourNameEn, String colourNameMs, int fill);
    }

    private static class Region {
        final Path path = new Path();
        final RectF bounds = new RectF();
        int fill = Color.WHITE;
        String en;
        String ms;
    }

    private final List<Region> regions = new ArrayList<>();
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int activeColor = 0xFFFF5252;
    private OnColorListener listener;
    private int picture;

    public static final int PICTURE_HOUSE = 0;
    public static final int PICTURE_FLOWER = 1;
    public static final int PICTURE_FISH = 2;
    private static final int PICTURE_COUNT = 3;

    public ColorPageView(Context context) {
        this(context, null);
    }

    public ColorPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(Util.dp(context, 4));
        outline.setColor(0xFF2A1B54);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setStrokeCap(Paint.Cap.ROUND);
        build(PICTURE_HOUSE);
    }

    public void setOnColorListener(OnColorListener l) {
        this.listener = l;
    }

    public void setActiveColor(int color) {
        activeColor = color;
    }

    public int nextPicture() {
        picture = (picture + 1) % PICTURE_COUNT;
        build(picture);
        invalidate();
        return picture;
    }

    public void clearAll() {
        for (Region r : regions) {
            r.fill = Color.WHITE;
        }
        invalidate();
    }

    public String pictureName() {
        switch (picture) {
            case PICTURE_FLOWER:
                return "Flower";
            case PICTURE_FISH:
                return "Fish";
            default:
                return "House";
        }
    }

    // ------------------------------------------------------------------
    // Scene definition
    // ------------------------------------------------------------------

    private void build(int which) {
        regions.clear();
        switch (which) {
            case PICTURE_FLOWER:
                addOvalRect(0.30f, 0.10f, 0.70f, 0.50f, "petal", "kelopak");   // flower head
                addCircle(0.50f, 0.30f, 0.11f, "centre", "pusat");
                addRect(0.46f, 0.48f, 0.54f, 0.86f, "stem", "batang");
                addOvalRect(0.20f, 0.60f, 0.46f, 0.74f, "leaf", "daun");
                addOvalRect(0.54f, 0.66f, 0.80f, 0.80f, "leaf", "daun");
                addRect(0.30f, 0.86f, 0.70f, 0.96f, "pot", "pasu");
                break;
            case PICTURE_FISH:
                addOvalRect(0.18f, 0.34f, 0.74f, 0.70f, "body", "badan");
                addTriangle(0.74f, 0.34f, 0.94f, 0.52f, 0.74f, 0.70f, "tail", "ekor");
                addCircle(0.34f, 0.48f, 0.05f, "eye", "mata");
                addTriangle(0.44f, 0.34f, 0.56f, 0.20f, 0.58f, 0.36f, "fin", "sirip");
                break;
            default:
                addRect(0.20f, 0.44f, 0.80f, 0.88f, "wall", "dinding");
                addTriangle(0.14f, 0.44f, 0.50f, 0.16f, 0.86f, 0.44f, "roof", "bumbung");
                addRect(0.42f, 0.62f, 0.58f, 0.88f, "door", "pintu");
                addRect(0.26f, 0.52f, 0.38f, 0.62f, "window", "tingkap");
                addRect(0.62f, 0.52f, 0.74f, 0.62f, "window", "tingkap");
                addCircle(0.68f, 0.20f, 0.07f, "sun", "matahari");
                break;
        }
    }

    private Region last() {
        return regions.get(regions.size() - 1);
    }

    private void addRect(float l, float t, float r, float b, String en, String ms) {
        Region region = new Region();
        region.en = en;
        region.ms = ms;
        region.bounds.set(l, t, r, b);
        region.path.addRect(l, t, r, b, Path.Direction.CW);
        regions.add(region);
    }

    private void addOvalRect(float l, float t, float r, float b, String en, String ms) {
        Region region = new Region();
        region.en = en;
        region.ms = ms;
        region.bounds.set(l, t, r, b);
        region.path.addOval(new RectF(l, t, r, b), Path.Direction.CW);
        regions.add(region);
    }

    private void addCircle(float cx, float cy, float radius, String en, String ms) {
        addOvalRect(cx - radius, cy - radius, cx + radius, cy + radius, en, ms);
    }

    private void addTriangle(float x1, float y1, float x2, float y2, float x3, float y3,
                             String en, String ms) {
        Region region = new Region();
        region.en = en;
        region.ms = ms;
        region.path.moveTo(x1, y1);
        region.path.lineTo(x2, y2);
        region.path.lineTo(x3, y3);
        region.path.close();
        region.path.computeBounds(region.bounds, true);
        regions.add(region);
    }

    // ------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        // Soft paper background
        fillPaint.setColor(0xFFFFFDE7);
        canvas.drawRect(0, 0, w, h, fillPaint);

        // Sky strip for the house picture
        if (picture == PICTURE_HOUSE) {
            fillPaint.setColor(0xFFB3E5FC);
            canvas.drawRect(0, 0, w, h * 0.45f, fillPaint);
        } else if (picture == PICTURE_FISH) {
            fillPaint.setColor(0xFFB3E5FC);
            canvas.drawRect(0, 0, w, h, fillPaint);
        } else {
            fillPaint.setColor(0xFFDCEDC8);
            canvas.drawRect(0, 0, w, h, fillPaint);
        }

        for (Region region : regions) {
            Path scaled = new Path(region.path);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setScale(w, h);
            scaled.transform(m);
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(region.fill);
            canvas.drawPath(scaled, fillPaint);
            canvas.drawPath(scaled, outline);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        float x = event.getX() / getWidth();
        float y = event.getY() / getHeight();
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region region = regions.get(i);
            RectF b = region.bounds;
            float pad = 0.01f;
            if (x >= b.left - pad && x <= b.right + pad && y >= b.top - pad && y <= b.bottom + pad) {
                region.fill = activeColor;
                invalidate();
                if (listener != null) {
                    String en = "colour";
                    String ms = "warna";
                    for (Content.Swatch s : Content.SWATCHES) {
                        if (s.color == activeColor) {
                            en = s.en;
                            ms = s.ms;
                            break;
                        }
                    }
                    listener.onRegionColored(en, ms, activeColor);
                }
                break;
            }
        }
        return true;
    }
}
