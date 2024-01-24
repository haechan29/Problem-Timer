package kr.co.problemtimertest.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kr.co.problemtimertest.listener.OnItemClickListener;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.model.Book;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    private List<Book> items = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView bookNameText = (TextView) itemView.findViewById(R.id.text_book_name);

        public ViewHolder(View itemView) {

            super(itemView);
        }

        public void setItem(Book item) {
            bookNameText.setText(item.getName());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_book, viewGroup, false);

        return new BookAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Book item = items.get(position);
        viewHolder.setItem(item);

        return;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Book getItem(int position) {
        return items.get(position);
    }

    public List<Book> getItems() {
        return items;
    }

    public void addItem(Book item) {
        items.add(item);
    }

    public void removeItem(Book item) {
        items.remove(item);
    }

    public void clearItems() { items.clear(); }
}
