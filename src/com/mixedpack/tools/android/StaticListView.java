package com.mixedpack.tools.android;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.LinearLayout;

public class StaticListView extends LinearLayout {
  protected Adapter           adapter;
  protected Observer          observer          = new Observer(this);
  private OnItemClickListener itemClickListener = null;

  public StaticListView(Context context) {
    super(context);
  }

  public StaticListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setAdapter(Adapter adapter) {
    if (this.adapter != null)
      this.adapter.unregisterDataSetObserver(observer);
    this.adapter = adapter;
    adapter.registerDataSetObserver(observer);
    observer.onChanged();
  }

  public void setOnItemClickListener(OnItemClickListener l) {
    this.itemClickListener = l;
  }

  private void clicked(View v, int position) {
    if (itemClickListener != null)
      itemClickListener.onItemClick(this, v, position, adapter.getItemId(position));
  }

  private class Observer extends DataSetObserver {
    StaticListView context;

    public Observer(StaticListView context) {
      this.context = context;
    }

    @Override
    public void onChanged() {
      List<View> oldViews = new ArrayList<View>(context.getChildCount());
      for (int i = 0; i < context.getChildCount(); i++)
        oldViews.add(context.getChildAt(i));
      Iterator<View> iter = oldViews.iterator();
      context.removeAllViews();
      for (int i = 0; i < context.adapter.getCount(); i++) {
        View convertView = iter.hasNext() ? iter.next() : null;
        View view = context.adapter.getView(i, convertView, context);
        final int position = i;
        view.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            clicked(v, position);
          }
        });
        context.addView(view);
      }
      super.onChanged();
    }

    @Override
    public void onInvalidated() {
      context.removeAllViews();
      super.onInvalidated();
    }
  }

  /**
   * Interface definition for a callback to be invoked when an item in this AdapterView has been clicked.
   */
  public static interface OnItemClickListener {
    /**
     * Callback method to be invoked when an item in this AdapterView has been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access the data associated with the selected item.
     * 
     * @param parent
     *          The StaticListView where the click happened.
     * @param view
     *          The view within the AdapterView that was clicked (this will be a view provided by the adapter)
     * @param position
     *          The position of the view in the adapter.
     * @param id
     *          The col id of the item that was clicked.
     */
    void onItemClick(StaticListView parent, View view, int position, long id);
  }
}