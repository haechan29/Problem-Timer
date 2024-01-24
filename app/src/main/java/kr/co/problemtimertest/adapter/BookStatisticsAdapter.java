package kr.co.problemtimertest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.impl.SetBookStatisticsRecyclerInterfaceImpl;
import kr.co.problemtimertest.listener.SetBookStatisticsRecyclerInterface;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;

public class BookStatisticsAdapter extends RecyclerView.Adapter<BookStatisticsAdapter.ViewHolder> {

    private List<Book> items = new ArrayList<>();

    private Student student;

    public BookStatisticsAdapter(Student student) {
        this.student = student;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout bookContainer;
        private ConstraintLayout bookPercentageContainer;
        private TextView bookPercentageText;
        private TextView bookSchoolText;
        private TextView bookSubjectText;
        private TextView bookNameText;
        private TextView bookScoreText;

        public ViewHolder(View itemView) {

            super(itemView);

            // 변수를 초기화한다.
            initializeVariables();
        }

        // 변수를 초기화한다.
        private void initializeVariables() {

            bookContainer = (ConstraintLayout) itemView.findViewById(R.id.container_book);
            bookPercentageContainer = (ConstraintLayout) itemView.findViewById(R.id.container_book_percentage);
            bookPercentageText = (TextView) itemView.findViewById(R.id.text_book_percentage);
            bookSchoolText = (TextView) itemView.findViewById(R.id.text_book_school);
            bookSubjectText = (TextView) itemView.findViewById(R.id.text_book_subject);
            bookNameText = (TextView) itemView.findViewById(R.id.text_book_name);
            bookScoreText = (TextView) itemView.findViewById(R.id.text_book_score);
        }

        public void setItem(Book item) {

            // 학교 텍스트와 과목 텍스트를 설정한다.
            setBookSchoolAndSubjectText(item);

            // 교재 텍스트를 설정한다.
            bookNameText.setText(item.getName());
        }

        // bookSchoolText, bookSubjectText에 학년과 과목을 설정한다.
        private void setBookSchoolAndSubjectText(Book item) {

            switch (item.getSchool()) {

                case 0:

                    bookSchoolText.setText("초등");

                    switch (item.getSubject()) {

                        case 0:  bookSubjectText.setText("1-1"); break;
                        case 1:  bookSubjectText.setText("1-2"); break;
                        case 2:  bookSubjectText.setText("2-1"); break;
                        case 3:  bookSubjectText.setText("2-2"); break;
                        case 4:  bookSubjectText.setText("3-1"); break;
                        case 5:  bookSubjectText.setText("3-2"); break;
                        case 6:  bookSubjectText.setText("4-1"); break;
                        case 7:  bookSubjectText.setText("4-2"); break;
                        case 8:  bookSubjectText.setText("5-1"); break;
                        case 9:  bookSubjectText.setText("5-2"); break;
                        case 10: bookSubjectText.setText("6-1"); break;
                        case 11: bookSubjectText.setText("6-2");
                    }
                    break;

                case 1:

                    bookSchoolText.setText("중등");

                    switch (item.getSubject()) {

                        case 0:  bookSubjectText.setText("1-1"); break;
                        case 1:  bookSubjectText.setText("1-2"); break;
                        case 2:  bookSubjectText.setText("2-1"); break;
                        case 3:  bookSubjectText.setText("2-2"); break;
                        case 4:  bookSubjectText.setText("3-1"); break;
                        case 5:  bookSubjectText.setText("3-2");
                    }
                    break;

                case 2:

                    bookSchoolText.setText("고등");

                    switch (item.getSubject()) {

                        case 0: bookSubjectText.setText("수학(상)");   break;
                        case 1: bookSubjectText.setText("수학(하)");   break;
                        case 2: bookSubjectText.setText("수학1");     break;
                        case 3: bookSubjectText.setText("수학2");     break;
                        case 4: bookSubjectText.setText("미적분");     break;
                        case 5: bookSubjectText.setText("확률과 통계"); break;
                        case 6: bookSubjectText.setText("기하");
                    }
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_book_statistics, viewGroup, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        Book item = items.get(position);
        viewHolder.setItem(item);

        new SetBookStatisticsRecyclerInterfaceImpl().setBookStatisticsRecycler(viewHolder, position, student, item);

        return;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(Book item) {
        items.add(item);
    }

    public Book getItem(int position) { return items.get(position); }
}
