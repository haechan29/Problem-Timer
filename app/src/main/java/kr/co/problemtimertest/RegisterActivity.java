package kr.co.problemtimertest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import kr.co.problemtimertest.adapter.AcademyAdapter;
import kr.co.problemtimertest.adapter.PopupSchoolAdapter;
import kr.co.problemtimertest.api.LoginApi;
import kr.co.problemtimertest.jsonobject.AcademyInfoObject;
import kr.co.problemtimertest.jsonobject.SchoolInfoObject;
import kr.co.problemtimertest.model.Academy;
import kr.co.problemtimertest.model.School;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RegisterActivity extends AppCompatActivity {

    final static List<String> regions = Arrays.asList("gangwon", "gyeonggi", "gyeungnam", "gyeungbuk", "gwangju", "daegu", "daejeon", "busan", "seoul", "sejong",
            "ulsan", "incheon", "jeonnam", "jeonbuk", "jeju", "chungnam", "chungbuk");

    final static List<String> regionCodes = Arrays.asList("K10", "J10", "S10", "R10", "F10", "D10", "G10", "C10", "B10", "I10", "H10", "E10", "Q10", "P10", "T10", "N10", "M10");

    float dp;

    boolean isSpinnerClickedAlready;

    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabaseReference;

    private ScrollView registerScroll;
    private LinearLayout jobContainer, emailContainer, passwordContainer, passwordCheckContainer,
            nameContainer, schoolGradeContainer, regionAcademyContainer, registerContainer;
    private RadioGroup jobRadioGroup;
    private AppCompatRadioButton studentRadioBtn,teacherRadioBtn;
    private EditText emailEdit, passwordEdit, passwordCheckEdit, nameEdit, schoolEdit, academyEdit;
    private TextView emailErrorMsgText, passwordErrorMsgText, passwordCheckErrorMsgText, nameErrorMsgText;
    private Spinner gradeSpinner, regionSpinner;
    private View registerBackgroundLeftView, registerBackgroundCenterView, registerBackgroundRightView;
    private ProgressBar schoolProgress, academyProgress, registerProgress;

    private ListPopupWindow schoolPopup;
    private PopupSchoolAdapter popupSchoolAdapter;

    private ListPopupWindow academyPopup;
    private AcademyAdapter academyAdapter;

    private boolean isJobProper;
    private boolean isEmailProper;
    private boolean isPasswordProper;
    private boolean isPasswordCheckProper;
    private boolean isNameProper;
    private boolean isSchoolProper;
    private boolean isGradeProper;
    private boolean isRegionProper;
    private boolean isAcademyProper;

    // 이메일 중복 확인 변수
    private boolean isStudentInitialized, isTeacherInitialized;
    private Student student;
    private Teacher teacher;

    private RequestQueue requestQueue;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final LoginApi loginApi = retrofit.create(LoginApi.class);

    // ThreadPool 관련 변수
    private final ExecutorService threadPool = new ThreadPoolExecutor(
            50,                     // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 변수를 초기화한다.
        initializeVariable();

        // 직업 탭
        setJobContainer();

        // 이메일 탭
        setEmailContainer();

        // 비밀번호 탭
        setPasswordContainer();

        // 비밀번호 확인 탭
        setPasswordCheckContainer();

        // 이름 탭
        setNameContainer();

        // 학교/학년 탭
        setSchoolAndGradeContainer();

        // 학원 탭
        setRegionAcademyContainer();
    }

    // 변수를 초기화한다.
    private void initializeVariable() {

        dp = getResources().getDisplayMetrics().density;

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("problemTimer");

        requestQueue = Volley.newRequestQueue(RegisterActivity.this);

        registerScroll = (ScrollView) findViewById(R.id.scroll_register);

        jobContainer = (LinearLayout) findViewById(R.id.container_job);
        emailContainer = (LinearLayout) findViewById(R.id.container_email);
        passwordContainer = (LinearLayout) findViewById(R.id.container_password);
        passwordCheckContainer = (LinearLayout) findViewById(R.id.container_password_check);
        nameContainer = (LinearLayout) findViewById(R.id.container_name);
        schoolGradeContainer = (LinearLayout) findViewById(R.id.container_school_grade);
        regionAcademyContainer = (LinearLayout) findViewById(R.id.container_region_academy);
        registerContainer = (LinearLayout) findViewById(R.id.container_register);

        jobRadioGroup = (RadioGroup) findViewById(R.id.radio_group_job);
        studentRadioBtn = (AppCompatRadioButton) findViewById(R.id.radio_btn_student);
        teacherRadioBtn = (AppCompatRadioButton) findViewById(R.id.radio_btn_teacher);

        emailEdit = (EditText) findViewById(R.id.edit_email);
        passwordEdit = (EditText) findViewById(R.id.edit_password);
        passwordCheckEdit = (EditText) findViewById(R.id.edit_password_check);
        nameEdit = (EditText) findViewById(R.id.edit_name);
        schoolEdit = (EditText) findViewById(R.id.edit_school);
        academyEdit = (EditText) findViewById(R.id.edit_academy);

        emailErrorMsgText = (TextView) findViewById(R.id.text_email_error_msg);
        passwordErrorMsgText = (TextView) findViewById(R.id.text_password_error_msg);
        passwordCheckErrorMsgText = (TextView) findViewById(R.id.text_password_check_error_msg);
        nameErrorMsgText = (TextView) findViewById(R.id.text_name_error_msg);

        gradeSpinner = (Spinner) findViewById(R.id.spinner_grade);
        regionSpinner = (Spinner) findViewById(R.id.spinner_region);

        registerBackgroundLeftView = (View) findViewById(R.id.view_register_background_left);
        registerBackgroundCenterView = (View) findViewById(R.id.view_register_background_center);
        registerBackgroundRightView = (View) findViewById(R.id.view_register_background_right);

        schoolProgress = (ProgressBar) findViewById(R.id.progress_school);
        academyProgress = (ProgressBar) findViewById(R.id.progress_academy);
        registerProgress = (ProgressBar) findViewById(R.id.progress_register);
    }

    // 직업 탭을 설정한다.
    private void setJobContainer() {

        View.OnClickListener onRadioBtnClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 직업 탭에 색깔을 설정한다.
                setColorToJobContainer(view);

                // 이메일, 비밀번호, 이름 등을 올바르게 입력했는지 확인한다.
                checkInput();

                if (!isEmailProper) {

                    // 이메일 탭을 보이게 한다.
                    emailContainer.setVisibility(View.VISIBLE);

                    // 이메일 에딧에 포커스를 준다.
                    emailEdit.setFocusableInTouchMode(true);
                    emailEdit.requestFocus();

                    // 키보드를 보이게 한다.
                    InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(emailEdit, 0);
                }
                else {

                    // 회원가입 탭에 포커스를 준다.
                    registerContainer.setFocusableInTouchMode(true);
                    registerContainer.requestFocus();

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(registerContainer.getWindowToken(), 0);
                }

                if (schoolGradeContainer.getVisibility() == View.VISIBLE || regionAcademyContainer.getVisibility() == View.VISIBLE) {

                    switch (jobRadioGroup.getCheckedRadioButtonId()) {

                        case R.id.radio_btn_student:

                            schoolGradeContainer.setVisibility(View.VISIBLE);
                            regionAcademyContainer.setVisibility(View.GONE);
                            break;

                        case R.id.radio_btn_teacher:

                            schoolGradeContainer.setVisibility(View.GONE);
                            regionAcademyContainer.setVisibility(View.VISIBLE);
                    }
                }

                isJobProper = true;

                // 회원가입 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToRegisterBtn();
            }
        };

        studentRadioBtn.setOnClickListener(onRadioBtnClickListener);
        teacherRadioBtn.setOnClickListener(onRadioBtnClickListener);
    }

    // 직업 탭에 색깔을 설정한다.
    private void setColorToJobContainer(View view) {

        boolean isChecked = ((AppCompatRadioButton) view).isChecked();

        switch (view.getId()) {

            // 학생 버튼
            case R.id.radio_btn_student:

                if (isChecked) {

                    studentRadioBtn.setTextColor(Color.WHITE);
                    teacherRadioBtn.setTextColor(getResources().getColor(R.color.activated, null));
                }

                break;

            // 강사 버튼
            case R.id.radio_btn_teacher:

                if (isChecked) {

                    teacherRadioBtn.setTextColor(Color.WHITE);
                    studentRadioBtn.setTextColor(getResources().getColor(R.color.activated, null));
                }
        }
    }

    // 이메일 탭을 설정한다.
    private void setEmailContainer() {

        emailEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                isStudentInitialized = false;
                student = null;

                isTeacherInitialized = false;
                teacher = null;

                // 완료 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_DONE) {

                    String email = String.valueOf(emailEdit.getText());
                    Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;

                    // 이메일을 잘못된 형식으로 입력하면
                    if (!pattern.matcher(email).matches()) {

                        // 이메일 형식
                        notifyEmailWrongFormatError();

                        // 포커스를 유지한다.
                        return true;
                    }
                    else {

                        // 이메일이 존재하는지 데이터베이스를 확인한다.
                        checkEmailDuplication();
                    }
                }

                if (!isPasswordProper)
                    return false;
                else {

                    // 회원가입 탭에 포커스를 준다.
                    registerContainer.setFocusableInTouchMode(true);
                    registerContainer.requestFocus();

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(registerContainer.getWindowToken(), 0);

                    return true;
                }
            }
        });
    }

    // 비밀번호 탭을 설정한다.
    private void setPasswordContainer() {

        passwordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_NEXT) {

                    String password = String.valueOf(passwordEdit.getText());
                    String passwordCheck = String.valueOf(passwordCheckEdit.getText());

                    // 비밀번호가 6자 미만이면
                    if (password.length() < 6) {

                        // 비밀번호 형식
                        notifyPasswordWrongFormatError();

                        // 포커스를 유지한다.
                        return true;
                    }
                    else if (isPasswordCheckProper && !password.equals(passwordCheck)) {

                        // 비밀번호 불일치
                        notifyPasswordNotMatchError();

                        // 포커스를 유지한다.
                        return true;
                    }
                    else {

                        // 비밀번호 탭을 원래대로 되돌린다.
                        resetPasswordContainer();
                    }
                }

                if (!isPasswordCheckProper)
                    return false;
                else {

                    // 회원가입 탭에 포커스를 준다.
                    registerContainer.setFocusableInTouchMode(true);
                    registerContainer.requestFocus();

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(registerContainer.getWindowToken(), 0);

                    return true;
                }
            }
        });
    }

    // 비밀번호 확인 탭을 설정한다.
    private void setPasswordCheckContainer() {

        passwordCheckEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_NEXT) {

                    String password = String.valueOf(passwordEdit.getText());
                    String passwordCheck = String.valueOf(passwordCheckEdit.getText());

                    // 비밀번호가 일치하지 않으면
                    if (!password.equals(passwordCheck)) {

                        // 비밀번호 불일치
                        notifyPasswordNotMatchError();

                        // 포커스를 유지한다.
                        return true;
                    }
                    else {

                        // 비밀번호 탭과 비밀번호 확인 탭을 원래대로 되돌린다.
                        resetPasswordContainer();
                        resetPasswordCheckContainer();

                        nameContainer.setVisibility(View.VISIBLE);
                    }
                }

                if (!isNameProper)
                    return false;
                else {

                    // 회원가입 탭에 포커스를 준다.
                    registerContainer.setFocusableInTouchMode(true);
                    registerContainer.requestFocus();

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(registerContainer.getWindowToken(), 0);

                    return true;
                }
            }
        });
    }

    // 이름 탭을 설정한다.
    private void setNameContainer() {

        nameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_NEXT) {

                    if (nameEdit.getText().length() == 0) {

                        // 이름 유효성
                        notifyNameWrongFormatError();

                        // 포커스를 유지한다.
                        return true;
                    }
                    else {

                        // 이름 탭을 원래대로 되돌린다.
                        resetNameContainer();

                        switch (jobRadioGroup.getCheckedRadioButtonId()) {

                            case R.id.radio_btn_student:

                                schoolGradeContainer.setVisibility(View.VISIBLE);
                                regionAcademyContainer.setVisibility(View.GONE);

                                if (!isSchoolProper) {

                                    // 학교 에딧에 포커스를 준다.
                                    return false;
                                }

                                break;

                            case R.id.radio_btn_teacher:

                                schoolGradeContainer.setVisibility(View.GONE);
                                regionAcademyContainer.setVisibility(View.VISIBLE);

                                if (!isRegionProper) {

                                    // 키보드가 내려가는 시간을 고려하여 0.25초 후에 지역 스피너를 클릭한다.
                                    handler.postDelayed(new Runnable() {

                                        @Override
                                        public void run() {

                                            regionSpinner.performClick();
                                        }
                                    }, 250L);
                                }
                        }

                        // 회원가입 탭에 포커스를 준다.
                        registerContainer.setFocusableInTouchMode(true);
                        registerContainer.requestFocus();

                        // 키보드를 보이지 않게 한다.
                        InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(registerContainer.getWindowToken(), 0);
                    }
                }

                return true;
            }
        });
    }

    // 학교/학년 탭을 설정한다.
    private void setSchoolAndGradeContainer() {

        // 학교 에딧을 설정한다.
        setSchoolEdit();

        // 학교 팝업을 설정한다.
        setSchoolPopup();

        // 학년 스피너를 설정한다.
        setGradeSpinner();
    }

    // 학교 에딧을 설정한다.
    private void setSchoolEdit() {

        schoolEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 완료 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_DONE) {

                    if (String.valueOf(schoolEdit.getText()).isEmpty()) {

                        Toast.makeText(RegisterActivity.this, "학교 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    // 학교 프로그레스를 띄운다.
                    showSchoolProgress();

                    // 학교 팝업을 띄운다.
                    showSchoolPopup();

                    // 회원가입 탭에 포커스를 준다.
                    registerContainer.setFocusableInTouchMode(true);
                    registerContainer.requestFocus();

                    // 키보드를 보이지 않게 한다.
                    InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(registerContainer.getWindowToken(), 0);
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

    // 스피너에 학교 리스트를 띄운다.
    private void showSchoolPopup() {

        Integer[] count = { 0 };
        Map<String, List<SchoolInfoObject.SchoolInfo>> schoolMap = new HashMap<>();

        String url = "https://www.career.go.kr/cnet/openapi/getOpenApi?" +
                "apiKey=083d9b778ff00aed07750b3306fa7d37&" +
                "svcType=api&svcCode=SCHOOL&contentType=json&" +
                "searchSchulNm=" + schoolEdit.getText() + "&" +
                "gubun=";

        List<String> gubuns = Arrays.asList(new String[] {"elem_list", "midd_list", "high_list"});

        for (String gubun : gubuns) {

            String schoolUrl = url + gubun;

            StringRequest schoolRequest = new StringRequest(
                    Request.Method.GET,
                    schoolUrl,
                    new com.android.volley.Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {

                            // 학교 프로그레스를 보이지 않게 한다.
                            schoolProgress.setVisibility(View.GONE);

                            // 학교 맵을 얻는다.
                            SchoolInfoObject schoolInfoObject = new Gson().fromJson(response, SchoolInfoObject.class);

                            switch (gubuns.indexOf(gubun)) {

                                case 0 : schoolMap.put("elementary", schoolInfoObject.dataSearch.content); break; // 초등학교
                                case 1 : schoolMap.put("middle", schoolInfoObject.dataSearch.content); break;     // 중학교
                                case 2 : schoolMap.put("high", schoolInfoObject.dataSearch.content);              // 고등학교
                            }

                            if (count[0] == 2) {

                                // 학교 맵을 이용해 학교 리스트를 얻는다.
                                List<SchoolInfoObject.SchoolInfo> schoolInfoList = new ArrayList<>();
                                schoolInfoList.addAll(schoolMap.get("elementary"));
                                schoolInfoList.addAll(schoolMap.get("middle"));
                                schoolInfoList.addAll(schoolMap.get("high"));

                                // 학교 리스트가 비어있다면
                                if (schoolInfoList.isEmpty()) {

                                    Toast.makeText(RegisterActivity.this,
                                            "해당 학교가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                                }
                                // 학교 리스트가 비어있지 않다면
                                else {

                                    List<School> schools = schoolInfoList.stream()
                                            .map(schoolInfo -> School.toSchool(schoolInfo))
                                            .collect(Collectors.toList());

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

                            count[0]++;
                        }
                    },
                    new com.android.volley.Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("error", error.getMessage());
                        }
                    }
            ) {};

            schoolRequest.setShouldCache(false);
            requestQueue.add(schoolRequest);
        }
    }

    // 학교 팝업을 설정한다.
    private void setSchoolPopup() {

        // 학교 팝업을 얻는다.
        schoolPopup = new ListPopupWindow(RegisterActivity.this);

        schoolPopup.setAnchorView(schoolEdit);

        // 학교 팝업의 너비와 높이는 학교 에딧의 onEditorActionListener에서 설정한다.

        // 학교 팝업의 아이템을 클릭하면
        schoolPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long resId) {

                // 학교 팝업을 없앤다.
                schoolPopup.dismiss();

                isSchoolProper = true;

                // 회원가입 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToRegisterBtn();

                // 선택한 메뉴에 해당하는 아이템을 얻는다.
                School item = popupSchoolAdapter.getItem(pos);

                // 학교 에딧에 아이템을 설정한다.
                setItemToSchoolEdit(item);

                // 학년 스피너에 아이템을 설정한다.
                setItemToGradeSpinner(item);
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

    // 학년 스피너에 아이템을 설정한다.
    private void setItemToGradeSpinner(School item) {

        // 학교 에딧에서 포커스를 제거한다.
        gradeSpinner.setFocusableInTouchMode(true);
        gradeSpinner.requestFocus();

        gradeSpinner.setDropDownHorizontalOffset((int) (-14 * dp));

        // 학년 어댑터를 얻는다.
        ArrayAdapter<String> gradeAdapter =
                new ArrayAdapter<String>(RegisterActivity.this,
                        R.layout.item_grade,
                        getResources().getStringArray(item.getName().contains("초등학교") ?
                                R.array.grade_long : R.array.grade_short));

        gradeAdapter.setDropDownViewResource(R.layout.item_dropdown_grade);

        // 학년 스피너에 학년 어댑터를 설정한다.
        gradeSpinner.setAdapter(gradeAdapter);

        // 학년 스피너를 클릭한다.
        gradeSpinner.performClick();
    }

    // 학년 스피너를 설정한다.
    private void setGradeSpinner() {

        gradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long resId) {

                isGradeProper = true;

                // 회원가입 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToRegisterBtn();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    // 지역/학원 탭을 설정한다.
    private void setRegionAcademyContainer() {

        // 지역 스피너를 설정한다.
        setRegionSpinner();

        // 학원 에딧을 설정한다.
        setAcademyEdit();

        // 학원 팝업을 설정한다.
        setAcademyPopup();
    }

    // 지역 스피너를 설정한다.
    private void setRegionSpinner() {

        regionSpinner.setDropDownHorizontalOffset((int) (-14 * dp));

        // 지역 어댑터를 얻는다.
        ArrayAdapter<String> regionAdapter =
                new ArrayAdapter<String>(RegisterActivity.this,
                        R.layout.item_region,
                        getResources().getStringArray(R.array.region));

        regionAdapter.setDropDownViewResource(R.layout.item_dropdown_region);

        // 지역 스피너에 지역 어댑터를 설정한다.
        regionSpinner.setAdapter(regionAdapter);

        regionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long resId) {

                isRegionProper = true;

                // 회원가입 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToRegisterBtn();

                // 처음 클릭하면
                if (!isSpinnerClickedAlready) {

                    isSpinnerClickedAlready = true;
                }
                else {

                    // 학원 에딧에 포커스를 준다.
                    academyEdit.setFocusableInTouchMode(true);
                    academyEdit.requestFocus();

                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {

                            // 키보드를 보이게 한다.
                            InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(academyEdit, 0);
                        }
                    }, 100L);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    // 학원 에딧을 설정한다.
    private void setAcademyEdit() {

        academyEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 완료 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_DONE) {

                    if (String.valueOf(academyEdit.getText()).isEmpty()) {

                        Toast.makeText(RegisterActivity.this, "학원 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    // 학원 프로그레스를 띄운다.
                    showAcademyProgress();

                    // 학원 팝업을 띄운다.
                    showAcademyPopup();
                }

                return false;
            }
        });
    }

    // 학원 프로그레스를 띄운다.
    private void showAcademyProgress() {

        FrameLayout.LayoutParams progressParam = new FrameLayout.LayoutParams((int) (25 * dp), (int) (25 * dp));

        Rect rect = new Rect();
        academyEdit.getPaint().getTextBounds(academyEdit.getText().toString(), 0, academyEdit.getText().length(), rect);

        progressParam.setMargins((int) (rect.width() + 15 * dp), (int) (1 * dp), 0, 0);
        progressParam.gravity = Gravity.CENTER_VERTICAL;

        academyProgress.setLayoutParams(progressParam);

        academyProgress.setVisibility(View.VISIBLE);
    }

    // 스피너에 학원 리스트를 띄운다.
    private void showAcademyPopup() {

        Integer[] count = { 0 };
        Map<String, List<AcademyInfoObject.AcademyInfo>> academyMap = new HashMap<>();

        String url =  "https://open.neis.go.kr/hub/acaInsTiInfo?" +
                "KEY=4eaf9303051a45449ebc41cd9e524240&Type=json&pIndex=1&pSize=100&" +
                "ACA_NM=" + academyEdit.getText() + "&" +
                "ATPT_OFCDC_SC_CODE=";

        if (regionSpinner.getSelectedItem() != null) {

            String regionCode = regionCodes.get(regionSpinner.getSelectedItemPosition());

            String academyUrl = url + regionCode;

            StringRequest academyRequest = new StringRequest(
                    Request.Method.GET,
                    academyUrl,
                    new com.android.volley.Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {

                            // 학원 프로그레스를 보이지 않게 한다.
                            academyProgress.setVisibility(View.GONE);

                            // 학원 리스트를 얻는다.
                            AcademyInfoObject academyInfoObject = new Gson().fromJson(response, AcademyInfoObject.class);
                            List<AcademyInfoObject.AcaInsTiInfo> acaInsTiInfoList = academyInfoObject.acaInsTiInfo;

                            List<AcademyInfoObject.AcademyInfo> academyInfoList =
                                    !acaInsTiInfoList.isEmpty() ? acaInsTiInfoList.get(1).row : new ArrayList<>();

                            // 학원 리스트가 비어있다면
                            if (academyInfoList.isEmpty()) {

                                Toast.makeText(RegisterActivity.this,
                                        "해당 학원이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                            }
                            // 학원 리스트가 비어있지 않다면
                            else {

                                // 학원 어댑터를 얻는다.
                                academyAdapter = new AcademyAdapter(academyInfoList);

                                // 학원 팝업에 학원 어댑터를 연결한다.
                                academyPopup.setAdapter(academyAdapter);

                                academyPopup.setWidth(academyPopup.getAnchorView().getWidth());
                                academyPopup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

                                // 학원 팝업을 띄운다.
                                academyPopup.show();
                            }

                            count[0]++;
                        }
                    },
                    new com.android.volley.Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("error", error.getMessage());
                        }
                    }
            ) {};

            academyRequest.setShouldCache(false);
            requestQueue.add(academyRequest);
        }
        else {

            for (String regionCode : regionCodes) {

                String academyUrl = url + regionCode;

                StringRequest academyRequest = new StringRequest(
                        Request.Method.GET,
                        academyUrl,
                        new com.android.volley.Response.Listener<String>() {

                            @Override
                            public void onResponse(String response) {

                                // 학원 프로그레스를 보이지 않게 한다.
                                academyProgress.setVisibility(View.GONE);

                                // 학원 맵에 학원 리스트를 넣는다.
                                AcademyInfoObject academyInfoObject = new Gson().fromJson(response, AcademyInfoObject.class);

                                List<AcademyInfoObject.AcaInsTiInfo> acaInsTiInfoList = academyInfoObject.acaInsTiInfo;

                                academyMap.put(regions.get(regionCodes.indexOf(regionCode)),
                                        !acaInsTiInfoList.isEmpty() ? acaInsTiInfoList.get(1).row : new ArrayList<>());

                                if (count[0] == 16) {

                                    // 학원 맵을 이용해 학원 리스트를 얻는다.
                                    List<AcademyInfoObject.AcademyInfo> academyInfoList = new ArrayList<>();

                                    for (String region : regions)
                                        academyInfoList.addAll(academyMap.get(region));

                                    // 학원 리스트가 비어있다면
                                    if (academyInfoList.isEmpty()) {

                                        Toast.makeText(RegisterActivity.this,
                                                "해당 학원이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                    // 학원 리스트가 비어있지 않다면
                                    else {

                                        // 학원 어댑터를 얻는다.
                                        academyAdapter = new AcademyAdapter(academyInfoList);

                                        // 학원 팝업에 학원 어댑터를 연결한다.
                                        academyPopup.setAdapter(academyAdapter);

                                        academyPopup.setWidth(academyPopup.getAnchorView().getWidth());
                                        academyPopup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

                                        // 학원 팝업을 띄운다.
                                        academyPopup.show();
                                    }
                                }

                                count[0]++;
                            }
                        },
                        new com.android.volley.Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("error", error.getMessage());
                            }
                        }
                ) {};

                academyRequest.setShouldCache(false);
                requestQueue.add(academyRequest);
            }
        }
    }

    // 학원 팝업을 설정한다.
    private void setAcademyPopup() {

        // 학원 팝업을 얻는다.
        academyPopup = new ListPopupWindow(RegisterActivity.this);

        academyPopup.setAnchorView(academyEdit);

        // 학원 팝업의 너비와 높이는 학교 에딧의 onEditorActionListener에서 설정한다.

        // 학원 팝업의 아이템을 클릭하면
        academyPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long resId) {

                // 학원 팝업을 없앤다.
                academyPopup.dismiss();

                isAcademyProper = true;

                // 회원가입 버튼의 색깔과 리스너를 설정한다.
                setColorAndOnClickListenerToRegisterBtn();

                AcademyInfoObject.AcademyInfo item = academyAdapter.getItem(pos);

                // 학원 에딧에 아이템을 설정한다.
                setItemToAcademyEdit(item);
            }
        });
    }

    // 학원 에딧에 아이템을 설정한다.
    private void setItemToAcademyEdit(AcademyInfoObject.AcademyInfo item) {

        // 학원 에딧에 학원 이름을 설정한다.
        academyEdit.setText(item.ACA_NM);

        // 학원 에딧에 선택한 아이템을 태그한다.
        Map<String, AcademyInfoObject.AcademyInfo> itemMap = new HashMap<>();
        itemMap.put("itemSelected", item);

        academyEdit.setTag(itemMap);

        // 학원 에딧에서 포커스를 제거한다.
        registerContainer.setFocusableInTouchMode(true);
        registerContainer.requestFocus();
    }

    // 회원가입 버튼을 클릭하면
    // -> 회원가입 처리한다.
    private void setOnClickListenerToRegisterBtn() {

        if (isEmailProper && isPasswordProper && isPasswordCheckProper && isNameProper &&
                ((jobRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_student && isSchoolProper && isGradeProper) ||
                        (jobRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_teacher && isRegionProper && isAcademyProper))) {

            registerContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    // 회원가입 프로그레스를 보이게 한다.
                    registerProgress.setVisibility(View.VISIBLE);

                    // 네트워크가 연결되어 있지 않다면
                    if (!isNetworkConnected(RegisterActivity.this)) {

                        Toast.makeText(RegisterActivity.this,
                                "인터넷이 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                    else {

                        // 회원가입한다.
                        signIn();
                    }
                }
            });
        }
        else {
            registerContainer.setOnClickListener(null);
        }
    }

    // 회원가입한다.
    private void signIn() {

        // 이메일, 비밀번호, 이름 등을 올바르게 입력했는지 확인한다.
        checkInput();

        // 입력된 값을 얻는다.
        String email = String.valueOf(emailEdit.getText());
        String password = String.valueOf(passwordEdit.getText());
        String name = String.valueOf(nameEdit.getText());

        HashMap<String, School> schoolTag = (HashMap<String, School>) schoolEdit.getTag();
        School schoolSelected = schoolTag != null ? schoolTag.get("itemSelected") : null;

        HashMap<String, AcademyInfoObject.AcademyInfo> academyTag =
                (HashMap<String, AcademyInfoObject.AcademyInfo>) academyEdit.getTag();
        Academy academySelected = academyTag != null ? Academy.toAcademy(academyTag.get("itemSelected")) : null;

        Integer grade = gradeSpinner.getSelectedItemPosition() + 1;

        mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {

                    private School school;
                    private Student student;

                    private Academy academy;
                    private Teacher teacher;

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            switch (jobRadioGroup.getCheckedRadioButtonId()) {

                                case R.id.radio_btn_student:

                                    // 학생을 회원가입을 처리한다.
                                    processStudentRegistration();
                                    break;

                                case R.id.radio_btn_teacher:

                                    // 강사를 회원가입을 처리한다.
                                    processTeacherRegistration();
                            }
                        }
                        else {

                            // 에러를 처리한다.
                            checkErrorFromFirebase(task);
                        }
                    }

                    // 학생을 회원가입을 처리한다.
                    private void processStudentRegistration() {

                        school = null;
                        student = null;

                        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();

                        // 학교를 저장한다.
                        saveSchool();

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                Log.d("error", "executed");

                                // 학교가 0.5초 이내에 초기화되지 않는다면
                                if (isSchoolNullForNms(500L)) return;

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {

                                        // 학생을 저장한다.
                                        saveStudent(email, name, school, grade);

                                        threadPool.execute(new Runnable() {

                                            @Override
                                            public void run() {

                                                // 학생이 0.2초 이내에 초기화되지 않는다면
                                                if (isStudentNullForNms(200L)) return;

                                                handler.post(new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        mDatabaseReference.child("student").child(firebaseUser.getUid()).setValue(student);
                                                    }
                                                });
                                            }
                                        });

                                        Toast.makeText(RegisterActivity.this,
                                                "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                                        // 회원가입 액티비티를 종료한다.
                                        finish();
                                    }
                                });
                            }
                        });
                    }

                    // 학교를 저장한다.
                    private void saveSchool() {

                        Call<School> call = loginApi.create(schoolSelected);

                        call.enqueue(new Callback<School>() {

                            @Override
                            public void onResponse(Call<School> call, Response<School> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "code : " + response.code());
                                    return;
                                }

                                school = response.body();
                            }

                            @Override
                            public void onFailure(Call<School> call, Throwable t) {
                                Log.d("error", t.getMessage());
                            }
                        });
                    }

                    // 학교가 null인지 0.00n초간 확인한다.
                    private boolean isSchoolNullForNms(long n) {

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

                            if (school == null) continue;

                            return false;
                        }

                        try {
                            Thread.sleep(15L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Log.d("error", "failed to initialize : school");

                        return true;
                    }

                    // 학생을 저장한다.
                    private void saveStudent(String email, String name, School school, Integer grade) {

                        Call<Student> call = loginApi.create(new Student(null, email, name, school.getId(), grade, LocalDate.now()));

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

                        Log.d("error", "failed to initialize : student");

                        return true;
                    }

                    // 강사를 회원가입을 처리한다.
                    private void processTeacherRegistration() {

                        academy = null;
                        teacher = null;

                        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();

                        // 학원을 저장한다.
                        saveAcademy();

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 학원이 0.2초 이내에 초기화되지 않는다면
                                if (isAcademyNullForNms(200L)) return;

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {

                                        // 강사를 저장한다.
                                        saveTeacher(email, name, academy);

                                        threadPool.execute(new Runnable() {

                                            @Override
                                            public void run() {

                                                // 강사가 0.2초 이내에 초기화되지 않는다면
                                                if (isTeacherNullForNms(200L)) return;

                                                handler.post(new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        mDatabaseReference.child("teacher").child(firebaseUser.getUid()).setValue(teacher);
                                                    }
                                                });
                                            }
                                        });

                                        Toast.makeText(RegisterActivity.this,
                                                "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                                        // 회원가입 액티비티를 종료한다.
                                        finish();
                                    }
                                });
                            }
                        });
                    }

                    // 학원을 저장한다.
                    private void saveAcademy() {

                        Call<Academy> call = loginApi.create(academySelected);

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

                    // 학원이 null인지 0.00n초간 확인한다.
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

                            if (academy == null) continue;

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

                    // 강사를 저장한다.
                    private void saveTeacher(String email, String name, Academy academy) {

                        Call<Teacher> call = loginApi.create(new Teacher(null, email, name, academy.getId()));

                        call.enqueue(new Callback<Teacher>() {

                            @Override
                            public void onResponse(Call<Teacher> call, Response<Teacher> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "code : " + response.code());
                                    return;
                                }

                                teacher = response.body();
                            }

                            @Override
                            public void onFailure(Call<Teacher> call, Throwable t) {
                                Log.d("error", t.getMessage());
                            }
                        });
                    }

                    // 강사가 null인지 0.00n초간 확인한다.
                    private boolean isTeacherNullForNms(long n) {

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

                            if (teacher == null) continue;

                            return false;
                        }

                        try {
                            Thread.sleep(15L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Log.d("error", "failed to initialize : teacher");

                        return true;
                    }
                });
    }

    // 이메일, 비밀번호, 이름 등을 올바르게 입력했는지 확인한다.
    private void checkInput() {

        if (isEmailProper && isPasswordProper && isPasswordCheckProper && isNameProper &&
                ((jobRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_student && isSchoolProper && isGradeProper) ||
                        (jobRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_teacher && isRegionProper && isAcademyProper))) {

            // 이메일 중복
            checkEmailDuplication();

            // 이메일 형식
            checkEmailFormat();

            // 비밀번호 형식
            checkPasswordFormat();

            // 비밀번호 일치
            checkPasswordMatch();

            // 이름 형식
            checkNameFormat();
        }
    }

    // 이메일이 중복되었는지 확인한다.
    private void checkEmailDuplication() {

        String email = String.valueOf(emailEdit.getText());

        // 학생을 얻는다.
        getStudent(email);

        // 강사를 얻는다.
        getTeacher(email);

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 학생과 강사가 0.2초 이내에 초기화되지 않는다면
                if (isTeacherAndStudentNotInitializedForNms(200L)) return;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 중복된 이메일이라면
                        if (student != null || teacher != null) {

                            // 이메일 중복
                            notifyEmailDuplicationError();

                            // 이메일 에딧에 포커스를 준다.
                            emailEdit.setFocusableInTouchMode(true);
                            emailEdit.requestFocus();

                            // 키보드를 보이게 한다.
                            InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(emailEdit, 0);
                        }
                        else {

                            // 이메일 탭을 원래대로 되돌린다.
                            resetEmailContainer();

                            if (!isPasswordProper) {

                                passwordContainer.setVisibility(View.VISIBLE);
                                passwordCheckContainer.setVisibility(View.VISIBLE);

                                // 비밀번호 에딧에 포커스를 준다.
                                passwordEdit.setFocusableInTouchMode(true);
                                passwordEdit.requestFocus();

                                // 키보드를 보이게 한다.
                                InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(passwordEdit, 0);
                            }
                        }
                    }
                });
            }
        });
    }

    // 학생을 얻는다.
    private void getStudent(String email) {

        Call<Student> call = loginApi.read(email);

        call.enqueue(new Callback<Student>() {

            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                student = response.body();
                isStudentInitialized = true;
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 강사를 얻는다.
    private void getTeacher(String email) {

        Call<Teacher> call = loginApi.readTeacher(email);

        call.enqueue(new Callback<Teacher>() {

            @Override
            public void onResponse(Call<Teacher> call, Response<Teacher> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                teacher = response.body();
                isTeacherInitialized = true;
            }

            @Override
            public void onFailure(Call<Teacher> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 학생이 null인지 0.00n초간 확인한다.
    private boolean isTeacherAndStudentNotInitializedForNms(long n) {

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

            if (!isStudentInitialized || !isTeacherInitialized) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("error", "failed to initialize : student, teacher");

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 네트워크가 연결되어 있지 않다면
                if (!isNetworkConnected(RegisterActivity.this)) {

                    Toast.makeText(RegisterActivity.this,
                            "인터넷이 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                }
                else {

                    Toast.makeText(RegisterActivity.this,
                            "알 수 없는 오류입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return true;
    }

    // 이메일 형식이 올바른지 확인한다.
    private void checkEmailFormat() {

        String email = String.valueOf(emailEdit.getText());

        Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;

        // 이메일을 잘못된 형식으로 입력하면
        if (!pattern.matcher(email).matches()) {

            // 이메일 형식
            notifyEmailWrongFormatError();
        }
        else {

            // 이메일 탭을 원래대로 되돌린다.
            resetEmailContainer();
        }
    }

    // 비밀번호 형식이 올바른지 확인한다.
    private void checkPasswordFormat() {

        String password = String.valueOf(passwordEdit.getText());

        // 비밀번호를 입력하지 않으면
        if (password.length() == 0) {

            // 비밀번호 형식
            notifyPasswordWrongFormatError();
        }
        else {

            // 비밀번호 탭을 원래대로 되돌린다.
            resetPasswordContainer();
        }
    }

    // 비밀번호가 일치하는지 확인한다.
    private void checkPasswordMatch() {

        String password = String.valueOf(passwordEdit.getText());
        String passwordCheck = String.valueOf(passwordCheckEdit.getText());

        // 비밀번호가 일치하지 않으면
        if (!password.equals(passwordCheck)) {

            // 비밀번호 불일치
            notifyPasswordNotMatchError();
        }
        else {

            // 비밀번호 탭과 비밀번호 확인 탭을 원래대로 되돌린다.
            resetPasswordContainer();
            resetPasswordCheckContainer();
        }
    }

    // 이름 형식이 올바른지 확인한다.
    private void checkNameFormat() {

        if (nameEdit.getText().length() == 0) {

            // 이름 유효성
            notifyNameWrongFormatError();
        }
        else {

            // 이름 탭을 원래대로 되돌린다.
            resetNameContainer();
        }
    }

    // 파이어베이스의 에러를 확인한다.
    private void checkErrorFromFirebase(Task<AuthResult> task) {

        // 이메일 중복
        if (task.getException() instanceof FirebaseAuthUserCollisionException) {

            notifyEmailDuplicationError();
        }
        // 이메일 형식
        else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {

            notifyEmailWrongFormatError();
        }

        // 비밀번호 형식
        if (task.getException() instanceof FirebaseAuthWeakPasswordException) {

            notifyPasswordWrongFormatError();
        }

        // 회원가입 프로그레스를 보이지 않게 한다.
        registerProgress.setVisibility(View.GONE);

        Toast.makeText(RegisterActivity.this,
                "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
    }

    // 이메일 형식
    private void notifyEmailWrongFormatError() {

        isEmailProper = false;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        emailErrorMsgText.setVisibility(View.VISIBLE);
        emailErrorMsgText.setText("이메일 형식이 올바르지 않습니다.");

        emailEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));

        // 이메일 에딧에 포커스를 준다.
        emailEdit.setFocusableInTouchMode(true);
        emailEdit.requestFocus();

        // 키보드를 보이게 한다.
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(emailEdit, 0);
    }

    // 이메일 중복
    private void notifyEmailDuplicationError() {

        isEmailProper = false;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        emailErrorMsgText.setVisibility(View.VISIBLE);
        emailErrorMsgText.setText("이미 사용 중인 이메일입니다.");

        emailEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));

        // 이메일 에딧에 포커스를 준다.
        emailEdit.setFocusableInTouchMode(true);
        emailEdit.requestFocus();

        // 키보드를 보이게 한다.
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(emailEdit, 0);
    }

    // 비밀번호 형식
    private void notifyPasswordWrongFormatError() {

        isPasswordProper = false;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        passwordErrorMsgText.setVisibility(View.VISIBLE);
        passwordErrorMsgText.setText("비밀번호가 너무 짧습니다.");

        passwordEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));

        // 비밀번호 에딧에 포커스를 준다.
        passwordEdit.setFocusableInTouchMode(true);
        passwordEdit.requestFocus();

        // 키보드를 보이게 한다.
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(passwordEdit, 0);
    }

    // 비밀번호 불일치
    private void notifyPasswordNotMatchError() {

        isPasswordCheckProper = false;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        passwordErrorMsgText.setText("비밀번호가 일치하지 않습니다.");
        passwordErrorMsgText.setVisibility(View.VISIBLE);

        passwordEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));
        passwordCheckEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));

        // 비밀번호 확인 에딧에 포커스를 준다.
        passwordCheckEdit.setFocusableInTouchMode(true);
        passwordCheckEdit.requestFocus();

        // 키보드를 보이게 한다.
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(passwordCheckEdit, 0);
    }

    // 이름 형식
    private void notifyNameWrongFormatError() {

        isNameProper = false;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        nameErrorMsgText.setText("이름을 입력해주세요.");
        nameErrorMsgText.setVisibility(View.VISIBLE);

        nameEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));

        // 이름 에딧에 포커스를 준다.
        nameEdit.setFocusableInTouchMode(true);
        nameEdit.requestFocus();

        // 키보드를 보이게 한다.
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(nameEdit, 0);
    }

    // 이메일 탭을 원래대로 되돌린다.
    private void resetEmailContainer() {

        isEmailProper = true;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        emailErrorMsgText.setVisibility(View.GONE);
        emailErrorMsgText.setText(null);

        emailEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle, null));
    }

    // 비밀번호 탭을 원래대로 되돌린다.
    private void resetPasswordContainer() {

        isPasswordProper = true;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        passwordErrorMsgText.setVisibility(View.GONE);
        passwordErrorMsgText.setText(null);

        passwordEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle, null));
    }

    // 비밀번호 확인 탭을 원래대로 되돌린다.
    private void resetPasswordCheckContainer() {

        isPasswordCheckProper = true;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        passwordCheckErrorMsgText.setVisibility(View.GONE);
        passwordCheckErrorMsgText.setText(null);

        passwordCheckEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle, null));
    }

    // 이름 탭을 원래대로 되돌린다.
    private void resetNameContainer() {

        isNameProper = true;

        // 회원가입 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToRegisterBtn();

        nameErrorMsgText.setVisibility(View.GONE);
        nameErrorMsgText.setText(null);

        nameEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle, null));
    }

    // 회원가입 버튼의 색깔과 리스너를 설정한다.
    private void setColorAndOnClickListenerToRegisterBtn() {

        // 회원가입 버튼의 색깔을 설정한다.
        setColorToRegisterBtn();

        // 회원가입 버튼을 클릭하면
        // -> 회원가입 처리한다.
        setOnClickListenerToRegisterBtn();
    }

    // 회원가입 버튼의 색깔을 설정한다.
    private void setColorToRegisterBtn() {

        int count = 0;

        if (isJobProper) count++;
        if (isEmailProper) count++;
        if (isPasswordProper) count++;
        if (isPasswordCheckProper) count++;
        if (isNameProper) count++;
        if (jobRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_student && isSchoolProper && isGradeProper) count++;
        if (jobRadioGroup.getCheckedRadioButtonId() == R.id.radio_btn_teacher && isRegionProper && isAcademyProper) count++;

        float progress = count / 6F;

        if (progress < 0.001F) {

            setWeightToRegisterBackgroundView(0F, 0F, 1F);
        }
        else if (progress > 0.999F) {
            setWeightToRegisterBackgroundView(1F, 0F, 0F);
        }
        else {
            setWeightToRegisterBackgroundView(progress - 0.1F, 0.2F, (1 - progress) - 0.1F);
        }
    }

    // 회원가입 배경 뷰에 weight을 설정한다.
    private void setWeightToRegisterBackgroundView(float v1, float v2, float v3) {

        // 왼쪽 뷰
        LinearLayout.LayoutParams leftParam = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT);
        leftParam.weight = v1;
        registerBackgroundLeftView.setLayoutParams(leftParam);

        // 가운데 뷰
        LinearLayout.LayoutParams centerParam = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT);
        centerParam.weight = v2;
        registerBackgroundCenterView.setLayoutParams(centerParam);

        // 오른쪽 뷰
        LinearLayout.LayoutParams rightParam = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT);
        rightParam.weight = v3;
        registerBackgroundRightView.setLayoutParams(rightParam);
    }

    // 네트워크가 연결되어있는지 확인한다.
    public boolean isNetworkConnected(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo wimax = manager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

        return mobile == null || mobile.isConnected() || wifi.isConnected() || (wimax != null && wimax.isConnected());
    }
}