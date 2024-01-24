package kr.co.problemtimertest.impl;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.ProblemStatisticsAdapter;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.listener.SetBookStatisticsRecyclerInterface;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.BookUnit1;
import kr.co.problemtimertest.model.BookUnit2;
import kr.co.problemtimertest.model.ScoreStatistics;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SetBookStatisticsRecyclerInterfaceImpl implements SetBookStatisticsRecyclerInterface {

    private float dp;

    private RecyclerView.ViewHolder holder;
    private int position;
    private Student student;
    private Book item;

    private boolean isPageOpen;

    private ConstraintLayout bookContainer;
    private ConstraintLayout bookPercentageContainer;
    private TextView bookPercentageTopText;
    private TextView bookPercentageText;
    private TextView bookScoreText;

    private LinearLayout problemStatisticsContainer;
    private SwitchCompat howToViewSwitch;
    private LinearLayout byUnitContainer;
    private LinearLayout byProblemContainer;

    private TextView unitNameArrowLeft;
    private TextView unitNameArrowRight;
    private TextView unitNameUnit1Text;
    private TextView unitNameUnit2Text;
    private LinearLayout unitNameScoreContainer;

    private LinearLayout formByProblemContainer;
    private RecyclerView byProblemRecycler;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
    private final StatisticsApi statisticsApi = retrofit.create(StatisticsApi.class);

    // ThreadPool 관련 변수
    private final ExecutorService threadPool = new ThreadPoolExecutor(
            3,                      // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void setBookStatisticsRecycler(RecyclerView.ViewHolder holder, int position, Student student, Book item) {

        // 변수를 초기화한다.
        initializeVariables(holder, position, student, item);

        // 교재 탭을 설정한다.
        setBookContainer();

        // 스위치를 설정한다.
        setHowToViewSwitch();
    }

    // 변수를 초기화한다.
    private void initializeVariables(RecyclerView.ViewHolder holder, int position, Student student, Book item) {

        this.holder = holder;
        this.position = position;
        this.student = student;
        this.item = item;

        dp = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        bookContainer = (ConstraintLayout) holder.itemView.findViewById(R.id.container_book);
        bookPercentageContainer =
                (ConstraintLayout) holder.itemView.findViewById(R.id.container_book_percentage);
        bookPercentageTopText = (TextView) holder.itemView.findViewById(R.id.text_book_percentage_top);
        bookPercentageText = (TextView) holder.itemView.findViewById(R.id.text_book_percentage);
        bookScoreText = (TextView) holder.itemView.findViewById(R.id.text_book_score);

        problemStatisticsContainer =
                (LinearLayout) holder.itemView.findViewById(R.id.container_problem_statistics);
        howToViewSwitch = (SwitchCompat) holder.itemView.findViewById(R.id.switch_how_to_view);
        byUnitContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_by_unit);
        byProblemContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_by_problem);

        unitNameArrowLeft = (TextView) holder.itemView.findViewById(R.id.arrow_left_unit_name);
        unitNameArrowRight = (TextView) holder.itemView.findViewById(R.id.arrow_right_unit_name);
        unitNameUnit1Text = (TextView) holder.itemView.findViewById(R.id.text_unit_name_unit_1);
        unitNameUnit2Text = (TextView) holder.itemView.findViewById(R.id.text_unit_name_unit_2);
        unitNameScoreContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_unit_name_score);

        formByProblemContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_by_problem_form);
        byProblemRecycler = (RecyclerView) holder.itemView.findViewById(R.id.recycler_by_problem);
    }

    // 교재 탭을 설정한다.
    private void setBookContainer() {

        // 뷰 홀더에 아이템을 설정한다.
        setItemToViewHolder();

        // 교재 탭을 클릭하면
        // -> 열리고 닫힌다.
        setOnClickListenerToBookContainer();

        // 단원 점수표를 클릭하면
        // -> 문제별 문제 통계 탭을 설정한다.
        setOnClickListenerToScoreByUnit2Grid();
    }

    // 뷰 홀더에 아이템을 설정한다.
    private void setItemToViewHolder() {

        threadPool.execute(new Runnable() {

            private Float avgOfPercentages;
            private Float sum;

            @Override
            public void run() {

                // 단원별 점수의 백분율 평균을 얻는다.
                getAvgOfPercentages();

                // 단원별 점수의 합을 얻는다.
                getSum();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 단원별 점수의 백분율 평균이 0.05초 이내에 초기화되지 않으면
                        if (isAvgOfPercentageNullForNms(50L)) {}

                        // 단원별 점수의 합이 0.05초 이내에 초기화되지 않으면
                        if (isSumNullForNms(50L)) {}

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 백분율 텍스트
                                if (avgOfPercentages != null) {

                                    // 백분율 컨테이너의 색깔을 설정한다.
                                    setBackgroundToBookPercentageContainer(avgOfPercentages, bookPercentageContainer);

                                    bookPercentageTopText.setText("상위");

                                    bookPercentageText.setText(String.format("%.1f%%", avgOfPercentages));
                                }
                                else {
                                    bookPercentageText.setText(" - ");
                                }

                                // 점수 텍스트
                                bookScoreText.setText(sum != null ?
                                        String.format("%.1f", sum) : " - ");
                            }
                        });
                    }
                });
            }

            // 단원별 점수의 백분율 평균을 얻는다.
            private void getAvgOfPercentages() {

                Call<Float> call = statisticsApi.calculate(student.getId(), item.getId());

                call.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        avgOfPercentages = response.body();
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", "error : " + t.getMessage());
                    }
                });
            }

            // 단원별 점수의 합을 얻는다.
            private void getSum() {

                Call<Float> call = statisticsApi.calculateScores(student.getId(), item.getId());

                call.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        sum = response.body();
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", "error : " + t.getMessage());
                    }
                });
            }

            // 단원별 점수의 백분율 평균이 null인지 0.00n초간 확인한다.
            private boolean isAvgOfPercentageNullForNms(long n) {

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

                    if (avgOfPercentages == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 단원별 점수의 합이 null인지 0.00n초간 확인한다.
            private boolean isSumNullForNms(long n) {

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

                    if (sum == null) continue;

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

    // 백분율 컨테이너의 색깔을 설정한다.
    private void setBackgroundToBookPercentageContainer(Float avgOfPercentages, ConstraintLayout bookPercentageContainer) {

        if (avgOfPercentages > 96F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude1_circle, null));
        else if (avgOfPercentages > 89F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude2_circle, null));
        else if (avgOfPercentages > 77F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude3_circle, null));
        else if (avgOfPercentages > 60F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude4_circle, null));
        else if (avgOfPercentages > 40F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude5_circle, null));
        else if (avgOfPercentages > 23F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude6_circle, null));
        else if (avgOfPercentages > 11F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude7_circle, null));
        else if (avgOfPercentages > 4F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude8_circle, null));
        else if (avgOfPercentages > 1F)
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude9_circle, null));
        else
            bookPercentageContainer.setBackground(
                    holder.itemView.getResources().getDrawable(
                            R.drawable.shape_magnitude10plus_circle, null));
    }

    // 교재 탭을 클릭하면
    // -> 열리고 닫힌다.
    private void setOnClickListenerToBookContainer() {

        bookContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 단원 점수표를 설정한다.
                setScoreByUnit2GridToByUnitContainer();
            }
        });
    }

    // 단원 점수표를 설정한다.
    private void setScoreByUnit2GridToByUnitContainer() {

        // 문제 통계 탭의 visibility에 따라 isPageOpen을 설정한다.
        updateIsPageOpen();

        // 문제 통계 탭에 애니메이션을 설정한다.
        setAnimationListener();

        // 문제 통계 탭이 닫혀있다면
        if (!isPageOpen) {

            // 단원별 문제 통계 탭에 단원 점수표를 추가한다.
            addScoreByUnit2GridToByUnitContainer();
        }
        // 문제 통계 탭이 열려있다면
        else {

            // 단원별 문제 통계 탭을 비운다.
            byUnitContainer.removeAllViews();
        }

        // 교재 탭을 0.5초간 사용할 수 없게 한다.
        setEnabledFalseToBookContainer();
    }

    // 문제 통계 탭의 visibility에 따라 isPageOpen을 설정한다.
    private void updateIsPageOpen() {

        switch (problemStatisticsContainer.getVisibility()) {

            // 문제 통계 탭이 보이지 않게 되어 있는 경우
            case View.GONE:
                isPageOpen = false;
                break;

            // 문제 통계 탭이 보이게 설정되어 있는 경우
            case View.VISIBLE:
                isPageOpen = true;
        }
    }

    // 문제 통계 탭에 애니메이션을 설정한다.
    private void setAnimationListener() {

        // 위로 올라오기 애니메이션과 아래로 내려가기 애니메이션을 얻는다.
        Animation translateUpAnim = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.translate_up);
        Animation translateDownAnim = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.translate_down);

        // SlidingPageAnimation 객체를 만들고 위로 올라오기, 아래로 내려가기 애니메이션에 해당 객체를 적용한다.
        Animation.AnimationListener animListener = new AnimationListenerImpl(isPageOpen, problemStatisticsContainer);
        translateDownAnim.setAnimationListener(animListener);
        translateUpAnim.setAnimationListener(animListener);

        // 페이지가 열려있는 경우
        if (isPageOpen) {

            // 문제 통계 탭에 아래로 내려가기 애니메이션을 적용한다.
            problemStatisticsContainer.startAnimation(translateDownAnim);
        }
        // 페이지가 닫혀있는 경우
        else {

            // 문제 통계 탭을 보이지 않게 하고, 위로 올라가기 애니메이션을 적용한다.
            problemStatisticsContainer.setVisibility(View.VISIBLE);
            problemStatisticsContainer.startAnimation(translateUpAnim);
        }
    }

    // 단원별 문제 통계 탭에 단원 점수표를 추가한다.
    private void addScoreByUnit2GridToByUnitContainer() {

        new AddScoreByUnit2GridInterfaceImpl(holder, student, item).addScoreByUnit2Grid();
    }

    // 교재 탭을 0.5초간 사용할 수 없게 한다.
    private void setEnabledFalseToBookContainer() {

        // 교재 탭을 0.5초간 사용할 수 없게 한다.
        bookContainer.setEnabled(false);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                bookContainer.setEnabled(true);
            }
        }, 500L);
    }

    // 단원 점수표를 클릭하면
    // -> 문제별 문제 통계 탭을 설정한다.
    private void setOnClickListenerToScoreByUnit2Grid() {

        byUnitContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {

                child.setOnClickListener(new OnScoreByUnit2GridClickListenerImpl((GridLayout) child, holder, student));
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {}
        });
    }

    // 스위치를 설정한다.
    private void setHowToViewSwitch() {

        howToViewSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 단원별 문제 통계 탭을 비운다.
                byUnitContainer.removeAllViews();

                // 단원별 보기 모드인 경우
                if (byUnitContainer.getVisibility() == View.VISIBLE) {

                    // 단원별 문제 통계 탭에 단원 점수표를 추가한다.
                    addScoreByUnit2GridToByUnitContainer();
                }
                // 문제별 보기 모드인 경우
                else {

                    // 중단원 텍스트로부터 중단원을 얻는다.
                    BookUnit2 bookUnit2 = (BookUnit2) unitNameUnit2Text.getTag();

                    // 점수 컨테이너를 설정한다.
                    setUnitNameScoreContainer(bookUnit2);

                    // 문제별 문제 통계 폼 컨테이너를 설정한다.
                    setFormByProblemContainer();

                    // 문제 통계 어댑터를 설정한다.
                    setProblemStatisticsAdapter();
                }

                // 스위치를 0.5초간 사용할 수 없게 한다.
                pauseViewForNms(howToViewSwitch, 500L);
            }
        });
    }

    // 점수 컨테이너에 텍스트를 추가한다.
    private void addTextToUnitNameScoreContainer(Float score, BookUnit2 bookUnit2) {

        // 점수 보기 모드라면
        if (!howToViewSwitch.isChecked()) {

            // 점수 컨테이너에 점수 텍스트를 추가한다.
            addScoreTextToUnitNameScoreContainer(score);
        }
        // 등급 보기 모드라면
        else {

            threadPool.execute(new Runnable() {

                ScoreStatistics scoreStatistics;

                @Override
                public void run() {

                    // 점수 통계를 얻는다.
                    getScoreStatistics(bookUnit2);

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            if (score != null) {

                                // 점수 통계가 0.05초 이내에 초기화되지 않으면
                                if (isScoreStatisticsNullForNms(50L)) {}
                            }

                            // 등급 텍스트를 점수 컨테이너에 추가한다.
                            addRankTextToUnitNameScoreContainer(score, scoreStatistics);

                            // 백분율 텍스트를 점수 컨테이너에 추가한다.
                            addPercentageTextToUnitNameScoreContainer(score, scoreStatistics);
                        }
                    });
                }

                // 해당 중단원의 점수 통계를 얻는다.
                private void getScoreStatistics(BookUnit2 bookUnit2) {

                    Call<ScoreStatistics> scoreStatisticsCall = statisticsApi.readScoreStatistics(bookUnit2.getId());

                    scoreStatisticsCall.enqueue(new Callback<ScoreStatistics>() {

                        @Override
                        public void onResponse(Call<ScoreStatistics> call, Response<ScoreStatistics> response) {

                            if (!response.isSuccessful()) {

                                Log.d("error", "code : " + response.code());
                                return;
                            }

                            scoreStatistics = response.body();
                        }

                        @Override
                        public void onFailure(Call<ScoreStatistics> call, Throwable t) {
                            Log.d("error", t.getMessage());
                        }
                    });
                }

                // 점수 통계가 null인지 0.00n초간 확인한다.
                private boolean isScoreStatisticsNullForNms(long n) {

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

                        if (scoreStatistics == null)
                            continue;

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

    // 점수 텍스트를 컨테이너에 추가한다.
    private void addScoreTextToUnitNameScoreContainer(Float score) {

        TextView scoreText = new TextView(holder.itemView.getContext());

        scoreText.setText(score != null ? score + "점" : " -     ");
        scoreText.setTextColor(Color.BLACK);
        scoreText.setTextSize(13);

        LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 점수 컨테이너에 점수 텍스트를 추가한다.
                unitNameScoreContainer.addView(scoreText, scoreParams);
            }
        });
    }

    // 등급 텍스트를 컨테이너에 추가한다.
    private void addRankTextToUnitNameScoreContainer(Float score, ScoreStatistics scoreStatistics) {

        // 등급 텍스트를 만든다.
        TextView rankText = new TextView(holder.itemView.getContext());

        Integer rank = null;

        if (score != null && scoreStatistics != null) {
            rank = ConversionService.zToRank(
                    (score - scoreStatistics.getAvg()) / scoreStatistics.getSd());
        }

        rankText.setText(rank != null ? rank + "등급" : " - ");
        rankText.setTextColor(Color.BLACK);
        rankText.setTextSize(13);

        LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 등급 텍스트를 점수 컨테이너에 추가한다.
                unitNameScoreContainer.addView(rankText, rankParams);
            }
        });
    }

    // 백분율 텍스트를 컨테이너에 추가한다.
    private void addPercentageTextToUnitNameScoreContainer(Float score, ScoreStatistics scoreStatistics) {

        // 백분율 텍스트뷰를 만든다.
        TextView percentageText = new TextView(holder.itemView.getContext());

        Float percentage = null;

        if (score != null && scoreStatistics != null) {

            percentage = ConversionService.zToPercentage(
                    (score - scoreStatistics.getAvg()) / scoreStatistics.getSd());
        }

        percentageText.setText(percentage != null ? "(" + percentage + "%)" : null);
        percentageText.setTextColor(Color.BLACK);
        percentageText.setTextSize(13);
        percentageText.setPadding((int) (5 * dp), 0, 0, 0);

        LinearLayout.LayoutParams percentageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 백분율 텍스트를 점수 컨테이너에 추가한다.
                unitNameScoreContainer.addView(percentageText, percentageParams);
            }
        });
    }

    // 점수 컨테이너를 설정한다.
    private void setUnitNameScoreContainer(BookUnit2 bookUnit2) {

        // 점수 컨테이너를 비운다.
        unitNameScoreContainer.removeAllViews();

        threadPool.execute(new Runnable() {

            Float score;

            @Override
            public void run() {

                // 해당 중단원의 중단원 점수를 얻는다.
                getScore(bookUnit2);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 중단원 점수가 0.05초 이내로 초기화되지 않으면
                        if (isScoreNullForNms(50L)) {}

                        // 점수 컨테이너에 텍스트를 추가한다.
                        addTextToUnitNameScoreContainer(score, bookUnit2);
                    }
                });
            }

            // 해당 중단원의 중단원 점수를 얻는다.
            private void getScore(BookUnit2 bookUnit2) {

                Call<Float> scoreCall = statisticsApi.read(student.getId(), bookUnit2.getId());

                scoreCall.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        score = response.body();
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 중단원 점수가 null인지 0.00n초간 확인한다.
            private boolean isScoreNullForNms(long n) {

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

                    if (score == null) continue;

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

    // 문제별 문제 통계 폼 컨테이너를 설정한다.
    private void setFormByProblemContainer() {

        // 문제별 문제 통계 폼 컨테이너의 내 점수 탭을 설정한다.
        for (int i = 0; i < formByProblemContainer.getChildCount(); i++) {

            LinearLayout formByProblem = (LinearLayout) formByProblemContainer.getChildAt(i);
            LinearLayout myScoreContainer = (LinearLayout) formByProblem.getChildAt(2);
            TextView myScoreText = (TextView) myScoreContainer.getChildAt(1);

            myScoreText.setText((!howToViewSwitch.isChecked()) ? "내 점수" : "내 등급");
        }
    }

    // 문제 통계 어댑터를 설정한다.
    private void setProblemStatisticsAdapter() {

        // 문제 통계 어댑터를 얻는다.
        ProblemStatisticsAdapter problemStatisticsAdapter = (ProblemStatisticsAdapter) byProblemRecycler.getAdapter();

        // 문제 통계 어댑터의 보기 모드를 변경한다.
        problemStatisticsAdapter.setHowToViewSwitchChecked(howToViewSwitch.isChecked());

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 문제 통계 어댑터를 새로고침한다.
                problemStatisticsAdapter.notifyDataSetChanged();
            }
        });
    }

    // 뷰를 0.00n초간 사용할 수 없게 한다.
    private void pauseViewForNms(View view, long n) {

        view.setEnabled(false);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                view.setEnabled(true);
            }
        }, n);
    }
}
