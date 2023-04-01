package com.stripe1.xmouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.MotionEventCompat;

/**
 * Created by bradley on 12/15/15.
 */
public class MyMouseView extends View {
  //TODO: two finger scroll, pinch to zoom

  //private final float MINP = 0.25f;
  //private final float MAXP = 0.75f;
  private Paint mPaint;
  private Bitmap mBitmap;
  //private Canvas  mCanvas;
  private Path mPath;
  private Paint mBitmapPaint;
  private float scrollStart = 120f; //pixels to draw from left, for scroll bar
  final int CLICK = 3;
  private int w=0;
  private int h=0;
  Canvas canvas;
  PointF start = new PointF();
  long downStart = 0;
  long downEnd = 0;
  boolean dragging = false;
  boolean draggable = true;
  boolean touching = false;

  // coordinate rounding errors
  private float reX = 0;
  private float reY = 0;

  enum ClickType {

    Left_click,
    Right_click,
    Drag_Down,
    Drag_up,
    Zoom_in,
    Zoom_out
  }

  public MyMouseView(Context c) {
    super(c);

    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);

    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeJoin(Paint.Join.ROUND);
    mPaint.setStrokeCap(Paint.Cap.ROUND);


    //mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);

    //mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
    mPath = new Path();
    mBitmapPaint = new Paint(Paint.DITHER_FLAG);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    this.w=w;
    this.h=h;

