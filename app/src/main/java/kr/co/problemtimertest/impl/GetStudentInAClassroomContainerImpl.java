package kr.co.problemtimertest.impl;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.adapter.StudentAdapter;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.listener.GetStudentInAClassroomContainerInterface;
import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class GetStudentInAClassroomContainerImpl implements GetStudentInAClassroomContainerInterface {

    private float dp;

    private final Context context;
    private final Teacher teacher;
    private final String classroomName;
    private final List<Student> students;
    private final boolean isAlignedByGrade;

    private LinearLayout studentsInAClassroomContainer;

    private StudentAdapter studentAdapter;
    private RecyclerView studentRecycler;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
    private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);

    // ThreadPool 관련 변수
    private final ExecutorService threadPool = new ThreadPoolExecutor(
            3,                      // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private final Handler handler = new Handler(Looper.getMainLooper());

    public GetStudentInAClassroomContainerImpl(Context context, Teacher teacher, String classroomName, List<Student> students, boolean isAlignedByGrade) {

        this.context = context;
        this.teacher = teacher;
        this.classroomName = classroomName;
        this.students = students;
        this.isAlignedByGrade = isAlignedByGrade;
    }

    @Override
    public LinearLayout getStudentInAClassroomContainer() {

        // 변수를 초기화한다.
        initializeVariable();

        // 수업 이름 텍스트와 학생 리싸이클러를 포함하는 컨테이너를 얻는다.
        studentsInAClassroomContainer = getStudentsInAClassroomContainer();

        // 수업 이름 텍스트를 설정한다.
        setClassroomNameText();

        // 학생 리싸이클러를 설정한다.
        setStudentRecycler();

        return studentsInAClassroomContainer;
    }

    // 변수를 초기화한다.
    private void initializeVariable() {

        dp = context.getResources().getDisplayMetrics().density;
    }

    // 수업 이름 텍스트와 학생 리싸이클러를 포함하는 컨테이너를 반환한다.
    private LinearLayout getStudentsInAClassroomContainer() {

        LinearLayout studentsInAClassroomContainer = new LinearLayout(context);

        studentsInAClassroomContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams studentInAClassroomContainerParam =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        studentInAClassroomContainerParam.setMargins(0, (int) (5 * dp), 0, 0);

        studentsInAClassroomContainer.setLayoutParams(studentInAClassroomContainerParam);

        // 수업 이름 텍스트를 학생 탭에 추가한다.
        studentsInAClassroomContainer.addView(new TextView(context));

        // 학생 리싸이클러를 학생 탭에 추가한다.
        studentsInAClassroomContainer.addView(new RecyclerView(context));

        return studentsInAClassroomContainer;
    }

    // 수업 이름 텍스트를 설정한다.
    private void setClassroomNameText() {

        // 수업 이름 텍스트를 얻는다.
        TextView classroomNameText = (TextView) studentsInAClassroomContainer.getChildAt(0);

        // 수업 이름 텍스트를 설정한다.
        classroomNameText.setText(classroomName);

        classroomNameText.setTextColor(Color.BLACK);
        classroomNameText.setTextSize(14);
        classroomNameText.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), 0);

        LinearLayout.LayoutParams classroomNameParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        classroomNameParam.setMargins((int) (5 * dp), 0, 0, 0);

        classroomNameText.setLayoutParams(classroomNameParam);
    }

    // 학생 리싸이클러를 설정한다.
    private void setStudentRecycler() {

        // 학생 어댑터를 얻는다.
        studentAdapter = new StudentAdapter(
                teacher.getId(), classroomName);

        // 학생 어댑터에 보기 모드를 설정한다.
        studentAdapter.setIsAlignedByGrade(isAlignedByGrade);

        // 학생 어댑터에 아이템을 추가한다.
        students.forEach(student -> studentAdapter.addItem(student));

        // 학생 리싸이클러를 얻는다.
        studentRecycler = getStudentRecyclerView();

        // 학생 리싸이클러에 학생 어댑터를 연결한다.
        setStudentAdapterToStudentRecycler();

        // 학생 어댑터를 새로고침한다.
        studentAdapter.notifyDataSetChanged();
    }

    // 학생 리싸이클러를 반환한다.
    private RecyclerView getStudentRecyclerView() {

        RecyclerView studentRecycler = (RecyclerView) studentsInAClassroomContainer.getChildAt(1);

        studentRecycler.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        studentRecycler.setPadding(0, 0, 0, (int) (5 * dp));

        int studentRecyclerId = View.generateViewId();

        studentRecycler.setId(studentRecyclerId);

        return studentRecycler;
    }

    // 학생 리싸이클러에 학생 어댑터를 연결한다.
    private void setStudentAdapterToStudentRecycler() {

        studentRecycler.setLayoutManager(new LinearLayoutManager(
                context, LinearLayoutManager.VERTICAL, false));

        studentRecycler.setAdapter(studentAdapter);
    }
}
