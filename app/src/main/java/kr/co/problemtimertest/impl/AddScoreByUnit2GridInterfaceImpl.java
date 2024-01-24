package kr.co.problemtimertest.impl;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.listener.AddScoreByUnit2GridInterface;
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

public class AddScoreByUnit2GridInterfaceImpl implements AddScoreByUnit2GridInterface {

    private RecyclerView.ViewHolder holder;
    private Student student;
    private Book book;

    private float dp;

    private List<BookUnit1> bookUnit1List;

    private SwitchCompat howToViewSwitch;
    private LinearLayout byUnitContainer;
    private LinearLayout byProblemContainer;

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

    public AddScoreByUnit2GridInterfaceImpl(RecyclerView.ViewHolder holder, Student student, Book book) {

        this.holder = holder;
        this.student = student;
        this.book = book;
    }

    @Override
    public void addScoreByUnit2Grid() {

        // 변수를 초기화한다.
        initializeVariable();

        // 대단원 리스트를 얻는다.
        getBookUnit1List(book);

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 대단원 리스트가 null인지 0.2초간 확인한다.
                if (isBookUnit1ListNullForNms(200L)) {

                    Log.d("error", "failed to initialize : bookUnit1List");
                    return;
                }

                for (BookUnit1 bookUnit1 : bookUnit1List) {

                    threadPool.execute(new Runnable() {

                        private List<BookUnit2> bookUnit2List;

                        @Override
                        public void run() {

                            // 단원 점수 표가 0.4초 이내에 단원 점수 탭에 추가되지 않았다면
                            if (isGridNotAddedForNms(400L)) {

                                Log.d("error", "scoreByUnit2Grid is not added to byUnitContainer");
                                return;
                            }

                            // 중단원 리스트를 얻는다.
                            getBookUnit2List(bookUnit1);

                            threadPool.execute(new Runnable() {

                                private GridLayout scoreByUnit2Grid;

                                @Override
                                public void run() {

                                    // 중단원 리스트가 null인지 0.2초간 확인한다.
                                    if (isBookUnit2ListNullForNms(200L)) {

                                        Log.d("error", "failed to initialize : bookUnit2List");
                                        return;
                                    }

                                    // 그리드 레이아웃을 만든다.
                                    scoreByUnit2Grid = new GridLayout(holder.itemView.getContext());

                                    // 그리드 레이아웃에 단원별 점수 스타일을 적용한다.
                                    setScoreByUnit2GridStyle(scoreByUnit2Grid, bookUnit1, bookUnit2List);

                                    // 대단원 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
                                    addUnit1TextToScoreByUnit2Grid(scoreByUnit2Grid, bookUnit1, bookUnit2List);

                                    // 중단원 텍스트뷰, 중단원 점수 텍스트뷰, 중단원 등급 텍스트뷰를 그리드 레이아웃에 추가한다.
                                    for (BookUnit2 bookUnit2 : bookUnit2List) {

                                        // 중단원 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
                                        addUnit2TextToScoreByUnit2Grid(scoreByUnit2Grid, bookUnit2);

                                        // 중단원 점수와 등급 텍스트뷰를 설정한다.
                                        setScoreAndRankText(scoreByUnit2Grid, bookUnit2);
                                    }

                                    // 화살표 텍스트를 만들고 그리드 레이아웃에 추가한다.
                                    addArrowTextToScoreByUnit2Grid(bookUnit2List, scoreByUnit2Grid);

                                    threadPool.execute(new Runnable() {

                                        @Override
                                        public void run() {

                                            // 단원 점수 표가 0.5초 이내에 초기화되지 않았다면
                                            if (isScoreByUnit2GridNotInitializedForNms(500L)) {

                                                Log.d("error", "failed to initialize : scoreByUnit2Grid");
                                                return;
                                            }

                                            handler.post(new Runnable() {

                                                @Override
                                                public void run() {

                                                    // 만든 단원 점수 표를 단원 점수 탭에 추가한다.
                                                    byUnitContainer.addView(scoreByUnit2Grid);
                                                }
                                            });
                                        }
                                    });
                                }

                                // 단원 점수 표가 초기화되었는지 0.00n초간 확인한다.
                                private boolean isScoreByUnit2GridNotInitializedForNms(long n) {

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

                                        if (scoreByUnit2Grid.getChildCount() ==
                                                (1 + bookUnit2List.size() *
                                                        (!howToViewSwitch.isChecked() ? 2 : 3) + 1))
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

                        // 단원 점수 표가 단원 점수 탭에 추가되었는지 0.00n초간 확인한다.
                        private boolean isGridNotAddedForNms(long n) {

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

                                if (byUnitContainer.getChildCount() < bookUnit1List.indexOf(bookUnit1)) continue;

                                return false;
                            }

                            try {
                                Thread.sleep(15L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            return true;
                        }

                        // 중단원 리스트를 얻는다.
                        private void getBookUnit2List(BookUnit1 bookUnit1){

                            Call<List<BookUnit2>> bookUnit2Call = statisticsApi.readBookUnits2(
                                    bookUnit1.getBookId(),
                                    bookUnit1.getUnit1Number());

                            bookUnit2Call.enqueue(new Callback<List<BookUnit2>>() {

                                @Override
                                public void onResponse(Call<List<BookUnit2>> call, Response<List<BookUnit2>> response) {

                                    if (!response.isSuccessful()) {

                                        Log.d("error", "code : " + response.code());
                                        return;
                                    }

                                    bookUnit2List = response.body();
                                }

                                @Override
                                public void onFailure(Call<List<BookUnit2>> call, Throwable t) {
                                    Log.d("error", t.getMessage());
                                }
                            });
                        }

                        // 중단원 리스트가 null인지 0.00n초간 확인한다.
                        private boolean isBookUnit2ListNullForNms(long n) {

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

                                if (bookUnit2List == null) continue;

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
        });
    }

    // 변수를 초기화한다.
    private void initializeVariable() {

        dp = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        howToViewSwitch = (SwitchCompat) holder.itemView.findViewById(R.id.switch_how_to_view);
        byUnitContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_by_unit);
        byProblemContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_by_problem);
    }

    // 대단원 리스트를 얻는다.
    private void getBookUnit1List(Book book) {

        // Todo : 매개변수 bookId로 변경하기
        Call<List<BookUnit1>> bookUnit1Call = statisticsApi.readBookUnits1(book.getId());

        bookUnit1Call.enqueue(new Callback<List<BookUnit1>>() {

            @Override
            public void onResponse(Call<List<BookUnit1>> call, Response<List<BookUnit1>> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }

                bookUnit1List = response.body();
            }

            @Override
            public void onFailure(Call<List<BookUnit1>> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 대단원 리스트가 null인지 0.00n초간 확인한다.
    private boolean isBookUnit1ListNullForNms(long n) {

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

            if (bookUnit1List == null) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    // 그리드 레이아웃에 단원별 점수 스타일을 적용한다.
    private void setScoreByUnit2GridStyle(
            GridLayout scoreByUnit2Grid,
            BookUnit1 bookUnit1,
            List<BookUnit2> bookUnit2List) {

        scoreByUnit2Grid.setId(R.id.grid_score_by_unit_2);                      // id
        scoreByUnit2Grid.setOrientation(GridLayout.VERTICAL);                   // orientation

        scoreByUnit2Grid.setBackgroundColor(
                bookUnit1.getUnit1Number() % 2 == 1 ?
                        Color.WHITE : Color.parseColor("#11000000")); // backgroundColor

        scoreByUnit2Grid.setRowCount(bookUnit2List.size());                  // rowCount

        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,                         // layout_width
                LinearLayout.LayoutParams.WRAP_CONTENT);                        // layout_height

        scoreByUnit2Grid.setLayoutParams(gridParams);

        scoreByUnit2Grid.setPadding(                                            // padding
                (int) (15 * dp),
                (int) (5 * dp),
                0,
                (int) (5 * dp));
    }

    // 대단원 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
    private void addUnit1TextToScoreByUnit2Grid(
            GridLayout scoreByUnit2Grid,
            BookUnit1 bookUnit1,
            List<BookUnit2> bookUnit2List) {

        // 대단원 텍스트뷰를 만든다.
        TextView unit1Text = new TextView(holder.itemView.getContext());

        // 텍스트뷰에 대단원 텍스트뷰 스타일을 적용한다.
        setUnit1TextStyle(unit1Text, bookUnit1, bookUnit2List);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 대단원 텍스트뷰를 그리드 레이아웃에 추가한다.
                scoreByUnit2Grid.addView(unit1Text);
            }
        });
    }

    // 텍스트뷰에 대단원 텍스트뷰 스타일을 적용한다.
    private void setUnit1TextStyle(TextView unit1Text, BookUnit1 bookUnit1, List<BookUnit2> bookUnit2List) {

        unit1Text.setWidth((int) (120 * dp));                            // width
        unit1Text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);   // gravity
        unit1Text.setTextColor(Color.BLACK);                             // textColor
        unit1Text.setTextSize(12);                                       // textSize

        unit1Text.setPadding(0, 0, (int) (20 * dp), 0);  // padding
        unit1Text.setSingleLine();                                       // singleLine
        unit1Text.setEllipsize(TextUtils.TruncateAt.END);                // ellipsize

        unit1Text.setText(bookUnit1.getUnit1Number() + ". " +
                bookUnit1.getUnit1Name());

        unit1Text.setTag(bookUnit1);                                     // tag

        GridLayout.LayoutParams unit1Params = new GridLayout.LayoutParams( // layout_rowSpan
                GridLayout.spec(0, bookUnit2List.size(), 1F),
                GridLayout.spec(0));

        unit1Text.setLayoutParams(unit1Params);
    }

    // 중단원 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
    private void addUnit2TextToScoreByUnit2Grid(GridLayout scoreByUnit2Grid, BookUnit2 bookUnit2) {

        // 중단원 텍스트뷰를 만든다.
        TextView unit2Text = new TextView(holder.itemView.getContext());

        // 중단원 텍스트 스타일을 설정한다.
        setUnit2TextStyle(unit2Text, bookUnit2);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 중단원 텍스트뷰를 그리드 레이아웃에 추가한다.
                scoreByUnit2Grid.addView(unit2Text);
            }
        });
    }

    // 중단원 텍스트 스타일을 설정한다.
    private void setUnit2TextStyle(TextView unit2Text, BookUnit2 bookUnit2) {

        unit2Text.setWidth((int) (0 * dp));
        unit2Text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);   // gravity
        unit2Text.setTextColor(Color.BLACK);                             // textColor
        unit2Text.setTextSize(12);                                       // textSize
        unit2Text.setPadding(0, 0, (int) (20 * dp), 0);  // padding
        unit2Text.setSingleLine();                                       // singleLine
        unit2Text.setEllipsize(TextUtils.TruncateAt.END);                // ellipsize

        unit2Text.setText("0" + bookUnit2.getUnit2Number() + " " +
                bookUnit2.getUnit2Name());                               // text

        unit2Text.setTag(bookUnit2);                                     // tag

        // 중단원 텍스트뷰의 layout_row, layout_height을 설정한다.
        GridLayout.LayoutParams unit2Params = new GridLayout.LayoutParams(
                GridLayout.spec(bookUnit2.getUnit2Number() - 1),
                GridLayout.spec(1, 1F)
        );

        unit2Params.setMargins(0, 0, 0, (int) (3 * dp));   // layout_margin
        unit2Text.setLayoutParams(unit2Params);
    }

