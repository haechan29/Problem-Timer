package kr.co.problemtimertest.impl;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.StudentMainActivity;
import kr.co.problemtimertest.TeacherMainActivity;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.ProblemStatisticsAdapter;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.fragment.TimerFragment;
import kr.co.problemtimertest.listener.OnItemClickListener;
import kr.co.problemtimertest.model.BookUnit1;
import kr.co.problemtimertest.model.BookUnit2;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.ProblemStatistics;
import kr.co.problemtimertest.model.ScoreStatistics;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class OnScoreByUnit2GridClickListenerImpl implements View.OnClickListener {

    private float dp;

    private final GridLayout grid;
    private final RecyclerView.ViewHolder holder;
    private final Student student;

    private BookUnit1 bookUnit1;
    private List<BookUnit2> bookUnit2List;

    // 교재 통계 관련 변수
    private ConstraintLayout bookContainer;
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

    // 문제 통계 리싸이클러 관련 변수
    private ProblemStatisticsAdapter problemStatisticsAdapter;

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

    public OnScoreByUnit2GridClickListenerImpl(GridLayout grid, RecyclerView.ViewHolder holder, Student student) {

        this.grid = grid;
        this.holder = holder;
        this.student = student;
    }

    @Override
    public void onClick(View view) {

        // 변수를 초기화한다.
        initializeVariables();

        // 문제별 문제 통계 탭을 설정한다.
        setByProblemContainer(view.getId());

        // 왼쪽 화살표 텍스트가 클릭되면
        // -> 단원별 보기 탭으로 돌아오거나 중단원을 바꾼다.
        unitNameArrowLeft.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 문제별 보기 탭을 설정한다.
                setByProblemContainer(view.getId());
            }
        });

        // 오른쪽 화살표 텍스트가 클릭되면
        // -> 단원별 보기 탭으로 돌아오거나 중단원을 바꾼다.
        unitNameArrowRight.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 문제별 보기 탭을 설정한다.
                setByProblemContainer(view.getId());
            }
        });

        // 단원별 문제 통계 탭을 보이지 않게 하고, 문제별 문제 통계 탭을 보이게 한다.
        byUnitContainer.setVisibility(View.GONE);
        byProblemContainer.setVisibility(View.VISIBLE);
    }

    // 변수를 초기화한다.
    private void initializeVariables() {

        dp = grid.getContext().getResources().getDisplayMetrics().density;

        bookContainer = (ConstraintLayout) holder.itemView.findViewById(R.id.container_book);
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

        // 대단원
        TextView bookUnit1Text = (TextView) grid.getChildAt(0);
        bookUnit1 = (BookUnit1) bookUnit1Text.getTag();

        // 중단원
        bookUnit2List = new ArrayList<>();

        for (int i = 0; i < grid.getChildCount(); i++) {

            TextView text = (TextView) grid.getChildAt(i);

            if (((GridLayout.LayoutParams) text.getLayoutParams()).columnSpec
                    .equals(GridLayout.spec(1))) {

                BookUnit2 bookUnit2 = (BookUnit2) text.getTag();
                bookUnit2List.add(bookUnit2);
            }
        }
    }

    // 문제별 보기 탭을 설정한다.
    private void setByProblemContainer(int resId) {

        switch (resId) {

            // 문제별 보기 탭의 그리드가 클릭되었다면
            case R.id.grid_score_by_unit_2:

                // 단원명 탭을 설정한다.
                setUnitNameContainer(bookUnit2List.get(0));

                // 리사이클러를 설정한다.
                setByProblemRecycler(bookUnit2List.get(0));

                break;

            // 왼쪽 화살표 버튼이 클릭되었다면
            case R.id.arrow_left_unit_name:

                // 몇째 탭에서 클릭되었는지 확인한다.
                int byProblemIndex = ((BookUnit2) unitNameUnit2Text.getTag()).getUnit2Number() - 1;

                // 첫째 탭에서 클릭되었다면
                if (byProblemIndex == 0) {

                    // 문제별 문제 통계 탭을 원래 상태로 되돌린다.
                    resetByProblemContainer();

                    // 다시 단원별 보기 탭을 보이게 한다.
                    byProblemContainer.setVisibility(View.GONE);
                    byUnitContainer.setVisibility(View.VISIBLE);
                }
                // 첫째 이후의 탭에서 클릭되었다면
                else {

                    // 단원명 탭을 설정한다.
                    setUnitNameContainer(bookUnit2List.get(byProblemIndex - 1));

                    // 리사이클러를 설정한다.
                    setByProblemRecycler(bookUnit2List.get(byProblemIndex - 1));
                }

                break;

            // 오른쪽 화살표 버튼이 클릭되었다면
            case R.id.arrow_right_unit_name:

                // 몇째 탭에서 클릭되었는지 확인한다.
                byProblemIndex = ((BookUnit2) unitNameUnit2Text.getTag()).getUnit2Number() - 1;

                // 마지막 탭에서 클릭되었다면
                if (byProblemIndex == bookUnit2List.size() - 1) {

                    // 문제별 문제 통계 탭을 원래 상태로 되돌린다.
                    resetByProblemContainer();

                    // 다시 단원별 보기 탭을 보이게 한다.
                    byProblemContainer.setVisibility(View.GONE);
                    byUnitContainer.setVisibility(View.VISIBLE);
                }
                // 마지막 이전의 탭에서 클릭되었다면
                else {

                    // 단원명 탭을 설정한다.
                    setUnitNameContainer(bookUnit2List.get(byProblemIndex + 1));

                    // 리사이클러를 설정한다.
                    setByProblemRecycler(bookUnit2List.get(byProblemIndex + 1));
                }
        }
    }

    // 단원명 탭을 설정한다.
    private void setUnitNameContainer(BookUnit2 bookUnit2) {

        // 대단원 텍스트를 설정한다.
        setUnitNameUnit1Text();

        // 중단원 텍스트를 설정한다.
        setUnitNameUnit2Text(bookUnit2);

        // 점수 컨테이너를 설정한다.
        setUnitNameScoreContainer(bookUnit2);
    }

    // 대단원 텍스트를 설정한다.
    private void setUnitNameUnit1Text() {

        // 대단원 텍스트를 설정한다.
        unitNameUnit1Text.setText(String.format("%d. %s",
                bookUnit1.getUnit1Number(),
                bookUnit1.getUnit1Name()));

        unitNameUnit1Text.setTag(bookUnit1);
    }

    // 중단원 텍스트를 설정한다.
    private void setUnitNameUnit2Text(BookUnit2 bookUnit2) {

        // 중단원 텍스트를 설정한다.
        unitNameUnit2Text.setText(String.format("%02d %s",
                bookUnit2.getUnit2Number(),
                bookUnit2.getUnit2Name()));

        unitNameUnit2Text.setTag(bookUnit2);
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
                        addTextToUnitNameScoreContainer(bookUnit2, score);
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

    // 점수 컨테이너에 텍스트를 추가한다.
    private void addTextToUnitNameScoreContainer(BookUnit2 bookUnit2, Float score) {

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

                    if (score != null) {

                        // 점수 통계를 얻는다.
                        getScoreStatistics(bookUnit2);
                    }
                    else {
                        scoreStatistics = null;
                    }

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 점수 통계가 0.05초 이내에 초기화되지 않으면
                            if (isScoreStatisticsNullForNms(50L)) {}

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

        TextView scoreText = new TextView(grid.getContext());

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
    private void addRankTextToUnitNameScoreContainer(
            Float score, ScoreStatistics scoreStatistics) {

        // 등급 텍스트를 만든다.
        TextView rankText = new TextView(grid.getContext());

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
    private void addPercentageTextToUnitNameScoreContainer(
            Float score, ScoreStatistics scoreStatistics) {

        // 백분율 텍스트뷰를 만든다.
        TextView percentageText = new TextView(grid.getContext());

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

    // 리사이클러를 설정한다.
    private void setByProblemRecycler(BookUnit2 bookUnit2) {

        // 문제 통계 어댑터를 얻는다.
        problemStatisticsAdapter = new ProblemStatisticsAdapter();

        // 문제 통계 어댑터에 아이템을 추가한다.
        addItemsToProblemStatisticsAdapter(bookUnit2);

        // 문제 통계 어댑터의 아이템을 클릭하면
        // -> 문제 풀기 탭의 해당 문제로 이동한다.
        setOnClickListenerToProblemStatisticsAdapter();

        // 리사이클러에 문제 통계 어댑터를 연결한다.
        setAdapterToByProblemRecycler();
    }

    // 문제 통계 어댑터에 아이템을 추가한다.
    private void addItemsToProblemStatisticsAdapter(BookUnit2 bookUnit2) {

        threadPool.execute(new Runnable() {

            private List<ProblemStatistics> problemStatisticsList;

            @Override
            public void run() {

                // 문제 통계 리스트를 얻는다.
                getProblemStatisticsList(bookUnit2);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 문제 통계 리스트가 0.2초 이내로 초기화되지 않으면
                        if (isProblemStatisticsListNullForNms(200L)) {

                            Log.d("error", "failed to initialize : problemStatisticsList");
                            return;
                        }

                        // 문제별 문제 통계 폼 컨테이너를 설정한다.
                        setFormByProblemContainer(problemStatisticsList);

                        // 문제 통계 어댑터에 아이템을 추가한다.
                        problemStatisticsList.forEach(
                                problemStatistics -> problemStatisticsAdapter.addItem(problemStatistics));

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
                });
            }

            // 문제 통계 리스트를 얻는다.
            private void getProblemStatisticsList(BookUnit2 bookUnit2) {

                Call<List<ProblemStatistics>> call =
                        statisticsApi.readProblemStatistics(student.getId(), bookUnit2.getId());

                call.enqueue(new Callback<List<ProblemStatistics>>() {

                    @Override
                    public void onResponse(Call<List<ProblemStatistics>> call, Response<List<ProblemStatistics>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        problemStatisticsList = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<ProblemStatistics>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 문제 통계 리스트가 null인지 0.00n초간 확인한다.
            private boolean isProblemStatisticsListNullForNms(long n) {

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

                    if (problemStatisticsList == null) continue;

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
    private void setFormByProblemContainer(List<ProblemStatistics> problemStatisticsList) {

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 문제별 문제 통계 폼 컨테이너를 비운다.
                formByProblemContainer.removeAllViews();
            }
        });

        for (int i = 0; i < (problemStatisticsList.size() + 4) / 5; i++) {

            // 문제별 문제 통계 폼을 얻는다.
            LinearLayout formByProblem = getFormByProblem();

            // 문제 번호 텍스트를 문제별 보기 폼에 추가한다.
            addProblemNumberTextToFormByProblem(formByProblem);

            // 정답률 텍스트를 문제별 보기 폼에 추가한다.
            addCorrectAnswerRateAvgTextToFormByProblem(formByProblem);

            // 내 점수 텍스트를 문제별 보기 폼에 추가한다.
            addMyScoreTextToFormByProblem(formByProblem);

            handler.post(new Runnable() {

                @Override
                public void run() {

                    // 문제별 문제 통계 폼 컨테이너에 폼을 추가한다.
                    formByProblemContainer.addView(formByProblem);
                }
            });
        }
    }

    // 문제별 문제 통계 폼을 반환한다.
    private LinearLayout getFormByProblem() {

        LinearLayout formByProblem = new LinearLayout(grid.getContext());

        formByProblem.setOrientation(LinearLayout.VERTICAL);
        formByProblem.setGravity(Gravity.CENTER);
        formByProblem.setPadding(0, (int) (10 * dp), 0, (int) (10 * dp));

        LinearLayout.LayoutParams formByProblemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        formByProblem.setLayoutParams(formByProblemParams);

        return formByProblem;
    }

    // 문제 번호 텍스트를 문제별 보기 폼에 추가한다.
    private void addProblemNumberTextToFormByProblem(LinearLayout formByProblem) {

        TextView problemNumberText = new TextView(grid.getContext());

        problemNumberText.setTextSize(12);
        problemNumberText.setPadding(0, 0, 0, (int) (4 * dp));
        problemNumberText.setBackgroundResource(R.drawable.shape_border_bottom_gray);

        LinearLayout.LayoutParams problemNumberParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        problemNumberText.setLayoutParams(problemNumberParams);

        handler.post(new Runnable() {

            @Override
            public void run() {
                formByProblem.addView(problemNumberText);
            }
        });
    }

    // 정답률 텍스트를 문제별 보기 폼에 추가한다.
    private void addCorrectAnswerRateAvgTextToFormByProblem(LinearLayout formByProblem) {

        // 정답률 컨테이너
        LinearLayout correctAnswerRateAvgContainer = new LinearLayout(grid.getContext());

        correctAnswerRateAvgContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        correctAnswerRateAvgContainer.setOrientation(LinearLayout.HORIZONTAL);
        correctAnswerRateAvgContainer.setGravity(Gravity.CENTER);
        correctAnswerRateAvgContainer.setPadding(0, (int) (2 * dp), 0, 0);

        // 자리 채우기 텍스트
        TextView correctAnswerRateAvgPlaceHolder = new TextView(grid.getContext());

        correctAnswerRateAvgPlaceHolder.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        correctAnswerRateAvgPlaceHolder.setTextSize(11);

        // 정답률 텍스트
        TextView correctAnswerRateAvgText = new TextView(grid.getContext());

        correctAnswerRateAvgText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        correctAnswerRateAvgText.setText("정답률");
        correctAnswerRateAvgText.setTextSize(10);
        correctAnswerRateAvgText.setTextColor(Color.parseColor("#77000000"));

        handler.post(new Runnable() {

            @Override
            public void run() {

                correctAnswerRateAvgContainer.addView(correctAnswerRateAvgPlaceHolder);
                correctAnswerRateAvgContainer.addView(correctAnswerRateAvgText);

                formByProblem.addView(correctAnswerRateAvgContainer);
            }
        });
    }

    // 내 점수 텍스트를 문제별 보기 폼에 추가한다.
    private void addMyScoreTextToFormByProblem(LinearLayout formByProblem) {

        // 내 점수 컨테이너
        LinearLayout myScoreContainer = new LinearLayout(grid.getContext());

        myScoreContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        myScoreContainer.setOrientation(LinearLayout.HORIZONTAL);

        // 자리 채우기 텍스트
        TextView myScorePlaceHolder = new TextView(grid.getContext());

        myScorePlaceHolder.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        myScorePlaceHolder.setTextSize(11);

        // 내 점수 텍스트
        TextView myScoreText = new TextView(grid.getContext());

        myScoreText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        myScoreText.setText(!howToViewSwitch.isChecked() ? "내 점수" : "내 등급");
        myScoreText.setTextSize(10);
        myScoreText.setTextColor(Color.parseColor("#77000000"));

        handler.post(new Runnable() {

            @Override
            public void run() {

                myScoreContainer.addView(myScorePlaceHolder);
                myScoreContainer.addView(myScoreText);

                formByProblem.addView(myScoreContainer);
            }
        });
    }

    // 문제 통계 어댑터의 아이템을 클릭하면
    // -> 문제 풀기 탭의 해당 문제로 이동한다.
    private void setOnClickListenerToProblemStatisticsAdapter() {

        problemStatisticsAdapter.setOnContainerClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(RecyclerView.ViewHolder holder, View view, int position) {

                threadPool.execute(new Runnable() {

                    List<Problem> problemsInUnit2;

                    @Override
                    public void run() {

                        // 해당 중단원의 문제 리스트를 얻는다.
                        getProblemsInUnit2();

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 문제 리스트가 0.2초 이내에 초기화되지 않으면
                                if (isProblemsInUnit2NullForNms(200L)) {

                                    Log.d("error", "failed to initialize : problemsInUnit2");
                                    return;
                                }

                                // 클릭한 문제를 얻는다.
                                Problem problem = getClickedProblem(holder, problemsInUnit2);

                                // Todo : 그냥 이렇게 바꿔도 되지 않나..?
                                //  만약에 이게 되면 problemId를 이용해서 직접 problem 찾는 방식도 해보자
                                // Problem problem = newVersionGetClickedProblem(position, problemsInUnit2);

                                // 문제 풀기 탭의 해당 문제로 이동한다.
                                moveToProblem(problem);
                            }

                            private Problem newVersionGetClickedProblem(int position, List<Problem> problemsInUnit2) {

                                ProblemStatistics clickedItem = problemStatisticsAdapter.getItem(position);

                                for (Problem p : problemsInUnit2) {

                                    if (p.getId() == clickedItem.getProblemId()) {
                                        return p;
                                    }
                                }

                                return null;
                            }
                        });
                    }

                    // 해당 중단원의 문제 리스트를 얻는다.
                    private void getProblemsInUnit2() {

                        // 중단원을 얻는다.
                        BookUnit2 bookUnit2 = (BookUnit2) unitNameUnit2Text.getTag();

                        Call<List<Problem>> call = statisticsApi.list(bookUnit2.getId());

                        call.enqueue(new Callback<List<Problem>>() {

                            @Override
                            public void onResponse(Call<List<Problem>> call, Response<List<Problem>> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "code : " + response.code());
                                    return;
                                }

                                problemsInUnit2 = response.body();
                            }

                            @Override
                            public void onFailure(Call<List<Problem>> call, Throwable t) {
                                Log.d("error", t.getMessage());
                            }
                        });
                    }

                    // 문제 리스트가 null인지 0.00n초간 확인한다.
                    private boolean isProblemsInUnit2NullForNms(long n) {

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

                            if (problemsInUnit2 == null) continue;

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

    // 클릭한 문제를 얻는다.
    private Problem getClickedProblem(RecyclerView.ViewHolder holder, List<Problem> problemsInUnit2) {

        TextView problemNumberText =
                (TextView) holder.itemView.findViewById(R.id.text_problem_number);

        for (Problem p : problemsInUnit2) {

            if (p.getNumber().equals(problemNumberText.getText())) {
                return p;
            }
        }

        return null;
    }

    // 문제 풀기 탭의 해당 문제로 이동한다.
    private void moveToProblem(Problem problem) {

        TimerFragment timerFragment = ((StudentMainActivity) grid.getContext()).getTimerFragment();

        ((TeacherMainActivity) grid.getContext()).getSupportFragmentManager().beginTransaction()
                .replace(R.id.scroll_register, timerFragment).commit();

        // 문제 풀기 탭의 해당 문제로 이동한다.
        timerFragment.moveToProblem(problem);
    }

    // 리사이클러에 어댑터를 연결한다.
    private void setAdapterToByProblemRecycler() {

        // 리사이클러뷰가 다섯 줄로 나열되도록 설정한다.
        byProblemRecycler.setLayoutManager(new GridLayoutManager(grid.getContext(), 5));

        // 리사이클러뷰에 어댑터를 연결한다.
        byProblemRecycler.setAdapter(problemStatisticsAdapter);
    }

    // 문제별 문제 통계 탭을 원래 상태로 되돌린다.
    private void resetByProblemContainer() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                unitNameUnit1Text.setText(null);
                unitNameUnit2Text.setText(null);
                unitNameScoreContainer.removeAllViews();
            }
        });
    }
}
