package kr.co.problemtimertest.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.StudentMainActivity;
import kr.co.problemtimertest.adapter.BookStatisticsAdapter;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.ScoreByUnit2;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import kr.co.problemtimertest.service.TimerService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StudentInfoFragment extends Fragment {

    private Student student;

    private float dp;

    private TextView studentNicknameText;
    private TextView studentGradeText;
    private TextView scoreText;

    private RecyclerView bookStatisticsRecycler;
    private BookStatisticsAdapter bookStatisticsAdapter;

    // 학습 시간 관련 변수
    private TextView studyTimeForDayText;
    private TextView studyTimeForWeekText;
    private TextView studyTimeForMonthText;

    private SwipeRefreshLayout studentInfoSwipeRefresh;
    private NestedScrollView studentInfoNestedScroll;
    private ProgressBar studentInfoProgress;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
    private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);

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

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_student_info, container, false);

        // 변수를 초기화한다.
        initializeVariables(rootView);

        // 학생 정보 탭을 설정한다.
        setStudentInfoTab();

        // 학습 시간 탭을 설정한다.
        setStudyTimeTab();

        // 교재 통계 리싸이클러를 설정한다.
        setBookStatisticsRecycler();

        // 학생 정보 스와이프를 설정한다.
        setStudentInfoSwipeRefresh();

        return rootView;
    }

    // 변수를 초기화한다.
    private void initializeVariables(ViewGroup rootView) {

        student = ((StudentMainActivity) getActivity()).getStudent();

        dp = getResources().getDisplayMetrics().density;

        studentNicknameText = (TextView) rootView.findViewById(R.id.text_student_nickname);
        studentGradeText = (TextView) rootView.findViewById(R.id.text_student_grade);
        scoreText = (TextView) rootView.findViewById(R.id.text_score);

        bookStatisticsRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_my_book);

        studyTimeForDayText = (TextView) rootView.findViewById(R.id.text_study_time_for_day);
        studyTimeForWeekText = (TextView) rootView.findViewById(R.id.text_study_time_for_week);
        studyTimeForMonthText = (TextView) rootView.findViewById(R.id.text_study_time_for_month);

        studentInfoSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_student_info);
        studentInfoNestedScroll = (NestedScrollView) rootView.findViewById(R.id.nested_scroll_student_info);
        studentInfoProgress = (ProgressBar) rootView.findViewById(R.id.progress_student_info);
    }

    // 학생 정보 탭을 설정한다.
    private void setStudentInfoTab() {

        threadPool.execute(new Runnable() {

            private List<ScoreByUnit2> scoreByUnit2List;

            @Override
            public void run() {

                // 단원별 점수 리스트를 얻는다.
                getScoreByUnit2List();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 학생이 0.2초 이내에 초기화되지 않으면
                        if (isStudentNullForNms(200L)) {

                            Log.d("error", "failed to initialize : student");
                            return;
                        }

                        // 단원별 점수 리스트가 0.2초 이내에 초기화되지 않으면
                        if (isScoreByUnit2ListNullForNms(200L)) {

                            Log.d("error", "failed to initialize : scoreByUnit2List");
                            return;
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 학년 텍스트
                                studentGradeText.setText(ConversionService.gradeToStr(student.getGrade()));

                                // 닉네임 텍스트
                                studentNicknameText.setText(student.getEmail());

                                // 점수 텍스트
                                Float sum = 0F;

                                for (ScoreByUnit2 scoreByUnit2 : scoreByUnit2List) {
                                    sum += scoreByUnit2.getScore();
                                }

                                scoreText.setText(String.valueOf(sum));
                            }
                        });
                    }
                });
            }

            // 단원별 점수 리스트를 얻는다.
            private void getScoreByUnit2List() {

                Call<List<ScoreByUnit2>> call = statisticsApi.readScores(student.getId());

                call.enqueue(new Callback<List<ScoreByUnit2>>() {

                    @Override
                    public void onResponse(Call<List<ScoreByUnit2>> call, Response<List<ScoreByUnit2>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        scoreByUnit2List = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<ScoreByUnit2>> call, Throwable t) {
                        Log.d("error", "error : " + t.getMessage());
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

            // 단원별 점수 리스트가 null인지 0.00n초간 확인한다.
            private boolean isScoreByUnit2ListNullForNms(long n) {

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

                    if (scoreByUnit2List == null) continue;

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

    // 학습 시간 탭을 설정한다.
    private void setStudyTimeTab() {

        threadPool.execute(new Runnable() {

            private Float studyTimeForDay;
            private Float studyTimeForWeek;
            private Float studyTimeForMonth;

            @Override
            public void run() {

                // 하루 공부 시간을 얻는다.
                initializeStudyTimeForDay();

                // 일주일 공부 시간을 얻는다.
                initializeStudyTimeForWeek();

                // 한달 공부 시간을 얻는다.
                initializeStudyTimeForMonth();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 하루 공부 시간이 0.2초 안에 초기화되지 않으면
                        if (isStudyTimeForDayNullForNms(200L)) {

                            Log.d("error", "failed to initialize : studyTimeForDay");
                            return;
                        }

                        // 일주일 공부 시간이 0.2초 안에 초기화되지 않으면
                        if (isStudyTimeForWeekNullForNms(200L)) {

                            Log.d("error", "failed to initialize : studyTimeForWeek");
                            return;
                        }

                        // 한달 공부 시간이 0.2초 안에 초기화되지 않으면
                        if (isStudyTimeForMonthNullForNms(200L)) {

                            Log.d("error", "failed to initialize : studyTimeForMonth");
                            return;
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 하루 공부 시간 텍스트를 설정한다.
                                studyTimeForDayText.setText(TimerService.convertToTimestamp(
                                        (long) Math.round(studyTimeForDay * 1000F)));

                                // 일주일 공부 시간 텍스트를 설정한다.
                                studyTimeForWeekText.setText(TimerService.convertToTimestamp(
                                        (long) Math.round(studyTimeForWeek * 1000F)));

                                // 한달 공부 시간 텍스트를 설정한다.
                                studyTimeForMonthText.setText(TimerService.convertToTimestamp(
                                        (long) Math.round(studyTimeForMonth * 1000F)));
                            }
                        });
                    }
                });
            }

            // 하루 공부 시간을 얻는다.
            private void initializeStudyTimeForDay() {

                Call<Float> studyTimeForDayCall = statisticsApi.read(student.getId(), LocalDate.now(), LocalDate.now());

                studyTimeForDayCall.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        studyTimeForDay = response.body();
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", "\n" + t.getMessage());
                    }
                });
            }

            // 일주일 공부 시간을 얻는다.
            private void initializeStudyTimeForWeek() {

                Call<Float> studyTimeForWeekCall = statisticsApi.read(student.getId(), LocalDate.now().plusDays(-7), LocalDate.now());

                studyTimeForWeekCall.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        studyTimeForWeek = response.body();
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", "\n" + t.getMessage());
                    }
                });
            }

            // 한달 공부 시간을 얻는다.
            private void initializeStudyTimeForMonth() {

                Call<Float> studyTimeForMonthCall = statisticsApi.read(student.getId(), LocalDate.now().plusDays(-30), LocalDate.now());

                studyTimeForMonthCall.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        studyTimeForMonth = response.body();
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", "\n" + t.getMessage());
                    }
                });
            }

            // 하루 공부 시간이 null인지 0.00n초간 확인한다.
            private boolean isStudyTimeForDayNullForNms(long n) {

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

                    if (studyTimeForDay == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 일주일 공부 시간이 null인지 0.00n초간 확인한다.
            private boolean isStudyTimeForWeekNullForNms(long n) {

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

                    if (studyTimeForWeek == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 한달 공부 시간이 null인지 0.00n초간 확인한다.
            private boolean isStudyTimeForMonthNullForNms(long n) {

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

                    if (studyTimeForMonth == null) continue;

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

    // 교재 통계 리싸이클러를 설정한다.
    private void setBookStatisticsRecycler() {

        // 교재 통계 어댑터를 얻는다.
        bookStatisticsAdapter = new BookStatisticsAdapter(student);

        // 교재 통계 어댑터에 아이템을 추가한다.
        addItemsToBookStatisticsAdapter();

        // 교재 통계 리싸이클러에 교재 통계 어댑터를 설정한다.
        setBookStatisticsAdapterToBookStatisticsRecycler();
    }

    // 교재 통계 어댑터에 아이템을 추가한다.
    private void addItemsToBookStatisticsAdapter() {

        threadPool.execute(new Runnable() {

            private List<Book> books;

            @Override
            public void run() {

                // 교재 리스트를 초기화한다.
                initializeBooks();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재 리스트가 0.2초 안에 초기화되지 않으면
                        if (isBooksNullForNms(200L)) {

                            Log.d("error", "failed to initialize : books");
                            return;
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 통계 어댑터에 아이템을 추가한다.
                                books.forEach(book -> bookStatisticsAdapter.addItem(book));

                                bookStatisticsAdapter.notifyDataSetChanged();

                                handler.postDelayed(new Runnable() {

                                    @Override
                                    public void run() {

                                        studentInfoProgress.setVisibility(View.GONE);

                                        studentInfoNestedScroll.setVisibility(View.VISIBLE);

                                        // 새로고침을 끝낸다.
                                        studentInfoSwipeRefresh.setRefreshing(false);
                                    }
                                }, 200L);
                            }
                        });
                    }
                });
            }

            // 교재 리스트를 초기화한다.
            private void initializeBooks() {

                Call<List<Book>> call = timerApi.list(1L);

                call.enqueue(new Callback<List<Book>>() {

                    @Override
                    public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        books = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<Book>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 교재 리스트가 null인지 0.00n초간 확인한다.
            private boolean isBooksNullForNms(long n) {

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

                    if (books == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        studentInfoProgress.setVisibility(View.GONE);

                        studentInfoNestedScroll.setVisibility(View.VISIBLE);

                        // 새로고침을 끝낸다.
                        studentInfoSwipeRefresh.setRefreshing(false);
                    }
                });

                return true;
            }
        });
    }

    // 교재 통계 리싸이클러에 교재 통계 어댑터를 설정한다.
    private void setBookStatisticsAdapterToBookStatisticsRecycler() {

        // 교재 통계 리싸이클러의 방향을 세로로 설정한다.
        bookStatisticsRecycler.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));

        // 교재 통계 리싸이클러에 교재 통계 어댑터를 설정한다.
        bookStatisticsRecycler.setAdapter(bookStatisticsAdapter);
    }

    // 학생 정보 스와이프를 설정한다.
    private void setStudentInfoSwipeRefresh() {

        studentInfoSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                studentInfoNestedScroll.setVisibility(View.GONE);

                // 교재 통계 리싸이클러를 설정한다.
                setBookStatisticsRecycler();
            }
        });
    }
}