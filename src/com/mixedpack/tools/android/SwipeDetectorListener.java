package com.mixedpack.tools.android;

import android.view.MotionEvent;
import android.view.View;

/**
 * Class swipe detection to View
 */
public abstract class SwipeDetectorListener implements View.OnTouchListener {
  public static enum SwipeAction {
    LR, // Left to right
    RL, // Right to left
    TB, // Top to bottom
    BT, // Bottom to top
    None // Action not found
  }

  public static class SwipeEvent {
    private SwipeAction action;
    private MotionEvent event;
    private float       startX, startY, x, y;
    private float       minX, maxX, minY, maxY;

    public SwipeEvent(SwipeAction action, MotionEvent event, float x, float y, float startX, float startY, float minX, float maxX, float minY, float maxY) {
      this.action = action;
      this.event = event;
      this.startX = startX;
      this.startY = startY;
      this.x = x;
      this.y = y;
      this.minX = minX;
      this.maxX = maxX;
      this.minY = minY;
      this.maxY = maxY;
    }

    public SwipeAction getAction() {
      return action;
    }

    public MotionEvent getEvent() {
      return event;
    }

    public float getStartX() {
      return startX;
    }

    public float getStartY() {
      return startY;
    }

    public float getX() {
      return x;
    }

    public float getY() {
      return y;
    }

    public float getMinX() {
      return minX;
    }

    public float getMaxX() {
      return maxX;
    }

    public float getMinY() {
      return minY;
    }

    public float getMaxY() {
      return maxY;
    }
  }

  private float startX, startY, x, y;    // Coordinates
  private float minX, maxX, minY, maxY;  // Coordinates
  private long  lastCoordinatesCheck = 0;
  private long  sensitivityDelay;

  public SwipeDetectorListener(long sensitivityDelay) {
    this.sensitivityDelay = sensitivityDelay;
  }

  /**
   * Swipe detection
   */
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    switch (event.getAction()) {
    case MotionEvent.ACTION_CANCEL:
    case MotionEvent.ACTION_OUTSIDE:
    case MotionEvent.ACTION_UP:
      minX = maxX = startX = -1;
      minY = maxY = startY = -1;
      lastCoordinatesCheck = 0;
      return onNonSwipe(v, event); // allow other events like Click to be processed
    case MotionEvent.ACTION_DOWN:
      minX = maxX = startX = event.getX();
      minY = maxY = startY = event.getY();
      lastCoordinatesCheck = System.currentTimeMillis();
      return onNonSwipe(v, event); // allow other events like Click to be processed
    case MotionEvent.ACTION_MOVE:
      long now = System.currentTimeMillis();
      if (lastCoordinatesCheck + sensitivityDelay > now)
        return false; // allow other events like Click to be processed
      lastCoordinatesCheck = now;
      x = event.getX();
      y = event.getY();
      if (x < minX)
        minX = x;
      if (y < minY)
        minY = y;
      if (x > maxX)
        maxX = x;
      if (y > maxY)
        maxY = y;
      float deltaX = startX - x;
      float deltaY = startY - y;
      if (Math.abs(deltaY) > Math.abs(deltaX))
        if (deltaY < 0)
          return onSwipe(v, new SwipeEvent(SwipeAction.TB, event, x, y, startX, startY, minX, maxX, minY, maxY));
        else
          return onSwipe(v, new SwipeEvent(SwipeAction.BT, event, x, y, startX, startY, minX, maxX, minY, maxY));
      if (Math.abs(deltaX) > Math.abs(deltaY))
        if (deltaX < 0)
          return onSwipe(v, new SwipeEvent(SwipeAction.LR, event, x, y, startX, startY, minX, maxX, minY, maxY));
        else
          return onSwipe(v, new SwipeEvent(SwipeAction.RL, event, x, y, startX, startY, minX, maxX, minY, maxY));
    }
    return onNonSwipe(v, event);
  }

  public abstract boolean onSwipe(View v, SwipeEvent swipeEvent);

  public abstract boolean onNonSwipe(View v, MotionEvent event);
}