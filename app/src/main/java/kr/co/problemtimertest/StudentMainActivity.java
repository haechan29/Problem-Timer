package kr.co.problemtimertest;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.api.LoginApi;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.fragment.ManagementFragment;
import kr.co.problemtimertest.fragment.StudentAssignmentFragment;
import kr.co.problemtimertest.fragment.StudentInfoFragment;
import kr.co.problemtimertest.fragment.TeacherAssignmentFragment;
import kr.co.problemtimertest.fragment.TimerFragment;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StudentMainActivity extends AppCompatActivity {

    private Student student;

    private TimerFragment timerFragment;
    private StudentAssignmentFragment studentAssignmentFragment;
    private StudentInfoFragment studentInfoFragment;

    // firebase 관련 변수
    private final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private final DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference("problemTimer");

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
    private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);
    private final ManagementApi managementApi = retrofit.create(ManagementApi.class);
    private final LoginApi loginApi = retrofit.create(LoginApi.class);

    // ThreadPool 관련 변수
    private ExecutorService threadPool = new ThreadPoolExecutor(
            3,                      // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        // 변수를 초기화한다.
        initializeVariable();

        // 학생을 얻는다.
        initializeStudent();

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 학생이 0.2초 이내에 초기화되지 않으면
                if (isStudentNullForNms(200L)) return;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 첫째 프래그먼트를 디폴트로 설정한다.
                        getSupportFragmentManager().beginTransaction().replace(R.id.container_student_main, timerFragment).commit();

                        // 하단탭을 만들고 리소스를 설정한다.
                        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

                        // 하단탭이 클릭되면 프래그먼트가 보이도록 설정한다.
                        bottomNavigationView.setOnNavigationItemSelectedListener(
                                new BottomNavigationView.OnNavigationItemSelectedListener() {
                                    @Override
                                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                                        switch (item.getItemId()) {

                                            case R.id.tab_problem:
                                                getSupportFragmentManager().beginTransaction()
                                                        .replace(R.id.container_student_main, timerFragment).commit();

                                                return true;

                                            case R.id.tab_assignment_student:
                                                getSupportFragmentManager().beginTransaction()
                                                        .replace(R.id.container_student_main, studentAssignmentFragment).commit();

                                                return true;

                                            case R.id.tab_statistics:
                                                getSupportFragmentManager().beginTransaction()
                                                        .replace(R.id.container_student_main, studentInfoFragment).commit();

                                                return true;
                                        }

                                        return false;
                                    }
                                }
                        );
                    }
                });
            }
        });
    }

    // 변수를 초기화한다.
    private void initializeVariable() {

        timerFragment = new TimerFragment();
        studentAssignmentFragment = new StudentAssignmentFragment();
        studentInfoFragment = new StudentInfoFragment();
    }

    // 학생을 얻는다.
    private void initializeStudent() {

        Call<Student> call = loginApi.read(mFirebaseAuth.getCurrentUser().getEmail());

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

        Log.d("error", "failed to get : student");

        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(StudentMainActivity.this, "학생 정보를 받아오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    public Student getStudent() {
        return student;
    }

    public TimerFragment getTimerFragment() {
        return timerFragment;
    }
}