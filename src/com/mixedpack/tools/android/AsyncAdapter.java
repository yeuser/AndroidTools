package com.mixedpack.tools.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class AsyncAdapter<T> extends BaseAdapter {
  // private final static Logger logger = Logger.getLogger(AsyncAdapter.class);
  private final List<T>       list;
  private final List<Integer> inUsePositions = new ArrayList<Integer>();
  private final int           delay;
  protected final Handler     handler        = new Handler();

  public AsyncAdapter(Collection<T> list, int delay) {
    this.list = new ArrayList<T>(list);
    this.delay = delay;
  }

  public void appendAll(Collection<T> list) {
    this.list.addAll(list);
    notifyDataSetChanged();
  }

  public void append(T row) {
    this.list.add(row);
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return list.size();
  }

  @Override
  public T getItem(int position) {
    return list.get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;// Lazy
  }

  @Override
  public View getView(final int position, View convertView, final ViewGroup parent) {
    Integer newTag = Integer.valueOf(position);
    boolean wereBeingDisplayed = inUsePositions.contains(newTag);
    if (convertView != null) {
      Integer tag = (Integer) convertView.getTag();
      inUsePositions.remove(tag);
      if (tag != null && tag.intValue() == position)
        return convertView;
    }
    final T item = getItem(position);
    if (convertView == null || needsViewCreation(position, item, convertView)) {
      convertView = createView(position, item, parent);
    } else {
    }
    inUsePositions.add(newTag);
    final View view = convertView;
    view.setTag(newTag);
    populateView(position, item, view);
    if (wereBeingDisplayed) {
      lazyPopulateView(position, item, view);
    } else {
      AndroidUtils.doInBackground(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(delay);
          } catch (InterruptedException e) {
          }
          // synchronized (view) {
          if ((Integer) view.getTag() != position)
            return;
          //
          handler.post(new Runnable() {
            @Override
            public void run() {
              lazyPopulateView(position, item, view);
            }
          });
          // }
        }
      });
    }
    return view;
  }

  protected boolean needsViewCreation(int position, T item, View convertView) {
    return false;
  }

  protected abstract View createView(int position, T item, ViewGroup parent);

  protected abstract void populateView(int position, T item, View view);

  protected abstract void lazyPopulateView(int position, T item, View view);
}