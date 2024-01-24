package kr.co.problemtimertest.impl;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.PopupSchoolAdapter;
import kr.co.problemtimertest.adapter.PopupStudentAdapter;
import kr.co.problemtimertest.adapter.StudentAdapter;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.School;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AddStudentDialogUtil {

    private float dp;

    private Dialog addStudentDialog;
    private LinearLayout studentContainer;
    private Map<String, Integer> studentRecyclerIdMap;
    private Teacher teacher;

    private ConstraintLayout classroomContainer, schoolStudentContainer;
    private CheckBox createClassroomCheck;
    private RadioGroup contentClassroomRadioGroup;
    private FrameLayout studentFrame;
    private EditText classroomEdit, schoolEdit, studentEdit;
    private ProgressBar schoolProgress, studentProgress;
    private LinearLayout cancelContainer, addContainer;
    private TextView cancelText, addText;

    private ListPopupWindow schoolPopup, studentPopup;
    private PopupSchoolAdapter popupSchoolAdapter;
    private PopupStudentAdapter popupStudentAdapter;

    private List<Classroom> classrooms;
    private List<School> schools;
    private List<Student> students;

    private RequestQueue requestQueue;

    private RadioButton checkedClassroomRadioBtn;

    private boolean isSchoolItemSelected;

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

    // 학생 추가 대화상자를 반환한다.
    public Dialog getDialog() {
        return addStudentDialog;
    }

    public AddStudentDialogUtil(
            Context context,
            LinearLayout studentContainer,
            Map<String, Integer> studentRecyclerIdMap,
            Teacher teacher) {

        // 교재 추가 대화상자를 얻는다.
        addStudentDialog = getAddStudentDialog(context);

        // 변수를 초기화한다.
        initializeVariable(studentContainer, studentRecyclerIdMap, teacher);

        // 수업 탭
        setClassroomContainer();

        // 학생 탭
        setStudentContainer();

        // 취소 버튼
        setCancelContainer();
    }

    // 학생 추가 대화상자를 반환한다.
    private Dialog getAddStudentDialog(Context context) {

        Dialog addStudentDialog = new Dialog(context);

        // addStudentDialog의 타이틀을 없애고, 리소스를 연결한다.
        addStudentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addStudentDialog.setContentView(R.layout.dialog_add_student);

        return addStudentDialog;
    }

    // 변수를 초기화한다.
    private void initializeVariable(LinearLayout studentContainer, Map<String, Integer> studentRecyclerIdMap, Teacher teacher) {

        dp = addStudentDialog.getContext().getResources().getDisplayMetrics().density;

        requestQueue = Volley.newRequestQueue(addStudentDialog.getContext());

        this.studentContainer = studentContainer;
        this.studentRecyclerIdMap = studentRecyclerIdMap;
        this.teacher = teacher;

        classroomContainer = (ConstraintLayout) addStudentDialog.findViewById(R.id.container_classroom);
        schoolStudentContainer = (ConstraintLayout) addStudentDialog.findViewById(R.id.container_school_student);

        createClassroomCheck = (CheckBox) addStudentDialog.findViewById(R.id.check_create_classroom);

        contentClassroomRadioGroup = (RadioGroup) addStudentDialog.findViewById(R.id.radio_group_content_classroom);

        studentFrame = (FrameLayout) addStudentDialog.findViewById(R.id.frame_student);

        classroomEdit = (EditText) addStudentDialog.findViewById(R.id.edit_classroom);
        schoolEdit = (EditText) addStudentDialog.findViewById(R.id.edit_school);
        studentEdit = (EditText) addStudentDialog.findViewById(R.id.edit_student);

        schoolProgress = (ProgressBar) addStudentDialog.findViewById(R.id.progress_school);
        studentProgress = (ProgressBar) addStudentDialog.findViewById(R.id.progress_student);

        cancelContainer = (LinearLayout) addStudentDialog.findViewById(R.id.container_cancel);
        addContainer = (LinearLayout) addStudentDialog.findViewById(R.id.container_add);

        cancelText = (TextView) addStudentDialog.findViewById(R.id.text_cancel);
        addText = (TextView) addStudentDialog.findViewById(R.id.text_add);
    }

    // 수업 탭을 설정한다.
    private void setClassroomContainer() {

        // 수업 라디오그룹에 수업 라디오버튼을 추가한다.
        addClassroomRadioBtnToContentClassroomRadioGroup();

        // 수업 라디오버튼이 클릭되면
        // -> 학교/학생 탭을 보이게 한다.
        setOnClickListenerToClassroomRadioBtn();

        // 수업 새로만들기 체크박스가 클릭되면
        // -> 수업 에딧을 보이게 한다.
        setOnCheckedChangeListenerToCreateClassroomCheck();

        // 수업 에디트에 입력을 마치면
        // -> 학교 에딧에 포커스를 준다.
        setOnEditorActionListenerToClassroomEdit();
    }

    // 수업 라디오그룹에 수업 라디오버튼을 추가한다.
    private void addClassroomRadioBtnToContentClassroomRadioGroup() {

        // 수업 리스트를 얻는다.
        getClassrooms();

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 수업 리스트가 0.2초 이내에 초기화되지 않으면
                if (isClassroomsNullForNms(200L)) return;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        List<String> classroomNames = classrooms.stream()
                                .map(classroom -> classroom.getName())
                                .distinct()
                                .collect(Collectors.toList());

                        for (String classroomName : classroomNames) {

                            // 수업 라디오버튼을 얻는다.
                            RadioButton classroomRadioBtn = getClassroomRadioBtn(classroomName);

                            // 수업 라디오그룹에 수업 라디오버튼을 추가한다.
                            contentClassroomRadioGroup.addView(classroomRadioBtn);
                        }
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

        Log.d("error", "failed to initialize : classrooms");

        return true;
    }

    // 학년 버튼을 반환한다.
    private RadioButton getClassroomRadioBtn(String classroomName) {

        RadioButton classroomRadioBtn = new RadioButton(addStudentDialog.getContext());

        classroomRadioBtn.setText(classroomName);
        classroomRadioBtn.setTextSize(15);
        classroomRadioBtn.setTextColor(addStudentDialog.getContext().
                getResources().getColorStateList(R.color.selector_text, null));
        classroomRadioBtn.setBackgroundResource(R.drawable.selector_radio);
        classroomRadioBtn.setButtonDrawable(R.color.transparent);
        classroomRadioBtn.setElevation((int) (5 * dp));
        classroomRadioBtn.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        LinearLayout.LayoutParams classroomParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        classroomParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp));

        classroomRadioBtn.setLayoutParams(classroomParams);

        return classroomRadioBtn;
    }

    // 수업 새로만들기 체크박스가 클릭되면
    // -> 수업 에딧을 보이게 한다.
    private void setOnCheckedChangeListenerToCreateClassroomCheck() {

        createClassroomCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {

                    // 수업 라디오그룹을 보이지 않게 한다.
                    contentClassroomRadioGroup.setVisibility(View.GONE);

                    classroomEdit.setText(null);

                    // 수업 에딧을 보이게 한다.
                    classroomEdit.setVisibility(View.VISIBLE);
                }
                else {

                    // 수업 에딧을 보이지 않게 한다.
                    classroomEdit.setVisibility(View.GONE);

                    contentClassroomRadioGroup.clearCheck();

                    // 수업 라디오그룹을 보이게 한다.
                    contentClassroomRadioGroup.setVisibility(View.VISIBLE);
                }

                // 대화 상자의 뷰를 원래 상태로 되돌린다.
                resetViews(compoundButton.getId());
            }
        });
    }

    // 수업 에디트에 입력을 마치면
    // -> 학교 에딧에 포커스를 준다.
    private void setOnEditorActionListenerToClassroomEdit() {

        classroomEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키가 입력되었다면
                if (keyCode == EditorInfo.IME_ACTION_NEXT) {

                    String classroomName = String.valueOf(classroomEdit.getText());

                    if (!classroomName.isEmpty()) {

                        // 수업 이름이 중복되는지 확인한다.
                        if (isClassroomNameDuplicated(classroomName)) return true;

                        // 학교/학생 탭을 보이게 한다.
                        schoolStudentContainer.setVisibility(View.VISIBLE);

                        // 추가 버튼의 색깔과 리스너를 설정한다.
                        setColorAndOnClickListenerToAddContainer();

                        // 학교 에디트에 포커스를 준다.
                        schoolEdit.setFocusableInTouchMode(true);
                        schoolEdit.requestFocus();

                        return false;
                    }
                }

                // 포커스를 유지한다.
                return true;
            }
        });
    }

    // 수업 이름이 중복되는지 확인한다.
    private boolean isClassroomNameDuplicated(String classroomName) {

        for (int i = 0; i < studentContainer.getChildCount(); i++) {

            LinearLayout studentInAClassroomContainer = (LinearLayout) studentContainer.getChildAt(i);

            TextView classroomNameText = (TextView) studentInAClassroomContainer.getChildAt(0);

            if (classroomName.equals(classroomNameText.getText())) {

                Toast.makeText(addStudentDialog.getContext(),
                        "같은 이름의 반이 이미 존재합니다.", Toast.LENGTH_SHORT).show();

                // 수업 에딧에 포커스를 준다.
                classroomEdit.setFocusableInTouchMode(true);
                classroomEdit.requestFocus();

                // 키보드를 보이게 한다.
                InputMethodManager imm = (InputMethodManager) addStudentDialog.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.showSoftInput(classroomEdit, 0);

                return true;
            }
        }

        return false;
    }

    // 수업 라디오버튼이 클릭되면
    // -> 학교/학생 탭을 보이게 한다.
    private void setOnClickListenerToClassroomRadioBtn() {

        contentClassroomRadioGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {

                RadioButton classroomRadioBtn = (RadioButton) child;

                classroomRadioBtn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        // 처음 클릭한 경우
                        if (classroomRadioBtn != checkedClassroomRadioBtn) {

                            // 학교/학생 탭을 보이게 한다.
                            schoolStudentContainer.setVisibility(View.VISIBLE);

                            if (String.valueOf(schoolEdit.getText()).isEmpty()) {

                                // 학교 에딧에 포커스를 준다.
                                schoolEdit.setFocusableInTouchMode(true);
                                schoolEdit.requestFocus();

                                // 키보드를 보이게 한다.
                                InputMethodManager imm = (InputMethodManager) addStudentDialog.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(schoolEdit, 0);
                            }

                            // 추가 버튼의 색깔과 리스너를 설정한다.
                            setColorAndOnClickListenerToAddContainer();

                            checkedClassroomRadioBtn = classroomRadioBtn;
                        }
                        // 같은 버튼을 다시 클릭한 경우
                        else {

                            // 체크된 뷰를 체크 해제한다.
                            contentClassroomRadioGroup.clearCheck();

                            checkedClassroomRadioBtn = null;

                            // 학교/학생 탭을 보이지 않게 한다.
                            schoolStudentContainer.setVisibility(View.GONE);

                            // 대화 상자의 뷰를 원래 상태로 되돌린다.
                            resetViews(parent.getId());
                        }
                    }
                });
            }

            @Override
            public void onChildViewRemoved(View view, View view1) {}
        });
    }

    // 학생 탭을 설정한다.
    private void setStudentContainer() {

        // 학교 에딧을 설정한다.
        setSchoolEdit();

        // 학교 팝업을 설정한다.
        setSchoolPopup();

        // 학생 에딧을 설정한다.
        setStudentEdit();

        // 학교 팝업을 설정한다.
        setStudentPopup();
    }

    // 학원 에딧을 설정한다.
    private void setSchoolEdit() {

        schoolEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 완료 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_DONE) {

                    if (String.valueOf(schoolEdit.getText()).isEmpty()) {

                        Toast.makeText(addStudentDialog.getContext(), "학교 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    // 학교 프로그레스를 띄운다.
                    showSchoolProgress();

                    // 학교 팝업을 띄운다.
                    showSchoolPopup();
                }

                return true;
            }
        });
    }

    // 학교 프로그레스를 띄운다.
    private void showSchoolProgress() {

        FrameLayout.LayoutParams progressParam = new FrameLayout.LayoutParams((int) (25 * dp), (int) (25 * dp));

        Rect rect = new Rect();
        schoolEdit.getPaint().getTextBounds(schoolEdit.getText().toString(), 0, schoolEdit.getText().length(), rect);

        progressParam.setMargins((int) (rect.width() + 15 * dp), (int) (1 * dp), 0, 0);
        progressParam.gravity = Gravity.CENTER_VERTICAL;

        schoolProgress.setLayoutParams(progressParam);

        schoolProgress.setVisibility(View.VISIBLE);
    }

    // 학교 팝업을 띄운다.
    private void showSchoolPopup() {

        schools = null;

        // 학교 리스트를 얻는다.
        getSchools();

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 학교 리스트가 0.2초 이내에 초기화되지 않으면
                if (isSchoolsNullForNms(200L)) return;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 학교 리스트가 비어있다면
                        if (schools.isEmpty()) {

                            Toast.makeText(addStudentDialog.getContext(),
                                    "해당 학교가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                        // 학교 리스트가 비어있지 않다면
                        else {

                            // 학교 어댑터를 얻는다.
                            popupSchoolAdapter = new PopupSchoolAdapter(schools);

                            // 학교 팝업에 학교 어댑터를 연결한다.
                            schoolPopup.setAdapter(popupSchoolAdapter);

                            schoolPopup.setWidth(schoolPopup.getAnchorView().getWidth());
                            schoolPopup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

                            // 학교 팝업을 띄운다.
                            schoolPopup.show();
                        }
                    }
                });
            }
        });
    }

    // 학교 리스트를 얻는다.
    private void getSchools() {

        String schoolName = String.valueOf(schoolEdit.getText());

        Call<List<School>> call = managementApi.listSchools(schoolName);

        call.enqueue(new Callback<List<School>>() {

            @Override
            public void onResponse(Call<List<School>> call, Response<List<School>> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                schools = response.body();

                // 학교 프로그레스를 보이지 않게 한다.
                schoolProgress.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<List<School>> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 학교 리스트가 초기화되었는지 0.0n초간 확인한다.
    private boolean isSchoolsNullForNms(long n) {

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

            if (schools == null) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to initialize : schools");

        return true;
    }

    // 학교 팝업을 설정한다.
    private void setSchoolPopup() {

        // 학교 팝업을 얻는다.
        schoolPopup = new ListPopupWindow(addStudentDialog.getContext());

        schoolPopup.setAnchorView(schoolEdit);

        // 학교 팝업의 너비와 높이는 학교 에딧의 onEditorActionListener에서 설정한다.

        // 학교 팝업의 아이템을 클릭하면
        schoolPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long resId) {

                // 학교 팝업을 없앤다.
                schoolPopup.dismiss();

                // 선택한 메뉴에 해당하는 아이템을 얻는다.
                School item = popupSchoolAdapter.getItem(pos);

                // 학교 에딧에 아이템을 설정한다.
                setItemToSchoolEdit(item);

                // 추가 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToAddContainer();

                isSchoolItemSelected = true;
                studentEdit.setFocusable(true);

                // 학생 애딧에 포커스를 준다.
                studentEdit.setFocusableInTouchMode(true);
                studentEdit.requestFocus();

                // 키보드를 보이게 한다.
                InputMethodManager imm = (InputMethodManager) addStudentDialog.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.showSoftInput(studentEdit, 0);
            }
        });
    }

    // 학교 에딧에 아이템을 설정한다.
    private void setItemToSchoolEdit(School item) {

        // 학교 에딧에 학교 이름을 설정한다.
        schoolEdit.setText(item.getName());

        // 학교 에딧에 선택한 아이템을 태그한다.
        Map<String, School> itemMap = new HashMap<>();
        itemMap.put("itemSelected", item);

        schoolEdit.setTag(itemMap);
    }

    // 학생 에딧을 설정한다.
    private void setStudentEdit() {

        // 학생 에딧을 클릭하면
        // -> 학교를 먼저 입력해달라는 토스트를 띄운다.
        setOnClickListenerToStudentEdit();

        // 학생 에딧에 입력을 마치면
        // -> 학생 팝업에 학생 리스트를 띄운다.
        setOnEditorActionListenerToStudentEdit();
    }

    // 학생 에딧을 클릭하면
    // -> 학교를 먼저 입력해달라는 토스트를 띄운다.
    private void setOnClickListenerToStudentEdit() {

        studentEdit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (!isSchoolItemSelected) {

                    Toast.makeText(addStudentDialog.getContext(), "학교를 먼저 입력해주세요.", Toast.LENGTH_SHORT).show();

                    // 학교 에딧에 포커스를 준다.
                    schoolEdit.setFocusableInTouchMode(true);
                    schoolEdit.requestFocus();

                    // 키보드를 보이게 한다.
                    InputMethodManager imm = (InputMethodManager) addStudentDialog.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(schoolEdit, 0);
                }
            }
        });
    }

    // 학생 에딧에 입력을 마치면
    // -> 학생 팝업에 학생 리스트를 띄운다.
    private void setOnEditorActionListenerToStudentEdit() {

        studentEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 완료 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_DONE) {

                    if (String.valueOf(studentEdit.getText()).isEmpty()) {

                        Toast.makeText(addStudentDialog.getContext(), "학생 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    // 학생 프로그레스를 띄운다.
                    showStudentProgress();

                    // 학생 팝업을 띄운다.
                    showStudentPopup();
                }

                return true;
            }
        });
    }

    // 학생 프로그레스를 띄운다.
    private void showStudentProgress() {

        FrameLayout.LayoutParams progressParam = new FrameLayout.LayoutParams((int) (25 * dp), (int) (25 * dp));

        Rect rect = new Rect();
        studentEdit.getPaint().getTextBounds(studentEdit.getText().toString(), 0, studentEdit.getText().length(), rect);

        progressParam.setMargins((int) (rect.width() + 15 * dp), (int) (1 * dp), 0, 0);
        progressParam.gravity = Gravity.CENTER_VERTICAL;

        studentProgress.setLayoutParams(progressParam);

        studentProgress.setVisibility(View.VISIBLE);
    }

    // 학생 팝업을 띄운다.
    private void showStudentPopup() {

        students = null;

        // 학생 리스트를 얻는다.
        getStudents();

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 학생 리스트가 0.00n초 이내에 초기화되지 않으면
                if (isStudentsNullForNms(200L)) return;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 선택한 학교를 얻는다.
                        Map<String, School> itemMap = (Map<String, School>) schoolEdit.getTag();
                        School item = itemMap.get("itemSelected");

                        // 선택한 학교에 속하는 학생 리스트를 얻는다.
                        List<Student> studentsInASchool = students.stream()
                                .filter(student -> student.getSchoolId().equals(item.getId()))
                                .collect(Collectors.toList());

                        // 선택 가능한 학생 리스트와 선택 불가능한 학생 리스트를 얻는다.
                        List<Student> studentsSelectable = getStudentsSelectable(studentsInASchool);

                        List<Student> studentsUnselectable = getStudentsUnselectable(studentsInASchool);

                        // 선택 가능한 학생 리스트가 비어있다면
                        if (studentsSelectable.isEmpty()) {

                            Toast.makeText(addStudentDialog.getContext(),
                                    "해당 학생이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                        // 선택 가능한 학생 리스트가 비어있지 않다면
                        else {

                            // 학생 어댑터를 얻는다.
                            popupStudentAdapter = new PopupStudentAdapter(studentsSelectable);

                            // 학생 팝업에 학생 어댑터를 연결한다.
                            studentPopup.setAdapter(popupStudentAdapter);

                            studentPopup.setWidth(studentPopup.getAnchorView().getWidth());
                            studentPopup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

                            // 학생 팝업을 띄운다.
                            studentPopup.show();
                        }
                    }
                });
            }
        });
    }

    // 선택 가능한 학생 리스트를 반환한다.
    private List<Student> getStudentsSelectable(List<Student> studentsInASchool) {

        // 반을 새로 만들지 않는 경우
        if (!createClassroomCheck.isChecked()) {

            // 선택한 반에 속하는 학생 리스트를 얻는다.
            List<Student> studentsInAClassroom = getStudentsInAClassroom();

            return studentsInASchool.stream()
                    .filter(student -> !studentsInAClassroom.contains(student))
                    .collect(Collectors.toList());
        }
        // 반을 새로 만드는 경우
        else {

            return studentsInASchool;
        }
    }

    // 선택 불가능한 학생 리스트를 반환한다.
    private List<Student> getStudentsUnselectable(List<Student> studentsInASchool) {

        // 반을 새로 만들지 않는 경우
        if (!createClassroomCheck.isChecked()) {

            // 선택한 반에 속하는 학생 리스트를 얻는다.
            List<Student> studentsInAClassroom = getStudentsInAClassroom();

            return studentsInASchool.stream()
                    .filter(student -> studentsInAClassroom.contains(student))
                    .collect(Collectors.toList());
        }
        // 반을 새로 만드는 경우
        else {

            return new ArrayList<>();
        }
    }

    // 선택한 반에 속하는 학생 리스트를 반환한다.
    private List<Student> getStudentsInAClassroom() {

        String classroomName = String.valueOf(checkedClassroomRadioBtn.getText());

        RecyclerView studentInAClassroomRecycler = studentContainer.findViewById(studentRecyclerIdMap.get(classroomName));

        return ((StudentAdapter) studentInAClassroomRecycler.getAdapter()).getItems();
    }

    // 학생 리스트를 얻는다.
    private void getStudents() {

        Call<List<Student>> call = managementApi.list(String.valueOf(studentEdit.getText()));

        call.enqueue(new Callback<List<Student>>() {

            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                students = response.body();

                // 학생 프로그레스를 보이지 않게 한다.
                studentProgress.setVisibility(View.GONE);
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

        Log.d("error", "failed to initialize : students");

        return true;
    }

    // 학생 팝업을 설정한다.
    private void setStudentPopup() {

        // 학생 팝업을 얻는다.
        studentPopup = new ListPopupWindow(addStudentDialog.getContext());

        studentPopup.setAnchorView(studentEdit);

        // 학생 팝업의 너비와 높이는 학교 에딧의 onEditorActionListener에서 설정한다.

        // 학생 팝업의 아이템을 클릭하면
        studentPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long resId) {

                // 학생 팝업을 없앤다.
                studentPopup.dismiss();

                // 선택한 메뉴에 해당하는 아이템을 얻는다.
                Student item = popupStudentAdapter.getItem(pos);

                // 학생 에딧에 아이템을 설정한다.
                setItemToStudentEdit(item);

                // 추가 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToAddContainer();

                // 취소 텍스트에 포커스를 준다.
                cancelText.setFocusableInTouchMode(true);
                cancelText.requestFocus();

                // 키보드를 보이지 않게 한다.
                InputMethodManager imm = (InputMethodManager) addStudentDialog.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(cancelText.getWindowToken(), 0);
            }
        });
    }

    // 학생 에딧에 아이템을 설정한다.
    private void setItemToStudentEdit(Student item) {

        // 학생 에딧에 학생 이름을 설정한다.
        studentEdit.setText(item.getName());

        // 학생 에딧에 선택한 아이템을 태그한다.
        Map<String, Student> itemMap = new HashMap<>();
        itemMap.put("itemSelected", item);

        studentEdit.setTag(itemMap);
    }

    // 취소 버튼을 설정한다.
    private void setCancelContainer() {

        cancelContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 대화상자를 보이지 않게 한다.
                addStudentDialog.dismiss();

                // 대화 상자의 뷰를 원래 상태로 되돌린다.
                resetViews(view.getId());
            }
        });
    }

    // 추가 버튼의 색깔과 리스너를 설정한다.
    private void setColorAndOnClickListenerToAddContainer() {

        if (((!createClassroomCheck.isChecked() && contentClassroomRadioGroup.getCheckedRadioButtonId() != -1) ||
                (createClassroomCheck.isChecked() && !String.valueOf(classroomEdit.getText()).isEmpty())) &&
                !String.valueOf(schoolEdit.getText()).isEmpty() && !String.valueOf(studentEdit.getText()).isEmpty()) {

            addContainer.setBackground(addStudentDialog.getContext().getResources().getDrawable(R.drawable.shape_btn_soft_blue));
            addText.setTextColor(Color.BLACK);

            addContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    if (((!createClassroomCheck.isChecked() && contentClassroomRadioGroup.getCheckedRadioButtonId() != -1) ||
                            (createClassroomCheck.isChecked() && !String.valueOf(classroomEdit.getText()).isEmpty())) &&
                            !String.valueOf(schoolEdit.getText()).isEmpty() && !String.valueOf(studentEdit.getText()).isEmpty()) {

                        String classroomName = String.valueOf(classroomEdit.getText());

                        // 수업 이름이 중복되는지 확인한다.
                        if (createClassroomCheck.isChecked() && isClassroomNameDuplicated(classroomName)) return;

                        // 대화 상자를 보이지 않게 한다.
                        addStudentDialog.dismiss();

                        // 학생을 저장한다.
                        saveStudent();

                        // 학생 어댑터에 아이템을 추가한다.
                        addItemToStudentAdapter();

                        // 과제 추가 대화 상자의 뷰를 원래 상태로 되돌린다.
                        resetViews(view.getId());
                    }
                    else {

                        Toast.makeText(addStudentDialog.getContext(),
                                "잘못 입력된 값이 있습니다.", Toast.LENGTH_SHORT).show();

                        addContainer.setBackground(addStudentDialog.getContext().getResources().getDrawable(R.drawable.shape_btn_soft_disabled));
                        addText.setTextColor(addStudentDialog.getContext().getResources().getColor(R.color.disabled));

                        addContainer.setOnClickListener(null);
                    }
                }
            });
        }
        else {

            addContainer.setBackground(addStudentDialog.getContext().getResources().getDrawable(R.drawable.shape_btn_soft_disabled));
            addText.setTextColor(addStudentDialog.getContext().getResources().getColor(R.color.disabled));

            addContainer.setOnClickListener(null);
        }
    }

    // 학생을 저장한다.
    private void saveStudent() {

        // 학생 에딧에 태그한 아이템을 얻는다.
        Map<String, Student> itemMap = (Map<String, Student>) studentEdit.getTag();
        Student itemSelected = itemMap.get("itemSelected");

        Call<Classroom> call = managementApi.create(new Classroom(
                String.valueOf((!createClassroomCheck.isChecked() ? checkedClassroomRadioBtn : classroomEdit).getText()),
                teacher.getId(),
                itemSelected.getId()));

        call.enqueue(new Callback<Classroom>() {

            @Override
            public void onResponse(Call<Classroom> call, Response<Classroom> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }
            }

            @Override
            public void onFailure(Call<Classroom> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 학생 어댑터에 아이템을 추가한다.
    private void addItemToStudentAdapter() {

        // 반을 새로 만드는 경우
        if (createClassroomCheck.isChecked()) {

            // 수업 이름 텍스트와 학생 리싸이클러를 포함하는 컨테이너를 얻는다.
            LinearLayout studentsInAClassroomContainer = getStudentsInAClassroomContainer();

            // 수업 이름 텍스트를 설정한다.
            setClassroomNameText(studentsInAClassroomContainer);

            // 학생 리싸이클러를 설정한다.
            setStudentRecycler(studentsInAClassroomContainer);

            // 만든 리니어를 학생 리니어에 추가한다.
            studentContainer.addView(studentsInAClassroomContainer);
        }
        // 기존의 반에 학생을 추가하는 경우
        else {

            String classroomName = String.valueOf(checkedClassroomRadioBtn.getText());

            RecyclerView studentRecycler = (RecyclerView) studentContainer.findViewById(studentRecyclerIdMap.get(classroomName));

            StudentAdapter studentAdapter = (StudentAdapter) studentRecycler.getAdapter();

            // 학생 에딧에 태그한 아이템을 얻는다.
            Map<String, Student> itemMap = (Map<String, Student>) studentEdit.getTag();
            Student itemSelected = itemMap.get("itemSelected");

            studentAdapter.addItem(itemSelected);

            studentAdapter.notifyDataSetChanged();
        }
    }

    // 수업 이름 텍스트와 학생 리싸이클러를 포함하는 컨테이너를 반환한다.
    private LinearLayout getStudentsInAClassroomContainer() {

        LinearLayout studentsInAClassroomContainer = new LinearLayout(addStudentDialog.getContext());

        studentsInAClassroomContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams studentInAClassroomContainerParam =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        studentInAClassroomContainerParam.setMargins(0, (int) (5 * dp), 0, 0);

        studentsInAClassroomContainer.setLayoutParams(studentInAClassroomContainerParam);

        // 수업 이름 텍스트를 학생 탭에 추가한다.
        studentsInAClassroomContainer.addView(new TextView(addStudentDialog.getContext()));

        // 학생 리싸이클러를 학생 탭에 추가한다.
        studentsInAClassroomContainer.addView(new RecyclerView(addStudentDialog.getContext()));

        return studentsInAClassroomContainer;
    }

    // 수업 이름 텍스트를 설정한다.
    private void setClassroomNameText(LinearLayout studentInAClassroomContainer) {

        // 수업 이름 텍스트를 얻는다.
        TextView classroomNameText = (TextView) studentInAClassroomContainer.getChildAt(0);

        String classroomName = String.valueOf(classroomEdit.getText());

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
    private void setStudentRecycler(LinearLayout studentInAClassroomContainer) {

        String classroomName = String.valueOf(classroomEdit.getText());

        // 학생 에딧에 태그한 아이템을 얻는다.
        Map<String, Student> itemMap = (Map<String, Student>) studentEdit.getTag();
        Student itemSelected = itemMap.get("itemSelected");

        // 학생 어댑터를 얻는다.
        StudentAdapter studentAdapter = new StudentAdapter(teacher.getId(), classroomName);

        // 학생 어댑터에 아이템을 추가한다.
        studentAdapter.addItem(itemSelected);

        // 학생 리싸이클러를 얻는다.
        RecyclerView studentRecycler = getStudentRecyclerView(studentInAClassroomContainer);

        // 학생 리싸이클러에 학생 어댑터를 연결한다.
        setStudentAdapterToStudentRecycler(studentAdapter, studentRecycler);

        // 학생 어댑터를 새로고침한다.
        studentAdapter.notifyDataSetChanged();
    }

    // 학생 리싸이클러를 반환한다.
    private RecyclerView getStudentRecyclerView(LinearLayout studentInAClassroomContainer) {

        String classroomName = String.valueOf(classroomEdit.getText());

        RecyclerView studentRecycler = (RecyclerView) studentInAClassroomContainer.getChildAt(1);

        studentRecycler.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        studentRecycler.setPadding(0, 0, 0, (int) (5 * dp));

        int studentRecyclerId = View.generateViewId();

        studentRecycler.setId(studentRecyclerId);

        studentRecyclerIdMap.put(classroomName, studentRecyclerId);

        return studentRecycler;
    }

    // 학생 리싸이클러에 학생 어댑터를 연결한다.
    private void setStudentAdapterToStudentRecycler(StudentAdapter studentAdapter, RecyclerView studentRecycler) {

        studentRecycler.setLayoutManager(new LinearLayoutManager(
                addStudentDialog.getContext(), LinearLayoutManager.VERTICAL, false));

        studentRecycler.setAdapter(studentAdapter);
    }

    // 대화 상자의 뷰를 원래 상태로 되돌린다.
    private void resetViews(int id) {

        switch (id) {

            case R.id.container_add:
            case R.id.container_cancel:
                createClassroomCheck.setChecked(false);
                contentClassroomRadioGroup.clearCheck();
                classroomEdit.setText(null);

            case R.id.check_create_classroom :
                schoolStudentContainer.setVisibility(View.GONE);

            case R.id.radio_group_content_classroom:
            case R.id.edit_classroom:
                schoolEdit.setText(null);

            case R.id.edit_school:
                studentEdit.setText(null);
        }
    }
}
