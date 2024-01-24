package kr.co.problemtimertest.adapter;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.impl.SetTeacherAssignmentRecyclerInterfaceImpl;
import kr.co.problemtimertest.listener.OnItemClickListener;
import kr.co.problemtimertest.listener.SetTeacherAssignmentRecyclerInterface;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.StudentAssignment;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.model.TeacherAssignment;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TeacherAssignmentAdapter extends RecyclerView.Adapter<TeacherAssignmentAdapter.ViewHolder> {

    private List<TeacherAssignment> items = new ArrayList<>();
    private Teacher teacher;

    public TeacherAssignmentAdapter(Teacher teacher) {
        this.teacher = teacher;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout assignmentContainer;
        private TextView targetText;
        private TextView bookNameText;
        private TextView assignmentFromText;
        private TextView assignmentToText;
        private TextView assignmentUnitText;
        private TextView dueDateText;

        // 레트로핏 관련 변수
        private RetrofitService retrofitService = new RetrofitService();
        private Retrofit retrofit = retrofitService.getRetrofit();
        private TimerApi timerApi = retrofit.create(TimerApi.class);
        private StudentAssignmentApi studentAssignmentApi = retrofit.create(StudentAssignmentApi.class);
        private TeacherAssignmentApi teacherAssignmentApi = retrofit.create(TeacherAssignmentApi.class);
        private StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);
        private ManagementApi managementApi = retrofit.create(ManagementApi.class);

        // ThreadPool 관련 변수
        private ExecutorService threadPool = new ThreadPoolExecutor(
                3,                      // 코어 스레드 개수
                100,                // 최대 스레드 개수
                120L,                  // 놀고 있는 시간
                TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
                new SynchronousQueue<Runnable>()); // 작업 큐

        private Handler handler = new Handler();

        public ViewHolder(View itemView) {

            super(itemView);

            // 변수를 초기화한다.
            initializeVariables(itemView);
        }

        // 변수를 초기화한다.
        private void initializeVariables(View itemView) {

            assignmentContainer = (LinearLayout) itemView.findViewById(R.id.container_assignment);
            targetText = (TextView) itemView.findViewById(R.id.text_target);
            bookNameText = (TextView) itemView.findViewById(R.id.text_book_name);
            assignmentFromText = (TextView) itemView.findViewById(R.id.text_assignment_from);
            assignmentToText = (TextView) itemView.findViewById(R.id.text_assignment_to);
            assignmentUnitText = (TextView) itemView.findViewById(R.id.text_assignment_unit);
            dueDateText = (TextView) itemView.findViewById(R.id.text_due_date);
        }

        public void setItem(TeacherAssignment item) {

            // 대상 텍스트
            setTargetText(item);

            // 교재 이름 텍스트
            bookNameText.setText(String.valueOf(item.getBookName()));

            // 범위 텍스트
            setRange(item);

            // 기한 텍스트
            dueDateText.setText(String.valueOf(item.getDueDate().format(DateTimeFormatter.ofPattern("M월 d일"))));
        }

        // 대상 텍스트를 설정한다.
        private void setTargetText(TeacherAssignment item) {

            // 학년 별로 과제가 출제되었다면
            if (item.getClassroomName() == null) {

                targetText.setText(String.format("학생 %d명", item.getNumberOfStudent()));
            }
            // 반 별로 과제가 출제되었다면
            else {

                if (item.isAssignedToAll()) {

                    targetText.setText(String.valueOf(item.getClassroomName()));
                }
                else {
                    targetText.setText(String.format("학생 %d명", item.getNumberOfStudent()));
                }
            }
        }

        // 범위 텍스트를 설정한다.
        private void setRange(TeacherAssignment item) {

            // 숙제가 페이지로 출제된 경우
            if (item.getPageFrom() != null && item.getPageTo() != null) {

                assignmentFromText.setText(String.valueOf(item.getPageFrom()));
                assignmentToText.setText(String.valueOf(item.getPageTo()));
                assignmentUnitText.setText("P");
            }

            // 숙제가 문제 번호로 출제한 경우
            else {

                assignmentFromText.setText(String.valueOf(item.getPageFrom()));
                assignmentToText.setText(String.valueOf(item.getPageTo()));
                assignmentUnitText.setText("번");
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_teacher_assignment, viewGroup, false);

        return new TeacherAssignmentAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        TeacherAssignment item = items.get(position);
        viewHolder.setItem(item);

        new SetTeacherAssignmentRecyclerInterfaceImpl().setTeacherAssignmentRecycler(
                viewHolder, teacher, position, new TeacherAssignmentAdapter[] { TeacherAssignmentAdapter.this });

        return;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(TeacherAssignment item) {
        items.add(item);
    }

    public void removeItem(int index) { items.remove(index); }

    public void clearItems() {
        items.clear();
    }

    public TeacherAssignment getItem(int position) {
        return items.get(position);
    }

    public List<TeacherAssignment> getItems() {
        return items;
    }

    // 아이템 리스트를 수업 순서대로 나열한다.
    public void alignItemsByClassroomName() {

        Comparator<TeacherAssignment> comparatorByClassroomName =
                (ta1, ta2) -> {
            if (ta2.getClassroomName() == null) return 1;
            else if (ta1.getClassroomName() == null) return -1;
            else return ta1.getDueDate().compareTo(ta2.getDueDate());
        };

        items = items.stream()
                .sorted(comparatorByClassroomName)
                .collect(Collectors.toList());
    }

    // 아이템 리스트를 학년 순서대로 나열한다.
    public void alignItemsByGrade() {

        Comparator<TeacherAssignment> comparatorByGrade =
                (ta1, ta2) -> {
                    if (ta2.getGrade() == null) return 1;
                    else if (ta1.getGrade() == null) return -1;
                    else return ta1.getGrade().compareTo(ta2.getGrade());
                };

        items = items.stream()
                .sorted(comparatorByGrade)
                .collect(Collectors.toList());
    }

    // 아이템 리스트를 기한 순서대로 나열한다.
    public void alignItemsByDueDate() {

        Comparator<TeacherAssignment> comparatorByDueDate =
                (ta1, ta2) -> ta1.getDueDate().compareTo(ta2.getDueDate());

        items = items.stream()
                .sorted(comparatorByDueDate)
                .collect(Collectors.toList());
    }

    // 아이템 리스트를 출제 일자 순서대로 나열한다.
    public void alignItemsByAssignedAt() {

        Comparator<TeacherAssignment> comparatorByAssignedAt =
                (ta1, ta2) -> ta1.getAssignedAt().compareTo(ta2.getAssignedAt());

        items = items.stream()
                .sorted(comparatorByAssignedAt)
                .collect(Collectors.toList());
    }
}
