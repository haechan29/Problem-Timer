package kr.co.problemtimertest.impl;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.TeacherAssignmentAdapter;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.model.TeacherAssignment;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AddAssignmentDialogUtil {

    private float dp;

    private Teacher teacher;
    private int position;
    private TeacherAssignmentAdapter adapter;

    private Dialog addAssignmentDialog;

    private RadioButton clickedContentClass;
    private RadioButton clickedBook;

    private RadioGroup titleClassRadioGroup;
    private RadioButton titleClassRadioBtn;
    private RadioButton titleGradeRadioBtn;
    private RadioGroup contentClassRadioGroup;

    private ConstraintLayout studentContainer;
    private LinearLayout contentStudentContainer;

    private ConstraintLayout bookContainer;
    private CheckBox typeMyselfCheck;
    private RadioGroup contentBookRadioGroup;
    private EditText typeMyselfEdit;

    private ConstraintLayout rangeContainer;
    private EditText contentRangeFromEdit;
    private EditText contentRangeToEdit;

    private ConstraintLayout dueDateContainer;
    private DatePicker contentDueDateDatePicker;
    private TextView contentDueDateWeekText;
    private TextView contentDueDateText;

    private LinearLayout cancelContainer, addContainer;
    private TextView cancelText, addText;

    private View assignmentIdView;

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

    private Handler handler = new Handler(Looper.getMainLooper());

    public AddAssignmentDialogUtil(Context context, Teacher teacher, int position, TeacherAssignmentAdapter[] adapter) {

        // 과제 추가 대화상자를 얻는다.
        addAssignmentDialog = new Dialog(context);

        // 과제 추가 대화상자의 타이틀을 없애고, 리소스를 연결한다.
        addAssignmentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addAssignmentDialog.setContentView(R.layout.dialog_add_assignment);

        // 과제 추가 대화상자에 리스너를 설정한다.
        setListenerToAddAssignmentDialog(teacher, position, adapter);
    }

    // 과제 추가 대화상자를 반환한다.
    public Dialog getDialog() {
        return addAssignmentDialog;
    }

    // 과제를 대화상자에 입력한다.
    public void updateDialog(List<Assignment> assignmentsAssignedAtADay) {

        // 아이디 텍스트
        assignmentIdView.setTag(
                assignmentsAssignedAtADay.stream()
                        .map(assignment -> assignment.getId())
                        .collect(Collectors.toList()));

        // 수업 제목 탭
        updateTitleClassTab(assignmentsAssignedAtADay.get(0));

        // 수업 내용 탭
        updateContentClassTab(assignmentsAssignedAtADay.get(0));

        // 학생 탭
        updateStudentTab(assignmentsAssignedAtADay);

        // 교재 탭
        updateBookTab(assignmentsAssignedAtADay);

        // 범위 탭
        updateRangeTab(assignmentsAssignedAtADay.get(0));

        // 기한 탭
        updateDueDateTab(assignmentsAssignedAtADay.get(0));

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 과제 추가 대화상자를 띄운다.
                addAssignmentDialog.show();
            }
        });
    }

    // 수업 제목 탭에 과제를 입력한다.
    private void updateTitleClassTab(Assignment assignment) {

        String classroomName = assignment.getClassroomName();

        // 수업이 입력된 경우
        if (classroomName != null) {

            // 수업 버튼을 클릭한다.
            titleClassRadioBtn.performClick();
        }
        // 학년이 입력된 경우
        else {

            // 학년 버튼을 클릭한다.
            titleGradeRadioBtn.performClick();
        }
    }

    // 수업 내용 탭에 과제를 입력한다.
    private void updateContentClassTab(Assignment assignment) {

        threadPool.execute(new Runnable() {

            private Student student;

            @Override
            public void run() {

                // 반 내용 탭에 2초 이내에 자식뷰가 추가되지 않으면
                if (isChildNotAddedToContentClassRadioGroupForNms(2000L)) {

                    Log.d("error", "failed to add child to : contentClassRadioGroup");
                    return;
                }

                String classroomName = assignment.getClassroomName();

                // 수업이 입력된 경우
                if (classroomName != null) {

                    // 얻은 수업 이름에 해당하는 버튼을 클릭한다.
                    clickContentClassRadioBtn(classroomName);
                }
                // 학년이 입력된 경우
                else {

                    // 과제 대상인 학생을 얻는다.
                    getStudent(assignment);

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 학생이 0.2초 이내에 초기화되지 않으면
                            if (isStudentNullForNms(200L)) {

                                Log.d("error", "failed to initialize : student");
                                return;
                            }

                            // 특정 학년에 해당하는 버튼을 클릭한다.
                            clickContentGradeRadioBtn(student.getGrade());
                        }
                    });
                }
            }

            // 반 내용 탭에 자식뷰가 추가되었는지 0.00n초간 확인한다.
            private boolean isChildNotAddedToContentClassRadioGroupForNms(long n) {

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

                    if (contentClassRadioGroup.getChildCount() == 0) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 과제 대상인 학생을 얻는다.
            private void getStudent(Assignment assignment) {

                Call<Student> call = statisticsApi.read(assignment.getStudentId());

                call.enqueue(new Callback<Student>() {

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
        });
    }

    // 해당 수업 이름에 해당하는 버튼을 클릭한다.
    private void clickContentClassRadioBtn(String classroomName) {

        for (int i = 0; i < contentClassRadioGroup.getChildCount(); i++) {

            RadioButton contentClassRadioBtn = (RadioButton) contentClassRadioGroup.getChildAt(i);

            if (contentClassRadioBtn.getText().equals(classroomName)) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        contentClassRadioBtn.performClick();
                    }
                });
            }
        }
    }

    // 특정 학년에 해당하는 버튼을 클릭한다.
    private void clickContentGradeRadioBtn(Integer grade) {

        for (int i = 0; i < contentClassRadioGroup.getChildCount(); i++) {

            RadioButton contentGradeRadioBtn = (RadioButton) contentClassRadioGroup.getChildAt(i);

            handler.post(new Runnable() {

                @Override
                public void run() {

                    if (ConversionService.strToGrade(
                            String.valueOf(contentGradeRadioBtn.getText())).equals(grade)){

                        contentGradeRadioBtn.performClick();
                    }
                }
            });
        }
    }

    // 학생 탭에 과제를 입력한다.
    private void updateStudentTab(List<Assignment> assignmentsAssignedAtADay) {

        for (Assignment assignmentAssignedAtADay : assignmentsAssignedAtADay) {

            threadPool.execute(new Runnable() {

                private Student student;

                @Override
                public void run() {

                    // 학생 내용 탭에 2초 이내로 자식 뷰가 추가되지 않으면
                    if (isChildNotAddedToContentStudentContainerForNms(2000L)) {

                        Log.d("error", "failed to add child to : contentStudentContainer");
                        return;
                    };

                    // 학생 체크박스가 2초 이내에 체크되지 않으면
                    if (isStudentCheckNotCheckedForNms(2000L, assignmentAssignedAtADay)) {}

                    // 학생을 얻는다.
                    getStudent(assignmentAssignedAtADay);

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 학생이 0.2초 이내에 초기화되지 않으면
                            if (isStudentNullForNms(200L)) {

                                Log.d("error", "failed to initialize : student");
                                return;
                            }

                            // 얻은 학생에 해당하는 체크박스를 클릭한다.
                            clickStudentCheck(student);
                        }
                    });
                }

                // 학생 내용 탭에 자식 뷰가 추가되었는지 0.00n초간 확인한다.
                private boolean isChildNotAddedToContentStudentContainerForNms(long n) {

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

                        if (contentStudentContainer.getChildCount() == 0) continue;

                        return false;
                    }

                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    return true;
                }

                // 학생을 얻는다.
                private void getStudent(Assignment assignment) {

                    Call<Student> call = statisticsApi.read(assignment.getStudentId());

                    call.enqueue(new Callback<Student>() {

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

                // 학생이 초기화되었는지 0.00n초간 확인한다.
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

                // 학생 체크박스가 0.00n초 이내에 체크되었는지 확인한다.
                private boolean isStudentCheckNotCheckedForNms(long n, Assignment assignmentAssignedAtADay) {

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

                        // 학생 내용 탭에서 체크한 학생 체크박스의 수를 얻는다.
                        int count = 0;

                        for (int j = 0; j < contentStudentContainer.getChildCount(); j++)
                            if (((CheckBox) contentStudentContainer.getChildAt(j)).isChecked())
                                count++;

                        if (count == assignmentsAssignedAtADay.indexOf(assignmentAssignedAtADay)) {

                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            return false;
                        }
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
    }

    // 얻은 학생에 해당하는 체크박스를 클릭한다.
    private void clickStudentCheck(Student student) {

        for (int i = 0; i < contentStudentContainer.getChildCount(); i++) {

            CheckBox studentCheck = (CheckBox) contentStudentContainer.getChildAt(i);

            Student tag = (Student) studentCheck.getTag();

            if (tag.equals(student)) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        studentCheck.performClick();
                    }
                });
            }
        }
    }

    // 교재 탭에 과제를 입력한다.
    private void updateBookTab(List<Assignment> assignmentsAssignedAtADay) {

        threadPool.execute(new Runnable() {

            private Book book;

            @Override
            public void run() {

                // 학생 체크박스가 2초 이내에 클릭되지 않으면
                if (isStudentCheckNotCheckedForNms(2000L, assignmentsAssignedAtADay)) {

                    Log.d("error", "failed to check : contentStudentCheck");
                    return;
                }

                // 교재 내용 탭에 2초 이내로 자식 뷰가 추가되지 않으면
                if (isChildNotAddedToContentBookRadioGroupForNms(2000L)) {

                    Log.d("error", "failed to add Child to : contentBookRadioGroup");
                    return;
                }

                // 교재를 얻는다.
                getBook(assignmentsAssignedAtADay.get(0));

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재가 0.2초 이내로 초기화되지 않으면
                        if (isBookNullForNms(200L)) {

                            Log.d("error", "failed to initialize : book");
                            return;
                        }

                        // 얻은 교재에 해당하는 버튼을 클릭한다.
                        clickContentBookRadioBtn(book);
                    }
                });
            }

            // 학생 체크박스가 클릭되었는지 0.00n초간 확인한다.
            private boolean isStudentCheckNotCheckedForNms(long n, List<Assignment> assignmentsAssignedAtADay) {

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

                    // 학생 내용 탭에서 체크한 학생 체크박스의 수를 얻는다.
                    int count = 0;

                    for (int j = 0; j < contentStudentContainer.getChildCount(); j++)
                        if (((CheckBox) contentStudentContainer.getChildAt(j)).isChecked())
                            count++;


                    Log.d("error", "count : " + count + " // assignments : " + assignmentsAssignedAtADay.size());
                    if (count == assignmentsAssignedAtADay.size()) return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 교재 내용 탭에 자식 뷰가 추가되었는지 0.00n초간 확인한다.
            private boolean isChildNotAddedToContentBookRadioGroupForNms(long n) {

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

                    if (contentBookRadioGroup.getChildCount() == 0) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 교재를 얻는다.
            private void getBook(Assignment assignment) {

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
        });
    }

    // 얻은 교재에 해당하는 버튼을 클릭한다.
    private void clickContentBookRadioBtn(Book book) {

        for (int i = 0; i < contentBookRadioGroup.getChildCount(); i++) {

            RadioButton contentBookRadioBtn = (RadioButton) contentBookRadioGroup.getChildAt(i);

            if (((Book) contentBookRadioBtn.getTag()).equals(book)) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Log.d("error", "book clicked");
                        contentBookRadioBtn.performClick();
                    }
                });
            }
        }
    }

    // 범위 탭에 과제를 입력한다.
    private void updateRangeTab(Assignment assignment) {

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 범위 탭이 2초 이내로 보이지 않으면
                if (isRangeContainerGoneForNms(2000L)) {

                    Log.d("error", "failed to set visible to : rangeContainer");
                    return;
                }

                Integer pageFrom = assignment.getPageFrom();
                Integer pageTo = assignment.getPageTo();

                contentRangeFromEdit.setText(String.valueOf(pageFrom));
                contentRangeToEdit.setText(String.valueOf(pageTo));
            }

            // 범위 탭이 보이지 않는지 0.00n초간 확인한다.
            private boolean isRangeContainerGoneForNms(long n) {

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

                    if (rangeContainer.getVisibility() == View.GONE) continue;

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

    // 기한 탭에 과제의 내용을 입력한다.
    private void updateDueDateTab(Assignment assignment) {

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 기한 탭이 2초 이내로 보이지 않으면
                if (isDueDateContainerGoneForNms(2000L)) {

                    Log.d("error", "failed to set visible to : dueDateContainer");
                    return;
                }

                updateDatePicker(contentDueDateDatePicker, assignment.getDueDate());
            }

            // 기한 탭이 보이지 않는지 0.00n초간 확인한다.
            private boolean isDueDateContainerGoneForNms(long n) {

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

                    if (dueDateContainer.getVisibility() == View.GONE) continue;

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

    // 과제 추가 대화상자에 리스너를 설정한다.
    private void setListenerToAddAssignmentDialog(Teacher teacher, int position, TeacherAssignmentAdapter[] adapter) {

        // 과제 추가 대화상자 관련 변수를 초기화한다.
        initializeDialogVariables(teacher, position, adapter);

        // 반 탭을 설정한다.
        setClassTab();

        // 학생 탭을 설정한다.
        setStudentTab();

        // 교재 탭을 설정한다.
        setBookTab();

        // 범위 탭을 설정한다.
        setRangeTab();

        // 기한 탭을 설정한다.
        setDueDateText();

        // 취소 버튼을 설정한다.
        setCancelContainer();

        // 추가 버튼을 설정한다.
        setAddContainer();
    }

    // 과제 추가 대화상자 관련 변수를 초기화한다.
    private void initializeDialogVariables(Teacher teacher, int position, TeacherAssignmentAdapter[] adapter) {

        this.teacher = teacher;
        this.position = position;
        this.adapter = adapter[0];

        dp = addAssignmentDialog.getContext().getResources().getDisplayMetrics().density;

        titleClassRadioGroup = (RadioGroup) addAssignmentDialog.findViewById(R.id.radio_group_title_class);
        titleClassRadioBtn = (RadioButton) addAssignmentDialog.findViewById(R.id.radio_btn_title_class);
        titleGradeRadioBtn = (RadioButton) addAssignmentDialog.findViewById(R.id.radio_btn_title_grade);
        contentClassRadioGroup = (RadioGroup) addAssignmentDialog.findViewById(R.id.radio_group_content_class);

        studentContainer = (ConstraintLayout) addAssignmentDialog.findViewById(R.id.container_student);
        contentStudentContainer = (LinearLayout) addAssignmentDialog.findViewById(R.id.container_content_student);

        bookContainer = (ConstraintLayout) addAssignmentDialog.findViewById(R.id.container_book);
        typeMyselfCheck = (CheckBox) addAssignmentDialog.findViewById(R.id.check_type_myself);
        contentBookRadioGroup = (RadioGroup) addAssignmentDialog.findViewById(R.id.radio_group_content_book);
        typeMyselfEdit = (EditText) addAssignmentDialog.findViewById(R.id.edit_type_myself);

        rangeContainer = (ConstraintLayout) addAssignmentDialog.findViewById(R.id.container_range);
        contentRangeFromEdit = (EditText) addAssignmentDialog.findViewById(R.id.edit_content_range_from);
        contentRangeToEdit = (EditText) addAssignmentDialog.findViewById(R.id.edit_content_range_to);

        dueDateContainer = (ConstraintLayout) addAssignmentDialog.findViewById(R.id.container_due_date);
        contentDueDateDatePicker = (DatePicker) addAssignmentDialog.findViewById(R.id.date_picker_content_due_date);
        contentDueDateWeekText = (TextView) addAssignmentDialog.findViewById(R.id.text_content_due_date_week);
        contentDueDateText = (TextView) addAssignmentDialog.findViewById(R.id.text_content_due_date);

        cancelContainer = (LinearLayout) addAssignmentDialog.findViewById(R.id.container_cancel);
        addContainer = (LinearLayout) addAssignmentDialog.findViewById(R.id.container_add);

        cancelText = (TextView) addAssignmentDialog.findViewById(R.id.text_cancel);
        addText = (TextView) addAssignmentDialog.findViewById(R.id.text_add);

        assignmentIdView = (View) addAssignmentDialog.findViewById(R.id.view_assignment_id);
    }

    // 반 탭을 설정한다.
    private void setClassTab() {

        // 반 제목 탭의 버튼이 클릭되면
        // -> 반 내용 탭을 설정한다.
        setOnCheckedChangeListenerToTitleClassRadioGroup();

        // 반 내용 탭의 버튼이 클릭되면
        // -> 학생 탭을 설정한다.
        setOnClickListenerToContentClassRadioBtn();
    }

    // 반 제목 탭의 버튼이 클릭되면
    // -> 반 내용 탭을 설정한다.
    private void setOnCheckedChangeListenerToTitleClassRadioGroup() {

        titleClassRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 반 내용 탭을 비운다.
                        contentClassRadioGroup.removeAllViews();
                    }
                });

                switch (resId) {

                    // 반 버튼
                    case R.id.radio_btn_title_class:

                        // 반 제목 라디오버튼을 체크한 경우
                        if (titleClassRadioBtn.isChecked()) {

                            // 반 버튼을 추가한다.
                            addClassBtn();
                        }

                        break;

                    // 학년 버튼
                    case R.id.radio_btn_title_grade:

                        // 학년 제목 라디오버튼을 체크한 경우
                        if (titleGradeRadioBtn.isChecked()) {

                            // 학년 버튼을 추가한다.
                            addGradeBtn();
                        }
                }

                // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                resetViews(radioGroup.getId());
            }
        });
    }

    // 반 버튼을 추가한다.
    private void addClassBtn() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 반 내용 탭을 비운다.
                contentClassRadioGroup.removeAllViews();
            }
        });

        threadPool.execute(new Runnable() {

            private List<Classroom> classrooms;

            @Override
            public void run() {

                // 수업 리스트를 얻는다.
                getClassrooms();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 수업 리스트가 null인지 0.2초간 확인한다.
                        if (isClassroomsNullForNms(200L)) {

                            Log.d("error", "failed to initialize classrooms");
                            return;
                        };

                        // 수업 이름 리스트를 얻는다.
                        List<String> classroomNames = classrooms.stream()
                                .map(classroom -> classroom.getName())
                                .distinct()
                                .collect(Collectors.toList());

                        for (String classroomName : classroomNames) {

                            // 수업 버튼을 얻는다.
                            RadioButton classRadioBtn = getClassRadioBtn(classroomName);

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 반 탭에 반 버튼을 추가한다.
                                    contentClassRadioGroup.addView(classRadioBtn);
                                }
                            });
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                // 반 탭을 보이게 한다.
                                contentClassRadioGroup.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }

            // 수업 리스트를 얻는다.
            private void getClassrooms() {

                Call<List<Classroom>> call = managementApi.list(teacher.getId());

                call.enqueue(new Callback<List<Classroom>>() {

                    @Override
                    public void onResponse(Call<List<Classroom>> call, Response<List<Classroom>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

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

                return true;
            }
        });
    }

    // 수업 버튼을 반환한다.
    private RadioButton getClassRadioBtn(String classroomName) {

        RadioButton classRadioBtn = new RadioButton(addAssignmentDialog.getContext());

        classRadioBtn.setText(String.valueOf(classroomName));
        classRadioBtn.setTextSize(15);
        classRadioBtn.setTextColor(addAssignmentDialog.getContext().
                getResources().getColorStateList(R.color.selector_text, null));
        classRadioBtn.setBackgroundResource(R.drawable.selector_radio);
        classRadioBtn.setButtonDrawable(R.color.transparent);
        classRadioBtn.setElevation((int) (5 * dp));
        classRadioBtn.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        LinearLayout.LayoutParams classParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        classParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        classRadioBtn.setLayoutParams(classParams);

        return classRadioBtn;
    }

    // 학년 버튼을 추가한다.
    private void addGradeBtn() {

        threadPool.execute(new Runnable() {

            private List<Student> students;

            @Override
            public void run() {

                // 학생 리스트를 얻는다.
                getStudents();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 학생 리스트가 null인지 0.2초간 확인한다.
                        if (isStudentsNullForNms(200L)) {

                            Log.d("error", "failed to initialize students");
                            return;
                        }

                        // 학생 리스트로부터 학년 리스트를 얻는다.
                        List<Integer> grades = students.stream()
                                .map(student -> student.getGrade())
                                .distinct()
                                .collect(Collectors.toList());

                        for (Integer grade : grades) {

                            // 학년 버튼을 얻는다.
                            RadioButton gradeRadioBtn = getGradeRadioBtn(grade);

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 반 내용 탭에 학년 버튼을 추가한다.
                                    contentClassRadioGroup.addView(gradeRadioBtn);
                                }
                            });
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 반 내용 탭을 보이게 한다.
                                contentClassRadioGroup.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }

            // 학생 리스트를 얻는다.
            private void getStudents() {

                Call<List<Student>> call = teacherAssignmentApi.listStudents(teacher.getId());

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

    // 학년 버튼을 반환한다.
    private RadioButton getGradeRadioBtn(Integer grade) {

        RadioButton gradeRadioBtn = new RadioButton(addAssignmentDialog.getContext());

        gradeRadioBtn.setText(ConversionService.gradeToStr(grade));
        gradeRadioBtn.setTextSize(15);
        gradeRadioBtn.setTextColor(addAssignmentDialog.getContext().
                getResources().getColorStateList(R.color.selector_text, null));
        gradeRadioBtn.setBackgroundResource(R.drawable.selector_radio);
        gradeRadioBtn.setButtonDrawable(R.color.transparent);
        gradeRadioBtn.setElevation((int) (5 * dp));
        gradeRadioBtn.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        LinearLayout.LayoutParams gradeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        gradeParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        gradeRadioBtn.setLayoutParams(gradeParams);

        return gradeRadioBtn;
    }

    // 반 내용 탭의 버튼이 클릭되면
    // -> 학생 탭을 설정한다.
    private void setOnClickListenerToContentClassRadioBtn() {

        contentClassRadioGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {

                child.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 학생 내용 탭을 비운다.
                                contentStudentContainer.removeAllViews();
                            }
                        });

                        // 학생 탭에 학생 체크박스를 설정한다.
                        setStudentChecks(view);

                        // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                        resetViews(parent.getId());
                    }
                });
            }

            @Override
            public void onChildViewRemoved(View view, View view1) {}

            // 학생 탭에 학생 체크박스를 설정한다.
            private void setStudentChecks(View view) {

                // 클릭한 버튼을 얻는다.
                RadioButton contentClassRadioBtn = (RadioButton) view;

                // 처음 클릭된 경우
                if (contentClassRadioBtn != clickedContentClass) {

                    // clickedContentClass을 업데이트한다.
                    clickedContentClass = contentClassRadioBtn;

                    switch (titleClassRadioGroup.getCheckedRadioButtonId()) {

                        case R.id.radio_btn_title_class:

                            // 학생 내용 탭에 특정 반의 학생 체크박스를 추가한다.
                            addStudentInAClassroomCheck();
                            break;

                        case R.id.radio_btn_title_grade:

                            // 학생 내용 탭에 특정 학년의 학생 체크박스를 추가한다.
                            addStudentInAGradeCheck();
                    }

                    // TODO : 애니메이션 적용해서 자연스럽게 내려가도록 하기
                    // 학생 탭을 보이게 한다.
                    studentContainer.setVisibility(View.VISIBLE);
                }
                // 다시 클릭된 경우
                else {

                    // 수업 내용 탭을 체크 해제한다.
                    contentClassRadioGroup.clearCheck();

                    // clickedContentClass을 업데이트한다.
                    clickedContentClass = null;
                }
            }
        });
    }

    // 학생 내용 탭에 특정 반의 학생 체크박스를 추가한다.
    private void addStudentInAClassroomCheck() {

        threadPool.execute(new Runnable() {

            List<Student> studentsInAClassroom;

            @Override
            public void run() {

                // 학생 리스트를 얻는다.
                getStudentsInAClassroom();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 학생 리스트가 0.2초 이내에 초기화되지 않으면
                        if (isStudentsNullForNms(200L)) {

                            Log.d("error", "failed to initialize students");
                            return;
                        }

                        for (Student student : studentsInAClassroom) {

                            // 학생 체크박스를 얻는다.
                            CheckBox studentCheck = getStudentCheck(student);

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 학생 내용 탭에 학생 체크박스를 추가한다.
                                    contentStudentContainer.addView(studentCheck);
                                }
                            });
                        }
                    }
                });
            }

            // 학생 리스트를 반환한다.
            private void getStudentsInAClassroom() {

                Call<List<Student>> call =
                        teacherAssignmentApi.list(String.valueOf(clickedContentClass.getText()), teacher.getId());

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
        });
    }

    // 학생 체크박스를 반환한다.
    private CheckBox getStudentCheck(Student student) {

        CheckBox studentCheck = new CheckBox(addAssignmentDialog.getContext());

        studentCheck.setTag(student);

        studentCheck.setText(String.valueOf(student.getName()));
        studentCheck.setTextSize(15);
        studentCheck.setTextColor(addAssignmentDialog.getContext().
                getResources().getColorStateList(R.color.selector_text, null));
        studentCheck.setBackgroundResource(R.drawable.selector_radio);
        studentCheck.setButtonDrawable(R.color.transparent);
        studentCheck.setElevation((int) (5 * dp));
        studentCheck.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        checkParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        studentCheck.setLayoutParams(checkParams);

        return studentCheck;
    }

    // 학생 내용 탭에 특정 학년의 학생 체크박스를 추가한다.
    private void addStudentInAGradeCheck() {

        threadPool.execute(new Runnable() {

            private List<Student> students;

            @Override
            public void run() {

                // teacherId와 subject를 이용해 학생 리스트를 얻는다.
                getStudents();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 학생 리스트가 null인지 0.2초간 확인한다.
                        if (isStudentsNullForNms(200L)) {

                            Log.d("error", "failed to initialize students");
                            return;
                        }

                        // 특정 학년의 학생 리스트를 반환한다.
                        List<Student> studentsOfAGrade = getStudentsOfAGrade(students);

                        // 학생 리스트를 순회하며 체크박스를 만든다.
                        for (Student student : studentsOfAGrade) {

                            CheckBox studentCheck = getStudentCheck(student);

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 학생 내용 탭에 학생 체크박스를 추가한다.
                                    contentStudentContainer.addView(studentCheck);
                                }
                            });
                        }
                    }
                });
            }

            // 학생 리스트를 반환한다.
            private void getStudents() {

                Call<List<Student>> call = teacherAssignmentApi.listStudents(teacher.getId());

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

    // 특정 학년의 학생 리스트를 반환한다.
    private List<Student> getStudentsOfAGrade(List<Student> students) {

        // clickedContentClass로부터 학년을 얻는다.
        Integer grade = ConversionService.strToGrade(
                String.valueOf(clickedContentClass.getText()));

        List<Student> studentsOfAGrade = students.stream()
                .filter(student -> student.getGrade().equals(grade))
                .collect(Collectors.toList());

        return studentsOfAGrade;
    }

    // 학생 탭을 설정한다.
    private void setStudentTab() {

        // 학생 체크박스가 클릭되면
        // -> 교재 탭을 설정한다.
        setOnClickListenerToStudentCheck();
    }

    // 학생 체크박스가 클릭되면
    // -> 교재 탭을 설정한다.
    private void setOnClickListenerToStudentCheck() {

        contentStudentContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {

                child.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 내용 탭을 비운다.
                                contentBookRadioGroup.removeAllViews();
                            }
                        });

                        // 교재 버튼을 설정한다.
                        setBookRadioBtn();

                        // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                        resetViews(parent.getId());
                    }
                });
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {}
        });
    }

    // 교재 버튼을 설정한다.
    private void setBookRadioBtn() {

        threadPool.execute(new Runnable() {

            private List<Book> books;

            private int numberOfCheckedStudentCheck;
            private int numberOfTimesBooksAreFiltered;

            @Override
            public void run() {

                // 교재 리스트를 얻는다.
                getBooks();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재 리스트가 초기화되었는지 확인한다.
                        if (isBooksNotInitializedForNms(200L)) {

                            Log.d("error", "failed to initialize : books");
                            return;
                        }

                        // 학생 리스트를 순회하며 버튼을 만든다.
                        for (Book book : books) {

                            // 교재 라디오버튼을 반환한다.
                            RadioButton bookRadioBtn = getBookRadioBtn(book);

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 교재 라디오버튼을 추가한다.
                                    contentBookRadioGroup.addView(bookRadioBtn);
                                }
                            });
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 체크한 뷰가 남아있는 경우
                                if (numberOfCheckedStudentCheck != 0) {

                                    // TODO : 애니메이션 적용해서 자연스럽게 내려가도록 하기
                                    // 교재 탭을 보이게 한다.
                                    bookContainer.setVisibility(View.VISIBLE);
                                }
                                // 모든 뷰를 체크 해제한 경우
                                else {

                                    // 교재 탭을 보이지 않게 한다.
                                    bookContainer.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                });
            }

            // 교재 리스트를 얻는다.
            private void getBooks() {

                // 학생 내용 탭에서 체크한 학생 체크박스의 수를 얻는다.
                for (int i = 0; i < contentStudentContainer.getChildCount(); i++)
                    if (((CheckBox) contentStudentContainer.getChildAt(i)).isChecked())
                        numberOfCheckedStudentCheck++;

                // 체크한 학생 체크박스가 없다면
                if (numberOfCheckedStudentCheck == 0) {
                    books = new ArrayList<>();
                }
                else {

                    for (int i = 0; i < contentStudentContainer.getChildCount(); i++) {

                        CheckBox studentCheck = (CheckBox) contentStudentContainer.getChildAt(i);

                        if (studentCheck.isChecked()) {

                            // 체크한 학생 체크박스에 해당하는 학생 아이디를 얻는다.
                            Long studentId = ((Student) studentCheck.getTag()).getId();

                            threadPool.execute(new Runnable() {

                                private List<Book> booksOfAStudent;

                                @Override
                                public void run() {

                                    // 특정 학생의 교재 리스트를 얻는다.
                                    getBooksOfAStudent(studentId);

                                    threadPool.execute(new Runnable() {

                                        @Override
                                        public void run() {

                                            // 특정 학생의 교재 리스트가 0.2초 이내에 초기화되지 않으면
                                            if (isBooksOfAStudentNullForNms(200L)) {

                                                Log.d("error", "failed to initialize booksOfAStudent");
                                                return;
                                            }

                                            // 교재 리스트에서 공통되는 교재만 남긴다.
                                            filterBooks(booksOfAStudent);
                                        }
                                    });
                                }

                                // 특정 학생의 교재 리스트를 얻는다.
                                private void getBooksOfAStudent(Long studentId) {

                                    Call<List<Book>> call = timerApi.list(studentId);

                                    call.enqueue(new Callback<List<Book>>() {

                                        @Override
                                        public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {

                                            if (!response.isSuccessful()) {

                                                Log.d("error", "code : " + response.code());
                                                return;
                                            }

                                            booksOfAStudent = response.body();
                                        }

                                        @Override
                                        public void onFailure(Call<List<Book>> call, Throwable t) {
                                            Log.d("error", t.getMessage());
                                        }
                                    });
                                }

                                // 특정 학생의 교재 리스트가 null인지 0.00n초간 확인한다.
                                private boolean isBooksOfAStudentNullForNms(long n) {

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

                                        if (booksOfAStudent == null) continue;

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
                    }
                }
            }

            // 교재 리스트에서 공통되는 교재만 남긴다.
            private synchronized void filterBooks(List<Book> booksOfAStudent) {

                // 첫째 학생이라면
                if (books == null) {
                    // 해당 학생의 교재 리스트를 모두 추가한다.
                    books = booksOfAStudent;
                }
                // 첫째 이후의 학생이라면
                else {

                    // 교재 리스트에서 해당 학생의 교재 리스트에 있는 교재만 남긴다.
                    books = books.stream()
                            .filter(book -> booksOfAStudent.contains(book))
                            .collect(Collectors.toList());
                }

                numberOfTimesBooksAreFiltered++;
            }

            // 교재 리스트가 초기화되었는지 0.00n초간 확인한다.
            private boolean isBooksNotInitializedForNms(long n) {

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

                    if (numberOfCheckedStudentCheck != numberOfTimesBooksAreFiltered) continue;

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

    // 교재 라디오버튼을 반환한다.
    private RadioButton getBookRadioBtn(Book book) {

        RadioButton bookRadioBtn = new RadioButton(addAssignmentDialog.getContext());

        bookRadioBtn.setTag(book);

        bookRadioBtn.setText(String.valueOf(book.getName()));
        bookRadioBtn.setTextSize(15);
        bookRadioBtn.setTextColor(addAssignmentDialog.getContext().
                getResources().getColorStateList(R.color.selector_text, null));
        bookRadioBtn.setBackgroundResource(R.drawable.selector_radio);
        bookRadioBtn.setButtonDrawable(R.color.transparent);
        bookRadioBtn.setElevation((int) (5 * dp));
        bookRadioBtn.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        LinearLayout.LayoutParams radioParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        radioParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        bookRadioBtn.setLayoutParams(radioParams);

        return bookRadioBtn;
    }

    // 교재 탭을 설정한다.
    private void setBookTab() {

        // 교재 직접 입력 체크박스가 클릭되면
        // -> 교재 직접 입력 에딧을 보이게 한다.
        setOnCheckedChangeListenerToTypeMySelfCheck();

        // 교재 직접 입력 에디트가 포커스를 얻으면
        // -> 에디트의 텍스트를 없앤다.
        setOnFocusListenerToTypeMyselfEdit();

        // 교재 직접 입력 에디트에 입력을 마치면
        // -> 범위 에딧에 포커스를 준다.
        setOnEditorActionListenerToTypeMySelfEdit();

        // 교재 라디오버튼이 클릭되면
        // -> 범위 탭과 기한 탭을 보이게 한다.
        setOnClickListenerToBookRadioBtn();
    }

    // 교재 직접 입력 체크박스가 클릭되면
    // -> 교재 직접 입력 에딧을 보이게 한다.
    private void setOnCheckedChangeListenerToTypeMySelfCheck() {

        typeMyselfCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {

                    // 교재 내용 탭을 보이지 않게 한다.
                    contentBookRadioGroup.setVisibility(View.GONE);

                    // 교재 직접 입력 에딧을 보이게 한다.
                    typeMyselfEdit.setVisibility(View.VISIBLE);
                }
                else {

                    // 교재 내용 탭을 보이게 한다.
                    contentBookRadioGroup.setVisibility(View.VISIBLE);

                    // 교재 직접 입력 에딧을 보이게 한다.
                    typeMyselfEdit.setVisibility(View.GONE);
                }

                // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                resetViews(compoundButton.getId());
            }
        });
    }

    // 교재 직접 입력 에디트가 포커스를 얻으면
    // -> 에디트의 텍스트를 없앤다.
    private void setOnFocusListenerToTypeMyselfEdit() {

        typeMyselfEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View view, boolean hasFocus) {

                if (hasFocus || String.valueOf(contentRangeFromEdit.getText()).equals(""))
                    contentRangeFromEdit.setText(null);

                if (!hasFocus && !contentRangeFromEdit.hasFocus() && !contentRangeToEdit.hasFocus()) {

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager immhide = (InputMethodManager) addAssignmentDialog.getContext()
                            .getSystemService(Activity.INPUT_METHOD_SERVICE);
                    immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                }
            }
        });
    }

    // 교재 직접 입력 에디트에 입력을 마치면
    // -> 범위 에딧에 포커스를 준다.
    private void setOnEditorActionListenerToTypeMySelfEdit() {

        typeMyselfEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키 또는 완료 키가 입력되었다면
                if (keyCode == EditorInfo.IME_ACTION_NEXT || keyCode == EditorInfo.IME_ACTION_DONE) {

                    if (!String.valueOf(typeMyselfEdit.getText()).equals("")) {

                        // 범위 탭과 기한 탭을 보이게 한다.
                        rangeContainer.setVisibility(View.VISIBLE);
                        dueDateContainer.setVisibility(View.VISIBLE);

                        // 범위 시작 에디트에 포커스를 준다.
                        contentRangeFromEdit.setFocusableInTouchMode(true);
                        contentRangeFromEdit.requestFocus();
                    }
                    else {
                        // 교재 직접 입력 에딧에 포커스를 준다.
                        typeMyselfCheck.setFocusableInTouchMode(true);
                        typeMyselfCheck.requestFocus();
                    }
                }

                return true;
            }
        });
    }

    // 교재 라디오버튼이 클릭되면
    // -> 범위 탭과 기한 탭을 보이게 한다.
    private void setOnClickListenerToBookRadioBtn() {

        contentBookRadioGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {

                RadioButton bookRadioBtn = (RadioButton) child;

                bookRadioBtn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        // 처음 클릭된 경우
                        if (bookRadioBtn != clickedBook) {

                            // 범위 탭과 기한 탭을 보이게 한다.
                            rangeContainer.setVisibility(View.VISIBLE);
                            dueDateContainer.setVisibility(View.VISIBLE);

                            // clicked를 업데이트한다.
                            clickedBook = bookRadioBtn;
                        }
                        // 같은 버튼이 다시 클릭된 경우
                        else {

                            // 체크된 뷰를 체크 해제한다.
                            contentBookRadioGroup.clearCheck();

                            // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                            resetViews(parent.getId());

                            // clicked를 업데이트한다.
                            clickedBook = null;
                        }
                    }
                });
            }

            @Override
            public void onChildViewRemoved(View view, View view1) {}
        });
    }

    // 범위 탭을 설정한다.
    private void setRangeTab() {

        // 범위 에디트가 포커스를 얻으면
        // -> 에디트의 텍스트를 없앤다.
        setOnFocusListenerToContentRangeEdits();

        // 범위 시작 에디트에 입력을 마치면
        // -> 범위 끝 에디트에 포커스를 주어 바로 입력할 수 있게 한다.
        setOnEditorActionListenerToContentRangeEdits();
    }

    // 범위 에디트가 포커스를 얻으면
    // -> 에디트의 텍스트를 없앤다.
    private void setOnFocusListenerToContentRangeEdits() {

        contentRangeFromEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View view, boolean hasFocus) {

                if (hasFocus || String.valueOf(contentRangeFromEdit.getText()).equals(""))
                    contentRangeFromEdit.setText(null);

                if (!hasFocus && !typeMyselfEdit.hasFocus() && !contentRangeToEdit.hasFocus()) {

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager immhide = (InputMethodManager) addAssignmentDialog.getContext()
                            .getSystemService(Activity.INPUT_METHOD_SERVICE);
                    immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                }
            }
        });

        contentRangeToEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View view, boolean hasFocus) {

                if (hasFocus || String.valueOf(contentRangeToEdit.getText()).equals(""))
                    contentRangeToEdit.setText(null);

                if (!hasFocus && !typeMyselfEdit.hasFocus() && !contentRangeFromEdit.hasFocus()) {

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager immhide = (InputMethodManager) addAssignmentDialog.getContext().
                            getSystemService(Activity.INPUT_METHOD_SERVICE);
                    immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                }
            }
        });
    }

    // 범위 시작 에디트에 입력을 마치면
    // -> 범위 끝 에디트에 포커스를 주어 바로 입력할 수 있게 한다.
    private void setOnEditorActionListenerToContentRangeEdits() {

        contentRangeFromEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키 또는 완료 키가 입력되었다면
                if (keyCode == EditorInfo.IME_ACTION_NEXT || keyCode == EditorInfo.IME_ACTION_DONE) {

                    // 범위 끝 에디트에 포커스를 준다.
                    contentRangeToEdit.setFocusableInTouchMode(true);
                    contentRangeToEdit.requestFocus();

                    // 추가 버튼의 enabled를 설정한다.
                    setEnabledToAddBtn();
                }

                return true;
            }
        });

        contentRangeToEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키 또는 완료 키가 입력되었다면
                if (keyCode == EditorInfo.IME_ACTION_NEXT || keyCode == EditorInfo.IME_ACTION_DONE) {

                    // 기한 내용 텍스트에 포커스를 준다.
                    contentDueDateText.setFocusableInTouchMode(true);
                    contentDueDateText.requestFocus();

                    // 추가 버튼의 enabled를 설정한다.
                    setEnabledToAddBtn();
                };

                return true;
            }
        });
    }

    // 추가 버튼의 enabled를 설정한다.
    private void setEnabledToAddBtn() {

        String from = String.valueOf(contentRangeFromEdit.getText());
        String to = String.valueOf(contentRangeToEdit.getText());

        // 범위 에디트가 모두 입력되었고
        if (from != null && !from.equals("") && to != null && !to.equals("")) {

            // 범위가 적절하게 입력되었다면
            if (Integer.parseInt(from) <= Integer.parseInt(to)) {
                setEnabledToAddContainer(true);
            }
            // 범위가 적절하게 입력되지 않았다면
            else {
                Toast.makeText(addAssignmentDialog.getContext(),
                        "범위가 제대로 입력되지 않았습니다." +
                                "\n입력된 범위 : " + Integer.parseInt(from) +
                                " ~ " + Integer.parseInt(to) + "P",
                        Toast.LENGTH_SHORT).show();

                contentRangeFromEdit.setText(null);
                contentRangeToEdit.setText(null);

                setEnabledToAddContainer(false);
            }
        }
        else {
            setEnabledToAddContainer(false);
        }
    }

    // 추가 버튼의 enabled를 설정한다.
    private void setEnabledToAddContainer(boolean b) {

        handler.post(new Runnable() {

            @Override
            public void run() {

                // enabled를 true로 설정하는 경우
                if (b) {

                    addContainer.setEnabled(true);
                    addText.setTextColor(Color.BLACK);
                    addContainer.setBackgroundResource(R.drawable.shape_btn_soft_blue);
                }
                // enabled를 false로 설정하는 경우
                else {

                    addContainer.setEnabled(false);
                    addText.setTextColor(addAssignmentDialog.getContext().
                            getResources().getColor(R.color.disabled, null));
                    addContainer.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
                }
            }
        });
    }

    // 기한 탭을 설정한다.
    private void setDueDateText() {

        // 기한 주 텍스트에 문자열을 입력한다.
        contentDueDateWeekText.setText("이번주");

        // 기한 텍스트를 오늘로 설정한다.
        contentDueDateText.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일")));

        // 기한 데이트피커를 오늘로 설정한다.
        updateDatePicker(contentDueDateDatePicker, LocalDate.now());

        // 기한 텍스트를 클릭하면
        // -> 기한 데이트피커의 visibility를 설정한다.
        setOnClickListenerToDueDateText();

        // 기한 데이트피커의 날짜가 바뀌면
        // -> 기한 텍스트를 해당 날짜로 설정한다.
        setOnDateChangedListenerToDatePicker(contentDueDateDatePicker);
    }

    // 기한 데이트피커를 특정 날짜로 설정한다.
    private void updateDatePicker(DatePicker datePicker, LocalDate date) {

        handler.post(new Runnable() {
            @Override
            public void run() {

                datePicker.updateDate(
                        date.getYear(),
                        date.getMonthValue() - 1,
                        date.getDayOfMonth());
            }
        });
    }

    // 기한 텍스트를 클릭하면
    // -> 기한 데이트피커의 visibility를 설정한다.
    private void setOnClickListenerToDueDateText() {

        contentDueDateText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                setVisibilityOnDatePicker();
            }
        });
    }

    // 기한 데이트피커의 visibility를 설정한다.
    private void setVisibilityOnDatePicker() {

        // 기한 텍스트가 처음 클릭된 경우
        if (contentDueDateDatePicker.getVisibility() == View.GONE) {

            // 기한 데이트피커를 보이게 한다.
            contentDueDateDatePicker.setVisibility(View.VISIBLE);
        }
        // 기한 텍스트가 다시 클릭된 경우
        else {

            // 기한 데이트피커를 보이지 않게 한다.
            contentDueDateDatePicker.setVisibility(View.GONE);
        }
    }

    // 기한 데이트피커의 날짜가 바뀌면
    // -> 기한 텍스트를 해당 날짜로 설정한다.
    private void setOnDateChangedListenerToDatePicker(DatePicker contentDueDateDatePicker) {

        contentDueDateDatePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int day) {

                // month가 1부터 12 사이의 값을 가지게 한다.
                month++;

                LocalDate datePicked = LocalDate.of(year, month, day);
                LocalDate today = LocalDate.now();

                // 오늘과 고른 날짜의 주 차이를 구한다.
                int weekDifference = getWeekDifference(datePicked, today, datePicker);

                // 기한 텍스트에 날짜와 주 차이를 설정한다.
                setTextToDueDateText(datePicked, weekDifference);

                // 추가 버튼의 enabled를 설정한다.
                setEnabledToAddBtn();
            }
        });
    }

    // 오늘과 고른 날짜의 주 차이를 구한다.
    private int getWeekDifference(LocalDate datePicked, LocalDate today, DatePicker datePicker) {

        if (datePicked.isBefore(LocalDate.now())) {

            handler.post(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(addAssignmentDialog.getContext(), "과제 기한은 오늘부터 설정될 수 있습니다.", Toast.LENGTH_SHORT).show();
                }
            });

            updateDatePicker(datePicker, LocalDate.now());
        }

        // 오늘과 고른 날짜의 주 차이를 구한다.
        int weekDifference = 0;

        // 오늘과 고른 날짜가 같은 해라면
        if (datePicked.getYear() == today.getYear()) {

            weekDifference += getWeekOfYear(datePicked) - getWeekOfYear(today);

        }
        // 오늘과 고른 날짜가 다른 해라면
        else if (datePicked.getYear() > today.getYear()) {

            // 이번 연도부터 고른 날짜의 연도까지 순회한다.
            for (int y = today.getYear(); y <= datePicked.getYear(); y++) {

                // 해당 연도가 몇 주인지 계산한다.
                LocalDate lastDateOfThisYear = LocalDate.of(y, 12, 31);
                int weekOfThisYear = getWeekOfYear(lastDateOfThisYear);

                // 이번 연도를 순회할 때는
                if (y == today.getYear()) {

                    weekDifference += weekOfThisYear - getWeekOfYear(today);

                    // 해당 연도의 마지막 날이 토요일이라면
                    // -> 해당 연도의 마지막 주와 다음 연도의 첫 주는 다른 주이다.
                    if (lastDateOfThisYear.getDayOfWeek() == DayOfWeek.SATURDAY)
                        weekDifference++;
                }
                // 고른 날짜의 연도를 순회할 때는
                else if (y == datePicked.getYear()) {

                    weekDifference += getWeekOfYear(datePicked) - 1;

                }
                else {

                    weekDifference += weekOfThisYear;

                    // 해당 연도의 마지막 날이 토요일이 아니라면
                    // -> 해당 연도의 마지막 주와 다음 연도의 첫 주는 다른 주이다.
                    if (lastDateOfThisYear.getDayOfWeek() != DayOfWeek.SATURDAY)
                        weekDifference--;
                }
            }
        }

        return weekDifference;
    }

    // LocalDate 타입 변수가 해당 연도의 몇째 주인지 구한다.
    private int getWeekOfYear(LocalDate date) {

        int weekOfYear = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());

        // 만약 12월의 마지막 주가 아니라면 주차를 반환한다.
        // 만약 12월의 마지막 주인 경우 전 주의 주차를 구하고, 구한 값에 1을 더하여 반환한다.
        return (!(date.getMonth() == Month.DECEMBER && weekOfYear == 1)) ?
                weekOfYear : getWeekOfYear(date.minusWeeks(1)) + 1;
    }

    // 기한 텍스트에 날짜와 주 차이를 설정한다.
    private void setTextToDueDateText(LocalDate datePicked, int weekDifference) {

        // 기한 텍스트에 설정할 문자열을 얻는다.
        String whatWeekOfMonth;

        switch (weekDifference) {
            case 0: whatWeekOfMonth = "이번주"; break;
            case 1: whatWeekOfMonth = "다음주"; break;
            case 2: whatWeekOfMonth = "다다음주"; break;
            default:
                whatWeekOfMonth = datePicked.getMonthValue() + "월 ";

                switch (toGregorianCalendar(datePicked).get(Calendar.WEEK_OF_MONTH)) {
                    case 1: whatWeekOfMonth += "첫째주"; break;
                    case 2: whatWeekOfMonth += "둘째주"; break;
                    case 3: whatWeekOfMonth += "셋째주"; break;
                    case 4: whatWeekOfMonth += "넷째주"; break;
                    case 5: whatWeekOfMonth += "다섯째주"; break;
                    case 6: whatWeekOfMonth += "여섯째주";
                }
        }

        // 기한 주 텍스트에 문자열을 입력한다.
        contentDueDateWeekText.setText(whatWeekOfMonth);

        // 기한 텍스트에 날짜를 입력한다.
        contentDueDateText.setText(String.format("%d년 %d월 %d일 (%s)요일",
                datePicked.getYear(),
                datePicked.getMonthValue(),
                datePicked.getDayOfMonth(),
                datePicked.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN)));
    }

    // LocalDate 타입 변수를 GregorianCalendar 타입으로 변환한다.
    private GregorianCalendar toGregorianCalendar(LocalDate date) {

        return new GregorianCalendar(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    // 취소 버튼을 설정한다.
    private void setCancelContainer() {

        cancelContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 대화상자를 보이지 않게 한다.
                addAssignmentDialog.dismiss();

                // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                resetViews(view.getId());

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 반 내용 탭을 비운다.
                        contentClassRadioGroup.removeAllViews();

                        // 학생 내용 탭을 비운다.
                        contentStudentContainer.removeAllViews();

                        // 교재 내용 탭을 비운다.
                        contentBookRadioGroup.removeAllViews();
                    }
                });
            }
        });
    }

    // 추가 버튼을 설정한다.
    private void setAddContainer() {

        addContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                threadPool.execute(new Runnable() {

                    private List<Assignment> assignments = new ArrayList<>();

                    @Override
                    public void run() {

                        // 대화상자로부터 저장할 과제를 얻는다.
                        getAssignmentsFromDialog(assignments);

                        // 과제를 저장한다.
                        saveAssignments(assignments);

                        // 강사 과제 어댑터에 아이템을 설정한다.
                        setItemToAdapter(assignments);

                        // 대화 상자를 보이지 않게 한다.
                        addAssignmentDialog.dismiss();

                        // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                        resetViews(view.getId());
                    }
                });
            }
        });
    }

    // 대화상자로부터 저장할 과제를 얻는다.
    private void getAssignmentsFromDialog(List<Assignment> assignments) {

        // 기존에 있던 과제의 아이디 리스트를 얻는다.
        List<Long> assignmentIds = (List<Long>) assignmentIdView.getTag();

        // 과제를 수정하는 경우
        if (assignmentIds != null)
            // 기존에 있던 과제를 삭제한다.
            assignmentIds.forEach(assignmentId -> deleteAssignment(assignmentId));

        for (int i = 0; i < contentStudentContainer.getChildCount(); i++) {

            if (!((CheckBox) contentStudentContainer.getChildAt(i)).isChecked()) continue;

            assignments.add(new Assignment(
                    null, // id
                    ((Book) addAssignmentDialog
                            .findViewById(contentBookRadioGroup.getCheckedRadioButtonId()).getTag()).getId(), // bookId
                    (typeMyselfCheck.isChecked()) ? String.valueOf(typeMyselfEdit.getText()) : null, // typedBookName
                    Integer.parseInt(String.valueOf(contentRangeFromEdit.getText())), // pageFrom
                    Integer.parseInt(String.valueOf(contentRangeToEdit.getText())), // pageTo
                    null, // numberFrom
                    null, // numberTo
                    LocalDate.of(contentDueDateDatePicker.getYear(),
                            contentDueDateDatePicker.getMonth() + 1,
                            contentDueDateDatePicker.getDayOfMonth()), // dueDate
                    LocalDateTime.now(), // assignedAt
                    (titleClassRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_title_class) ?
                            String.valueOf(((RadioButton) addAssignmentDialog
                                    .findViewById(contentClassRadioGroup.getCheckedRadioButtonId())).getText()) : null, // classroomName
                    ((Student) contentStudentContainer.getChildAt(i).getTag()).getId(), // studentId
                    teacher.getId()
            ));
        }
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
            }

            @Override
            public void onFailure(Call<Assignment> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 과제를 저장한다.
    private void saveAssignments(List<Assignment> assignments) {

        List<Call<Assignment>> calls = assignments.stream()
                .map(assignment -> teacherAssignmentApi.update(-1L, assignment))
                .collect(Collectors.toList());

        for (Call<Assignment> call : calls) {

            call.enqueue(new Callback<Assignment>() {

                @Override
                public void onResponse(Call<Assignment> call, Response<Assignment> response) {

                    if (!response.isSuccessful()) {

                        Log.d("error", "code : " + response.code());
                        return;
                    }

                    if (calls.indexOf(call) != 0) return;

                    // 과제 추가
                    if (assignmentIdView.getTag() == null) {
                        Toast.makeText(addAssignmentDialog.getContext(), "과제가 출제되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                    // 과제 수정
                    else {
                        Toast.makeText(addAssignmentDialog.getContext(), "과제를 수정했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Assignment> call, Throwable t) {
                    Log.d("error", t.getMessage());
                }
            });
        }
    }

    // 강사 과제 어댑터에 아이템을 설정한다.
    private void setItemToAdapter(List<Assignment> assignments) {

        threadPool.execute(new Runnable() {

            private TeacherAssignment item;

            @Override
            public void run() {

                // 강사 과제를 얻는다.
                getItem(assignments);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        if (isItemNullForNms(200L)) {

                            Log.d("error", "failed to initialize : item");
                            return;
                        }

                        // 과제를 수정하는 경우
                        if (assignmentIdView.getTag() != null) {
                            // 해당 아이템을 삭제한다.
                            adapter.removeItem(position);
                        }

                        // 강사 과제를 강사 과제 어댑터에 추가한다.
                        adapter.addItem(item);

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

            // 강사 과제를 얻는다.
            private void getItem(List<Assignment> assignments) {

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
                                if (isBookNullForNms(500L)) {

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

                                item = new TeacherAssignment(
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
            private boolean isItemNullForNms(long n) {

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

                    if (item == null) continue;

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

    // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
    private void resetViews(int resId) {

        handler.post(new Runnable() {

            @Override
            public void run() {

                switch (resId) {

                    case R.id.container_cancel:
                    case R.id.container_add:

                        // 반 제목 탭을 체크 해제한다.
                        titleClassRadioGroup.clearCheck();

                        // 반 내용 탭을 보이지 않게 한다.
                        contentClassRadioGroup.setVisibility(View.GONE);

                        // 반 내용 탭을 체크 해제한다.
                        contentClassRadioGroup.clearCheck();

                    case R.id.radio_group_title_class:

                        // 학생 탭을 안 보이게 한다.
                        studentContainer.setVisibility(View.GONE);

                    case R.id.radio_group_content_class:

                        // 학생 탭의 모든 뷰를 체크 해제한다.
                        for (int i = 0; i < contentStudentContainer.getChildCount(); i++) {

                            CheckBox studentCheck = (CheckBox) contentStudentContainer.getChildAt(i);

                            studentCheck.setChecked(false);
                        }

                        // 교재 탭을 안 보이게 한다.
                        bookContainer.setVisibility(View.GONE);

                    case R.id.container_content_student:
                    case R.id.check_type_myself:

                        // 교재 탭의 모든 뷰를 체크 해제한다.
                        contentBookRadioGroup.clearCheck();

                        // 교재 직접 입력하기 에딧을 비운다.
                        typeMyselfEdit.setText(null);

                        // 범위 탭, 기한 탭을 안 보이게 한다.
                        rangeContainer.setVisibility(View.GONE);
                        dueDateContainer.setVisibility(View.GONE);

                    case R.id.radio_group_content_book:

                        // 범위 탭과 기한 탭을 안 보이게 한다.
                        rangeContainer.setVisibility(View.GONE);
                        dueDateContainer.setVisibility(View.GONE);

                        // 기한 탭의 DatePicker 뷰를 안 보이게 한다.
                        contentDueDateDatePicker.setVisibility(View.GONE);

                        // 범위 탭의 텍스트를 없앤다.
                        contentRangeFromEdit.setText(null);
                        contentRangeToEdit.setText(null);

                        // 기한 탭의 날짜 텍스트를 오늘로 변경한다.
                        updateDatePicker(contentDueDateDatePicker, LocalDate.now());

                        // 추가하기 버튼을 사용할 수 없게 한다.
                        setEnabledToAddContainer(false);
                }
            }
        });
    }
}
