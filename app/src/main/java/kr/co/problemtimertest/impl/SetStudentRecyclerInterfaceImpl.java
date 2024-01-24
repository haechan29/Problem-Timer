package kr.co.problemtimertest.impl;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.BookAdapter;
import kr.co.problemtimertest.adapter.ProblemStatisticsAdapter;
import kr.co.problemtimertest.adapter.StudentAdapter;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.listener.SetStudentRecyclerInterface;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.BookUnit2;
import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.ScoreStatistics;
import kr.co.problemtimertest.model.StudentBook;
import kr.co.problemtimertest.service.ConversionService;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SetStudentRecyclerInterfaceImpl implements SetStudentRecyclerInterface {

    private float dp;

    private final StudentAdapter studentAdapter;
    private final RecyclerView.ViewHolder holder;
    private final int position;
    private final Long teacherId;
    private final String classroomName;
    private final boolean isAlignedByGrade;

    private boolean isPageOpen;

    private List<Book> books;

    private TextView clickedBookNameText;

    private BookAdapter bookAdapter;

    private Dialog addBookDialog;

    private PopupMenu deleteStudentPopup;

    // 교재 리싸이클러 관련 변수
    private RecyclerView bookRecycler;

    private LinearLayout bookByStudentContainer;
    private LinearLayout problemRecordContainer;

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

    private TextView addBookText;

    private List<Classroom> classrooms;

    // Retrofit 관련 변수
    private final RetrofitService retrofitService = new RetrofitService();
    private final Retrofit retrofit = retrofitService.getRetrofit();
    private final TimerApi timerApi = retrofit.create(TimerApi.class);
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

    public SetStudentRecyclerInterfaceImpl(
            StudentAdapter studentAdapter,
            RecyclerView.ViewHolder holder,
            int position,
            Long teacherId,
            String classroomName,
            boolean isAlignedByGrade) {

        this.studentAdapter = studentAdapter;
        this.holder = holder;
        this.position = position;
        this.teacherId = teacherId;
        this.classroomName = classroomName;
        this.isAlignedByGrade = isAlignedByGrade;
    }

    @Override
    public void setStudentRecycler() {

        // 변수를 초기화한다.
        initializeVariable();

        // 학생 탭을 설정한다.
        setStudentTab();

        // 교재 리싸이클러를 설정한다.
        setBookRecycler();

        // 교재 추가 버튼을 설정한다.
        setAddBookText();

        // 스위치를 설정한다.
        setHowToViewSwitch();
    }

    // 변수를 초기화한다.
    private void initializeVariable() {

        dp = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        bookAdapter = new BookAdapter();

        addBookDialog = new AddBookDialogUtil(
                holder.itemView.getContext(),
                studentAdapter.getItem(position),
                new BookAdapter[] { bookAdapter }).getDialog();

        bookRecycler = (RecyclerView) holder.itemView.findViewById(R.id.recycler_book);

        bookByStudentContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_book_by_student);
        problemRecordContainer =
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

        addBookText = (TextView) holder.itemView.findViewById(R.id.text_add_book);
    }

    // 학생 탭을 설정한다.
    private void setStudentTab() {

        // 학년 텍스트를 설정한다.
        setStudentGradeText();

        // 학생 삭제 팝업을 초기화한다.
        initializeDeleteStudentPopup();

        bookByStudentContainer.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {

                deleteStudentPopup.show();

                return true;
            }
        });
    }

    // 학년 텍스트를 설정한다.
    private void setStudentGradeText() {

        TextView studentGradeText = (TextView) holder.itemView.findViewById(R.id.text_student_grade);

        // 수업 별로 보기 모드일 때
        if (!isAlignedByGrade) {

            studentGradeText.setText(ConversionService.gradeToStr(studentAdapter.getItem(position).getGrade()));
        }
        // 학년 별로 보기 모드일 때
        else {

            // 수업 리스트를 얻는다.
            getClassrooms();

            threadPool.execute(new Runnable() {

                @Override
                public void run() {

                    if (isClassroomsNullForNms(500L)) return;

                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            List<Classroom> classroomsOfAStudent = classrooms.stream()
                                    .filter(classroom -> classroom.getStudentId().equals(studentAdapter.getItem(position).getId()))
                                    .collect(Collectors.toList());

                            String grade = "";

                            for (Classroom classroom : classroomsOfAStudent) {

                                if (classroomsOfAStudent.indexOf(classroom) == 0) grade = classroom.getName();
                                else grade += ", " + classroom.getName();
                            }

                            studentGradeText.setText(grade);
                        }
                    });
                }
            });
        }
    }

    // 수업 리스트를 얻는다.
    private void getClassrooms() {

        Call<List<Classroom>> call = managementApi.list(teacherId);

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

        Log.d("error", "failed to get : classrooms");

        return true;
    }

    // 학생 삭제 팝업을 초기화한다.
    private void initializeDeleteStudentPopup() {

        deleteStudentPopup = new PopupMenu(holder.itemView.getContext(), bookByStudentContainer);
        deleteStudentPopup.getMenuInflater().inflate(R.menu.popup_student, deleteStudentPopup.getMenu());

        // 학생 삭제 팝업의 아이템을 클릭하면
        // -> 학생을 삭제한다.
        setOnMenuItemClickListenerToPopup();
    }

    // 학생 삭제 팝업의 아이템을 클릭하면
    // -> 학생을 삭제한다.
    private void setOnMenuItemClickListenerToPopup() {

        deleteStudentPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                threadPool.execute(new Runnable() {

                    private Classroom deleted;

                    @Override
                    public void run() {

                        // 해당 학생을 삭제한다.
                        deleteStudent();

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 학생이 0.3초 이내에 삭제되지 않으면
                                if (isStudentNotDeletedForNms(300L)) {

                                    Log.d("error", "failed to delete : student");
                                    return;
                                }

                                // 해당 학생을 어탭터에서 제거한다.
                                removeStudentFromAdapter();
                            }
                        });
                    }

                    // 해당 학생을 삭제한다.
                    private void deleteStudent() {

                        Call<Classroom> call = managementApi.delete(
                                classroomName,
                                teacherId,
                                studentAdapter.getItem(position).getId());

                        call.enqueue(new Callback<Classroom>() {

                            @Override
                            public void onResponse(Call<Classroom> call, Response<Classroom> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "code : " + response.code());
                                    return;
                                }

                                deleted = response.body();

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(holder.itemView.getContext(),
                                                "학생을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Call<Classroom> call, Throwable t) {

                                Log.d("error", t.getMessage());

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(holder.itemView.getContext(),
                                                "학생을 삭제하지 못했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }

                    // 학생이 삭제되었는지 0.00n초간 확인한다.
                    private boolean isStudentNotDeletedForNms(long n) {

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

                            if (deleted == null) continue;

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

                // 해당 학생을 어댑터에서 삭제한다.
                return true;
            }
        });
    }

    // 해당 학생을 어탭터에서 제거한다.
    private void removeStudentFromAdapter() {

        // 해당 학생을 어탭터에서 제거한다.
        studentAdapter.removeItem(position);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 어댑터가 비어 있다면
                if (studentAdapter.getItemCount() == 0) {

                    LinearLayout studentInAClassroomContainer = (LinearLayout) holder.itemView.getParent().getParent();
                    LinearLayout studentContainer = (LinearLayout) studentInAClassroomContainer.getParent();

                    // 해당 수업 탭을 삭제한다.
                    studentContainer.removeView(studentInAClassroomContainer);
                }
                // 어댑터가 비어 있지 않다면
                else {
                    // 학생 어댑터를 새로고침한다.
                    studentAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // 교재 리싸이클러를 설정한다.
    private void setBookRecycler() {

        // 해당 학생이 추가했던 교재 리스트를 얻는다.
        initializeBooks();

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 교재 리스트가 0.5초 이내에 초기화되지 않으면
                if (isBooksNullForNms(500L)) {

                    Log.d("error", "failed to initialize : books");
                    return;
                }

                // 교재 어댑터를 교재 리싸이클러에 설정한다.
                setBookAdapterToBookRecycler();

                // 학생 탭의 교재가 클릭되면
                // -> 학생 탭의 단원 점수 탭에 단원 점수 표를 추가한다.
                setOnClickListenerToBookNameText();

                // 교재 어댑터에 아이템을 추가한다.
                books.forEach(book -> bookAdapter.addItem(book));

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        // 교제 어댑터를 새로고침한다.
                        bookAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    // 해당 학생이 추가했던 교재 리스트를 얻는다.
    private void initializeBooks() {

        Call<List<Book>> call = timerApi.list(studentAdapter.getItem(position).getId());

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
    private boolean isBooksNullForNms(long n){

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

        return true;
    }

    // 교재 어댑터를 교재 리싸이클러에 설정한다.
    private void setBookAdapterToBookRecycler() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                bookRecycler.setLayoutManager(new LinearLayoutManager(
                        holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));

                bookRecycler.setAdapter(bookAdapter);
            }
        });
    }

    // 학생 탭의 교재가 클릭되면
    // -> 학생 탭의 단원 점수 탭에 단원 점수 표를 추가한다.
    private void setOnClickListenerToBookNameText() {

        bookRecycler.addOnChildAttachStateChangeListener(
                new RecyclerView.OnChildAttachStateChangeListener() {

                    @Override
                    public void onChildViewAttachedToWindow(@NonNull View view) {

                        // 교재 버튼을 얻는다.
                        TextView bookNameText = (TextView) view.findViewById(R.id.text_book_name);

                        // 교재 버튼에 교재를 태그로 설정한다.
                        Book book = bookAdapter.getItem(bookRecycler.indexOfChild(view));
                        bookNameText.setTag(book);

                        // 교재 버튼을 클릭하면
                        // -> 버튼의 스타일을 바꾸고
                        // -> 단원 점수 탭에 단원 점수 표를 추가한다.
                        bookNameText.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                // 문제 기록 탭을 설정한다.
                                setProblemRecordContainer(bookNameText);

                                // 단원 점수 표가 보이게 한다.
                                byUnitContainer.setVisibility(View.VISIBLE);
                                byProblemContainer.setVisibility(View.GONE);

                                // 교재 버튼을 0.3초간 사용할 수 없게 한다.
                                bookNameText.setEnabled(false);

                                handler.postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        bookNameText.setEnabled(true);
                                    }
                                }, 300L);
                            }
                        });

                        // 교재 버튼을 길게 클릭하면
                        // -> 교재 팝업을 보여준다.
                        bookNameText.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View view) {

                                // 교재 팝업을 얻는다.
                                PopupMenu bookPopup = new PopupMenu(holder.itemView.getContext(), view);
                                bookPopup.getMenuInflater().inflate(R.menu.popup_book, bookPopup.getMenu());

                                // 교재 팝업에 삭제 기능을 설정한다.
                                setDeleteFunctionBookPopup(bookPopup, book);

                                // 교재 팝업을 띄운다.
                                bookPopup.show();

                                return true;
                            }
                        });
                    }

                    @Override
                    public void onChildViewDetachedFromWindow(@NonNull View view) {}
                }
        );
    }

    // 교재 팝업에 삭제 기능을 설정한다.
    private void setDeleteFunctionBookPopup(PopupMenu bookPopup, Book book) {

        bookPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                // 해당 학생의 해당 교재를 삭제한다.
                deleteBook(book);

                // 교재 어댑터에서 해당 교재를 삭제한다.
                bookAdapter.removeItem(book);

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        // 교재 어댑터를 새로고침한다.
                        bookAdapter.notifyDataSetChanged();
                    }
                });

                return true;
            }
        });
    }

    // 해당 학생의 해당 교재를 삭제한다.
    private void deleteBook(Book book) {

        Call<StudentBook> call = timerApi.delete(studentAdapter.getItem(position).getId(), book.getId());

        call.enqueue(new Callback<StudentBook>() {

            @Override
            public void onResponse(Call<StudentBook> call, Response<StudentBook> response) {

                if (!response.isSuccessful()) {

                    Log.d("error", "code : " + response.code());
                    return;
                }
            }

            @Override
            public void onFailure(Call<StudentBook> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    // 문제 기록 탭을 설정한다.
    private void setProblemRecordContainer(TextView clickedNow) {

        // isPageOpen을 업데이트한다.
        isPageOpen = (clickedBookNameText != null);

        // 문제 기록 탭이 열려있는지 닫혀있는지에 따라서 애니메이션을 적용한다.
        setAnimationListenerToProblemRecordContainer(clickedNow);

        // 단원별 문제 탭을 설정한다.
        setByUnitContainer(clickedNow);
    }

    // 문제 기록 탭이 열려있는지 닫혀있는지에 따라서 애니메이션을 적용한다.
    private void setAnimationListenerToProblemRecordContainer(TextView clickedNow) {

        // 위로 올라오기 애니메이션과 아래로 내려가기 애니메이션을 만든다.
        Animation translateUpAnim = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.translate_up);
        Animation translateDownAnim = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.translate_down);

        // 이전에 클릭되었던 뷰가 다시 클릭되었을 경우
        if (clickedNow == clickedBookNameText) {

            // 위로 올라가기, 아래로 내려가기 애니메이션에 AnimationListener을 설정한다.
            Animation.AnimationListener animListener = new Animation.AnimationListener() {

                @Override
                public void onAnimationEnd(Animation animation) {
                    problemRecordContainer.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            };

            translateDownAnim.setAnimationListener(animListener);
            translateUpAnim.setAnimationListener(animListener);
        }

        // 이전에 클릭되었던 뷰가 다시 클릭되었을 경우
        if (clickedNow == clickedBookNameText) {

            // 문제 기록 탭에 위로 올라가기 애니메이션을 적용한다.
            problemRecordContainer.startAnimation(translateUpAnim);
        }

        // 페이지가 닫혀있는 경우
        if (!isPageOpen) {
            // 문제 기록 탭을 보이게 하고, 아래로 내려가기 애니메이션을 적용한다.
            problemRecordContainer.setVisibility(View.VISIBLE);
            problemRecordContainer.startAnimation(translateDownAnim);
        }
    }

    // 단원별 문제 탭을 설정한다.
    private void setByUnitContainer(TextView clickedNow) {

        // 이전에 클릭되었던 뷰가 다시 클릭되었을 경우
        if (clickedNow == clickedBookNameText) {

            // 클릭된 뷰를 다시 회색 배경에 검은 글씨로 바꾼다.
            setNormalBookStyle(clickedNow);

            // clickedBookNameText를 초기화한다.
            clickedBookNameText = null;

            // 그리드 레이아웃을 삭제한다.
            byUnitContainer.removeAllViews();
        }
        // 이전과 다른 뷰가 클릭된 경우
        else if (clickedBookNameText != null) {

            // 그리드 레이아웃을 삭제한다.
            byUnitContainer.removeAllViews();

            // 이전에 클릭되었던 뷰를 회색 배경에 검은 글씨로 바꾼다.
            setNormalBookStyle(clickedBookNameText);

            // 클릭된 뷰를 검은 배경에 하얀 글씨로 바꾼다.
            setClickedBookStyle(clickedNow);

            // clickedBookNameText를 업데이트한다.
            clickedBookNameText = clickedNow;

            // 단원 점수 표를 설정한다.
            setScoreByUnit2Grid(clickedNow);
        }
        // 새로운 뷰가 처음 클릭된 경우
        else {

            // 클릭된 뷰를 검은 배경에 하얀 글씨로 바꾼다.
            setClickedBookStyle(clickedNow);

            // clickedBookNameText를 업데이트한다.
            clickedBookNameText = clickedNow;

            // 단원 점수 표를 설정한다.
            setScoreByUnit2Grid(clickedNow);
        }
    }

    // 텍스트뷰를 검은 배경에 하얀 글씨로 바꾼다.
    private void setClickedBookStyle(TextView textView) {
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundResource(R.drawable.shape_btn_black);
    }

    // 클릭된 뷰를 회색 배경에 검은 글씨로 바꾼다.
    private void setNormalBookStyle(TextView clickedText) {
        clickedText.setTextColor(Color.BLACK);
        clickedText.setBackgroundResource(R.drawable.shape_btn_gray);
    }

    // 단원 점수 표를 설정한다.
    private void setScoreByUnit2Grid(TextView clickedNow) {

        // 단원 점수 표를 단원 점수 탭에 추가한다.
        addScoreByUnit2Grid(clickedNow);

        // 단원 점수 표가 클릭되면
        // -> 문제 기록 표를 문제 기록 탭에 추가한다.
        setOnClickListenerToScoreByUnit2Grids();
    }

    // 단원 점수 표를 단원 점수 탭에 추가한다.
    private void addScoreByUnit2Grid(TextView clickedNow) {

        new AddScoreByUnit2GridInterfaceImpl(holder, studentAdapter.getItem(position), (Book) clickedNow.getTag()).addScoreByUnit2Grid();
    }

    // 단원 점수 표가 클릭되면
    // -> 문제 기록 표를 문제 기록 탭에 추가한다.
    private void setOnClickListenerToScoreByUnit2Grids() {

        byUnitContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {

                // 그리드 레이아웃을 클릭하면
                // -> 문제별 문제 통계 탭을 설정한다.
                child.setOnClickListener(new OnScoreByUnit2GridClickListenerImpl((GridLayout) child, holder, studentAdapter.getItem(position)));
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {}
        });
    }

    // 교재 추가 버튼을 설정한다.
    private void setAddBookText() {

        addBookText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 교재 추가 대화상자를 띄운다.
                addBookDialog.show();
            }
        });
    }

    // 스위치를 설정한다.
    private void setHowToViewSwitch() {

        // 스위치의 상태에 따라 보기 모드를 변경한다.
        setOnClickListenerToSwitch();
    }

    // 스위치의 상태에 따라 보기 모드를 변경한다.
    private void setOnClickListenerToSwitch() {

        howToViewSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 그리드 레이아웃을 삭제한다.
                byUnitContainer.removeAllViews();

                // 단원별 보기 모드인 경우
                if (byUnitContainer.getVisibility() == View.VISIBLE) {

                    // 단원 점수 표를 설정한다.
                    setScoreByUnit2Grid(clickedBookNameText);
                }
                // 문제별 보기 모드인 경우
                else {

                    // 단원명 탭의 점수 텍스트뷰를 설정한다.
                    BookUnit2 bookUnit2 = (BookUnit2) unitNameUnit2Text.getTag();

                    // 점수 텍스트를 설정한다.
                    setUnitNameScoreText(bookUnit2);

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

    // 점수 텍스트를 설정한다.
    private void setUnitNameScoreText(BookUnit2 bookUnit2) {

        threadPool.execute(new Runnable() {

            private Float score;

            @Override
            public void run() {

                // 점수 탭을 비운다.
                unitNameScoreContainer.removeAllViews();

                // 점수를 초기화한다.
                initializeScore(bookUnit2);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 점수 리스트가 null인지 0.1초간 확인한다.
                        if (isScoreNullForNms(100L)) {}

                        // 점수 텍스트를 점수 탭에 추가한다.
                        addTextsToUnitNameScoreContainer(score, bookUnit2);
                    }
                });
            }

            // 점수를 초기화한다.
            private void initializeScore(BookUnit2 bookUnit2) {

                Call<Float> scoreCall = statisticsApi.read(studentAdapter.getItem(position).getId(), bookUnit2.getId());

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

    // 점수 텍스트를 점수 탭에 추가한다.
    private void addTextsToUnitNameScoreContainer(Float score, BookUnit2 bookUnit2) {

        // 점수 보기 모드라면
        if (!howToViewSwitch.isChecked()) {

            // 점수 텍스트를 컨테이너에 추가한다.
            addScoreTextToUnitNameScoreContainer(score);
        }
        // 등급 보기 모드라면
        else {

            threadPool.execute(new Runnable() {

                private ScoreStatistics scoreStatistics;

                @Override
                public void run() {

                    // 점수 통계를 초기화한다.
                    initializeScoreStatistics(score, bookUnit2);

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            if (score != null) {

                                // 점수 통계가 null인지 확인한다.
                                if (isScoreStatisticsNullForNms(100L)) {}
                            }

                            // 등급 텍스트를 컨테이너에 추가한다.
                            addRankTextToUnitNameScoreContainer(score, scoreStatistics);

                            // 백분율 텍스트를 컨테이너에 추가한다.
                            addPercentageTextToUnitNameScoreContainer(score, scoreStatistics);
                        }
                    });
                }

                // 점수 통계를 초기화한다.
                private void initializeScoreStatistics(Float score, BookUnit2 bookUnit2) {

                    if (score == null) {
                        scoreStatistics = null;
                    }
                    else {

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
                }

                // 점수 통계가 null인지 확인한다.
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

                // 점수 텍스트를 점수 컨테이너에 추가한다.
                unitNameScoreContainer.addView(scoreText, scoreParams);
            }
        });
    }

    // 등급 텍스트를 컨테이너에 추가한다.
    private void addRankTextToUnitNameScoreContainer(
            Float score, ScoreStatistics scoreStatistics) {

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

                // 등급 텍스트를 등급 컨테이너에 추가한다.
                unitNameScoreContainer.addView(rankText, rankParams);
            }
        });
    }

    // 백분율 텍스트를 컨테이너에 추가한다.
    private void addPercentageTextToUnitNameScoreContainer(
            Float score, ScoreStatistics scoreStatistics) {

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

                // 백분율 텍스트를 등급 컨테이너에 추가한다.
                unitNameScoreContainer.addView(percentageText, percentageParams);
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