    // 중단원 점수와 등급 텍스트뷰를 설정한다.
    private void setScoreAndRankText(GridLayout scoreByUnit2Grid, BookUnit2 bookUnit2) {

        threadPool.execute(new Runnable() {

            private Float score;

            @Override
            public void run() {

                // 해당 중단원의 중단원 점수를 얻는다.
                getScore(bookUnit2);

                threadPool.execute(new Runnable() {

                    private ScoreStatistics scoreStatistics;

                    @Override
                    public void run() {

                        // 점수 리스트가 null인지 0.1초간 확인한다.
                        if (isScoreNullForNms(100L)) {}

                        if (score != null) {

                            // 해당 중단원의 점수 통계를 얻는다.
                            getScoreStatistics(bookUnit2);
                        }

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                if (score != null) {

                                    // 점수 통계 리스트가 null인지 0.1초간 확인한다.
                                    if (isScoreStatisticsNullForNms(100L)) {}
                                }

                                // 점수 보기 모드일 때는
                                if (!howToViewSwitch.isChecked()) {

                                    // 중단원 점수 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
                                    addScoreTextToScoreByUnit2Grid(scoreByUnit2Grid, bookUnit2, score);
                                }
                                // 등급 보기 모드일 때는
                                else {

                                    // 중단원 등급 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
                                    addRankTextToScoreByUnit2Grid(scoreByUnit2Grid, bookUnit2, score, scoreStatistics);

                                    // 중단원 백분율 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
                                    addPercentageTextToScoreByUnit2Grid(scoreByUnit2Grid, bookUnit2, score, scoreStatistics);
                                }
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

                    // 점수 통계 리스트가 null인지 0.00n초간 확인한다.
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

                            if (scoreStatistics == null) continue;

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

            // 점수 리스트가 null인지 0.00n초간 확인한다.
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

    // 중단원 점수 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
    private void addScoreTextToScoreByUnit2Grid(
            GridLayout scoreByUnit2Grid, BookUnit2 bookUnit2, Float score) {

        // 중단원 점수 텍스트뷰를 만든다.
        TextView scoreText = new TextView(holder.itemView.getContext());

        // 중단원 점수 스타일을 설정한다.
        setScoreTextStyle(scoreText, bookUnit2, score);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 중단원 점수 텍스트뷰를 그리드 레이아웃에 추가한다.
                scoreByUnit2Grid.addView(scoreText);
            }
        });
    }

    // 중단원 점수 스타일을 설정한다.
    private void setScoreTextStyle(TextView scoreText, BookUnit2 bookUnit2, Float score) {

        scoreText.setWidth((int) (90 * dp));
        scoreText.setTextColor(Color.BLACK);                             // textColor
        scoreText.setTextSize(12);                                       // textSize
        scoreText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);     // gravity
        scoreText.setSingleLine();                                       // singleLine
        scoreText.setEllipsize(TextUtils.TruncateAt.END);                // ellipsize
        scoreText.setTag(score);                                         // score

        GridLayout.LayoutParams scoreParams = new GridLayout.LayoutParams(
                GridLayout.spec(bookUnit2.getUnit2Number() - 1),
                GridLayout.spec(2));                                // rowCount, columnCount
        scoreParams.setMargins(0, 0, (int) (3 * dp), (int) (3 * dp));

        scoreText.setLayoutParams(scoreParams);

        // 해당 학생이 아직 해당 중단원의 문제를 풀지 않았다면
        if (score == null) {

            scoreText.setText(" - ");
            scoreText.setPadding(0, 0, (int) (10 * dp), 0);
        }
        // 해당 학생이 해당 중단원의 문제를 푼 적이 있다면
        else {
            scoreText.setText(Math.round(score) + "점");
        }
    }

    // 중단원 등급 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
    private void addRankTextToScoreByUnit2Grid(
            GridLayout scoreByUnit2Grid,
            BookUnit2 bookUnit2,
            Float score,
            ScoreStatistics scoreStatistics) {

        // 중단원 등급 텍스트를 만든다.
        TextView rankText = new TextView(holder.itemView.getContext());

        // 중단원 등급 텍스트 스타일을 설정한다.
        setRankTextStyle(rankText, bookUnit2, score, scoreStatistics);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 중단원 등급 텍스트를 그리드 레이아웃에 추가한다.
                scoreByUnit2Grid.addView(rankText);
            }
        });
    }

