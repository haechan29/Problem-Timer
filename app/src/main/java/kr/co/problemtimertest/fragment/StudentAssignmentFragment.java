package kr.co.problemtimertest.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.StudentMainActivity;
import kr.co.problemtimertest.TeacherMainActivity;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.StudentAssignmentAdapter;
import kr.co.problemtimertest.listener.SetStudentAssignmentRecyclerInterface;
import kr.co.problemtimertest.model.Academy;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.StudentAssignment;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StudentAssignmentFragment extends Fragment {

    private Student student;

    private boolean isAdapterSetToStudentAssignmentRecycler;
    private boolean isItemAddedToStudentAdapter;

    private Spinner alignStudentAssignmentBySpinner;

    private StudentAssignmentAdapter studentAssignmentAdapter;
    private RecyclerView studentAssignmentRecycler;

    private ProgressBar studentAssignmentProgress;

    private SwipeRefreshLayout studentAssignmentSwipeRefresh;
    private NestedScrollView studentAssignmentNestedScroll;

    // 레트로핏 관련 변수
    private RetrofitService retrofitService;
    private Retrofit retrofit;
    private TimerApi timerApi;
    private StudentAssignmentApi studentAssignmentApi;
    private TeacherAssignmentApi teacherAssignmentApi;
    private StatisticsApi statisticsApi;

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

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_student_assignment, container, false);

        // 변수를 초기화한다.
        initializeVariables(rootView);

        // 과제 보기 스피너를 설정한다.
        setAlignStudentAssignmentBySpinner();

        // 학생 과제 리싸이클러를 설정한다.
        setStudentAssignmentRecycler();

        // 학생 과제 스와이프를 설정한다.
        setStudentAssignmentSwipeRefresh();

        return rootView;
    }

    // 변수를 초기화한다.
    private void initializeVariables(ViewGroup rootView) {

        student = ((StudentMainActivity) getActivity()).getStudent();

        alignStudentAssignmentBySpinner = (Spinner) rootView.findViewById(R.id.spinner_align_student_assignment_by);

        studentAssignmentRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_assignment_student);

        studentAssignmentProgress = (ProgressBar) rootView.findViewById(R.id.progress_student_assignment);

        studentAssignmentSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_teacher_assignment);
        studentAssignmentNestedScroll = (NestedScrollView) rootView.findViewById(R.id.nested_scroll_student_assignment);

        retrofitService = new RetrofitService();
        retrofit = retrofitService.getRetrofit();
        timerApi = retrofit.create(TimerApi.class);
        studentAssignmentApi = retrofit.create(StudentAssignmentApi.class);
        teacherAssignmentApi = retrofit.create(TeacherAssignmentApi.class);
        statisticsApi = retrofit.create(StatisticsApi.class);
    }

    // 과제 보기 스피너를 설정한다.
    private void setAlignStudentAssignmentBySpinner() {

        alignStudentAssignmentBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 학생 과제 리싸이클러에 어댑터가 0.3초 이내에 설정되지 않으면
                        if (isAdapterNotSetToStudentAssignmentRecyclerForNms(300L)) return;

                        // 학생 과제 어댑터에 아이템이 0.5초 이내에 추가되지 않으면
                        if (isItemNotAddedToStudentAssignmentAdapterForNms(500L)) return;

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                switch (position) {

                                    // 기한
                                    case 0:

                                        studentAssignmentAdapter.alignItemsByDueDate();
                                        break;
                                    // 학원
                                    case 1:

                                        studentAssignmentAdapter.alignItemsByAcademyName();
                                        break;
                                    // 출제일
                                    case 2:

                                        studentAssignmentAdapter.alignItemsByAssignedAt();
                                }

                                // 학생 과제 어댑터를 새로고침한다.
                                studentAssignmentAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // 스피너를 기한 순서로 보기로 설정한다.
        alignStudentAssignmentBySpinner.setSelection(0);
    }

    // 학생 과제 리싸이클러에 어댑터가 설정되었는지 0.00n초간 확인한다.
    private boolean isAdapterNotSetToStudentAssignmentRecyclerForNms(long n) {

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

            if (!isAdapterSetToStudentAssignmentRecycler) continue;

            // 사용한 플래그를 초기화한다.
            isAdapterSetToStudentAssignmentRecycler = false;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to set adapter to : studentAssignmentRecycler");

        return true;
    }

    // 학생 과제 어댑터에 아이템이 추가되었는지 0.00n초간 확인한다.
    private boolean isItemNotAddedToStudentAssignmentAdapterForNms(long n) {

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

            if (!isItemAddedToStudentAdapter) continue;

            // 사용한 플래그를 초기화한다.
            isItemAddedToStudentAdapter = false;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to add item to : studentAssignmentAdapter");

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 학생 과제 프로그레스를 보이지 않게 한다.
                studentAssignmentProgress.setVisibility(View.GONE);

                studentAssignmentNestedScroll.setVisibility(View.VISIBLE);

                // 새로고침을 끝낸다.
                studentAssignmentSwipeRefresh.setRefreshing(false);

                Toast.makeText(getActivity(), "과제를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    // 학생 과제 리싸이클러를 설정한다.
    private void setStudentAssignmentRecycler() {

        // 학생 과제 어댑터를 얻는다.
        studentAssignmentAdapter = new StudentAssignmentAdapter();

        // 학생 과제 어댑터에 아이템을 추가한다.
        addItemToStudentAssignmentAdapter();

        // 학생 과제 어댑터에 setStudentAssignmentRecyclerInterface를 설정한다.
        setSetStudentAssignmentRecyclerInterfaceToAssignmentAdapter();

        // 학생 과제 리싸이클러에 학생 과제 어댑터를 설정한다.
        setStudentAssignmentAdapterToStudentAssignmentRecycler();
    }

    // 학생 과제 어댑터에 아이템을 추가한다.
    private void addItemToStudentAssignmentAdapter() {

        threadPool.execute(new Runnable() {

            private List<Assignment> assignments;

            @Override
            public void run() {

                // 과제 리스트를 얻는다.
                getAssignments();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 과제 리스트가 0.2초 이내에 초기화되지 않으면
                        if (isAssignmentsNullForNms(200L)) return;

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 과제 리스트가 비어 있다면
                                if (assignments.isEmpty()) {

                                    Toast.makeText(getActivity(), "과제가 없습니다.", Toast.LENGTH_SHORT).show();

                                    isItemAddedToStudentAdapter = true;

                                    handler.postDelayed(new Runnable() {

                                        @Override
                                        public void run() {

                                            studentAssignmentProgress.setVisibility(View.GONE);

                                            studentAssignmentNestedScroll.setVisibility(View.VISIBLE);

                                            // 새로고침을 끝낸다.
                                            studentAssignmentSwipeRefresh.setRefreshing(false);
                                        }
                                    }, 200L);
                                }
                                // 과제 리스트가 비어 있지 않으면
                                else {

                                    threadPool.execute(new Runnable() {

                                        @Override
                                        public void run() {

                                            // 학생 과제 리스트를 얻는다.
                                            List<StudentAssignment> items = getItems(assignments);

                                            handler.post(new Runnable() {

                                                @Override
                                                public void run() {

                                                    if (items == null) {

                                                        Log.d("error", "failed to initialize : items");
                                                        return;
                                                    }

                                                    // 과제 리스트를 과제 어댑터에 추가한다.
                                                    items.forEach(studentAssignment ->
                                                            studentAssignmentAdapter.addItem(studentAssignment));

                                                    isItemAddedToStudentAdapter = true;

                                                    // 학생 과제 어탭터를 새로고침한다.
                                                    studentAssignmentAdapter.notifyDataSetChanged();

                                                    handler.postDelayed(new Runnable() {

                                                        @Override
                                                        public void run() {

                                                            studentAssignmentProgress.setVisibility(View.GONE);

                                                            studentAssignmentNestedScroll.setVisibility(View.VISIBLE);

                                                            // 새로고침을 끝낸다.
                                                            studentAssignmentSwipeRefresh.setRefreshing(false);
                                                        }
                                                    }, 200L);
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }

            // 과제 리스트를 얻는다.
            private void getAssignments() {

                Call<List<Assignment>> call = studentAssignmentApi.listAssignments(student.getId());

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

            // 과제 리스트가 초기화되었는지 0.00n초간 확인한다.
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

                return true;
            }
        });
    }

    // 학생 과제 리스트를 얻는다.
    private List<StudentAssignment> getItems(List<Assignment> assignments) {

        List<StudentAssignment>[] items = new ArrayList[] { new ArrayList<>() };

        // 학생 과제를 초기화한다.
        initializeStudentAssignments(items, assignments);

        return !isStudentAssignmentsNotInitializedForNms(200L, items, assignments) ?
                items[0] : null;
    }

    // 학생 과제를 초기화한다.
    private void initializeStudentAssignments(List<StudentAssignment>[] items, List<Assignment> assignments) {

        for (Assignment assignment : assignments) {

            threadPool.execute(new Runnable() {

                private Academy academy;
                private Book book;
                private Float progressRate;

                @Override
                public void run() {

                    // 학원을 얻는다.
                    initializeAcademy(assignment);

                    // 입력된 교재 이름이 null이라면
                    if (assignment.getTypedBookName() == null) {

                        // 교재를 얻는다.
                        initializeBook(assignment);
                    }

                    // 진행률을 얻는다.
                    initializeProgressRate(assignment);

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 학원이 0.5초 이내에 초기화되지 않으면
                            if (isAcademyNullForNms(200L)) return;

                            // 입력된 교재 이름이 null이라면
                            if (assignment.getTypedBookName() == null) {

                                // 교재가 0.2초 이내에 초기화되지 않으면
                                if (isBookNullForNms(200L)) return;
                            }

                            // 진행률이 0.2초 이내에 초기화되지 않으면
                            if (isProgressRateNullForNms(200L)) return;

                            // 아이템 리스트에 학생 교재를 추가한다.
                            items[0].add(
                                    new StudentAssignment(
                                            assignment.getId(),
                                            academy.getName(),
                                            assignment.getTypedBookName() == null ?
                                                    book.getName() : assignment.getTypedBookName(),
                                            assignment.getPageFrom(),
                                            assignment.getPageTo(),
                                            assignment.getNumberFrom(),
                                            assignment.getNumberTo(),
                                            assignment.getDueDate(),
                                            assignment.getAssignedAt(),
                                            progressRate));
                        }
                    });
                }

                // 학원을 얻는다.
                private void initializeAcademy(Assignment assignment) {

                    Call<Academy> call = studentAssignmentApi.readAcademy(assignment.getTeacherId());

                    call.enqueue(new Callback<Academy>() {

                        @Override
                        public void onResponse(Call<Academy> call, Response<Academy> response) {

                            if (!response.isSuccessful()) {

                                Log.d("error", "code : " + response.code());
                                return;
                            }

                            academy = response.body();
                        }

                        @Override
                        public void onFailure(Call<Academy> call, Throwable t) {
                            Log.d("error", t.getMessage());
                        }
                    });
                }

                // 교재를 얻는다.
                private void initializeBook(Assignment assignment) {

                    Call<Book> call = teacherAssignmentApi.read(assignment.getBookId());

                    call.enqueue(new Callback<Book>() {

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
                            Log.d("error", "\n" + t.getMessage());
                        }
                    });
                }

                // 진행률을 얻는다.
                private void initializeProgressRate(Assignment assignment) {

                    Call<Float> call = teacherAssignmentApi.count(assignment.getStudentId(), assignment.getId());

                    call.enqueue(new Callback<Float>() {

                        @Override
                        public void onResponse(Call<Float> call, Response<Float> response) {

                            if (!response.isSuccessful()) {

                                Log.d("error", "code : " + response.code());
                                return;
                            }

                            progressRate = response.body();
                        }

                        @Override
                        public void onFailure(Call<Float> call, Throwable t) {
                            Log.d("error", t.getMessage());
                        }
                    });
                }

                // 학원이 초기화되었는지 0.00n초간 확인한다.
                private boolean isAcademyNullForNms(long n) {

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

                        return false;
                    }

                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d("error", "failed to initialize : academy");

                    return true;
                }

                // 교재가 초기화되었는지 0.00n초간 확인한다.
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

                    Log.d("error", "failed to initialize : book");

                    return true;
                }

                // 진행률이 초기화되었는지 0.00n초간 확인한다.
                private boolean isProgressRateNullForNms(long n) {

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

                        if (progressRate == null) continue;

                        return false;
                    }

                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d("error", "failed to initialize : progressRate");

                    return true;
                }
            });
        }
    }

    // 학생 과제가 초기화되었는지 0.00n초간 확인한다.
    private boolean isStudentAssignmentsNotInitializedForNms(
            long n, List<StudentAssignment>[] studentAssignments, List<Assignment> assignments) {

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

            if (studentAssignments[0].size() != assignments.size()) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    // 학생 과제 어댑터에 setStudentAssignmentRecyclerInterface를 설정한다.
    private void setSetStudentAssignmentRecyclerInterfaceToAssignmentAdapter() {

        studentAssignmentAdapter.setSetStudentAssignmentRecyclerInterface(new SetStudentAssignmentRecyclerInterface() {

            @Override
            public void setStudentAssignmentRecycler(RecyclerView.ViewHolder holder, int position) {

                // 과제 탭을 선택하면
                // -> 문제 풀기 페이지의 해당 문제로 이동한다.
                holder.itemView.setOnClickListener(new View.OnClickListener() {

                    private Problem problemToSolve;

                    @Override
                    public void onClick(View view) {

                        // 해당 학생 과제 탭에 해당하는 과제를 얻는다.
                        StudentAssignment studentAssignment = studentAssignmentAdapter.getItem(position);

                        // 얻은 과제의 범위에 있는 문제 중에서 풀지 않은 첫째 문제를 얻는다.
                        initializeProblemToSolve(studentAssignment);

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 문제가 0.2초 안에 초기화되지 않았다면
                                if (isProblemToSolveNullForNms(200L)) {

                                    Log.d("error", "failed to initialize : problemToSolve");
                                    return;
                                }

                                // 문제 풀기 페이지의 해당 문제로 이동한다.
                                moveToProblem(problemToSolve);
                            }
                        });
                    }

                    // 얻은 과제의 범위에 있는 문제 중에서 풀지 않은 첫째 문제를 얻는다.
                    private void initializeProblemToSolve(StudentAssignment studentAssignment) {

                        Call<Problem> call = studentAssignmentApi.read(student.getId(), studentAssignment.getAssignmentId());

                        call.enqueue(new Callback<Problem>() {

                            @Override
                            public void onResponse(Call<Problem> call, Response<Problem> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "code : " + response.code());
                                    return;
                                }

                                problemToSolve = response.body();
                            }

                            @Override
                            public void onFailure(Call<Problem> call, Throwable t) {
                                Log.d("error", t.getMessage());
                            }
                        });
                    }

                    // 문제가 초기화되었는지 0.00n초간 확인한다.
                    private boolean isProblemToSolveNullForNms(long n) {

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

                            if (problemToSolve == null) continue;

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
        });
    }

    // 문제 풀기 탭의 해당 문제로 이동한다.
    private void moveToProblem(Problem problemToSolve) {

        TimerFragment timerFragment = ((StudentMainActivity) getActivity()).getTimerFragment();

        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_student_main, timerFragment).commit();

        // 문제 풀기 탭의 해당 문제로 이동한다.
        timerFragment.moveToProblem(problemToSolve);
    }

    // 학생 과제 리싸이클러에 학생 과제 어댑터를 설정한다.
    private void setStudentAssignmentAdapterToStudentAssignmentRecycler() {

        studentAssignmentRecycler.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));

        // 학생 과제 리싸이클러에 학생 과제 어댑터를 설정한다.
        studentAssignmentRecycler.setAdapter(studentAssignmentAdapter);

        isAdapterSetToStudentAssignmentRecycler = true;
    }

    // 학생 과제 스와이프를 설정한다.
    private void setStudentAssignmentSwipeRefresh() {

        studentAssignmentSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                studentAssignmentNestedScroll.setVisibility(View.GONE);

                // 학생 과제 리싸이클러를 설정한다.
                setStudentAssignmentRecycler();
            }
        });
    }
}