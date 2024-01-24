package kr.co.problemtimertest.impl;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.TeacherAssignmentAdapter;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.listener.SetTeacherAssignmentRecyclerInterface;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.model.TeacherAssignment;
import kr.co.problemtimertest.service.RetrofitService;
import kr.co.problemtimertest.service.TimerService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SetTeacherAssignmentRecyclerInterfaceImpl implements SetTeacherAssignmentRecyclerInterface {

    private RecyclerView.ViewHolder holder;
    private Teacher teacher;
    private int position;
    private TeacherAssignmentAdapter adapter;

    private float dp;

    private boolean isPageOpen;

    private AddAssignmentDialogUtil addAssignmentDialogUtil;
    private PopupMenu assignmentPopup;

    private LinearLayout assignmentContainer;
    private LinearLayout assignmentRecordContainer;
    private GridLayout assignmentRecordGrid;
    private TextView targetText;
    private TextView bookNameText;
    private TextView assignmentFromText;
    private TextView assignmentToText;
    private TextView assignmentUnitText;
    private TextView dueDateText;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
    private final TeacherAssignmentApi teacherAssignmentApi = retrofit.create(TeacherAssignmentApi.class);
    private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);
    private final ManagementApi managementApi = retrofit.create(ManagementApi.class);

    // ThreadPool 관련 변수
    private final ExecutorService threadPool = new ThreadPoolExecutor(
            3,                      // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void setTeacherAssignmentRecycler(
            RecyclerView.ViewHolder holder, Teacher teacher, int position, TeacherAssignmentAdapter[] adapter) {

        // 변수를 초기화한다.
        initializeVariable(holder, teacher, position, adapter[0]);

        // 과제 탭을 클릭하면
        assignmentContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 과제 기록 탭을 열고 닫는다.
                showRecordContainer();

                // 과제 기록을 설정한다.
                setAssignmentRecord();
            }
        });

        // 과제 탭을 길게 클릭하면
        assignmentContainer.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {

                // 과제 팝업을 보여준다.
                assignmentPopup.show();

                return true;
            }
        });
    }

    // 변수를 초기화한다.
    private void initializeVariable(RecyclerView.ViewHolder holder, Teacher teacher, int position, TeacherAssignmentAdapter adapter) {

        this.holder = holder;
        this.teacher = teacher;
        this.position = position;
        this.adapter = adapter;

        dp = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        assignmentContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_assignment);
        assignmentRecordContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_assignment_record);
        assignmentRecordGrid = (GridLayout) holder.itemView.findViewById(R.id.grid_assignment_record);
        targetText = (TextView) holder.itemView.findViewById(R.id.text_target);
        bookNameText = (TextView) holder.itemView.findViewById(R.id.text_book_name);
        assignmentFromText = (TextView) holder.itemView.findViewById(R.id.text_assignment_from);
        assignmentToText = (TextView) holder.itemView.findViewById(R.id.text_assignment_to);
        assignmentUnitText = (TextView) holder.itemView.findViewById(R.id.text_assignment_unit);
        dueDateText = (TextView) holder.itemView.findViewById(R.id.text_due_date);

        addAssignmentDialogUtil = new AddAssignmentDialogUtil(
                holder.itemView.getContext(), teacher, position, new TeacherAssignmentAdapter[] { adapter });
        assignmentPopup = getAssignmentPopup();
    }

    // 과제 기록 탭을 열고 닫는다.
    private void showRecordContainer() {

        // 과제 기록 탭의 visibility를 확인한다.
        switch (assignmentRecordContainer.getVisibility()) {

            case View.GONE:
                isPageOpen = false;
                break;

            case View.VISIBLE:
                isPageOpen = true;
        }

        // Todo : 애니메이션 적용하기
        // 위로 올라오기 애니메이션과 아래로 내려가기 애니메이션을 만든다.
        Animation translateUpAnim = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.translate_up);
        Animation translateDownAnim = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.translate_down);

        // SlidingPageAnimation 객체를 만들고 위로 올라오기, 아래로 내려가기 애니메이션에 해당 객체를 적용한다.
        Animation.AnimationListener animListener = new AnimationListenerImpl(isPageOpen, assignmentRecordContainer);
        translateDownAnim.setAnimationListener(animListener);
        translateUpAnim.setAnimationListener(animListener);

        // 페이지가 열려있는 경우
        // -> 과제 기록 탭을 보이지 않게 하고, 위로 올라가기 애니메이션을 적용한다.
        if (isPageOpen) {
            assignmentRecordContainer.startAnimation(translateUpAnim);
        }
        // 페이지가 닫혀있는 경우
        // -> 과제 기록 탭에 아래로 내려가기 애니메이션을 적용한다.
        else {
            assignmentRecordContainer.setVisibility(View.VISIBLE);
            assignmentRecordContainer.startAnimation(translateDownAnim);
        }
    }

    // 과제 기록을 설정한다.
    private void setAssignmentRecord() {

        // 과제 탭이 닫혀있다면
        if (!isPageOpen) {

            // 과제 기록 리스트를 가져와서 과제 기록 표에 추가한다.
            addViewsToAssignmentRecordGrid(assignmentRecordGrid, position);
        }
        // 과제 탭이 열려있다면
        else {

            // 과제 기록 표의 맨 윗줄만 남기고 나머지는 삭제한다.
            assignmentRecordGrid.removeViews(3, assignmentRecordGrid.getChildCount() - 3);
        }
    }

    // 과제 기록 리스트를 가져와서 그리드뷰에 추가한다.
    private void addViewsToAssignmentRecordGrid(GridLayout grid, int position) {

        threadPool.execute(new Runnable() {

            private List<Assignment> assignments;

            @Override
            public void run() {

                // 과제 기록 리스트를 얻는다.
                getAssignments();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 과제 리스트가 0.2초 이내에 초기화되지 않으면
                        if (isAssignmentsNullForNms(200L)) {

                            Log.d("error", "failed to initialize : assignments");
                            return;
                        }

                        // 과제과 같은 날짜에 출제된 과제 리스트를 얻는다.
                        List<Assignment> assignmentsAssignedAtADay =
                                assignments.stream()
                                        .filter(assignment -> assignment.getAssignedAt().isEqual(
                                                adapter.getItem(position).getAssignedAt()))
                                        .collect(Collectors.toList());

                        for (Assignment assignment : assignmentsAssignedAtADay) {

                            boolean isEven = (assignmentsAssignedAtADay.indexOf(assignment) % 2 == 0);

                            threadPool.execute(new Runnable() {

                                private Student student;

                                @Override
                                public void run() {

                                    // 학생을 얻는다.
                                    getStudent(assignment.getStudentId());

                                    threadPool.execute(new Runnable() {

                                        @Override
                                        public void run() {

                                            // 학생이 0.2초 이내로 초기화되지 않으면
                                            if (isStudentNullForNms(200L)) {

                                                Log.d("error", "failed to initialize : student");
                                                return;
                                            }

                                            // 과제 기록표에 0.2초 이내에 자식뷰가 추가되지 않으면
                                            if (isChildNotAddedToGridForNms(200L, grid, assignment)) {

                                                Log.d("error", "failed to add child to : grid");
                                                return;
                                            }

                                            // 과제 기록표에 텍스트를 추가한다.
                                            addTextToGrid(student, assignment, grid, isEven);
                                        }
                                    });
                                }

                                // 학생을 얻는다.
                                private void getStudent(Long studentId) {

                                    Call<Student> studentCall = statisticsApi.read(studentId);

                                    studentCall.enqueue(new Callback<Student>() {

                                        @Override
                                        public void onResponse(Call<Student> call, Response<Student> response) {

                                            if (!response.isSuccessful()) {

                                                Log.d("error", "code : " + response.code());
                                                return;
                                            }

                                            student = response.body();
                                        }

                                        @Override
                                        public void onFailure(Call<Student> call, Throwable t) {
                                            Log.d("error", t.getMessage());
                                        }
                                    });
                                }

                                // 학생이 null인지 0.00n초간 확인한다.
                                private boolean isStudentNullForNms(long n) {

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

                                        if (student == null) continue;

                                        return false;
                                    }

                                    try {
                                        Thread.sleep(15L);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    return true;
                                }

                                // 과제 기록표에 자식뷰가 추가되었는지 0.00n초간 확인한다.
                                private boolean isChildNotAddedToGridForNms(long n, GridLayout grid, Assignment assignment) {

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

                                        if (grid.getChildCount() / 3 == assignmentsAssignedAtADay.indexOf(assignment) + 1)
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
                        };
                    }
                });
            }

            // 과제 기록 리스트를 얻는다.
            private void getAssignments() {

                Call<List<Assignment>> assignmentsCall = teacherAssignmentApi.listAssignments(teacher.getId());

                assignmentsCall.enqueue(new Callback<List<Assignment>>() {

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

                return true;
            }
        });
    }

    // 과제 기록표에 텍스트를 추가한다.
    private synchronized void addTextToGrid(Student student, Assignment assignment, GridLayout grid, boolean isEven) {

        // 과제 기록 표에 이름을 추가한다.
        addNameTextToGrid(student, grid, isEven);

        // 과제 기록 표에 걸린 시간을 추가한다.
        addStudyTimeTextToGrid(student, assignment, grid, isEven);

        // 과제 기록 표에 진행률을 추가한다.
        addProgressTextToGrid(student, assignment, grid, isEven);

        handler.post(new Runnable() {

            @Override
            public void run() {

                grid.setBackgroundColor(isEven ? Color.WHITE : Color.parseColor("#FFEFEFEF"));
            }
        });
    }

    // 과제 기록 표에 이름을 추가한다.
    private void addNameTextToGrid(Student student, GridLayout grid, boolean isEven) {

        TextView nameText = new TextView(holder.itemView.getContext());

        nameText.setText(String.valueOf(student.getName()));
        nameText.setPadding((int) (10 * dp), 0, 0, 0);

        setAssignmentRecordTextStyle(nameText, 1L, isEven);

        handler.post(new Runnable() {

            @Override
            public void run() {

                grid.addView(nameText);
            }
        });
    }

    // 과제 기록 표에 걸린 시간을 추가한다.
    private void addStudyTimeTextToGrid(Student student, Assignment assignment, GridLayout grid, boolean isEven) {

        TextView studyTimeText = new TextView(holder.itemView.getContext());

        Call<Float> studyTimeCall = teacherAssignmentApi.read(student.getId(), assignment.getId());

        studyTimeCall.enqueue(new Callback<Float>() {

            @Override
            public void onResponse(Call<Float> call, Response<Float> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                Float studyTime = response.body();

                if (studyTime == null)
                    return;

                if (studyTime < 0.001F)
                    studyTimeText.setText("-");
                else
                    studyTimeText.setText(String.valueOf(
                            TimerService.convertToTimestamp((long) Math.round(studyTime * 1000F))));
            }

            @Override
            public void onFailure(Call<Float> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });

        setAssignmentRecordTextStyle(studyTimeText, 2L, isEven);

        handler.post(new Runnable() {

            @Override
            public void run() {
                grid.addView(studyTimeText);
            }
        });
    }

    // 과제 기록 표에 진행률을 추가한다.
    private void addProgressTextToGrid(Student student, Assignment assignment, GridLayout grid, boolean isEven) {

        TextView progressText = new TextView(holder.itemView.getContext());

        Call<Float> progressCall = teacherAssignmentApi.count(student.getId(), assignment.getId());

        progressCall.enqueue(new Callback<Float>() {

            @Override
            public void onResponse(Call<Float> call, Response<Float> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                Float progress = response.body();

                if (progress == null)
                    return;

                if (progress <= 0.001F)
                    progressText.setText("-");
                else if (progress >= 99.999F)
                    progressText.setText("100%");
                else
                    progressText.setText(String.format("%4.1f", Math.round(progress * 1000F) / 10F) + "%");
            }

            @Override
            public void onFailure(Call<Float> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });

        progressText.setPadding(0, 0, ((int) (10 * dp)), 0);

        setAssignmentRecordTextStyle(progressText, 1L, isEven);

        handler.post(new Runnable() {

            @Override
            public void run() {

                grid.addView(progressText);
            }
        });
    }

    // assignmentRecordText 스타일을 적용한다.
    private void setAssignmentRecordTextStyle(TextView text, long columnWeight, boolean isEven) {

        text.setWidth((int) (0 * dp));
        text.setHeight((int) (20 * dp));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Color.BLACK);
        text.setTextSize(12);

        text.setBackgroundColor(isEven ? Color.WHITE : Color.parseColor("#FFEFEFEF"));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, columnWeight));

        text.setLayoutParams(params);
    }

    // 과제 팝업을 반환한다.
    private PopupMenu getAssignmentPopup() {

        PopupMenu assignmentPopup = new PopupMenu(holder.itemView.getContext(), assignmentContainer);
        assignmentPopup.getMenuInflater().inflate(R.menu.popup_assignment, assignmentPopup.getMenu());

        // 과제 팝업의 메뉴가 클릭되면
        // -> 과제가 수정되거나 삭제되도록 한다.
        setOnMenuItemClickListenerToAssignmentPopup(assignmentPopup);

        return assignmentPopup;
    }

    // 과제 팝업의 메뉴가 클릭되면
    // -> 과제가 수정되거나 삭제되도록 한다.
    private void setOnMenuItemClickListenerToAssignmentPopup(PopupMenu assignmentPopup) {

        assignmentPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                // 과제 기록표가 열려 있으면
                if (assignmentRecordContainer.getVisibility() == View.VISIBLE)

                    // 과제 기록표를 닫는다.
                    showRecordContainer();

                switch (menuItem.getItemId()) {

                    // 수정하기 버튼이 클릭되면
                    case R.id.edit:

                        // 과제의 내용을 대화 상자에 입력한다.
                        editAssignment();
                        break;

                    case R.id.delete:

                        // 과제를 삭제한다.
                        deleteAssignment();
                }

                return true;
            }
        });
    }

    // 과제의 내용을 대화 상자에 입력한다.
    private void editAssignment() {

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
                        if (isAssignmentsNullForNms(200L)) {

                            Log.d("error", "failed to initialize : assignments");
                            return;
                        }

                        // 아이템과 같은 날짜에 출제된 과제 리스트를 얻는다.
                        List<Assignment> assignmentsAssignedAtADay = assignments.stream()
                                .filter(assignment -> assignment.getAssignedAt().equals(
                                        adapter.getItem(position).getAssignedAt()))
                                .collect(Collectors.toList());

                        // 과제를 대화상자에 입력한다.
                        addAssignmentDialogUtil.updateDialog(assignmentsAssignedAtADay);
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

                return true;
            }
        });
    }

    // 과제를 삭제한다.
    private void deleteAssignment() {

        threadPool.execute(new Runnable() {

            private List<Assignment> assignments;

            @Override
            public void run() {

                // 과제 리스트를 얻는다.
                getAssignments();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 과제 리스트가 0.2초 이내로 초기화되지 않으면
                        if (isAssignmentsNullForNms(200L)) {

                            Log.d("error", "failed to initialize : assignments");
                            return;
                        }

                        // 과제와 같은 날짜에 출제된 과제 리스트를 얻는다.
                        List<Assignment> assignmentsAssignedAtADay = assignments.stream()
                                .filter(assignment -> assignment.getAssignedAt().equals(
                                        adapter.getItem(position).getAssignedAt()))
                                .collect(Collectors.toList());

                        // 얻은 과제 리스트를 삭제한다.
                        assignmentsAssignedAtADay.
                                forEach(assignment -> deleteAssignment(assignment.getId()));

                        // 강사 과제 어댑터에서 아이템을 삭제한다.
                        adapter.removeItem(position);

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 강사 과제 어댑터를 새로고침한다.
                                adapter.notifyDataSetChanged();
                            }
                        });
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

                return true;
            }
        });
    }

    // 과제를 삭제한다.
    private void deleteAssignment(Long assignmentId) {

        Call<Assignment> call = teacherAssignmentApi.delete(assignmentId);

        call.enqueue(new Callback<Assignment>() {

            @Override
            public void onResponse(Call<Assignment> call, Response<Assignment> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 과제를 삭제되었다는 토스트를 띄운다.
                        Toast.makeText(holder.itemView.getContext(),
                                "과제가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Assignment> call, Throwable t) {

                Log.d("error", t.getMessage());
            }
        });
    }

    // 강사 과제를 반환한다.
    private TeacherAssignment getItem(List<Assignment> assignments) {

        TeacherAssignment[] item = new TeacherAssignment[1];

        // 강사 과제를 초기화한다.
        initializeTeacherAssignments(item, assignments);

        return !isItemNullForNms(200L, item) ? item[0] : null;
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

        return true;
    }
}
