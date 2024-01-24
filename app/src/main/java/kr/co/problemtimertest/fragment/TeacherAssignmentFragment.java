package kr.co.problemtimertest.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.StudentMainActivity;
import kr.co.problemtimertest.TeacherMainActivity;
import kr.co.problemtimertest.impl.AddAssignmentDialogUtil;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.TeacherAssignmentAdapter;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.model.TeacherAssignment;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TeacherAssignmentFragment extends Fragment {

    private Teacher teacher;

    private boolean isItemAddedToTeacherAdapter;

    private Spinner alignTeacherAssignmentBySpinner;

    private Dialog addAssignmentDialog;
    private LinearLayout addAssignmentContainer;

    private TeacherAssignmentAdapter teacherAssignmentAdapter;
    private RecyclerView teacherAssignmentRecycler;

    private ProgressBar teacherAssignmentProgress;

    private SwipeRefreshLayout teacherAssignmentSwipeRefresh;
    private NestedScrollView teacherAssignmentNestedScroll;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_teacher_assignment, container, false);

        // 변수를 초기화한다.
        initializeVariables(rootView);

        // 과제 보기 스피너를 설정한다.
        setAlignTeacherAssignmentBySpinner();

        // 과제 추가 대화상자를 설정한다.
        setAddAssignmentDialog();

        // 강사 과제 리싸이클러를 설정한다.
        setTeacherAssignmentRecycler();

        // 강사 과제 스와이프를 설정한다.
        setTeacherAssignmentSwipeRefresh();

        return rootView;
    }

    // 변수를 초기화한다.
    private void initializeVariables(ViewGroup rootView) {

        teacher = ((TeacherMainActivity) getActivity()).getTeacher();

        alignTeacherAssignmentBySpinner = (Spinner) rootView.findViewById(R.id.spinner_align_teacher_assignment_by);

        addAssignmentContainer = (LinearLayout) rootView.findViewById(R.id.container_add_assignment);

        teacherAssignmentAdapter = new TeacherAssignmentAdapter(teacher);
        teacherAssignmentRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_teacher_assignment);

        addAssignmentDialog = new AddAssignmentDialogUtil(
                getActivity(), teacher, -1, new TeacherAssignmentAdapter[] { teacherAssignmentAdapter }).getDialog();

        teacherAssignmentProgress = (ProgressBar) rootView.findViewById(R.id.progress_teacher_assignment);

        teacherAssignmentSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_teacher_assignment);
        teacherAssignmentNestedScroll = (NestedScrollView) rootView.findViewById(R.id.nested_scroll_teacher_assignment);
    }

    // 과제 보기 스피너를 설정한다.
    private void setAlignTeacherAssignmentBySpinner() {

        alignTeacherAssignmentBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 강사 과제 리싸이클러에 어댑터가 0.2초 이내에 설정되지 않으면
                        if (isAdapterNotSetToTeacherAssignmentRecyclerForNms(200L)) return;

                        // 강사 과제 어댑터에 아이템이 0.5초 이내에 추가되지 않으면
                        if (isItemNotAddedToTeacherAssignmentAdapterForNms(500L)) return;

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                switch (position) {

                                    // 수업
                                    case 0:

                                        teacherAssignmentAdapter.alignItemsByClassroomName();
                                        break;
                                    // 학년
                                    case 1:

                                        teacherAssignmentAdapter.alignItemsByGrade();
                                        break;
                                    // 기한
                                    case 2:

                                        teacherAssignmentAdapter.alignItemsByDueDate();
                                        break;
                                    // 출제일
                                    case 3:

                                        teacherAssignmentAdapter.alignItemsByAssignedAt();
                                }

                                // 강사 과제 어댑터를 새로고침한다.
                                teacherAssignmentAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // 스피너를 수업 순서로 보기로 설정한다.
        alignTeacherAssignmentBySpinner.setSelection(0);
    }

    // 강사 과제 리싸이클러에 어댑터가 설정되었는지 0.00n초간 확인한다.
    private boolean isAdapterNotSetToTeacherAssignmentRecyclerForNms(long n) {

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

            if (teacherAssignmentRecycler.getAdapter() == null) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to set adapter to : teacherAssignmentRecycler");

        return true;
    }

    // 강사 과제 어댑터에 아이템이 추가되었는지 0.00n초간 확인한다.
    private boolean isItemNotAddedToTeacherAssignmentAdapterForNms(long n) {

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

            if (!isItemAddedToTeacherAdapter) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to add item to : teacherAssignmentAdapter");

        handler.post(new Runnable() {

            @Override
            public void run() {

                Toast.makeText(getActivity(), "과제를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    // 과제 추가 대화상자를 설정한다.
    private void setAddAssignmentDialog() {

        // 과제 추가 대화상자를 클릭하면
        // -> 과제 추가 대화상자를 띄운다.
        addAssignmentContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                addAssignmentDialog.show();
            }
        });
    }

    // 강사 과제 리싸이클러를 설정한다.
    private void setTeacherAssignmentRecycler() {

        // 강사 과제 어댑터에 아이템을 추가한다.
        addItemToTeacherAssignmentAdapter();

        // 강사 과제 리싸이클러에 강사 과제 어댑터를 연결한다.
        setTeacherAssignmentAdapterToTeacherAssignmentRecycler();
    }

    // 강사 과제 어댑터에 아이템을 추가한다.
    private void addItemToTeacherAssignmentAdapter() {

        threadPool.execute(new Runnable() {

            private List<Assignment> assignments;
            private List<TeacherAssignment> items = new ArrayList<>();

            @Override
            public void run() {

                // 강사 과제 어댑터를 비운다.
                teacherAssignmentAdapter.clearItems();

                isItemAddedToTeacherAdapter = false;

                // 과제 리스트를 얻는다.
                getAssignments();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 과제 리스트가 0.2초 이내에 초기화되지 않으면
                        if (isAssignmentsNullForNms(200L)) return;

                        // 과제 리스트에서 출제 일시 리스트를 얻는다.
                        List<LocalDateTime> assignedAtList = assignments.stream()
                                .map(assignment -> assignment.getAssignedAt())
                                .distinct()
                                .sorted()
                                .collect(Collectors.toList());

                        for (LocalDateTime assignedAt : assignedAtList) {

                            // 해당 출제 일시에 출제된 과제 리스트를 얻는다.
                            List<Assignment> assignmentsAssignedAtADay = assignments.stream()
                                    .filter(assignment -> assignment.getAssignedAt().isEqual(assignedAt))
                                    .collect(Collectors.toList());

                            // 강사 과제를 얻는다.
                            TeacherAssignment item = getItem(assignmentsAssignedAtADay);

                            if (item == null) return;

                            // 강사 과제를 아이템 리스트에 추가한다.
                            items.add(item);
                        }

                        // 아이템 리스트가 비어 있다면
                        if (items.isEmpty()) {

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 새로고침을 종료한다.
                                    finishRefreshing();

                                    Toast.makeText(getActivity(), "과제가 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        // 아이템 리스트가 비어 있지 않으면
                        else {

                            // 강사 과제 어댑터에 아이템 리스트를 추가한다.
                            items.forEach(item -> teacherAssignmentAdapter.addItem(item));

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 강사 과제 어탭터를 새로고침한다.
                                    teacherAssignmentAdapter.notifyDataSetChanged();
                                }
                            });

                            handler.postDelayed(new Runnable() {

                                @Override
                                public void run() {

                                    // 새로고침을 종료한다.
                                    finishRefreshing();
                                }
                            }, 200L);
                        }

                        isItemAddedToTeacherAdapter = true;
                    }
                });
            }

            // 과제 리스트를 얻는다.
            private void getAssignments() {

                Call<List<Assignment>> call = teacherAssignmentApi.listAssignments(teacher.getId());

                call.enqueue(new Callback<List<Assignment>>() {

                    @Override
                    public void onResponse(Call<List<Assignment>> call, Response<List<Assignment>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        assignments = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<Assignment>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 과제 리스트가 null인지 0.00n초간 확인한다.
            private boolean isAssignmentsNullForNms(long n) {

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

                    if (assignments == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.d("error", "failed to initialize : assignments");

                // 새로고침을 종료한다.
                finishRefreshing();

                return true;
            }
        });
    }

    // 강사 과제를 반환한다.
    private TeacherAssignment getItem(List<Assignment> assignments) {

        TeacherAssignment[] item = new TeacherAssignment[1];

        // 강사 과제를 초기화한다.
        initializeTeacherAssignments(item, assignments);

        return !isItemNullForNms(500L, item) ? item[0] : null;
    }

    // 강사 과제를 초기화한다.
    private void initializeTeacherAssignments(
            TeacherAssignment[] item, List<Assignment> assignments) {

        threadPool.execute(new Runnable() {

            private Book book;
            private List<Student> studentsInAClassroom;
            private List<Student> students;

            @Override
            public void run() {

                // 교재를 얻는다.
                getBook();

                // 과제가 반 별로 출제되었다면
                if (assignments.get(0).getClassroomName() != null) {

                    // 특정 반의 학생 리스트를 얻는다.
                    getStudentsInAClassroom();
                }
                // 과제가 학년 별로 출제되었다면
                else {

                    // 학생 리스트를 얻는다.
                    getStudents();
                }

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재가 0.2초 이내에 초기화되지 않으면
                        if (isBookNullForNms(200L)) {

                            Log.d("error", "failed to initialize : book");
                            return;
                        }

                        // 과제가 반 별로 출제되었다면
                        if (assignments.get(0).getClassroomName() != null) {

                            // 특정 반의 학생 리스트가 0.2초 이내에 초기화되지 않으면
                            if (isStudentsInAClassroomNullForNms(200L)) {

                                Log.d("error", "failed to initialize : studentsInAClassroom");
                                return;
                            }
                        }
                        // 과제가 학년 별로 출제되었다면
                        else {

                            // 학생 리스트가 0.2초 이내에 초기화되지 않으면
                            if (isStudentsNullForNms(200L)) {

                                Log.d("error", "failed to initialize : students");
                                return;
                            }
                        }

                        item[0] = new TeacherAssignment(
                                book.getName(), // name
                                assignments.get(0).getPageFrom(), // pageFrom
                                assignments.get(0).getPageTo(), // pageTo
                                assignments.get(0).getNumberFrom(), // numberFrom
                                assignments.get(0).getNumberTo(), // numberTo
                                assignments.get(0).getDueDate(), // dueDate
                                assignments.get(0).getAssignedAt(), // assignedAt
                                assignments.get(0).getClassroomName(), // classroomName
                                assignments.get(0).getClassroomName() != null ? null : students.get(0).getGrade(), // grade
                                assignments.size(), // numberOfStudent
                                assignments.get(0).getClassroomName() != null &&
                                        assignments.size() == studentsInAClassroom.size()); // assignedToAll
                    }
                });
            }

            // 교재를 얻는다.
            private void getBook() {

                Call<Book> bookCall = teacherAssignmentApi.read(assignments.get(0).getBookId());

                bookCall.enqueue(new Callback<Book>() {

                    @Override
                    public void onResponse(Call<Book> call, Response<Book> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        book = response.body();
                    }

                    @Override
                    public void onFailure(Call<Book> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 특정 반의 학생 리스트를 얻는다.
            private void getStudentsInAClassroom() {

                Call<List<Student>> call = teacherAssignmentApi.list(
                        assignments.get(0).getClassroomName(),
                        assignments.get(0).getTeacherId());

                call.enqueue(new Callback<List<Student>>() {

                    @Override
                    public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        studentsInAClassroom = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<Student>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 학생 리스트를 얻는다.
            private void getStudents() {

                Call<List<Student>> call = managementApi.list(
                        assignments.stream()
                                .map(assignment -> assignment.getId())
                                .collect(Collectors.toList()));

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

            // 교재가 null인지 0.00n초간 확인한다.
            private boolean isBookNullForNms(long n) {

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

                    if (book == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 특정 반의 학생 리스트가 null인지 0.00n초간 확인한다.
            private boolean isStudentsInAClassroomNullForNms(long n) {

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

                    if (studentsInAClassroom == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 학생 리스트가 null인지 0.00n초간 확인한다.
            private boolean isStudentsNullForNms(long n) {

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

                    if (students == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

    // 아이템이 null인지 0.00n초간 확인한다.
    private boolean isItemNullForNms(long n, TeacherAssignment[] item) {

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

            if (item[0] == null) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to initialize : item");

        // 새로고침을 종료한다.
        finishRefreshing();

        return true;
    }

    // 강사 과제 리싸이클러에 강사 과제 어댑터를 연결한다.
    private void setTeacherAssignmentAdapterToTeacherAssignmentRecycler() {

        // 강사 과제 리싸이클러에 강사 과제 어댑터를 연결한다.
        teacherAssignmentRecycler.setAdapter(teacherAssignmentAdapter);

        // 강사 과제 리싸이클러의 방향을 가로로 설정한다.
        teacherAssignmentRecycler.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    // 강사 과제 스와이프를 설정한다.
    private void setTeacherAssignmentSwipeRefresh() {

        teacherAssignmentSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                teacherAssignmentNestedScroll.setVisibility(View.GONE);

                // 강사 과제 리싸이클러를 설정한다.
                setTeacherAssignmentRecycler();
            }
        });
    }

    // 새로고침을 종료한다.
    private void finishRefreshing() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                teacherAssignmentProgress.setVisibility(View.GONE);

                teacherAssignmentNestedScroll.setVisibility(View.VISIBLE);

                teacherAssignmentSwipeRefresh.setRefreshing(false);
            }
        });
    }
}