    //Log.d("MyMouseView", String.valueOf(w + ',' + h));
    //mCanvas = new Canvas(mBitmap);
  }

  @Override
  protected void onDraw(Canvas c) {
    //canvas.drawColor(0xFFAAAAAA);
    //canvas.drawColor(0xFFAAAA4C);
    canvas=c;
    canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

    mPaint.setColor(Color.BLUE);
    mPaint.setStrokeWidth(3);
    canvas.drawLine(w-scrollStart, h*0.1f, w-scrollStart, h*0.9f, mPaint);

    //mPaint.setStrokeWidth(1);
    //mPaint.setTextSize(20);
    //canvas.drawText(String.valueOf(scrolling), 20, 20, mPaint);
    //canvas.drawText(mX+","+mY, 20, 40, mPaint);

    if(dragging){
      mPaint.setColor(Color.YELLOW);
    }else{
      mPaint.setColor(0xFFFF0000);//red

    }

    mPaint.setStrokeWidth(10);
    canvas.drawPath(mPath, mPaint);

    if(touching) {
      mPaint.setColor(Color.BLACK);
      if(dragging) {
        mPaint.setStrokeWidth(10);
      }else{
        mPaint.setStrokeWidth(3);
      }
      if(!scrolling && !zooming) {
        canvas.drawCircle(mX, mY, 80, mPaint);
      }
      if(zooming || scrolling){


        canvas.drawCircle(curr.x, curr.y, 80, mPaint);

        if(yX<scrollStart || twoFingerScroll) {
          canvas.drawCircle(yX, yY, 80, mPaint);

        }
      }
    }


  }

  private boolean twoFingerScroll = false;
  private int zoomCounter = 0;
  private int zoomOverFlow = 10;
  private float mX=0, mY=0, yX=scrollStart, yY=0;
  private final float TOUCH_TOLERANCE = 4;
  private final float SCROLL_TOLERANCE = 20;
  private boolean scrolling = false;
  private double scaleFactor = 1;
  private boolean firstTouch= true;
  private boolean zooming =false;
  private double dist = 0;
  private PointF curr;

  private void touch_start(float x, float y) {
    touching=true;
    if((w-x)<=scrollStart){

      scrolling=true;
      x = w-scrollStart/2;
      mPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));


    }else{
      scrolling=false;
      mPaint.setPathEffect(null);

    }

    mPath.reset();
    mPath.moveTo(x, y);
    mX = x;
    mY = y;

  }
  private void touch_move(float x, float y, boolean silent) {

    if (scrolling) {
      x = w - scrollStart / 2;
    }


    float rx = x - mX;
    float ry = y - mY;
    float dx = Math.abs(rx);
    float dy = Math.abs(ry);


    if (!zooming) {

      if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
        if(!silent) mPath.quadTo(mX, mY, (x+mX)/2, (y+mY)/2);
        mX = x;
        mY = y;

        OnXMouseMoved(rx, ry, scrolling);
      }
    }else{
      //zooming

      mPath.reset();
      mPath.setLastPoint(curr.x,curr.y);
      if(!silent) mPath.lineTo(yX,yY);
      //Log.d("current points","curr.x="+curr.x+" curr.y="+curr.y+" yX="+yX+"yY="+yY);

    }

  }
  private void touch_up(boolean linger) {
    mPath.lineTo(mX, mY);
    // commit the path to our offscreen
    //mCanvas.drawPath(mPath, mPaint);
    // kill this so we don't double draw
    if(!linger) mPath.reset();
    scrolling=false;
    touching=false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    curr = new PointF(event.getX(), event.getY());
    double newDist = 0;

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:

        downStart = System.currentTimeMillis();
        if (downStart - downEnd < MainActivity.setting_mdelay) {
          touch_move(x, y, true);
          mPath.moveTo(x, y);
          OnXMouseClicked(ClickType.Drag_Down);
          dragging = true;
        } else {
          start.set(curr);
          touch_start(x, y);
        }
        invalidate();
        break;
      case MotionEvent.ACTION_MOVE:

        int xDiff1 = (int) Math.abs(curr.x - start.x);
        int yDiff1 = (int) Math.abs(curr.y - start.y);

        if (event.getPointerCount() > 1) {

          //more than 1 finger
          yX = event.getX(1);
          yY = event.getY(1);

          newDist = Math.sqrt(Math.pow(yX - x, 2) + Math.pow(yY - y, 2));
          if (firstTouch) {
            dist = newDist;
            firstTouch = false;
          }

          scaleFactor = (newDist - dist) / dist;

          if (Math.abs(scaleFactor) > 0.5) {
            zooming = true;
            zoomCounter++;
            if (zoomCounter > zoomOverFlow) {

              if (scaleFactor > 0) {
                OnXMouseClicked(ClickType.Zoom_in);
              } else {
                OnXMouseClicked(ClickType.Zoom_out);
              }
              zoomCounter = 0;
            }

          } else if (!zooming) {
            twoFingerScroll = true;
            scrolling = true;

          }
        }

        long thisTime = System.currentTimeMillis() - downStart;
        if (xDiff1 < CLICK * 6 && yDiff1 < CLICK * 6) {
          if (draggable && dragging == false && thisTime > MainActivity.setting_delay) {
            OnXMouseClicked(ClickType.Drag_Down);
            dragging = true;
          }
        } else {
          draggable = false;
        }
        touch_move(x, y, false);
        invalidate();
        break;
      case MotionEvent.ACTION_UP:

        int xDiff = (int) Math.abs(curr.x - start.x);
        int yDiff = (int) Math.abs(curr.y - start.y);
        if (xDiff < CLICK && yDiff < CLICK) {
          if (scrolling) {
            OnXMouseClicked(ClickType.Right_click);
          } else {
            OnXMouseClicked(ClickType.Left_click);
          }
        }

        if (dragging) {
          dragging = false;
          OnXMouseClicked(ClickType.Drag_up);
          if (MainActivity.setting_mdelay > 0) {
            downEnd = System.currentTimeMillis();
          }
        } else {
          downEnd = 0;
        }

        firstTouch = true;
        zooming = false;
        draggable = true;
        twoFingerScroll = false;
        touch_up(downEnd > 0);
        invalidate();
        break;
    }
    return true;
  }


  public void OnXMouseMoved(float dx, float dy, boolean scroll) {


    dx=dx*MainActivity.setting_sensitivity;
    dy=dy*MainActivity.setting_sensitivity;

    dx += reX;
    dy += reY;
    reX = dx - Math.round(dx);
    reY = dy - Math.round(dy);
    dx -= reX;
    dy -= reY;

    if(dx <0 || dy <0){
      if(scroll){
        if(MainActivity.setting_invert_scroll){
          MainActivity.doTool.mouseWheelUp();
        }else {
          MainActivity.doTool.mouseWheelDown();
        }
      } else {
        MainActivity.doTool.mouseMoveRelative(dx, dy);
      }

    }else{
      if(scroll){
        if(MainActivity.setting_invert_scroll) {
          MainActivity.doTool.mouseWheelDown();
        }else{
          MainActivity.doTool.mouseWheelUp();
        }
      } else {
        MainActivity.doTool.mouseMoveRelative(dx, dy);
      }
    }
  }


  public void OnXMouseClicked(ClickType type) {
    switch(type){
      case Left_click:
        MainActivity.doTool.mouseClick(IDoTool.MOUSE_LEFT);
        break;
      case Right_click:
        MainActivity.doTool.mouseClick(IDoTool.MOUSE_RIGHT);
        break;
      case Drag_Down:
        MainActivity.doTool.mouseDown(IDoTool.MOUSE_LEFT);
        break;
      case Drag_up:
        MainActivity.doTool.mouseUp(IDoTool.MOUSE_LEFT);
        break;
      case Zoom_in:
        MainActivity.doTool.key("Ctrl+plus");
        break;
      case Zoom_out:
        MainActivity.doTool.key("Ctrl+minus");
        break;
      default:
        break;
    }
  }
}
