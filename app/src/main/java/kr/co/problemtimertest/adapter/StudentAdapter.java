package kr.co.problemtimertest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.impl.SetStudentRecyclerInterfaceImpl;
import kr.co.problemtimertest.listener.OnItemClickListener;
import kr.co.problemtimertest.listener.SetBookRecyclerInterface;
import kr.co.problemtimertest.listener.SetStudentRecyclerInterface;
import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Retrofit;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private List<Student> items = new ArrayList<>();

    private Long teacherId;
    private String classroomName;

    private boolean isAlignedByGrade;

    public StudentAdapter(Long teacherId, String classroomName) {

        this.teacherId = teacherId;
        this.classroomName = classroomName;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        // 변수
        private TextView studentGradeText;
        private TextView studentNameText;
        private RecyclerView bookRecycler;
        private TextView addBookText;
        private LinearLayout problemRecordContainer;

        // 레트로핏 관련 변수
        private final RetrofitService retrofitService = new RetrofitService();
        private final Retrofit retrofit = retrofitService.getRetrofit();
        private final TimerApi timerApi = retrofit.create(TimerApi.class);
        private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);
        private final ManagementApi managementApi = retrofit.create(ManagementApi.class);

        public ViewHolder(View itemView) {

            super(itemView);

            // 변수를 초기화한다.
            initializeVariable(itemView);
        }

        // 변수를 초기화한다.
        private void initializeVariable(View itemView) {

            studentGradeText = itemView.findViewById(R.id.text_student_grade);
            studentNameText = itemView.findViewById(R.id.text_student_name);
            bookRecycler = itemView.findViewById(R.id.recycler_book);
            addBookText = itemView.findViewById(R.id.text_add_book);
            problemRecordContainer = itemView.findViewById(R.id.container_problem_statistics);
        }

        public void setItem(Student item) {

            studentNameText.setText(item.getName());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_student, viewGroup, false);

        return new StudentAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        Student item = items.get(position);
        viewHolder.setItem(item);

        new SetStudentRecyclerInterfaceImpl(
                StudentAdapter.this, viewHolder, position, teacherId, classroomName, isAlignedByGrade).setStudentRecycler();

        return;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(Student item) {
        items.add(item);
    }

    public void removeItem(int position) {
        items.remove(position);
    }

    public Student getItem(int position) {
        return items.get(position);
    }

    public List<Student> getItems() {
        return items;
    }

    public void setIsAlignedByGrade(boolean isAlignedByGrade) {
        this.isAlignedByGrade = isAlignedByGrade;
    }
}