    // 등급 텍스트 스타일을 설정한다.
    private void setRankTextStyle(
            TextView rankText, BookUnit2 bookUnit2, Float score, ScoreStatistics scoreStatistics) {

        rankText.setWidth((int) (40 * dp));
        rankText.setTextColor(Color.BLACK);
        rankText.setTextSize(12);
        rankText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        GridLayout.LayoutParams rankParams = new GridLayout.LayoutParams(
                GridLayout.spec(bookUnit2.getUnit2Number() - 1),
                GridLayout.spec(2)
        );
        rankParams.setMargins((int) (1 * dp), 0, 0, (int) (3 * dp));

        rankText.setLayoutParams(rankParams);

        // 해당 학생이 아직 해당 중단원의 문제를 풀지 않았다면
        if (score == null) {

            rankText.setText(" - ");
            rankText.setPadding(0, 0, (int) (10 * dp), 0);
        }
        // 해당 학생이 해당 중단원의 문제를 푼 적이 있다면
        else {
            Integer rank =
                    ConversionService.zToRank(
                            (score - scoreStatistics.getAvg()) / scoreStatistics.getSd());

            rankText.setTag(rank);

            // 해당 학생의 해당 중단원 점수가 상위 40% 이하라면
            if (rank == null) {

                rankText.setText(" - ");
                rankText.setPadding(0, 0, (int) (10 * dp), 0);
            }
            // 해당 학생의 해당 중단원 점수가 상위 40% 이상이라면
            else {
                rankText.setText(rank + "등급");
            }
        }
    }

