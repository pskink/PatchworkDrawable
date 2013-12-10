package org.pskink.patchworkdrawable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.pskink.patchworkdrawable.drawable.PatchworkDrawable;
import org.pskink.patchworkdrawable.drawable.PatchworkDrawable.Layer;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

public class Show extends Activity {
	private final static String TAG = "Show";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		String name = i.getStringExtra("name");
		if (name.equals("android")) {
			android();
		} else
		if (name.equals("flag")) {
			flag();
		} else {
			Log.d(TAG, "onCreate unknown name [" + name + "]");
		}
	}

	private void android() {
	    final List<Layer> list = new LinkedList<Layer>();
		final ImageView iv = new ImageView(this);
		PatchworkDrawable ld = new PatchworkDrawable(this, R.drawable.android_background);
		Resources res = ld.getResources();
		iv.setImageDrawable(ld);
		Bitmap android = BitmapFactory.decodeResource(res, R.drawable.android);
		final Layer[] layers = new Layer[7];
		layers[0] = addLayer(ld, android, new Rect(0, 0, 54, 53));
		layers[1] = addLayer(ld, android, new Rect(64, 0, 118, 53));
		layers[2] = addLayer(ld, android, new Rect(128, 0, 182, 53));
		layers[3] = addLayer(ld, android, new Rect(192, 0, 246, 53));
		layers[4] = addLayer(ld, android, new Rect(256, 0, 310, 53));
		layers[5] = addLayer(ld, android, new Rect(320, 0, 331, 53));
		layers[6] = addLayer(ld, android, new Rect(341, 0, 396, 53));

		Path p = new Path();
		p.moveTo(-0.43829472f, 0.024136967f);
		p.cubicTo(-2.6036985f, -7.5983596f, 31.842459f, -63.573059f,
				39.464956f, -65.738463f);
		p.cubicTo(47.087452f, -67.903867f, 106.70955f, 10.369831f, 108.87495f,
				17.992327f);
		p.cubicTo(111.04035f, 25.614824f, 75.701482f, 32.821158f, 68.078986f,
				34.986562f);
		p.cubicTo(60.456489f, 37.151966f, 1.727109f, 7.6466335f, -0.43829472f,
				0.024136967f);
		p.close();
		final PathMeasure pm = new PathMeasure(p, false);

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Layer layer = layers[msg.what];
				Animation a = (Animation) msg.obj;
				layer.startLayerAnimation(a);
			}
		};
		OnClickListener ocl = new OnClickListener() {
			@Override
			public void onClick(View v) {
				int delay = 100;
				for (int i = 0; i < layers.length; i++) {
					Animation a = new PathTranslateAnimation(pm);
					Message msg = handler.obtainMessage(i, a);
					handler.sendMessageDelayed(msg, delay);
					delay += 150;
				}
				Log.d(TAG, "onClick ");
			}
		};
		iv.setOnClickListener(ocl);
		OnTouchListener otl = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
				float x = event.getX();
				float y = event.getY();
				PatchworkDrawable ld = (PatchworkDrawable) iv.getDrawable();
				ld.getLayersAt(list, iv.getImageMatrix(), x, y);
				Log.d(TAG, "onTouch **************");
				for (int i = 0; i < layers.length; i++) {
					for (Layer layer : list) {
						if (layer == layers[i]) {
							Log.d(TAG, "onTouch " + i);
						}
					}
				}
				return false;
			}
		};
		iv.setOnTouchListener(otl);
		setContentView(iv);
	}

	@Trace(false)
	private Layer addLayer(PatchworkDrawable ld, Bitmap b, Rect rect) {
		Drawable d = new SubImageDrawable(b, rect);
		Matrix matrix = new Matrix();
		matrix.preTranslate(rect.left + 10, rect.top + 10);
		return ld.addLayer(d, matrix);
	}

	class SubImageDrawable extends Drawable {
		private Bitmap mBitmap;
		private Rect mRect;
		private Rect mDst;

		public SubImageDrawable(Bitmap b, Rect bounds) {
			mBitmap = b;
			mRect = bounds;
			mDst = new Rect(bounds);
			mDst.offsetTo(0, 0);
			setBounds(mDst);
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawBitmap(mBitmap, mRect, mDst, null);
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}
	}

	class PathTranslateAnimation extends Animation {
		private PathMeasure pm;
		private float length;

		public PathTranslateAnimation(PathMeasure pm) {
			setDuration(1000);
			this.pm = pm;
			length = pm.getLength();
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			Matrix m = t.getMatrix();
			float distance = interpolatedTime * length;
			pm.getMatrix(distance, m, PathMeasure.POSITION_MATRIX_FLAG);

			m.preRotate((float) Math.sin(Math.PI * interpolatedTime) * 45);
			float scale = 1 + 2 * (float) Math.sin(Math.PI * interpolatedTime);
			m.preScale(scale, scale);
		}
	}

	private void flag() {
	    ImageView iv = new ImageView(this);
        PatchworkDrawable ld = new PatchworkDrawable(this, R.drawable.background);
        iv.setImageDrawable(ld);
        setContentView(iv);
        try {
            ld.addLayers(R.xml.layers);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		final Layer layer0 = ld.findLayerByName("layer0");
		final Layer layer1 = ld.findLayerByName("layer1");

		final Layer animationDrawableLayer = ld.findLayerByName("animation_drawable");
		final AnimationDrawable ad = (AnimationDrawable) animationDrawableLayer
				.getDrawable();
		iv.post(new Runnable() {
			@Override
			public void run() {
				ad.start();
			}
		});

		final Layer layer2 = ld.findLayerByName("gradient_drawable");

		final Animation as = AnimationUtils
				.loadAnimation(this, R.anim.anim_set);

		final Runnable action1 = new Runnable() {
			@Override
			public void run() {
				Animation a;
				Interpolator i;

				i = new Interpolator() {
					@Override
					public float getInterpolation(float input) {
						return (float) Math.sin(input * Math.PI);
					}
				};
				as.setInterpolator(i);
				layer0.startLayerAnimation(as);

				a = new TranslateAnimation(0, 0, 0, 100);
				a.setDuration(3000);
				i = new Interpolator() {
					@Override
					public float getInterpolation(float input) {
						float output = (float) Math.sin(Math.pow(input, 2.5f)
								* 12 * Math.PI);
						return (1 - input) * output;
					}
				};
				a.setInterpolator(i);
				layer1.startLayerAnimation(a);

				a = new AlphaAnimation(0, 1);
				i = new Interpolator() {
					@Override
					public float getInterpolation(float input) {
						return (float) (1 - Math.sin(input * Math.PI));
					}
				};
				a.setInterpolator(i);
				a.setDuration(2000);
				layer2.startLayerAnimation(a);
			}
		};
		OnClickListener l1 = new OnClickListener() {
			@Override
			public void onClick(View view) {
				action1.run();
			}
		};
		iv.setOnClickListener(l1);
		iv.postDelayed(action1, 2000);

		// final float[] values = new float[9];
		// final float[] pts = new float[2];
		// final Matrix inverse = new Matrix();;
		// OnTouchListener l = new OnTouchListener() {
		// @Override
		// public boolean onTouch(View view, MotionEvent event) {
		// int action = event.getAction();
		// if (action != MotionEvent.ACTION_UP) {
		// if (inverse.isIdentity()) {
		// v.getImageMatrix().invert(inverse);
		// Log.d(TAG, "onTouch set inverse");
		// }
		// pts[0] = event.getX();
		// pts[1] = event.getY();
		// inverse.mapPoints(pts);
		//
		// mm.getValues(values);
		// // gd's bounds are (0, 0, 100, 129);
		// values[Matrix.MTRANS_X] = pts[0] - 100 / 2;
		// values[Matrix.MTRANS_Y] = pts[1] - 129 / 2;
		// mm.setValues(values);
		// v.invalidate();
		// }
		// return false;
		// }
		// };
		// v.setOnTouchListener(l);
	}
}
