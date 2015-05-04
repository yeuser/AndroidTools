package com.mixedpack.tools.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.mixedpack.tools.R;

public class RoundImageView extends ImageView {
  private float mCornerRadius = 0f;
  private int   mCornerColor  = Color.WHITE;

  public RoundImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView);
    if (array != null) {
      mCornerRadius = array.getDimension(R.styleable.RoundedImageView_corner_radius, 0);
      mCornerColor = array.getColor(R.styleable.RoundedImageView_corner_color, Color.WHITE);
      array.recycle();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawColor(mCornerColor);
    if (getDrawable() instanceof BitmapDrawable) {
      Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
      canvas.scale(((float) getWidth()) / bitmap.getWidth(), ((float) getHeight()) / bitmap.getHeight());
      BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      Paint paint = new Paint();
      paint.setAntiAlias(true);
      paint.setShader(shader);
      RectF rect = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
      // rect contains the bounds of the shape
      // radius is the radius in pixels of the rounded corners
      // paint contains the shader that will texture the shape
      canvas.drawRoundRect(rect, mCornerRadius / getWidth() * bitmap.getWidth(), mCornerRadius / getHeight() * bitmap.getHeight(), paint);
    }
  }
}