    // 중단원 백분율 텍스트뷰를 만들고 그리드 레이아웃에 추가한다.
    private void addPercentageTextToScoreByUnit2Grid(
            GridLayout scoreByUnit2Grid,
            BookUnit2 bookUnit2,
            Float score,
            ScoreStatistics scoreStatistics) {

        // 중단원 백분율 텍스트뷰를 만든다.
        TextView percentageText = new TextView(holder.itemView.getContext());

        // 중단원 백분율 텍스트 스타일을 설정한다.
        setPercentageTextStyle(percentageText, bookUnit2, score, scoreStatistics);

        handler.post(new Runnable() {
            @Override
            public void run() {

                // 중단원 백분율 텍스트뷰를 그리드 레이아웃에 추가한다.
                scoreByUnit2Grid.addView(percentageText);
            }
        });
    }

    // 중단원 백분율 텍스트 스타일을 설정한다.
    private void setPercentageTextStyle(TextView percentageText, BookUnit2 bookUnit2, Float score, ScoreStatistics scoreStatistics) {

        percentageText.setWidth((int) (50 * dp));
        percentageText.setTextColor(Color.BLACK);
        percentageText.setTextSize(10);
        percentageText.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);

        GridLayout.LayoutParams percentageParams = new GridLayout.LayoutParams(
                GridLayout.spec(bookUnit2.getUnit2Number() - 1),
                GridLayout.spec(3));
        percentageParams.setMargins((int) (1 * dp), 0, 0, (int) (3 * dp));

