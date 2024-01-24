package kr.co.problemtimertest.listener;

import androidx.recyclerview.widget.RecyclerView;

import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;

public interface SetBookStatisticsRecyclerInterface {

    void setBookStatisticsRecycler(RecyclerView.ViewHolder holder, int position, Student student, Book item);
}
