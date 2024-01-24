package kr.co.problemtimertest.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.TeacherMainActivity;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.impl.AddStudentDialogUtil;
import kr.co.problemtimertest.impl.GetStudentInAClassroomContainerImpl;
import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ManagementFragment extends Fragment {

    private Teacher teacher;

    private float dp;

    private SwipeRefreshLayout managementSwipeRefresh;

    private ProgressBar managementProgress;

    private Spinner alignStudentBySpinner;

    private NestedScrollView managementNestedScroll;
    private LinearLayout studentContainer, addStudentContainer;
    private Dialog addStudentDialog;

    private List<Classroom> classrooms;
    private List<Student> students;

    Map<String, Integer> studentRecyclerIdMap = new HashMap<>();

    private boolean isSpinnerClickedBefore;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
    private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);
    private final ManagementApi managementApi = retrofit.create(ManagementApi.class);

    // ThreadPool 관련 변수
    private final ExecutorService threadPool = new ThreadPoolExecutor(
            50,                     // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private final Handler handler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_management, container, false);

        // 변수를 생성한다.
        initializeVariables(rootView);

        // 보기 모드 스위치를 설정한다.
        setAlignStudentBySpinner();

        // 학생 탭을 설정한다.
        setStudentContainer();

        // 학생 추가 버튼을 설정한다.
        setAddStudentBtn();

        // 관리 스와이프를 설정한다.
        setManagementSwipeRefresh();

        return rootView;
    }

    // 변수를 초기화한다.
    private void initializeVariables(ViewGroup rootView) {

        teacher = ((TeacherMainActivity) getActivity()).getTeacher();

        dp = getResources().getDisplayMetrics().density;

        managementSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_management);

        managementProgress = (ProgressBar) rootView.findViewById(R.id.progress_management);

        alignStudentBySpinner = (Spinner) rootView.findViewById(R.id.spinner_align_student_by);

        managementNestedScroll = (NestedScrollView) rootView.findViewById(R.id.nested_scroll_management);
        studentContainer = (LinearLayout) rootView.findViewById(R.id.container_student);
        addStudentContainer = (LinearLayout) rootView.findViewById(R.id.container_add_student);

        addStudentDialog = new AddStudentDialogUtil(getActivity(), studentContainer, studentRecyclerIdMap, teacher).getDialog();

        isSpinnerClickedBefore = false;
    }

    // 스피너를 설정한다.
    private void setAlignStudentBySpinner() {

        alignStudentBySpinner.setDropDownHorizontalOffset(-10);

        // getWidth()가 0을 반환하는 것을 방지하기 위해 작업을 0.1초 지연한다.
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                alignStudentBySpinner.setDropDownWidth(alignStudentBySpinner.getWidth());
            }
        }, 100L);

        alignStudentBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long redId) {

                // 스피너의 첫 클릭을 무시한다.
                if (!isSpinnerClickedBefore) {

                    isSpinnerClickedBefore = true;

                    return;
                }

                managementProgress.setVisibility(View.VISIBLE);

                managementNestedScroll.setVisibility(View.GONE);

                // 학생 탭을 설정한다.
                setStudentContainer();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    // 학생 탭을 설정한다.
    private void setStudentContainer() {

        // 학생 컨테이너의 자식 뷰를 없앤다.
        studentContainer.removeAllViews();

        // 수업 리스트를 초기화한다.
        initializeClassrooms();

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 수업 리스트가 null인지 0.5초간 확인한다.
                if (isClassroomsNullForNms(500L)) return;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 학생 리스트를 얻는다.
                        initializeStudents();

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 학생 리스트가 null인지 0.2초간 확인한다.
                                if (isStudentsNullForNms(200L)) return;

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {

                                        switch (alignStudentBySpinner.getSelectedItemPosition()) {

                                            // 수업 별로 보기
                                            case 0 :

                                                // 학생 컨테이너를 반 별로 설정한다.
                                                setStudentContainerByClassroomName();
                                                break;

                                            // 학년 별로 보기
                                            case 1 :
                                                // 학생 컨테이너를 학년 별로 설정한다.
                                                setStudentContainerByGrade();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    // 수업 리스트를 초기화한다.
    private void initializeClassrooms() {

        Call<List<Classroom>> call = managementApi.list(teacher.getId());

        call.enqueue(new Callback<List<Classroom>>() {

            @Override
            public void onResponse(Call<List<Classroom>> call, Response<List<Classroom>> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                // 수업 리스트를 얻는다.
                classrooms = response.body();
            }

            @Override
            public void onFailure(Call<List<Classroom>> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 수업 리스트가 null인지 0.00n초간 확인한다.
    private boolean isClassroomsNullForNms(long n) {

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < n / 10L; i++) {

            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (classrooms == null) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to get classrooms");

        finishRefreshing();

        return true;
    }

    // 학생 리스트를 얻는다.
    private void initializeStudents() {

        // 학생 아이디 리스트를 얻는다.
        List<Long> studentIds = classrooms.stream()
                .map(classroom -> classroom.getStudentId())
                .collect(Collectors.toList());

        // 학생 아이디 리스트를 이용해 학생 리스트를 얻는다.
        Call<List<Student>> call = managementApi.list(studentIds);

        call.enqueue(new Callback<List<Student>>() {

            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                students = response.body();
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 학생 리스트가 null인지 0.00n초간 확인한다.
    private boolean isStudentsNullForNms(long n) {

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i <= n / 10L; i++) {

            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (students == null) {
                continue;
            }

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to initialize : students");

        return true;
    }

    // 학생 컨테이너를 반 별로 설정한다.
    private void setStudentContainerByClassroomName() {

        // 수업 리스트를 통해 수업 이름 리스트를 얻는다.
        List<String> classroomNames = classrooms.stream()
                .map(classroom -> classroom.getName())
                .distinct()
                .collect(Collectors.toList());

        if (classroomNames.isEmpty()) {

            // 새로고침을 종료한다.
            finishRefreshing();

            Toast.makeText(getActivity(), "학생이 없습니다.", Toast.LENGTH_SHORT).show();
        }

        for (String classroomName : classroomNames) {

            List<Classroom> classroomsOfTheName = classrooms.stream()
                    .filter(classroom -> classroom.getName().equals(classroomName))
                    .collect(Collectors.toList());

            List<Long> studentIdsInTheClassroom = classroomsOfTheName.stream()
                    .map(classroom -> classroom.getStudentId())
                    .collect(Collectors.toList());

            List<Student> studentsInTheClassroom = students.stream()
                    .filter(student -> studentIdsInTheClassroom.contains(student.getId()))
                    .collect(Collectors.toList());

            LinearLayout studentsInAClassroomContainer = new GetStudentInAClassroomContainerImpl(
                    getActivity(),
                    teacher,
                    classroomName,
                    studentsInTheClassroom,
                    alignStudentBySpinner.getSelectedItemPosition() == 1).getStudentInAClassroomContainer();

            putIdToStudentRecyclerIdMap(studentsInAClassroomContainer);

            // 만든 리니어를 학생 리니어에 추가한다.
            studentContainer.addView(studentsInAClassroomContainer);
        }

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 수업 탭이 0.2초 이내에 설정되지 않으면
                if (isStudentContainerNotSetInNms(200L, classroomNames)) return;

                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {

                        // 새로고침을 종료한다.
                        finishRefreshing();
                    }
                }, 100L);
            }
        });
    }

    private void putIdToStudentRecyclerIdMap(LinearLayout studentsInAClassroomContainer) {

        TextView classroomNameText = (TextView) studentsInAClassroomContainer.getChildAt(0);
        String classroomName = String.valueOf(classroomNameText.getText());

        RecyclerView studentRecycler = (RecyclerView) studentsInAClassroomContainer.getChildAt(1);

        studentRecyclerIdMap.put(classroomName, studentRecycler.getId());
    }

    // 수업 탭이 설정되었는지 0.2초간 확인한다.
    private boolean isStudentContainerNotSetInNms(long n, List list) {

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < n / 10L; i++) {

            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (studentContainer.getChildCount() < list.size()) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to set : studentContainer");

        // 새로고침을 종료한다.
        finishRefreshing();

        return true;
    }

    // 학생 컨테이너를 반 별로 설정한다.
    private void setStudentContainerByGrade() {

        // 학생 리스트를 통해 학년 리스트를 얻는다.
        List<Integer> grades = students.stream()
                .map(student -> student.getGrade())
                .distinct()
                .collect(Collectors.toList());

        if (grades.isEmpty()) {

            // 새로고침을 종료한다.
            finishRefreshing();

            Toast.makeText(getActivity(), "학생이 없습니다.", Toast.LENGTH_SHORT).show();
        }

        for (int grade : grades) {

            List<Student> studentsOfTheGrade = students.stream()
                    .filter(student -> student.getGrade().equals(grade))
                    .collect(Collectors.toList());

            LinearLayout studentsInAClassroomContainer = new GetStudentInAClassroomContainerImpl(
                    getActivity(),
                    teacher,
                    ConversionService.gradeToStr(grade),
                    studentsOfTheGrade,
                    alignStudentBySpinner.getSelectedItemPosition() == 1).getStudentInAClassroomContainer();

            // 만든 리니어를 학생 리니어에 추가한다.
            studentContainer.addView(studentsInAClassroomContainer);
        }

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 수업 탭이 0.2초 이내에 설정되지 않으면
                if (isStudentContainerNotSetInNms(200L, grades)) return;

                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {

                        // 새로고침을 종료한다.
                        finishRefreshing();
                    }
                }, 100L);
            }
        });
    }

    // 학생 추가 버튼을 설정한다.
    private void setAddStudentBtn() {

        // 학생 추가 대화상자를 클릭하면
        // -> 학생 추가 대화상자를 띄운다.
        addStudentContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                addStudentDialog.show();
            }
        });
    }

    // 관리 스와이프를 설정한다.
    private void setManagementSwipeRefresh() {

        managementSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                managementNestedScroll.setVisibility(View.GONE);

                // 학생 탭을 설정한다.
                setStudentContainer();
            }
        });
    }

    // 새로고침을 종료한다.
    private void finishRefreshing() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                managementProgress.setVisibility(View.GONE);

                managementNestedScroll.setVisibility(View.VISIBLE);

                managementSwipeRefresh.setRefreshing(false);
            }
        });
    }
}