        percentageText.setLayoutParams(percentageParams);

        // 해당 학생이 아직 해당 중단원의 문제를 풀지 않았다면
        if (score == null) {

            percentageText.setText(" - ");
            percentageText.setPadding(0, 0, (int) (15 * dp), 0);
        }
        // 해당 학생이 해당 중단원의 문제를 푼 적이 있다면
        else {
            Float percentage =
                    ConversionService.zToPercentage(
                            (score - scoreStatistics.getAvg()) / scoreStatistics.getSd());

            percentageText.setTag(percentage);

            // 해당 학생의 해당 중단원 점수가 상위 40% 이하라면
            if (percentage == null) {

                percentageText.setText(" - ");
                percentageText.setPadding(0, 0, (int) (15 * dp), 0);
            }
            // 해당 학생의 해당 중단원 점수가 상위 40% 이상이라면
            else {
                percentageText.setText("(" + percentage + "%)");
            }
        }
    }

    // 화살표 텍스트를 만들고 그리드 레이아웃에 추가한다.
    private void addArrowTextToScoreByUnit2Grid(List<BookUnit2> bookUnit2List, GridLayout scoreByUnit2Grid) {

        // 화살표 텍스트를 만든다.
        TextView arrowText = new TextView(holder.itemView.getContext());

        // 화살표 텍스트 스타일을 설정한다.
        setArrowTextStyle(bookUnit2List, arrowText);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 그리드 레이아웃에 화살표 텍스트를 추가한다.
                scoreByUnit2Grid.addView(arrowText);
            }
        });
    }

    // 화살표 텍스트 스타일을 설정한다.
    private void setArrowTextStyle(List<BookUnit2> bookUnit2List, TextView arrowText) {

        arrowText.setGravity(Gravity.CENTER);
        arrowText.setText("〉");
        arrowText.setTextColor(Color.parseColor("#77000000"));
        arrowText.setTextSize(15);
        arrowText.setPadding((int) (15 * dp), 0, (int) (10 * dp), 0);

        GridLayout.LayoutParams arrowParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, bookUnit2List.size(), 1F),
                (!howToViewSwitch.isChecked()) ? GridLayout.spec(3) : GridLayout.spec(4));

        arrowText.setLayoutParams(arrowParams);
    }
}
