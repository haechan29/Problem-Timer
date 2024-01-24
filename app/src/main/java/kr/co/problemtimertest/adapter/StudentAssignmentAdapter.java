package kr.co.problemtimertest.adapter;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.listener.SetStudentAssignmentRecyclerInterface;
import kr.co.problemtimertest.model.StudentAssignment;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Retrofit;

public class StudentAssignmentAdapter extends RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder> {

    private List<StudentAssignment> items = new ArrayList<>();
    private SetStudentAssignmentRecyclerInterface setStudentAssignmentRecyclerInterface;

    // 레트로핏 관련 변수
    private static RetrofitService retrofitService;
    private static Retrofit retrofit;
    private static TimerApi timerApi;
    private static StudentAssignmentApi studentAssignmentApi;
    private static TeacherAssignmentApi teacherAssignmentApi;
    private static StatisticsApi statisticsApi;

    private static Handler handler;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView academyText;
        private TextView bookNameText;
        private TextView assignmentFromText;
        private TextView assignmentToText;
        private TextView assignmentUnitText;
        private TextView dueDateText;
        private TextView progressRateText;

        public ViewHolder(View itemView) {

            super(itemView);

            // 변수를 초기화한다.
            initializeVariables();
        }

        // 변수를 초기화한다.
        private void initializeVariables() {

            academyText = (TextView) itemView.findViewById(R.id.text_academy);
            bookNameText = (TextView) itemView.findViewById(R.id.text_book_name);
            assignmentFromText = (TextView) itemView.findViewById(R.id.text_assignment_from);
            assignmentToText = (TextView) itemView.findViewById(R.id.text_assignment_to);
            assignmentUnitText = (TextView) itemView.findViewById(R.id.text_assignment_unit);
            dueDateText = (TextView) itemView.findViewById(R.id.text_due_date);
            progressRateText = (TextView) itemView.findViewById(R.id.text_progress_rate);

            retrofitService = new RetrofitService();
            retrofit = retrofitService.getRetrofit();
            timerApi = retrofit.create(TimerApi.class);
            studentAssignmentApi = retrofit.create(StudentAssignmentApi.class);
            teacherAssignmentApi = retrofit.create(TeacherAssignmentApi.class);
            statisticsApi = retrofit.create(StatisticsApi.class);

            handler = new Handler();
        }

        public void setItem(StudentAssignment item) {

            // 학원 텍스트를 설정한다.
            setAcademyText(item);

            // 교재 이름 텍스트를 설정한다.
            bookNameText.setText(String.valueOf(item.getBookName()));

            // 범위 텍스트를 설정한다.
            setRangeText(item);

            // 기한 텍스트를 설정한다.
            dueDateText.setText(String.valueOf(item.getDueDate().format(DateTimeFormatter.ofPattern("M월 d일"))));

            // 진행률 텍스트를 설정한다.
            setProgressRateText(item);
        }

        // 학원 텍스트를 설정한다.
        private void setAcademyText(StudentAssignment item) {

            String academyName = String.valueOf(item.getAcademyName());

            if (academyName.length() <= 3) {

                academyText.setTextSize(12);
                academyText.setText(academyName);
            }
            else {

                academyText.setTextSize(10);
                academyText.setText(academyName.substring(0, Math.round(academyName.length() / 2F)) +
                        "\n" + academyName.substring(Math.round(academyName.length() / 2F)));
            }
        }

        // 범위 텍스트를 설정한다.
        private void setRangeText(StudentAssignment item) {

            // 숙제를 페이지로 출제한 경우
            if (item.getPageFrom() != null && item.getPageTo() != null) {

                assignmentFromText.setText(String.valueOf(item.getPageFrom()));
                assignmentToText.setText(String.valueOf(item.getPageTo()));

                assignmentUnitText.setText("P");
            }
            // 숙제를 문제 번호로 출제한 경우
            else {

                assignmentFromText.setText(item.getPageFrom());
                assignmentToText.setText(item.getPageTo());

                assignmentUnitText.setText("번");
            }
        }

        // 진행률 텍스트를 설정한다.
        private void setProgressRateText(StudentAssignment item) {

            Float progressRate = item.getProgressRate();

            // 진행률이 0인 경우
            //if (progressRate < 0.0001F) { progressRateText.setText("-"); }
            // 진행률이 100인 경우
            if (progressRate > 0.9999F) { progressRateText.setText("100%"); }
            // 진행률이 0과 100 사이의 값을 가지는 경우
            else { progressRateText.setText( Math.round(progressRate * 1000F) / 10F + "%"); }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_student_assignment, viewGroup, false);

        return new StudentAssignmentAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        StudentAssignment item = items.get(position);

        viewHolder.setItem(item);

        setStudentAssignmentRecyclerInterface.setStudentAssignmentRecycler(viewHolder, position);

        return;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(StudentAssignment item) {
        items.add(item);
    }

    public void removeItem(StudentAssignment item) {
        items.remove(item);
    }

    public void clearItems() { items.clear(); }

    public StudentAssignment getItem(int position) {
        return items.get(position);
    }

    public List<StudentAssignment> getItems() {
        return items;
    }

    public void setSetStudentAssignmentRecyclerInterface(
            SetStudentAssignmentRecyclerInterface setStudentAssignmentRecyclerInterface) {
        this.setStudentAssignmentRecyclerInterface = setStudentAssignmentRecyclerInterface;
    }

    // 아이템 리스트를 학원 순서대로 나열한다.
    public void alignItemsByAcademyName() {

        Comparator<StudentAssignment> comparatorByAcademyName =
                (sa1, sa2) -> sa1.getAcademyName().compareTo(sa2.getAcademyName());

        items = items.stream()
                .sorted(comparatorByAcademyName)
                .collect(Collectors.toList());
    }

    // 아이템 리스트를 기한 순서대로 나열한다.
    public void alignItemsByDueDate() {

        Comparator<StudentAssignment> comparatorByDueDate =
                (sa1, sa2) -> sa1.getDueDate().compareTo(sa2.getDueDate());

        items = items.stream()
                .sorted(comparatorByDueDate)
                .collect(Collectors.toList());
    }

    // 아이템 리스트를 출제 일자 순서대로 나열한다.
    public void alignItemsByAssignedAt() {

        Comparator<StudentAssignment> comparatorByAssignedAt =
                (sa1, sa2) -> sa1.getAssignedAt().compareTo(sa2.getAssignedAt());

        items = items.stream()
                .sorted(comparatorByAssignedAt)
                .collect(Collectors.toList());
    }
}