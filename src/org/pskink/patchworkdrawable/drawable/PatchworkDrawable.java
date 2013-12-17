package org.pskink.patchworkdrawable.drawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pskink.patchworkdrawable.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class PatchworkDrawable extends Drawable implements Callback {
    private final static String TAG = "PatchworkDrawable";

    private ArrayList<Layer> mLayers;
    private Drawable mBackground;
    private Resources mResources;
    private Matrix mDrawMatrix;

    /**
     * Creates a new PatchworkDrawable.
     * 
     * A background Drawable associated with resId has to have non empty
     * intrinsic size: 
     * both {@link #getIntrinsicWidth()} and {@link #getIntrinsicHeight()}
     * return > 0 (e.g. BitmapDrawable)
     * 
     * @param ctx
     * @param resId The resource identifier of the background Drawable
     */
    public PatchworkDrawable(Context ctx, int resId) {
        this(ctx, resId, 0, 0);
    }
    
    /**
     * Creates a new PatchworkDrawable 
     * @param ctx
     * @param resId The resource identifier of the background Drawable
     * @param width The width of background Drawable
     * @param height The height of background Drawable
     */
    public PatchworkDrawable(Context ctx, int resId, int width, int height) {
        init(ctx);
        mBackground = mResources.getDrawable(resId);
        if (width > 0 && height > 0) {
            mBackground.setBounds(0, 0, width, height);
        } else {
            checkBounds(mBackground);
        }
        setBounds(mBackground.getBounds());
    }


    /**
     * Creates a new PatchworkDrawable
     * @param ctx
     * @param d The background Drawable
     */
    public PatchworkDrawable(Context ctx, Drawable d) {
        init(ctx);
        mBackground = d;
        checkBounds(mBackground);
        setBounds(mBackground.getBounds());
    }
    
    /**
     * Get number of layers
     * @return Number of layers
     */
    public int getLayersSize() {
        return mLayers.size();
    }

    /**
     * Find a layer by name (used when adding layers with {@link #addLayers(int)})
     * @param name Name of a layer
     * @return A layer with given name or null when not found
     */
    public Layer findLayerByName(String name) {
        Iterator<Layer> iter = mLayers.iterator();
        while (iter.hasNext()) {
            PatchworkDrawable.Layer layer = iter.next();
            if (name.equals(layer.layerName)) {
                return layer;
            }
        }
        return null;
    }

    private RectF mTmpRect = new RectF();
    private Matrix mTmpMatrix = new Matrix();
    private Matrix mTmpInverseMatrix = new Matrix();
    private float[] mTmpPts = new float[4];

    /**
     * Find all layers which bounds contain point (x, y). The coordinate is a pixel
     * based, relative to the top-left corner of the view this Drawable is 
     * drawn with (so you can pass TouchEvent#getX() and TouchEvent#getY())
     * @param list Where the layers are written
     * @param m The matrix this Drawable is drawn with (or null if none). For
     * example if used with {@link ImageView} your should pass 
     * {@link ImageView#getImageMatrix()}  
     * @param x The X coordinate of the point being tested for containment
     * @param y The Y coordinate of the point being tested for containment
     */
    public void getLayersAt(List<Layer> list, Matrix m, float x, float y) {
        Iterator<Layer> iter = mLayers.iterator();
        list.clear();
        float[] pts = mTmpPts;
        pts[0] = x;
        pts[1] = y;
        
        while (iter.hasNext()) {
            Layer layer = iter.next();
        
            mTmpMatrix.reset();
            if (m != null) {
                mTmpMatrix.preConcat(m);
            }
            if (layer.matrix != null) {
                mTmpMatrix.preConcat(layer.matrix);
            }
            if (mTmpMatrix.invert(mTmpInverseMatrix)) {
                mTmpInverseMatrix.mapPoints(pts, 2, pts, 0, 1);
                mTmpRect.set(layer.drawable.getBounds());
                if (mTmpRect.contains(pts[2], pts[3])) {
                    list.add(layer);
                }
            }
        }
    }

    /**
     * Adds a new layer
     * @param d Drawable to be drawn on this Layer
     * @param m Matrix to apply to the Drawable
     * @return A new layer
     */
    public Layer addLayer(Drawable d, Matrix m) {
        Layer layer = new Layer(d, m);
        mLayers.add(layer);
        invalidateSelf();
        return layer;
    }

    /**
     * Adds a new layer
     * @param d Drawable to be drawn on this Layer
     * @return A new layer
     */
    public Layer addLayer(Drawable d) {
        return addLayer(d, null);
    }
    
    /**
     * Adds a new layer
     * @param idx Index in the layers array 
     * @param d Drawable to be drawn on this Layer
     * @param m Matrix to apply to the Drawable
     * @return A new layer
     */
    public Layer addLayer(int idx, Drawable d, Matrix m) {
        Layer layer = new Layer(d, m);
        mLayers.add(idx, layer);
        invalidateSelf();
        return layer;
    }

    /**
     * Adds a new layer
     * @param idx Index in the layers array 
     * @param d Drawable to be drawn on this Layer
     * @return A new layer
     */
    public Layer addLayer(int idx, Drawable d) {
        return addLayer(idx, d, null);
    }

    /**
     * Remove a layer
     * @param layer Layer to be removed
     */
    public void removeLayer(Layer layer) {
        layer.valid = false;
        mLayers.remove(layer);
    }

    /**
     * Remove all layers
     */
    public void removeAllLayers() {
        Iterator<Layer> iter = mLayers.iterator();
        while (iter.hasNext()) {
            PatchworkDrawable.Layer layer = iter.next();
            layer.valid = false;
            iter.remove();
        }
        Log.d(TAG, "removeAllLayers " + mLayers.size());
        invalidateSelf();
    }

    /**
     * Adds layers from a xml file
     * @param id Id of the sml file
     * @throws XmlPullParserException
     * @throws IOException
     */
    public void addLayers(int id) throws XmlPullParserException, IOException {
        Resources res = mResources;
        XmlResourceParser parser = res.getXml(id);
        AttributeSet attrset = Xml.asAttributeSet(parser);
        int type;
        
        while ((type=parser.next()) != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        String name = parser.getName();
        if (!name.equals("layers")) {
            throw new XmlPullParserException("No <layers> start tag found");
        }
        int[] attrs = {
                android.R.attr.name,
                android.R.attr.drawable,
        };
        boolean invalidate = false;
        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                name = parser.getName();
                if (!name.equals("layer")) {
                    throw new XmlPullParserException("tag <layer> expected, found <" + name + "> instead");
                }
                TypedArray a = res.obtainAttributes(attrset, attrs);
                String layerName = a.getString(0);
                int drawableId = a.getResourceId(1, 0);
                a.recycle();

                if (drawableId == 0) {
                    String msg = parser.getPositionDescription() + "no android:drawable attribute found";
                    throw new XmlPullParserException(msg);
                }
                Drawable dr = res.getDrawable(drawableId);
                Layer layer = new Layer(dr, layerName);
                layer.parseInner(res, parser, attrset);
                checkBounds(dr);
                mLayers.add(layer);
                invalidate = true;
//                  Log.d(TAG, name + "drawable " + dr + " layerId " + layerId);
//                  Log.d(TAG, name + "drawable bounds " + dr.getBounds());
            }
        }
        if (invalidate) {
            invalidateSelf();
        }
    }
    
    public Resources getResources() {
        return mResources;
    }

    private void checkBounds(Drawable d) {
        Rect bounds = d.getBounds();
        if (bounds.isEmpty()) {
            int iw = d.getIntrinsicWidth();
            int ih = d.getIntrinsicHeight();
            if (iw > 0 && ih > 0) {
                d.setBounds(0, 0, iw, ih);
            } else {
                String detailMessage = "drawable bounds are empty, use d.setBounds()";
                throw new RuntimeException(detailMessage);
            }
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return getBounds().width();
    }
    
    @Override
    public int getIntrinsicHeight() {
        return getBounds().height();
    }
    
    private boolean verifyDrawable(Drawable dr) {
        if (dr == this) {
            return true;
        }
        for (int i = 0; i < mLayers.size(); i++) {
            Layer layer = mLayers.get(i);
            if (layer.drawable == dr) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
//        Log.d(TAG, "******************** unscheduleDrawable " + who);
        if (verifyDrawable(who) && what != null) {
            unscheduleSelf(what);
        }
    }
    
    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
//        Log.d(TAG, "******************** scheduleDrawable " + who);
        if (verifyDrawable(who) && what != null) {
            scheduleSelf(what, when);
        }
    }
    
    @Override
    public void invalidateDrawable(Drawable who) {
//        Log.d(TAG, "******************** invalidateDrawable " + who);
        if (verifyDrawable(who)) {
            invalidateSelf();
        }
    }
    
    private void init(Context ctx) {
        mLayers = new ArrayList<Layer>();
        mResources = new PatchworkDrawableResources(ctx);
        mDrawMatrix = new Matrix();
    }

    @Override
    public void draw(Canvas canvas) {
        mBackground.draw(canvas);
        int numLayers = mLayers.size();
        boolean pendingAnimations = false;
        for (int i = 0; i < numLayers; i++) {
            mDrawMatrix.reset();
            Layer layer = mLayers.get(i);
            if (layer.matrix != null) {
                mDrawMatrix.preConcat(layer.matrix);
            }
            if (layer.animation == null) {
                draw(canvas, layer.drawable, mDrawMatrix, 255);
            } else {
                Animation a = layer.animation;
                if (!a.isInitialized()) {
                    Rect bounds = layer.drawable.getBounds();
                    Drawable parentDrawable = mBackground;
                    if (parentDrawable != null) {
                        Rect parentBounds = parentDrawable.getBounds();
                        a.initialize(bounds.width(), bounds.height(), parentBounds.width(), parentBounds.height());
                    } else {
                        a.initialize(bounds.width(), bounds.height(), 0, 0);
                    }
                }
                long currentTime = AnimationUtils.currentAnimationTimeMillis();
                boolean running = a.getTransformation(currentTime, layer.transformation);
                if (running) {
                    // animation is running: draw animation frame
                    Matrix animationFrameMatrix = layer.transformation.getMatrix();
                    mDrawMatrix.preConcat(animationFrameMatrix);

                    int alpha = (int) (255 * layer.transformation.getAlpha());
//Log.d(TAG, "onDraw ********** [" + i + "], alpha: " + alpha + ", matrix: " + animationFrameMatrix);
                    draw(canvas, layer.drawable, mDrawMatrix, alpha);
                    pendingAnimations = true;
                } else {
                    // animation ended: set it to null
                    layer.animation = null;
                    draw(canvas, layer.drawable, mDrawMatrix, 255);
                }
            }
        }
        if (pendingAnimations) {
            // invalidate if any pending animations
            invalidateSelf();
        }
    }
    
    private void draw(Canvas canvas, Drawable drawable, Matrix matrix, int alpha) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.concat(matrix);
        drawable.setAlpha(alpha);
        drawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        mBackground.setAlpha(alpha);
        int numLayers = mLayers.size();
        for (int i = 0; i < numLayers; i++) {
            Layer layer = mLayers.get(i);
            layer.getDrawable().setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mBackground.setColorFilter(cf);
        int numLayers = mLayers.size();
        for (int i = 0; i < numLayers; i++) {
            Layer layer = mLayers.get(i);
            layer.getDrawable().setColorFilter(cf);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public class Layer {
        private Drawable drawable;
        private Animation animation;
        private Transformation transformation;
        private Matrix matrix;
        private boolean valid;
        private String layerName;
        
        public void parseInner(Resources res, XmlResourceParser parser, AttributeSet attrset) throws XmlPullParserException, IOException {
            int[] attrs = {
                    R.attr.dx,      // idx 0
                    R.attr.dy,      // idx 1
                    R.attr.sx,      // idx 2
                    R.attr.sy,      // idx 3
                    R.attr.kx,      // idx 4
                    R.attr.ky,      // idx 5
                    R.attr.degrees, // idx 6
                    R.attr.px,      // idx 7
                    R.attr.py,      // idx 8
                    R.attr.left,    // idx 9
                    R.attr.top,     // idx 10
                    R.attr.right,   // idx 11
                    R.attr.bottom,  // idx 12
                    R.attr.width,   // idx 13
                    R.attr.height,  // idx 14
            };
            final int DX = 0;
            final int DY = 1;
            final int SX = 2;
            final int SY = 3;
            final int KX = 4;
            final int KY = 5;
            final int DEGREES = 6;
            final int PX = 7;
            final int PY = 8;
            final int LEFT = 9;
            final int TOP = 10;
            final int RIGHT = 11;
            final int BOTTOM = 12;
            final int WIDTH = 13;
            final int HEIGHT = 14;

            int depth = parser.getDepth();
            while (true) {
                int type = parser.next();
                String name = parser.getName();
                if (type == XmlPullParser.START_TAG) {
                    if (name.equals("matrix")) {
                        Matrix m = new Matrix();
                        type = parser.next();
                        name = parser.getName();
                        while (true) {
                            if (type == XmlPullParser.START_TAG) {
                                TypedArray a = res.obtainAttributes(attrset, attrs);
                                if (name.equals("translate")) {
                                    float dx = a.getFloat(DX, 0);
                                    float dy = a.getFloat(DY, 0);
                                    m.preTranslate(dx, dy);
                                } else
                                if (name.equals("scale")) {
                                    float sx = a.getFloat(SX, 0);
                                    float sy = a.getFloat(SY, 0);
                                    float px = a.getFloat(PX, 0);
                                    float py = a.getFloat(PY, 0);
                                    m.preScale(sx, sy, px, py);
                                } else
                                if (name.equals("rotate")) {
                                    float degrees = a.getFloat(DEGREES, 0);
                                    float px = a.getFloat(PX, 0);
                                    float py = a.getFloat(PY, 0);
                                    m.preRotate(degrees, px, py);
                                } else
                                if (name.equals("skew")) {
                                    float kx = a.getFloat(KX, 0);
                                    float ky = a.getFloat(KY, 0);
                                    float px = a.getFloat(PX, 0);
                                    float py = a.getFloat(PY, 0);
                                    m.preSkew(kx, ky, px, py);
                                } else {
                                    String msg = parser.getPositionDescription() + ": unexpected tag <" + name + "> found, " +
                                            "expected are <translate> | <scale> | <rotate> | <skew>";
                                    throw new XmlPullParserException(msg);
                                }
                                a.recycle();
                            } else
                            if (type == XmlPullParser.END_TAG && name.equals("matrix")) {
                                break;
                            }
                            type = parser.next();
                            name = parser.getName();
                        }
                        if (!m.isIdentity()) {
                            matrix = m;
                        }
                    }
                    if (name.equals("bounds")) {
                        TypedArray a = res.obtainAttributes(attrset, attrs);
                        int left = a.getInteger(LEFT, 0);
                        int top = a.getInteger(TOP, 0);
                        int right = a.getInteger(RIGHT, 0);
                        int bottom = a.getInteger(BOTTOM, 0);
                        int width = a.getInteger(WIDTH, 0);
                        int height = a.getInteger(HEIGHT, 0);
                        a.recycle();
                        drawable.setBounds(
                                left, 
                                top, 
                                width > 0? left + width : right, 
                                height > 0? top + height : bottom);
                    }
                }
                if (parser.getDepth() == depth) {
                    break;
                }
            }
        }

        private Layer(Drawable d, String name) {
            drawable = d;
            transformation = new Transformation();
            valid = true;
            layerName = name;
            d.setCallback(PatchworkDrawable.this);
        }
        
        private Layer(Drawable d, Matrix m) {
            drawable = d;
            transformation = new Transformation();
            matrix = m;
            valid = true;
            checkBounds(d);
            d.setCallback(PatchworkDrawable.this);
        }

        /**
         * Starts layer animation
         * @param Animation to start
         * @throws RuntimeException
         */
        public void startLayerAnimation(Animation a) throws RuntimeException {
            if (!valid) {
                String detailMessage = "this layer has already been removed";
                throw new RuntimeException(detailMessage);
            }
            transformation.clear();
            animation = a;
            if (a != null) {
                a.start();
            }
            invalidateSelf();
        }

        /**
         * Stops layer animation
         * @throws RuntimeException
         */
        public void stopLayerAnimation() throws RuntimeException {
            if (!valid) {
                String detailMessage = "this layer has already been removed";
                throw new RuntimeException(detailMessage);
            }
            if (animation != null) {
                animation = null;
                invalidateSelf();
            }
        }

        /**
         * Get a Drawable for this layer
         * @return Drawable used by this layer
         */
        public Drawable getDrawable() {
            return drawable;
        }
    }

    private class PatchworkDrawableResources extends Resources {

        public PatchworkDrawableResources(Context ctx) {
            super(ctx.getAssets(), new DisplayMetrics(), null);
        }
        
        @Override
        public Drawable getDrawable(int id) throws NotFoundException {
            Drawable d = super.getDrawable(id);
            if (d instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) d;
                bd.getBitmap().setDensity(DisplayMetrics.DENSITY_DEFAULT);
                bd.setTargetDensity(DisplayMetrics.DENSITY_DEFAULT);
            }
            return d;
        }
    }
}
