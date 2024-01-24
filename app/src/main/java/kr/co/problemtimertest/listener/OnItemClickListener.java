package kr.co.problemtimertest.listener;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public interface OnItemClickListener {

    void onItemClick(RecyclerView.ViewHolder holder, View view, int position);
}
