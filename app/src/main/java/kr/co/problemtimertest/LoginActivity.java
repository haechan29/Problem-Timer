package kr.co.problemtimertest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import kr.co.problemtimertest.api.LoginApi;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.Teacher;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabaseReference;

    private EditText emailEdit, passwordEdit;
    private TextView emailErrorMsgText, passwordErrorMsgText;
    private ProgressBar logInProgress;

    private LinearLayout logInContainer, logInBackgroundContainer, registerContainer;

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
        setContentView(R.layout.activity_login);

        // 변수를 초기화한다.
        initializeVariable();

        // 이메일 탭
        setEmailContainer();

        // 비밀번호 탭
        setPasswordContainer();

        // 회원가입 탭
        setRegistrationContainer();
    }

    // 변수를 초기화한다.
    private void initializeVariable() {

        emailEdit = (EditText) findViewById(R.id.edit_email);
        passwordEdit = (EditText) findViewById(R.id.edit_password);

        emailErrorMsgText = (TextView) findViewById(R.id.text_email_error_msg);
        passwordErrorMsgText = (TextView) findViewById(R.id.text_password_error_msg);

        logInProgress = (ProgressBar) findViewById(R.id.progress_login);

        logInContainer = (LinearLayout) findViewById(R.id.container_login);
        logInBackgroundContainer = (LinearLayout) findViewById(R.id.container_login_background);
        registerContainer = (LinearLayout) findViewById(R.id.container_register);
    }

    // 이메일 탭을 설정한다.
    private void setEmailContainer() {

        emailEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 다음 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_NEXT) {

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

                        // 이메일 탭을 원래대로 되돌린다.
                        resetEmailContainer();
                    }
                }

                return false;
            }
        });
    }

    // 비밀번호 탭을 설정한다.
    private void setPasswordContainer() {

        passwordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                // 완료 키가 입력될 경우
                if (keyCode == EditorInfo.IME_ACTION_DONE) {

                    String password = String.valueOf(passwordEdit.getText());

                    // 비밀번호를 입력하지 않으면
                    if (password.length() == 0) {

                        // 비밀번호 형식
                        notifyPasswordWrongFormatError();

                        // 포커스를 유지한다.
                        return true;
                    }
                    else {

                        // 비밀번호 탭을 원래대로 되돌린다.
                        resetPasswordContainer();

                        // 비밀번호 에딧에서 포커스를 제거한다.
                        logInContainer.setFocusableInTouchMode(true);
                        logInContainer.requestFocus();

                        // 키보드를 보이지 않게 한다.
                        InputMethodManager imm = (InputMethodManager) LoginActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {

                                logInContainer.performClick();
                            }
                        }, 200L);
                    }
                }

                return false;
            }
        });
    }

    // 회원가입 탭을 설정한다.
    private void setRegistrationContainer() {

        // 회원가입 버튼에 애니메이션을 적용한다.
        setAnimationToRegistrationContainer();

        // 회원가입 버튼을 클릭하면
        // -> 회원가입 액티비티로 이동한다.
        setOnClickListenerToRegisterContainer();
    }

    // 회원가입 버튼에 애니메이션을 적용한다.
    private void setAnimationToRegistrationContainer() {

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                Animation animation = new AlphaAnimation(0, 1);
                animation.setDuration(1000L);
                registerContainer.setAnimation(animation);
                registerContainer.setVisibility(View.VISIBLE);
            }
        }, 1000L);
    }

    // 회원가입 버튼을 클릭하면
    private void setOnClickListenerToRegisterContainer() {

        registerContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);

                // 회원가입 페이지로 이동한다.
                startActivity(intent);
            }
        });
    }

    // 로그인 버튼을 클릭하면
    // -> 로그인한다.
    private void setOnClickListenerToLogInBtn() {

        if (isEmailProper() && isPasswordProper()) {

            logInContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    if (isEmailProper() && isPasswordProper()) {

                        // 로그인 프로그레스를 보이게 한다.
                        logInProgress.setVisibility(View.VISIBLE);

                        // 네트워크가 연결되어 있지 않다면
                        if (!isNetworkConnected(LoginActivity.this)) {

                            Toast.makeText(LoginActivity.this,
                                    "인터넷이 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                        } else {

                            // 로그인한다.
                            signIn();
                        }
                    }
                    else {

                        logInBackgroundContainer.setBackgroundColor(getResources().getColor(R.color.disabled, null));

                        // 로그인 프로그레스를 보이지 않게 한다.
                        logInProgress.setVisibility(View.GONE);

                        logInContainer.setOnClickListener(null);
                    }
                }
            });
        }
        else {

            // 로그인 프로그레스를 보이지 않게 한다.
            logInProgress.setVisibility(View.GONE);

            logInContainer.setOnClickListener(null);
        }
    }

    // 로그인한다.
    private void signIn() {

        // 이메일과 비밀번호를 올바르게 입력했는지 확인한다.
        checkEmailAndPassword();

        // 이메일이 데이터베이스에 존재하는지 확인한다.
        checkDatabase();
    }

    private boolean isEmailProper() {

        String email = String.valueOf(emailEdit.getText());
        Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;

        return pattern.matcher(email).matches();
    }

    private boolean isPasswordProper() {

        String password = String.valueOf(passwordEdit.getText());

        return password.length() != 0;
    }

    // 이메일과 비밀번호를 올바르게 입력했는지 확인한다.
    private void checkEmailAndPassword() {

        String email = String.valueOf(emailEdit.getText());
        String password = String.valueOf(passwordEdit.getText());

        if (!email.isEmpty()  && !password.isEmpty()) {

            // 이메일 형식
            checkEmailFormat();

            // 비밀번호 형식
            checkPasswordFormat();
        }
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

    // 이메일이 데이터베이스에 존재하는지 확인한다.
    private void checkDatabase() {

        if (isEmailProper() && isPasswordProper()) {

            boolean[] isStudentInitialized = { false };
            Student[] student = new Student[1];

            boolean[] isTeacherInitialized = { false };
            Teacher[] teacher = new Teacher[1];

            String email = String.valueOf(emailEdit.getText());

            // 학생을 얻는다.
            getStudent(email, student, isStudentInitialized);

            // 강사를 얻는다.
            getTeacher(email, teacher, isTeacherInitialized);

            threadPool.execute(new Runnable() {

                @Override
                public void run() {

                    // 학생과 강사가 모두 0.5초 이내에 초기화되지 않는다면
                    if (isStudentAndTeacherNotInitializedForNms(500L, isStudentInitialized, isTeacherInitialized)) return;

                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {

                            // 이메일이 데이터베이스에 존재한다면
                            if (student[0] != null || teacher[0] != null) {

                                // 이메일 탭을 원래대로 되돌린다.
                                resetEmailContainer();

                                // 이메일이 파이어베이스에 존재하는지 확인한다.
                                checkFirebase(student[0], teacher[0]);
                            }
                            // 이메일이 데이터베이스에 존재하지 않으면
                            else {

                                // 이메일 오류
                                notifyEmailWrongInputError();

                                // 로그인 프로그레스를 보이지 않게 한다.
                                logInProgress.setVisibility(View.GONE);
                            }
                        }
                    }, 100L);
                }
            });
        }
    }

    // 학생을 얻는다.
    private void getStudent(String email, Student[] student, boolean[] isStudentInitialized) {

        Call<Student> call = loginApi.read(email);

        call.enqueue(new Callback<Student>() {

            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                student[0] = response.body();
                isStudentInitialized[0] = true;
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 강사를 얻는다.
    private void getTeacher(String email, Teacher[] teacher, boolean[] isTeacherInitialized) {

        Call<Teacher> call = loginApi.readTeacher(email);

        call.enqueue(new Callback<Teacher>() {

            @Override
            public void onResponse(Call<Teacher> call, Response<Teacher> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                teacher[0] = response.body();
                isTeacherInitialized[0] = true;
            }

            @Override
            public void onFailure(Call<Teacher> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 학생과 강사 중 하나라도 초기화되었는지 0.00n초간 확인한다.
    private boolean isStudentAndTeacherNotInitializedForNms(long n, boolean[] isStudentInitialized, boolean[] isTeacherInitialized) {

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

            // 둘 다 초기화되지 않았다면
            if (!isStudentInitialized[0] && !isTeacherInitialized[0]) continue;

            // 둘 중 하나라도 초기화되었다면
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

                // 로그인 프로그레스를 보이지 않게 한다.
                logInProgress.setVisibility(View.GONE);

                // 네트워크가 연결되어 있지 않다면
                if (!isNetworkConnected(LoginActivity.this)) {

                    Toast.makeText(LoginActivity.this,
                            "인터넷이 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                }
                else {

                    Toast.makeText(LoginActivity.this,
                            "알 수 없는 오류입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return true;
    }

    // 이메일이 파이어베이스에 존재하는지 확인한다.
    private void checkFirebase(Student student, Teacher teacher) {

        String email = String.valueOf(emailEdit.getText());
        String password = String.valueOf(passwordEdit.getText());

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("problemTimer");

        mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                // 로그인 프로그레스를 보이지 않게 한다.
                logInProgress.setVisibility(View.GONE);

                if (task.isSuccessful()) {

                    Intent intent = new Intent(LoginActivity.this,
                            student != null ? StudentMainActivity.class : TeacherMainActivity.class);

                    // 회원가입 페이지로 이동한다.
                    startActivity(intent);
                }
                else {

                    // 파이어베이스의 에러를 확인한다.
                    checkErrorFromFirebase(task);
                }
            }
        });
    }

    // 파이어베이스의 에러를 확인한다.
    private void checkErrorFromFirebase(Task<AuthResult> task) {

        // 이메일 형식
        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {

            String email = String.valueOf(emailEdit.getText());
            Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;

            if (!pattern.matcher(email).matches()) {
                notifyEmailWrongFormatError();
            }
        }
        // 이메일 오류
        else if (task.getException() instanceof FirebaseAuthInvalidUserException) {

            notifyEmailWrongInputError();
        }

        // 비밀번호 오류
        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {

            String email = String.valueOf(emailEdit.getText());
            Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;

            if (pattern.matcher(email).matches()) {
                notifyPasswordWrongInputError();
            }
        }
        // 비밀번호 형식
        else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {

            notifyPasswordWrongFormatError();
        }

        // 로그인 프로그레스를 보이지 않게 한다.
        logInProgress.setVisibility(View.GONE);

        Toast.makeText(LoginActivity.this,
                "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
    }

    // 이메일 오류
    private void notifyEmailWrongInputError() {

        // 로그인 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToLogInBtn();

        emailErrorMsgText.setVisibility(View.VISIBLE);
        emailErrorMsgText.setText("가입되지 않은 아이디입니다.");

        emailEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));
    }

    // 이메일 형식
    private void notifyEmailWrongFormatError() {

        // 로그인 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToLogInBtn();

        emailErrorMsgText.setVisibility(View.VISIBLE);
        emailErrorMsgText.setText("이메일 형식이 올바르지 않습니다.");

        emailEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));
    }

    // 비밀번호 오류
    private void notifyPasswordWrongInputError() {

        // 로그인 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToLogInBtn();

        passwordErrorMsgText.setVisibility(View.VISIBLE);
        passwordErrorMsgText.setText("비밀번호가 틀립니다.");

        passwordEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));
    }

    // 비밀번호 형식
    private void notifyPasswordWrongFormatError() {

        // 로그인 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToLogInBtn();

        passwordErrorMsgText.setVisibility(View.VISIBLE);
        passwordErrorMsgText.setText("비밀번호를 입력해주세요.");

        passwordEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle_warning, null));
    }

    // 이메일 탭을 원래대로 되돌린다.
    private void resetEmailContainer() {

        // 로그인 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToLogInBtn();

        emailErrorMsgText.setVisibility(View.GONE);
        emailErrorMsgText.setText(null);

        emailEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle, null));
    }

    // 비밀번호 탭을 원래대로 되돌린다.
    private void resetPasswordContainer() {

        // 로그인 버튼의 색깔과 리스너를 설정한다.
        setColorAndOnClickListenerToLogInBtn();

        passwordErrorMsgText.setVisibility(View.GONE);
        passwordErrorMsgText.setText(null);

        passwordEdit.setBackground(getResources().getDrawable(R.drawable.selector_edit_text_rectangle, null));
    }

    // 로그인 버튼의 색깔과 리스너를 설정한다.
    private void setColorAndOnClickListenerToLogInBtn() {

        // 색깔
        logInBackgroundContainer.setBackgroundColor(getResources().getColor(
                isEmailProper() && isPasswordProper() ? R.color.activated : R.color.disabled, null));

        // OnClickListener
        setOnClickListenerToLogInBtn();
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