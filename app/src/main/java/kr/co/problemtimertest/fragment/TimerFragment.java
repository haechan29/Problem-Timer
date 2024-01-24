package kr.co.problemtimertest.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.StudentMainActivity;
import kr.co.problemtimertest.api.LoginApi;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.impl.AddBookDialogUtil;
import kr.co.problemtimertest.impl.AnimationListenerImpl;
import kr.co.problemtimertest.listener.SetProblemRecyclerInterface;
import kr.co.problemtimertest.model.RecordStatistics;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.RetrofitService;
import kr.co.problemtimertest.adapter.ProblemAdapter;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.RecordByProblem;
import kr.co.problemtimertest.model.StudentBook;
import kr.co.problemtimertest.service.TimerService;
import kr.co.problemtimertest.adapter.BookAdapter;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.listener.TimerInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TimerFragment extends Fragment {

    private Student student;

    Map<Integer, Integer> isSolvedBefore = new HashMap<>();

    // 변수를 생성한다.
    boolean isPageOpen;

    private LinearLayout bookContainer;
    private EditText bookPageEdit;
    private TextView toPrevPageText, toNextPageText;
    private ProgressBar bookProgress;

    private SwipeRefreshLayout problemSwipeRefresh;
    private SwitchCompat gradeModeSwitch;
    private TextView gradeModeOffText, gradeModeOnText;

    // bookAdapter 관련 변수
    private BookAdapter bookAdapter;
    private RecyclerView bookRecycler;
    private TextView clickedBook;
    private Problem problemToSolve;

    // addBookDialog 관련 변수
    private Dialog addBookDialog;
    private TextView addBookText;

    // problemAdapter 관련 변수
    private ProblemAdapter problemAdapter;
    private RecyclerView problemRecycler;

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

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_timer, container, false);

        // 변수를 생성한다.
        initializeVariables(rootView);

        // 교재 추가 버튼을 설정한다.
        setAddBookText();

        // 교재 리싸이클러를 설정한다.
        setBookRecycler();

        // 문제 리싸이클러를 설정한다.
        setProblemRecycler();

        // 문제 스와이프를 설정한다.
        setProblemSwipeRefresh();

        // 채점 모드 스위치를 설정한다.
        setGradeModeSwitch();

        return rootView;
    }

    // 변수를 초기화한다.
    private void initializeVariables(ViewGroup rootView) {

        student = ((StudentMainActivity) getActivity()).getStudent();

        bookContainer = (LinearLayout) rootView.findViewById(R.id.container_book);
        bookPageEdit = (EditText) rootView.findViewById(R.id.edit_book_page);
        toPrevPageText = (TextView) rootView.findViewById(R.id.text_to_prev_page);
        toNextPageText = (TextView) rootView.findViewById(R.id.text_to_next_page);
        bookProgress = (ProgressBar) rootView.findViewById(R.id.progress_book);

        problemSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_problem);
        gradeModeSwitch = (SwitchCompat) rootView.findViewById(R.id.switch_grade_mode);
        gradeModeOffText = (TextView) rootView.findViewById(R.id.text_grade_mode_off);
        gradeModeOnText = (TextView) rootView.findViewById(R.id.text_grade_mode_on);

        problemRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_problem);

        bookAdapter = new BookAdapter();
        bookRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_book);
        clickedBook = null;
        problemToSolve = null;

        addBookDialog = new AddBookDialogUtil(
                getActivity(),
                student,
                new BookAdapter[] { bookAdapter }).getDialog();
        addBookText = (TextView) rootView.findViewById(R.id.text_add_book);
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

    // 교재 리싸이클러를 설정한다.
    private void setBookRecycler() {

        // 페이지 에딧을 설정한다.
        setBookPageEdit();

        // 교재 어댑터에 아이템을 추가한다.
        addItemsToBookAdapter();

        // 교재 버튼을 설정한다.
        setBookNameText();

        // 교재 어댑터에 교재 리싸이클러를 연결한다.
        setBookAdapterToBookRecycler();
    }

    // 페이지 에딧을 설정한다.
    private void setBookPageEdit() {

        // 페이지 에딧을 비활성화한다.
        deactivateBookPageEdit();

        // 페이지 에딧에 페이지가 입력되면
        // -> 해당 페이지로 이동한다.
        setOnEditorActionListenerToBookPageEdit();

        // 이전 페이지 버튼 또는 다음 페이지 버튼이 클릭되면
        // -> 해당 페이지로 이동한다.
        setOnClickListenerToPrevAndNextPageText();
    }

    // 페이지 에딧을 클릭하면
    // -> 교재를 선택해달라는 토스트를 띄우게 한다.
    private void deactivateBookPageEdit() {

        // 페이지 에디트를 0.1초간 초기화한다.
        clearEditForNms(bookPageEdit, 100L);

        bookPageEdit.setFocusable(false);
        bookPageEdit.setCursorVisible(false);

        bookPageEdit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "교재를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 페이지 에디트를 0.00n초간 초기화한다.
    private void clearEditForNms(EditText edit, long n) {

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                for (int i = 0; i < n / 10L; i++) {

                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (edit.getText() == null) break;

                    edit.setText(null);
                }
            }
        });
    }

    // 페이지 에딧에 페이지가 입력되면
    // -> 해당 페이지로 이동한다.
    private void setOnEditorActionListenerToBookPageEdit() {

        bookPageEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 완료 키가 입력될 경우
                        if (keyCode == EditorInfo.IME_ACTION_DONE) {

                            // 현재 페이지 탭에 입력된 숫자를 얻는다.
                            Integer currentPage = getCurrentPage();

                            if (currentPage == null) {

                                // 페이지를 입력해달라는 토스트를 띄운다.
                                Toast.makeText(getActivity(), "이동할 페이지를 입력해주세요.", Toast.LENGTH_SHORT).show();
                            }
                            else {

                                // 클릭된 교재와 입력된 페이지를 이용하여 문제 리스트를 보여준다.
                                showProblemsByPage(getBook(clickedBook), currentPage);
                            }

                            // 페이지 에딧으로부터 포커스를 제거한다.
                            clearFocusFromBookPageEdit();
                        }
                    }
                });

                return true;
            }
        });
    }

    // 현재 페이지 탭에 입력된 숫자를 반환한다.
    private Integer getCurrentPage() {

        // 페이지 에딧에 입력한 값을 얻는다.
        String bookPageEditInput = String.valueOf(bookPageEdit.getText());

        return (bookPageEditInput.length() != 0) ? Integer.parseInt(bookPageEditInput) : null;
    }

    // 클릭된 교재와 입력된 페이지를 이용하여 문제 리스트를 보여준다.
    private void showProblemsByPage(Book book, Integer page) {

        // 교재 어댑터를 비운다.
        problemAdapter.clearItems();

        threadPool.execute(new Runnable() {

            List<Problem> problems;

            @Override
            public void run() {

                // 교재와 페이지를 이용하여 문제 리스트를 얻는다.
                initializeProblemsByBookAndPage(book, page);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 문제 리스트가 0.2초 이내에 초기화되지 않으면
                        if (isProblemsNullForNms(200L)) {

                            Log.d("error", "failed to initialize : problems");
                            return;
                        }

                        // 문제 리스트가 비어있다면
                        if (problems.isEmpty()) {

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 문제가 존재하지 않는다는 토스트를 띄운다.
                                    Toast.makeText(getActivity(), "문제가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        // 문제 리스트가 비어있지 않다면
                        else {

                            // 문제 리스트를 문제 어댑터에 추가한다.
                            problems.forEach(problem -> problemAdapter.addItem(problem));
                        }

                        // 문제 어댑터를 새로고침한다.
                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 문제 어댑터를 새로고침한다.
                                problemAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            }

            // 교재와 페이지를 이용하여 문제 리스트를 얻는다.
            private void initializeProblemsByBookAndPage(Book book, Integer page) {

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재 리싸이클러에 0.2초 이내에 교재 버튼이 추가되지 않는다면
                        if (IsChildNotAddedToBookRecyclerForNms(200L)) {

                            Log.d("error", "failed to add views to : bookRecycler");
                            return;
                        }

                        // timerApi를 통해 해당 교재의 해당 페이지에 있는 문제 리스트를 얻는다.
                        Call<List<Problem>> call = timerApi.list(book.getId(), page);

                        call.enqueue(new Callback<List<Problem>>() {

                            @Override
                            public void onResponse(Call<List<Problem>> call, Response<List<Problem>> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "code : " + response.code());
                                    return;
                                }

                                problems = response.body();
                            }

                            @Override
                            public void onFailure(Call<List<Problem>> call, Throwable t) {
                                Log.d("error", t.getMessage());
                            }
                        });
                    }

                    // 교재 리싸이클러에 교재 버튼이 추가되었는지 0.00n초간 확인한다.
                    private boolean IsChildNotAddedToBookRecyclerForNms(long n) {

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

                            if (bookRecycler.getChildCount() == 0) continue;

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

            // 문제 리스트가 초기화되었는지 0.00n초간 확인한다.
            private boolean isProblemsNullForNms(long n) {

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

                    if (problems == null) continue;

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

    // 클릭한 교재 버튼에 해당하는 교재를 얻는다.
    private Book getBook(TextView clickedBook) {

        // 클릭된 교재 버튼의 인덱스를 얻는다.
        Integer position = null;

        for (int i = 0; i < bookRecycler.getChildCount(); i++) {

            LinearLayout bookNameTextContainer = (LinearLayout) bookRecycler.getChildAt(i);
            TextView bookNameText = (TextView) bookNameTextContainer.getChildAt(0);

            if (bookNameText.getText().equals(clickedBook.getText())) {
                position = i;
            }
        }

        // 얻은 인덱스를 이용해 교재를 얻는다.
        Book book = bookAdapter.getItem(position);

        // 얻은 교재를 반환한다.
        return book;
    }

    // 페이지 에딧으로부터 포커스를 제거한다.
    private void clearFocusFromBookPageEdit() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 페이지 에디트로부터 포커스를 제거한다.
                problemRecycler.setFocusableInTouchMode(true);
                problemRecycler.requestFocus();
            }
        });

        // 키보드를 보이지 않게 한다.
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    // 이전 페이지 버튼 또는 다음 페이지 버튼이 클릭되면
    // -> 해당 페이지로 이동한다.
    private void setOnClickListenerToPrevAndNextPageText() {

        // toPrevPageText에 이전 페이지로 이동하기 기능을 설정한다.
        toPrevPageText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 교재가 선택되었다면
                if (clickedBook != null) {

                    Integer currentPage = getCurrentPage();

                    if (currentPage != null) {

                        Integer prevPage = currentPage - 1;

                        // 클릭된 교재와 입력된 페이지를 이용하여 문제 리스트를 보여준다.
                        showProblemsByPage(getBook(clickedBook), prevPage);

                        // 페이지 에딧에 이전 페이지를 입력한다.
                        bookPageEdit.setText(String.valueOf(prevPage));
                    }
                }
                // 교재가 선택되지 않았다면
                else {
                    Toast.makeText(getActivity(), "교재를 선택해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // toNextPageText에 다음 페이지로 이동하기 기능을 설정한다.
        toNextPageText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 교재가 선택되었다면
                if (clickedBook != null) {

                    Integer currentPage = getCurrentPage();

                    if (currentPage != null) {

                        Integer nextPage = currentPage + 1;

                        // 클릭된 교재와 입력된 페이지를 이용하여 문제 리스트를 보여준다.
                        showProblemsByPage(getBook(clickedBook), nextPage);

                        // 페이지 에딧에 다음 페이지를 입력한다.
                        bookPageEdit.setText(String.valueOf(nextPage));
                    }
                }
                // 교재가 선택되지 않았다면
                else {
                    Toast.makeText(getActivity(), "교재를 선택해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 교재 어댑터에 아이템을 추가한다.
    private void addItemsToBookAdapter() {

        threadPool.execute(new Runnable() {

            private List<Book> books;

            @Override
            public void run() {

                // 해당 학생이 추가했던 교재 리스트를 얻는다.
                initializeBooks();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재 리스트가 0.2초 이내에 초기화되지 않았다면
                        if (isBooksNullForNms(500L)) {

                            Log.d("error", "failed to initialize : books");
                            return;
                        }

                        books.forEach(book -> bookAdapter.addItem(book));

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 어댑터를 새로고침한다.
                                bookAdapter.notifyDataSetChanged();
                            }
                        });

                        // 교재 프로그레스를 0.1초간은 보이게 하기 위해 작업을 지연한다.
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 프로그레스를 보이지 않게 한다.
                                bookProgress.setVisibility(View.GONE);

                                // 교재 탭을 보이게 한다.
                                bookContainer.setVisibility(View.VISIBLE);
                            }
                        }, 100L);
                    }
                });
            }

            // 해당 학생이 추가했던 교재 리스트를 얻는다.
            private void initializeBooks() {

                Call<List<Book>> call = timerApi.list(student.getId());

                call.enqueue(new Callback<List<Book>>() {

                    @Override
                    public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "\ncode : " + response.code());
                            return;
                        }

                        books = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<Book>> call, Throwable t) {
                        Log.d("error", "\n" + t.getMessage());
                    }
                });
            }

            // 교재 리스트가 초기화되었는지 0.00n초간 확인한다.
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

                return true;
            }
        });
    }

    // 교재 버튼을 설정한다.
    private void setBookNameText() {

        bookRecycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {

            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

                // 추가한 교재 버튼을 얻는다.
                TextView addedBook = ((TextView) ((LinearLayout) view).getChildAt(0));

                // 교재 버튼을 클릭하면
                // -> 클릭한 교재 버튼의 스타일을 바꾸고
                // -> 해당 교재의 첫 페이지에 있는 문제 리스트를 보여준다.
                setOnClickListenerToAddedBook(addedBook);

                // 교재 버튼을 길게 클릭하면
                // -> 교재 팝업을 띄운다.
                setOnLongClickListenerToAddedBook(
                        addedBook, bookAdapter.getItem(bookRecycler.indexOfChild(view)));
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {}
        });
    }

    // 교재 버튼이 클릭되면
    // -> 클릭한 교재 버튼의 스타일을 바꾸고
    // -> 해당 교재의 첫 페이지에 있는 문제 리스트를 보여준다.
    private void setOnClickListenerToAddedBook(TextView addedBook) {

        addedBook.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Todo: 과제하기 탭에서 들어온 경우 프로그레스 바를 보여주는 기능 구현하기

                // 특정 문제로 이동하는 경우
                if (problemToSolve != null) {

                    // 클릭된 뷰를 검은 배경에 하얀 글씨로 바꾼다.
                    setClickedBookStyle(addedBook);

                    // clickedBook을 업데이트한다.
                    clickedBook = addedBook;

                    // 페이지 에딧을 활성화한다.
                    activateBookPageEdit();

                    // problemToSolve를 초기화한다.
                    problemToSolve = null;
                }
                // 이전에 클릭되었던 뷰가 또 다시 클릭되었을 경우
                else if (addedBook == clickedBook) {

                    // 클릭된 뷰를 다시 회색 배경에 검은 글씨로 바꾼다.
                    setNormalBookStyle(clickedBook);

                    // clickedBook을 초기화한다.
                    clickedBook = null;

                    // 문제 어댑터를 비운다.
                    problemAdapter.clearItems();

                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            // 문제 어댑터를 새로고침한다.
                            problemAdapter.notifyDataSetChanged();
                        }
                    });

                    // 페이지 에딧을 비활성화한다.
                    deactivateBookPageEdit();
                }
                // 이전과 다른 뷰가 클릭된 경우
                else if (clickedBook != null) {

                    // 이전에 클릭되었던 뷰를 회색 배경에 검은 글씨로 바꾼다.
                    setNormalBookStyle(clickedBook);

                    // 클릭된 뷰를 검은 배경에 하얀 글씨로 바꾼다.
                    setClickedBookStyle(addedBook);

                    // clickedBook을 업데이트한다.
                    clickedBook = addedBook;

                    // 페이지 에딧을 활성화한다.
                    activateBookPageEdit();

                    // 해당 교재의 첫 페이지에 있는 문제들을 보여준다.
                    showProblemsInFirstPage(getBook(addedBook));
                }
                // 새로운 뷰가 처음 클릭된 경우
                else {

                    // 클릭된 뷰를 검은 배경에 하얀 글씨로 바꾼다.
                    setClickedBookStyle(addedBook);

                    // clickedBook을 업데이트한다.
                    clickedBook = addedBook;

                    // 페이지 에딧을 활성화한다.
                    activateBookPageEdit();

                    // 해당 교재의 첫 페이지에 있는 문제들을 보여준다.
                    showProblemsInFirstPage(getBook(addedBook));
                }

                // Todo : Bug - bookRecyclerView에 특정 뷰가 보이지 않음. 화면 크기만큼만 뷰를 가져오는 것으로 보임.
            }
        });
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

    // 페이지 에딧을 활성화한다.
    private void activateBookPageEdit() {

        bookPageEdit.setFocusableInTouchMode(true);
        bookPageEdit.setCursorVisible(true);
        bookPageEdit.setOnClickListener(null);
    }

    // 해당 교재의 첫 페이지에 있는 문제들을 보여준다.
    private void showProblemsInFirstPage(Book book) {

        // 문제 어댑터를 비운다.
        problemAdapter.clearItems();

        threadPool.execute(new Runnable() {

            List<Problem> problems;

            @Override
            public void run() {

                // 문제 리스트를 얻는다.
                initializeProblems();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 문제 리스트가 초기화되었는지 0.2초간 확인한다.
                        if (isProblemsNullForNms(200L)) {

                            Log.d("error", "failed to initialize : problems");
                            return;
                        }

                        if (problems.isEmpty()) {

                            // 페이지 에딧을 클릭하면
                            // -> 교재를 선택해달라는 토스트를 띄우게 한다.
                            deactivateBookPageEdit();
                        }
                        else {

                            // 페이지 에딧에 해당 교재의 첫 페이지를 입력한다.
                            bookPageEdit.setText(String.valueOf(problems.get(0).getPage()));

                            // 교재 어댑터에 얻은 문제 리스트를 추가한다.
                            problems.forEach(problem -> problemAdapter.addItem(problem));
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 어댑터를 새로고침한다.
                                problemAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            }

            // 문제 리스트를 얻는다.
            private void initializeProblems() {

                // timerApi를 이용해 해당 교재의 첫 페이지에 있는 문제들을 보여준다.
                Call<List<Problem>> call = timerApi.listProblemsInFirstPage(book.getId());

                call.enqueue(new Callback<List<Problem>>() {

                    @Override
                    public void onResponse(Call<List<Problem>> call, Response<List<Problem>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        problems = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<Problem>> call, Throwable t) {
                        t.getMessage();
                    }
                });
            }

            // 문제 리스트가 초기화되었는지 0.00n초간 확인한다.
            private boolean isProblemsNullForNms(long n) {

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

                    if (problems == null) continue;

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

    // 교재 버튼을 길게 클릭하면
    // -> 교재 팝업을 띄운다.
    private void setOnLongClickListenerToAddedBook(TextView addedBook, Book book) {

        addedBook.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {

                // 교재 팝업을 얻는다.
                PopupMenu bookPopup = new PopupMenu(getActivity(), addedBook);
                bookPopup.getMenuInflater().inflate(R.menu.popup_book, bookPopup.getMenu());

                bookPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        // 해당 학생의 교재 데이터베이스에서 해당 교재를 삭제한다.
                        deleteBookFromStudent(student.getId(), book.getId());

                        // 교재 어댑터에서 해당 교재를 삭제한다.
                        bookAdapter.removeItem(book);

                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 어댑터를 새로고침한다.
                                bookAdapter.notifyDataSetChanged();

                                // 삭제한 교재가 클릭되어 있었다면
                                if (addedBook.equals(clickedBook)) {

                                    clickedBook = null;

                                    for (int i = 0; i < bookRecycler.getChildCount(); i++) {

                                        TextView child = (TextView) bookRecycler.getChildAt(i).findViewById(R.id.text_book_name);

                                        // 자식 뷰를 회색 배경에 검은 글씨로 바꾼다.
                                        setNormalBookStyle(child);
                                    }

                                    // 페이지 에딧을 비운다.
                                    bookPageEdit.setText(null);

                                    // 문제 어댑터를 비운다.
                                    problemAdapter.clearItems();

                                    // 문제 어댑터를 새로고침한다.
                                    problemAdapter.notifyDataSetChanged();
                                }
                            }
                        }, 100L);

                        return true;
                    }
                });

                // 교재 팝업을 보여준다.
                bookPopup.show();

                return false;
            }
        });
    }

    // 해당 학생의 교재 데이터베이스에서 해당 교재를 삭제한다.
    private void deleteBookFromStudent(Long studentId, Long bookId) {

        Call<StudentBook> call = timerApi.delete(studentId, bookId);

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

    // 교재 어댑터에 교재 리싸이클러를 연결한다.
    private void setBookAdapterToBookRecycler() {

        bookRecycler.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.HORIZONTAL, false));

        // 교재 어댑터에 교재 리싸이클러를 연결한다.
        bookRecycler.setAdapter(bookAdapter);

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // 교재 어댑터를 새로고침한다.
                bookAdapter.notifyDataSetChanged();
            }
        }, 500L);
    }

    // 문제 리싸이클러를 설정한다.
    private void setProblemRecycler() {

        // 문제 어댑터를 얻는다.
        problemAdapter = new ProblemAdapter();

        // 교재 어댑터에 SetProblemRecyclerInterface를 설정한다.
        setSetProblemRecyclerInterface(problemAdapter);

        // 문제 리싸이클러에 문제 어댑터를 설정한다.
        setProblemAdapterToProblemRecycler();
    }

    // 교재 어댑터에 SetProblemRecyclerInterface를 설정한다.
    private void setSetProblemRecyclerInterface(ProblemAdapter problemAdapter) {

        problemAdapter.setSetProblemRecyclerInterface(new SetProblemRecyclerInterface() {

            @Override
            public void setProblemRecycler(RecyclerView.ViewHolder holder, int position) {

                // 사용된 뷰를 원래 상태로 되돌린다.
                resetView(holder);

                // 채점 기록 탭을 설정한다.
                setIsSolvedContainer(holder, position);

                // 문제 탭을 클릭하면
                // -> 문제 기록 탭을 보이게 하고
                // -> 문제 기록 탭에 문제 기록 표를 설정한다.
                setOnClickListenerToTimerContainer(holder.itemView, position);

                // 문제 탭의 타이머 버튼을 클릭하면
                // -> 타이머가 작동하고 멈추게 한다.
                setOnClickListenerToStartAndStopBtn(holder, position);

                // 채점 모드를 해제한다.
                setGradeModeOff(holder, position);
            }
        });
    }

    // 사용된 뷰를 원래 상태로 되돌린다.
    private void resetView(RecyclerView.ViewHolder holder) {

        // 문제 탭을 닫는다.
        LinearLayout recordContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_record);
        recordContainer.setVisibility(View.GONE);

        // 시작 버튼과 텍스트를 회색으로 설정한다.
        Button startBtn = holder.itemView.findViewById(R.id.btn_start);
        startBtn.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
        startBtn.setTextColor(Color.parseColor("#99000000"));

        // 채점 모드 스위치를 끈다.
        gradeModeSwitch.setChecked(false);
    }

    // 채점 기록 탭을 설정한다.
    private void setIsSolvedContainer(RecyclerView.ViewHolder holder, int position) {

        // 시간 탭을 얻는다.
        LinearLayout isSolvedContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_is_solved);

        threadPool.execute(new Runnable() {

            private List<RecordByProblem> recordsByProblem;

            @Override
            public void run() {

                // 문제 기록을 얻는다.
                initializeRecordsByProblem();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 문제 기록이 null인지 0.2초간 확인한다.
                        if (isRecordsByProblemNullForNms(200L)) {

                            Log.d("error", "failed to initialize : recordsByProblem");
                            return;
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {

                                // 채점 기록 탭을 비운다.
                                isSolvedContainer.removeAllViews();
                            }
                        });

                        // 만약 채점 기록이 3개 이하라면
                        if (recordsByProblem.size() <= 3) {

                            for (RecordByProblem recordByProblem : recordsByProblem) {

                                if (recordByProblem.getIsSolved() == null) break;

                                // 채점 기록 텍스트를 얻는다.
                                TextView isSolvedText = getIsSolvedText(recordByProblem);

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        // 채점 기록 텍스트를 채점 기록 탭에 추가한다.
                                        isSolvedContainer.addView(isSolvedText);
                                    }
                                });
                            }
                        }
                        // 만약 채점 기록이 3개 초과라면
                        else {

                            List<RecordByProblem> lastThreeRecordsByProblem =
                                    getLastThreeRecordsByProblem(recordsByProblem);

                            for (RecordByProblem recordByProblem : lastThreeRecordsByProblem) {

                                // 채점 기록 텍스트를 얻는다.
                                TextView isSolvedText = getIsSolvedText(recordByProblem);

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        // 채점 기록 텍스트를 채점 기록 탭에 추가한다.
                                        isSolvedContainer.addView(isSolvedText);
                                    }
                                });
                            }
                        }
                    }
                });
            }

            // 문제 기록을 얻는다.
            private void initializeRecordsByProblem() {

                Call<List<RecordByProblem>> call = timerApi.list(student.getId(), problemAdapter.getItem(position).getId());

                call.enqueue(new Callback<List<RecordByProblem>>() {

                    @Override
                    public void onResponse(Call<List<RecordByProblem>> call, Response<List<RecordByProblem>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        recordsByProblem = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<RecordByProblem>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 문제 기록이 null인지 0.00n초간 확인한다.
            private boolean isRecordsByProblemNullForNms(long n) {

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

                    if (recordsByProblem == null) continue;

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

    // 채점 기록 텍스트를 반환한다.
    private TextView getIsSolvedText(RecordByProblem recordByProblem) {

        TextView isSolvedText = new TextView(getContext());

        LinearLayout.LayoutParams isSolvedParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        isSolvedText.setLayoutParams(isSolvedParams);

        switch (recordByProblem.getIsSolved()) {

            case 0: isSolvedText.setText("⭕"); break;
            case 1: isSolvedText.setText("❌"); break;
            case 2: isSolvedText.setText("⭐");
        }

        isSolvedText.setTextSize(10);

        return isSolvedText;
    }

    // 채점 기록 텍스트를 얻는다.
    private List<RecordByProblem> getLastThreeRecordsByProblem(List<RecordByProblem> recordsByProblem) {

        List<RecordByProblem> lastThreeRecordsByProblem = new ArrayList<>();

        // 마지막 문제가 채점 되어있지 않다면
        if (recordsByProblem.get(recordsByProblem.size() - 1).getIsSolved() == null) {

            // 채점 기록 리스트의 마지막 세 요소를 가져와 새로운 리스트를 만든다.
            lastThreeRecordsByProblem.add(recordsByProblem.get(recordsByProblem.size() - 4));
            lastThreeRecordsByProblem.add(recordsByProblem.get(recordsByProblem.size() - 3));
            lastThreeRecordsByProblem.add(recordsByProblem.get(recordsByProblem.size() - 2));
        }
        // 마지막 문제가 채점 되어있지 않다면
        else {

            // 채점 기록 리스트의 마지막 세 요소를 가져와 새로운 리스트를 만든다.
            lastThreeRecordsByProblem.add(recordsByProblem.get(recordsByProblem.size() - 3));
            lastThreeRecordsByProblem.add(recordsByProblem.get(recordsByProblem.size() - 2));
            lastThreeRecordsByProblem.add(recordsByProblem.get(recordsByProblem.size() - 1));
        }

        return lastThreeRecordsByProblem;
    }

    // 문제 탭을 클릭하면
    // -> 문제 기록 탭을 보이게 하고
    // -> 문제 기록 탭에 문제 기록 표를 설정한다.
    private void setOnClickListenerToTimerContainer(View itemView, int position) {

        LinearLayout timerContainer = (LinearLayout) itemView.findViewById(R.id.container_timer);

        timerContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 문제 기록 표를 설정한다.
                setShowRecordContainer(itemView);

                // 문제 기록 탭에 문제 기록 표를 설정한다.
                setSetTimeRecord(itemView, position);
            }
        });
    }

    // 채점 모드를 설정한다.
    private void setGradeMode(View itemView, int position, boolean isGradeModeOn) {

        threadPool.execute(new Runnable() {

            private List<RecordByProblem> recordsByProblem;
            private RecordByProblem recordByProblem;

            private TextView timeText;
            private Button startBtn;
            private Button stopBtn;

            private LinearLayout timerContainer;
            private LinearLayout answerContainer;

            @Override
            public void run() {

                // 변수를 초기화한다.
                initializeVariables(itemView);

                // 채점하기 버튼을 눌렀을 때
                if (isGradeModeOn) {

                    // 문제 기록 리스트를 얻는다.
                    initializeRecordsByProblem();

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 문제 기록 리스트가 null인지 0.2초간 확인한다.
                            if (isRecordsByProblemNullForNms(200L)) {

                                Log.d("error", "failed to initialize : recordsByProblem");
                                return;
                            }

                            // 문제 탭을 채점 모드로 설정한다.
                            setGradeModeToProblemTab(recordsByProblem);
                        }
                    });
                }
                // 채점 완료 버튼을 눌렀을 때
                else {

                    // 채점 기록을 얻는다.
                    Integer isSolved = getIsSolved();

                    // 채점을 했고, 채점 기록이 달라졌다면
                    if (isSolved != null && isSolved != isSolvedBefore.getOrDefault(position, null)) {

                        // 학생의 데이터베이스에 문제 기록을 업데이트한다.
                        updateRecordByProblemToStudent(isSolved);

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 문제 기록이 null인지 0.00n초간 확인한다.
                                if (isRecordByProblemNullForNms(200L)) {

                                    Log.d("error", "failed to initialize : recordByProblem");
                                    return;
                                };

                                // 기록 통계를 업데이트한다.
                                updateRecordStatistics(recordByProblem);

                                // 학생의 중단원 점수를 업데이트한다.
                                updateScoreToStudent(recordByProblem);
                            }
                        });
                    }

                    // 문제 기록 리스트를 얻는다.
                    initializeRecordsByProblem();

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 문제 기록 리스트가 null인지 0.2초간 확인한다.
                            if (isRecordsByProblemNullForNms(200L)) {

                                Log.d("error", "failed to initialize : recordsByProblem");
                                return;
                            }

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    // 문제를 채점하지 않았다면
                                    if (recordsByProblem.isEmpty() ||
                                            recordsByProblem.get(recordsByProblem.size() - 1).getIsSolved() != null) {

                                        // 시작 버튼을 사용할 수 있게 한다.
                                        setEnabledToStartBtn(true);
                                    }
                                }
                            });
                        }
                    });

                    // 문제 탭의 타이머를 사용할 수 있게 한다.
                    setEnabledToTimer(true);

                    // 문제 탭에 채점 모드를 해제한다.
                    setGradeModeOffToProblemTab();
                }
            }

            // 변수를 초기화한다.
            private void initializeVariables(View itemView) {

                timeText = (TextView) itemView.findViewById(R.id.text_time);
                startBtn = (Button) itemView.findViewById(R.id.btn_start);
                stopBtn = (Button) itemView.findViewById(R.id.btn_stop);

                timerContainer = (LinearLayout) itemView.findViewById(R.id.container_timer);
                answerContainer = (LinearLayout) itemView.findViewById(R.id.container_answer);
            }

            // 문제 기록 리스트를 얻는다.
            private void initializeRecordsByProblem() {

                Call<List<RecordByProblem>> call = timerApi.list(student.getId(), problemAdapter.getItem(position).getId());

                call.enqueue(new Callback<List<RecordByProblem>>() {

                    @Override
                    public void onResponse(Call<List<RecordByProblem>> call, Response<List<RecordByProblem>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        recordsByProblem = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<RecordByProblem>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 문제 기록 리스트가 null인지 0.00n초간 확인한다.
            private boolean isRecordsByProblemNullForNms(long n) {

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

                    if (recordsByProblem == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 문제 탭을 채점 모드로 설정한다.
            private void setGradeModeToProblemTab(List<RecordByProblem> recordsByProblem) {

                // 문제를 푼 적이 없다면
                if (recordsByProblem.isEmpty()) {

                    // 문제 탭의 타이머를 사용할 수 없게 한다.
                    setEnabledToTimer(false);
                }
                // 문제를 푼 적이 있다면
                else {

                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            // 시간 텍스트, 시작 버튼, 멈춤 버튼을 보이지 않게 한다.
                            timeText.setVisibility(View.GONE);
                            startBtn.setVisibility(View.GONE);
                            stopBtn.setVisibility(View.GONE);

                            // 정답 탭을 보이게 한다.
                            answerContainer.setVisibility(View.VISIBLE);
                        }
                    });

                    // 문제 탭의 색깔을 설정한다.
                    setColorToProblemTab(recordsByProblem);
                }
            }

            // 문제 탭의 타이머를 사용할 수 있거나 없게 한다.
            private void setEnabledToTimer(boolean b) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        if (b) {

                            // 시간 텍스트, 시작 버튼, 멈춤 버튼을 보이게 한다.
                            timeText.setVisibility(View.VISIBLE);
                            startBtn.setVisibility(View.VISIBLE);
                            stopBtn.setVisibility(View.VISIBLE);

                            // 시작 버튼과 멈춤 버튼을 사용할 수 있게 한다.
                            startBtn.setEnabled(true);
                            stopBtn.setEnabled(true);
                        }
                        else {

                            // 시작 버튼과 멈춤 버튼을 사용할 수 없게 한다.
                            startBtn.setEnabled(false);
                            startBtn.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
                            startBtn.setTextColor(Color.parseColor("#99000000"));

                            stopBtn.setEnabled(false);

                            timerContainer.setBackgroundColor(Color.parseColor("#11000000"));

                            // 문제 탭을 클릭하면
                            // -> 문제를 풀지 않았다는 토스트를 띄운다.
                            timerContainer.setOnClickListener(null);
                            timerContainer.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View view) {
                                    Toast.makeText(getActivity(),
                                            problemAdapter.getItem(position).getNumber() + "번 문제를 풀지 않았습니다.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }

            // 문제 탭의 색깔을 설정한다.
            private void setColorToProblemTab(List<RecordByProblem> recordsByProblem) {

                // 마지막 문제 기록의 채점 기록을 얻는다.
                Integer isSolved = recordsByProblem.get(recordsByProblem.size() - 1).getIsSolved();

                if (isSolved != null) {

                    // 얻은 채점 기록에 따라 문제 탭의 색깔을 변경한다.
                    switch (isSolved) {

                        case 0:
                            timerContainer.setBackgroundResource(R.color.right);
                            break;

                        case 1:
                            timerContainer.setBackgroundResource(R.color.wrong);
                            break;

                        case 2:
                            timerContainer.setBackgroundResource(R.color.question);
                    }

                    // 마지막 문제 기록의 채점 기록을 isSolvedBefore에 저장한다.
                    isSolvedBefore.put(position, isSolved);
                }

                // 문제 탭을 클릭하면
                // -> 문제 탭의 색깔을 변경한다.
                // normal -> right -> wrong -> question -> right -> ...
                timerContainer.setOnClickListener(null);
                timerContainer.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        switch (((ColorDrawable) view.getBackground()).getColor()) {

                            case 0xFFFFFFFF: // R.color.normal
                            case 0xFFFAFABE: // R.color.question
                                view.setBackgroundResource(R.color.right);
                                break;

                            case 0xFFCDECFA: // R.color.right
                                view.setBackgroundResource(R.color.wrong);
                                break;

                            case 0xFFFFE1E6: // R.color.wrong
                                view.setBackgroundResource(R.color.question);
                        }
                    }
                });
            }

            // 채점 기록을 얻는다.
            private Integer getIsSolved() {

                Integer isSolved = null;

                switch (((ColorDrawable) timerContainer.getBackground()).getColor()) {

                    case 0x110000FF: // R.color.right
                        isSolved = 0;
                        break;

                    case 0x11FF0000: // R.color.wrong
                        isSolved = 1;
                        break;

                    case 0x11FFFF00: // R.color.question
                        isSolved = 2;
                }

                return isSolved;
            }

            // 학생의 데이터베이스에 문제 기록을 추가한다.
            private void updateRecordByProblemToStudent(Integer isSolved) {

                Call<RecordByProblem> call = timerApi.create(
                        new RecordByProblem(
                                null,
                                student.getId(),
                                problemAdapter.getItem(position).getId(),
                                null,
                                null,
                                isSolved));

                call.enqueue(new Callback<RecordByProblem>() {

                    @Override
                    public void onResponse(Call<RecordByProblem> call, Response<RecordByProblem> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        recordByProblem = response.body();
                    }

                    @Override
                    public void onFailure(Call<RecordByProblem> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 문제 기록이 null인지 0.00n초간 확인한다.
            private boolean isRecordByProblemNullForNms(long n) {

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

                    if (recordByProblem == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 기록 통계를 업데이트한다.
            private void updateRecordStatistics(RecordByProblem recordByProblem) {

                Call<RecordStatistics>  recordStatisticsCall = timerApi.update(new RecordByProblem(
                        null,
                        student.getId(),
                        recordByProblem.getProblemId(),
                        recordByProblem.getTimeRecord(),
                        null,
                        recordByProblem.getIsSolved()));

                recordStatisticsCall.enqueue(new Callback<RecordStatistics>() {
                    @Override
                    public void onResponse(Call<RecordStatistics> call, Response<RecordStatistics> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<RecordStatistics> call, Throwable t) {
                        t.getMessage();
                    }
                });

            }

            // 학생의 중단원 점수를 업데이트한다.
            private void updateScoreToStudent(RecordByProblem recordByProblem) {

                Call<Float> scoreCall = timerApi.calculate(
                        recordByProblem.getTimeRecord(),
                        recordByProblem.getStudentId(),
                        recordByProblem.getProblemId());

                scoreCall.enqueue(new Callback<Float>() {

                    @Override
                    public void onResponse(Call<Float> call, Response<Float> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        Float difference = response.body();

                        if (difference != null) {

                            if (difference >= 0.0001F) {

                                // 기록되었다는 토스트를 점수와 함께 띄운다.
                                Toast.makeText(getActivity(),
                                        "기록되었습니다. ( " + String.format("%+2.1f", difference) + " )",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {

                                // 기록되었다는 토스트만 띄운다.
                                Toast.makeText(getActivity(),
                                        "기록되었습니다.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Float> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 시작 버튼을 사용할 수 있거나 없게 한다.
            private void setEnabledToStartBtn(boolean b) {

                if (b) {

                    // 시작 버튼을 사용 할 수 있게 한다.
                    startBtn.setBackgroundResource(R.drawable.shape_btn_soft_activated);
                    startBtn.setTextColor(Color.BLACK);

                    // 시작 버튼을 누르면
                    // -> 타이머를 작동시킨다.
                    startBtn.setOnClickListener(null);
                    startBtn.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            // 문제로부터 타이머를 얻는다.
                            Problem item = problemAdapter.getItem(position);
                            TimerService timerService = item.getTimerService();

                            // 얻은 타이머에 TimerInterface 구현 객체를 설정한다.
                            TimerInterface timerInterface = getTimerInterfaceImpl(itemView, position);
                            timerService.setTimerInterface(timerInterface);

                            // 타이머를 작동시킨다.
                            item.startTimer();
                        }
                    });
                }
                else {

                }
            }

            // 문제 탭에 채점 모드를 해제한다.
            private void setGradeModeOffToProblemTab() {

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // 정답 탭을 보이지 않게 한다.
                        answerContainer.setVisibility(View.GONE);

                        // 문제 탭의 색깔을 원래대로 변경한다.
                        timerContainer.setBackgroundResource(R.color.normal);

                        // 문제 탭을 클릭하면
                        // -> 문제 기록 탭을 보이게 하고
                        // -> 문제 기록 탭에 문제 기록 표를 설정한다.
                        timerContainer.setOnClickListener(null);
                        setOnClickListenerToTimerContainer(itemView, position);
                    }
                });
            }
        });
    }

    // 문제 탭을 클릭하면
    // -> 문제 기록 표를 설정한다.
    private void setShowRecordContainer(View itemView) {

        LinearLayout recordContainer = (LinearLayout) itemView.findViewById(R.id.container_record);

        // isPageOpen을 업데이트한다.
        updateIsPageOpen(recordContainer);

        // 문제 기록 탭에 애니메이션을 설정한다.
        setAnimationToRecordContainer(recordContainer);
    }

    // isPageOpen을 업데이트한다.
    private void updateIsPageOpen(LinearLayout recordContainer) {

        // 문제 기록 탭의 보이는 상태를 확인한다.
        switch (recordContainer.getVisibility()) {

            // 문제 기록 탭이 보이지 않게 되어 있는 경우
            case View.GONE:
                isPageOpen = false;
                break;

            // 문제 기록 탭이 보이게 설정되어 있는 경우
            case View.VISIBLE:
                isPageOpen = true;
        }
    }

    // 문제 기록 탭에 애니메이션을 설정한다.
    private void setAnimationToRecordContainer(LinearLayout recordContainer) {

        // Todo : 애니메이션 적용하기
        // 위로 올라오기 애니메이션과 아래로 내려가기 애니메이션을 만든다.
        Animation translateUpAnim = AnimationUtils.loadAnimation(
                getActivity(), R.anim.translate_up);
        Animation translateDownAnim = AnimationUtils.loadAnimation(
                getActivity(), R.anim.translate_down);

        // SlidingPageAnimation 객체를 만들고 위로 올라오기, 아래로 내려가기 애니메이션에 해당 객체를 적용한다.
        Animation.AnimationListener animListener = new AnimationListenerImpl(isPageOpen, recordContainer);
        translateDownAnim.setAnimationListener(animListener);
        translateUpAnim.setAnimationListener(animListener);

        // 페이지가 열려있는 경우 recordTable에 아래로 내려가기 애니메이션을 적용한다.
        if (isPageOpen) {
            recordContainer.startAnimation(translateDownAnim);
        }
        // 페이지가 닫혀있는 경우 recordTable을 보이지 않게 하고, 위로 올라가기 애니메이션을 적용한다.
        else {
            recordContainer.setVisibility(View.VISIBLE);
            recordContainer.startAnimation(translateUpAnim);
        }
    }

    // 문제 탭을 클릭하면
    // -> 문제 기록 탭에 문제 기록 표를 설정한다.
    private void setSetTimeRecord(View itemView, int position) {

        GridLayout recordGrid = (GridLayout) itemView.findViewById(R.id.grid_record);

        // 만약 문제 기록 탭이 닫혀있다면
        if (!isPageOpen) {

            // 문제 기록 표에 문제 기록을 추가한다.
            addViewsToRecordGrid(recordGrid, position);
        }
        // 만약 문제 기록 탭이 열려있다면
        else {

            // 문제 기록에서 맨 윗줄만 남기고, 뷰를 삭제한다.
            recordGrid.removeViews(4, recordGrid.getChildCount() - 4);
        }
    }

    // 문제 기록 표에 문제 기록을 추가한다.
    private void addViewsToRecordGrid(GridLayout recordGrid, int position) {

        threadPool.execute(new Runnable() {

            private List<RecordByProblem> recordsByProblem;

            @Override
            public void run() {

                // 문제 기록을 초기화한다.
                initializeRecordsByProblem();

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 문제 기록 리스트가 null인지 0.2초간 확인한다.
                        if (isRecordsByProblemNullForNms(200L)) {

                            Log.d("error", "failed to initialize : recordsByProblem");
                            return;
                        }

                        // 시간 기록 리스트를 이용하여 문제 기록 표를 만든다.
                        for (int i = 0; i < recordsByProblem.size(); i++) {

                            RecordByProblem recordByProblem = recordsByProblem.get(i);

                            // 인덱스 텍스트
                            addIndexText(recordGrid, i);

                            // 날짜 텍스트
                            addDateText(recordByProblem, recordGrid, i);

                            // 기록 텍스트
                            addRecordText(recordByProblem, recordGrid);

                            // 채점 텍스트
                            addIsSolvedText(recordByProblem, recordGrid);
                        }
                    }
                });
            }

            // 문제 기록을 초기화한다.
            private void initializeRecordsByProblem() {

                Call<List<RecordByProblem>> call = timerApi.list(student.getId(), problemAdapter.getItem(position).getId());

                call.enqueue(new Callback<List<RecordByProblem>>() {

                    @Override
                    public void onResponse(Call<List<RecordByProblem>> call, Response<List<RecordByProblem>> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }

                        // 해당 학생이 문제를 푼 시간 기록 리스트를 가져온다.
                        recordsByProblem = response.body();
                    }

                    @Override
                    public void onFailure(Call<List<RecordByProblem>> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }

            // 문제 기록 리스트가 null인지 0.00n초간 확인한다.
            private boolean isRecordsByProblemNullForNms(long n) {

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

                    if (recordsByProblem == null) continue;

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

    // 문제 기록 표에 인덱스 텍스트를 추가한다.
    private void addIndexText(GridLayout recordGrid, int index) {

        // 인덱스 텍스트를 만든다.
        TextView indexText = new TextView(getContext());

        indexText.setText("# " + (index + 1));

        // 인덱스 텍스트에 ProblemRecordText 스타일을 적용한다.
        setProblemRecordTextStyle(indexText, 0.5F);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 문제 기록 표에 인덱스 텍스트를 추가한다.
                recordGrid.addView(indexText);
            }
        });
    }

    // 문제 기록 표에 날짜 텍스트를 추가한다.
    private void addDateText(RecordByProblem recordByProblem, GridLayout recordGrid, int i) {

        // 날짜 텍스트를 만든다.
        TextView dateText = new TextView(getContext());

        dateText.setText(recordByProblem.getRecordedAt().format(
                DateTimeFormatter.ofPattern("yyyy/ MM/ dd")));

        // 날짜 텍스트에 @style/ProblemRecordText 스타일을 적용한다.
        setProblemRecordTextStyle(dateText, 1F);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 문제 기록 표에 날짜 텍스트를 추가한다.
                recordGrid.addView(dateText);
            }
        });
    }

    // 문제 기록 표에 기록 텍스트를 추가한다.
    private void addRecordText(RecordByProblem recordByProblem, GridLayout recordGrid) {

        // 기록 텍스트를 만든다.
        TextView recordText = new TextView(getContext());

        recordText.setText(TimerService.convertToTimestamp((long) (recordByProblem.getTimeRecord() * 1000)));

        // 기록 텍스트에 기록 텍스트 스타일을 적용한다.
        setProblemRecordTextStyle(recordText, 1F);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 문제 기록 표에 기록 텍스트를 추가한다.
                recordGrid.addView(recordText);
            }
        });
    }

    // 문제 기록 표에 채점 텍스트를 추가한다.
    private void addIsSolvedText(RecordByProblem recordByProblem, GridLayout recordGrid) {

        // 채점 텍스트를 만든다.
        TextView isSolvedText = new TextView(getContext());

        // 문제 기록의 채점 결과에 따라 텍스트를 설정한다.
        if (recordByProblem.getIsSolved() != null) {

            switch (recordByProblem.getIsSolved()) {

                case 0:
                    isSolvedText.setText("⭕");
                    break;
                case 1:
                    isSolvedText.setText("❌");
                    break;
                case 2:
                    isSolvedText.setText("⭐");
            }
        } else {
            isSolvedText.setText("-");
        }

        // 채점 텍스트에 채점 텍스트 스타일을 적용한다.
        setProblemRecordTextStyle(isSolvedText, 0.5F);

        handler.post(new Runnable() {

            @Override
            public void run() {

                // 문제 기록 표에 채점 텍스트를 추가한다.
                recordGrid.addView(isSolvedText);
            }
        });
    }

    // textView에 @style/ProblemRecordText 스타일을 적용한다.
    private void setProblemRecordTextStyle(TextView textView, float columnWeight) {

        GridLayout.Spec rowSpan = GridLayout.spec(GridLayout.UNDEFINED);                  // width
        GridLayout.Spec columnSpan = GridLayout.spec(GridLayout.UNDEFINED, columnWeight); // height
        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams(rowSpan, columnSpan);
        textView.setLayoutParams(gridParams);

        textView.setTextColor(Color.BLACK);                                               // textColor
        textView.setTextSize(12);                                                         // textSize
        textView.setGravity(Gravity.CENTER);                                              // gravity
    }

    // 문제 탭의 타이머 버튼을 클릭하면
    // -> 타이머가 작동하고 멈추게 한다.
    private void setOnClickListenerToStartAndStopBtn(RecyclerView.ViewHolder holder, int position) {

        // 문제로부터 타이머를 얻는다.
        Problem item = problemAdapter.getItem(position);
        TimerService timerService = item.getTimerService();

        // 얻은 타이머에 TimerInterface 구현 객체를 설정한다.
        TimerInterface timerInterface = getTimerInterfaceImpl(holder.itemView, position);
        timerService.setTimerInterface(timerInterface);

        Button startBtn = (Button) holder.itemView.findViewById(R.id.btn_start);

        startBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 문제를 채점해달라는 토스트를 띄운다.
                Toast.makeText(getActivity(),
                        problemAdapter.getItem(position).getNumber() + "번 문제를 채점해주세요.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        Button stopBtn = (Button) holder.itemView.findViewById(R.id.btn_stop);

        stopBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 타이머를 멈춘다.
                item.stopTimer();
            }
        });
    }

    // 채점 모드를 해제한다.
    private void setGradeModeOff(RecyclerView.ViewHolder holder, int position) {

        // 문제 기록 탭을 닫는다.
        LinearLayout recordContainer = (LinearLayout) holder.itemView.findViewById(R.id.container_record);
        recordContainer.setVisibility(View.GONE);

        // 채점 모드를 해제한다.
        setGradeMode(holder.itemView, position, false);
    }

    // 문제 리싸이클러에 문제 어댑터를 설정한다.
    private void setProblemAdapterToProblemRecycler() {

        problemRecycler.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));

        // 문제 리싸이클러에 문제 어댑터를 설정한다.
        problemRecycler.setAdapter(problemAdapter);
    }

    // 문제 스와이프를 설정한다.
    private void setProblemSwipeRefresh() {

        problemSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                // 교재 어댑터를 새로고침한다.
                bookAdapter.notifyDataSetChanged();

                // 문제 어댑터를 새로고침한다.
                problemAdapter.notifyDataSetChanged();

                // 새로고침을 끝낸다.
                problemSwipeRefresh.setRefreshing(false);
            }
        });
    }

    // 채점 모드 스위치를 설정한다.
    private void setGradeModeSwitch() {

        gradeModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                // 새로고침 기능을 사용할 수 있거나 없게 한다.
                problemSwipeRefresh.setEnabled(!b);


                // 채점 모드 스위치 옆의 텍스트를 설정한다.
                if (b) {

                    gradeModeOnText.setTextColor(Color.parseColor("#77000000"));
                    gradeModeOffText.setVisibility(View.VISIBLE);
                }
                else {

                    gradeModeOnText.setTextColor(Color.BLACK);
                    gradeModeOffText.setVisibility(View.GONE);
                }

                for (int i = 0; i < problemRecycler.getChildCount(); i++) {

                    // 학생 탭을 얻는다.
                    View itemView = (View) problemRecycler.getChildAt(i);

                    // 문제 기록 탭을 닫는다.
                    LinearLayout recordContainer = (LinearLayout) itemView.findViewById(R.id.container_record);
                    recordContainer.setVisibility(View.GONE);

                    // 채점 모드를 설정한다.
                    setGradeMode(itemView, i, b);
                }

                // 채점 모드가 해제되면 새로고침한다.
                refreshAfterGradeModeIsTurnedOff(b);
            }
        });
    }

    // 채점 모드가 해제되면 새로고침한다.
    private void refreshAfterGradeModeIsTurnedOff(boolean isGradeModeOn) {

        // 채점 모드가 해제되면
        if (!isGradeModeOn) {

            threadPool.execute(new Runnable() {

                private LinearLayout answerContainer;

                @Override
                public void run() {

                    // 문제 탭이 하나라도 있으면
                    if (problemRecycler.getChildCount() != 0) {

                        // 첫째 탭을 얻는다.
                        View firstItemView = (View) problemRecycler.getChildAt(0);

                        // 정답 탭을 얻는다.
                        answerContainer = (LinearLayout) firstItemView.findViewById(R.id.container_answer);

                        // 정답 탭이 0.00n초 안에 보이지 않게 되었다면
                        if (isAnswerContainerGoneInNms(500L)) {

                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    problemAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }

                // 정답 탭이 보이지 않는 되었는지 0.00n초간 확인한다.
                private boolean isAnswerContainerGoneInNms(long n) {

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

                        // 정답 탭이 보이지 않게 되어 있으면
                        if (answerContainer.getVisibility() == View.GONE) return true;
                    }

                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    return false;
                }
            });
        }
    }

    // TimerInterface 구현 객체를 반환한다.
    public TimerInterface getTimerInterfaceImpl(View itemView, int position) {

        // holder으로부터 변수를 초기화한다.
        TextView time = (TextView) itemView.findViewById(R.id.text_time);
        Button startBtn = (Button) itemView.findViewById(R.id.btn_start);
        Button stopBtn = (Button) itemView.findViewById(R.id.btn_stop);

        // timerInterface를 생성한다.
        TimerInterface timerInterface = new TimerInterface() {

            @Override
            public void sleep(long milliSecond) {

                // Thread를 생성하여 0.1초동안 대기한다.
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // handler를 통해 time에 시간을 표시한다.
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        time.setText(TimerService.convertToTimestamp(milliSecond));
                    }
                });
            }

            @Override
            public void changeRes(int status, int pushed) {

                // 처음 시작 버튼을 눌렀을 때
                if (status == TimerService.INIT && pushed == TimerService.START) {

                    startBtn.setText("멈춤");
                    stopBtn.setText("기록");
                    stopBtn.setEnabled(true);
                    stopBtn.setBackgroundResource(R.drawable.shape_btn_soft_warning);
                    stopBtn.setTextColor(Color.BLACK);

                    gradeModeSwitch.setEnabled(false);
                    gradeModeSwitch.setTrackResource(R.drawable.selector_switch_gray);
                    gradeModeOnText.setTextColor(Color.parseColor("#99000000"));
                }

                // 타이머 작동 중에 일시정지 버튼을 눌렀을 때
                if (status == TimerService.RUN && pushed == TimerService.START) {

                    startBtn.setText("시작");
                    stopBtn.setText("리셋");
                }

                // 타이머가 일시정지된 상태에서 시작 버튼을 눌렀을 때
                if (status == TimerService.PAUSE && pushed == TimerService.START) {

                    startBtn.setText("멈춤");
                    stopBtn.setText("기록");
                }

                // 타이머가 작동 중에 기록하기 버튼을 눌렀을 때
                if (status == TimerService.RUN && pushed == TimerService.STOP) {

                    Toast.makeText(getActivity(), "기록되었습니다.", Toast.LENGTH_SHORT).show();

                    startBtn.setText("시작");
                    startBtn.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
                    startBtn.setTextColor(Color.parseColor("#99000000"));

                    // startBtn을 클릭하면 채점을 요청하는 토스트를 띄운다.
                    startBtn.setOnClickListener(null);
                    startBtn.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getActivity(),
                                    problemAdapter.getItem(position).getNumber() + "번 문제를 채점해주세요.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    stopBtn.setText("리셋");
                    stopBtn.setEnabled(false);
                    stopBtn.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
                    stopBtn.setTextColor(Color.parseColor("#99000000"));

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            time.setText("00:00:00");
                        }
                    }, 100L);

                    // 문제 기록 표를 얻는다.
                    GridLayout recordGrid = itemView.findViewById(R.id.grid_record);

                    // 문제 기록 표에 문제 기록을 추가한다.
                    addViewsToRecordGrid(recordGrid, position);

                    gradeModeSwitch.setEnabled(true);
                    gradeModeSwitch.setTrackResource(R.drawable.selector_switch);
                    gradeModeOnText.setTextColor(Color.BLACK);
                }

                // 타이머가 일시정지된 상태에서 리셋 버튼을 눌렀을 때
                if (status == TimerService.PAUSE && pushed == TimerService.STOP) {

                    startBtn.setText("시작");
                    stopBtn.setText("리셋");
                    stopBtn.setEnabled(false);
                    stopBtn.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
                    stopBtn.setTextColor(Color.parseColor("#99000000"));
                    time.setText("00:00:00");

                    gradeModeSwitch.setEnabled(true);
                    gradeModeSwitch.setTrackResource(R.drawable.selector_switch);
                    gradeModeOnText.setTextColor(Color.BLACK);
                }
            }

            @Override
            public void saveTimeRecord(long millisecond) {

                Call<RecordByProblem> recordByProblemCall = timerApi.createTimeRecord(new RecordByProblem(
                        null,
                        student.getId(),
                        problemAdapter.getItem(position).getId(),
                        (float) millisecond / 1000F,
                        null,
                        null));

                recordByProblemCall.enqueue(new Callback<RecordByProblem>() {

                    @Override
                    public void onResponse(Call<RecordByProblem> call, Response<RecordByProblem> response) {

                        if (!response.isSuccessful()) {

                            Log.d("error", "code : " + response.code());
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<RecordByProblem> call, Throwable t) {
                        Log.d("error", t.getMessage());
                    }
                });
            }
        };

        return timerInterface;
    }

    // 해당 문제로 이동한다.
    public void moveToProblem(Problem problem) {

        threadPool.execute(new Runnable() {

            private Integer indexOfProblem;

            @Override
            public void run() {

                // 얻은 문제가 교재 탭의 몇째 교재에 속해있는지를 얻는다.
                initializeIndexOfProblem(problem);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // index가 0.2초 이내에 초기화되지 않으면
                        if (isIndexOfProblemNullForNms(200L)) {

                            Log.d("error", "failed to initialize : index");
                            return;
                        }

                        // problemToSolve를 업데이트한다.
                        problemToSolve = problem;

                        // 얻은 교재에 해당하는 텍스트를 클릭한다.
                        clickBookNameText(indexOfProblem);

                        // 얻은 문제의 페이지를 페이지 에딧에 입력한다.
                        enterPageByProblem(problem);

                        // 얻은 문제에 해당하는 문제로 스크롤한다.
                        // scrollToPositionByProblem(problem);
                    }
                });
            }

            // 얻은 교재가 교재 탭의 몇째 교재인지를 얻는다.
            private void initializeIndexOfProblem(Problem problem) {

                threadPool.execute(new Runnable() {

                    private List<Book> books;

                    @Override
                    public void run() {

                        // 해당 학생이 추가했던 교재 리스트를 얻는다.
                        initializeBooks();

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {

                                // 교재 리스트가 0.2초 이내에 초기화되지 않았다면
                                if (isBooksNullForNms(200L)) {

                                    Log.d("error", "failed to initialize : books");
                                    return;
                                }

                                for (Book book : books) {

                                    if (book.getId().equals(problem.getBookId())) {

                                        indexOfProblem = books.indexOf(book);
                                    }
                                }
                            }
                        });
                    }

                    // 해당 학생이 추가했던 교재 리스트를 얻는다.
                    private void initializeBooks() {

                        Call<List<Book>> call = timerApi.list(student.getId());

                        call.enqueue(new Callback<List<Book>>() {

                            @Override
                            public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {

                                if (!response.isSuccessful()) {

                                    Log.d("error", "\ncode : " + response.code());
                                    return;
                                }

                                books = response.body();
                            }

                            @Override
                            public void onFailure(Call<List<Book>> call, Throwable t) {
                                Log.d("error", "\n" + t.getMessage());
                            }
                        });
                    }

                    // 교재 리스트가 초기화되었는지 0.00n초간 확인한다.
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

                        return true;
                    }
                });
            }

            // 인덱스가 null인지 0.00n초간 확인한다.
            private boolean isIndexOfProblemNullForNms(long n) {

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

                    if (indexOfProblem == null) continue;

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

    // 얻은 교재에 해당하는 텍스트를 클릭한다.
    private void clickBookNameText(Integer index) {

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 교재 리싸이클러에 0.2초 이내에 교재 버튼이 추가되지 않는다면
                if (IsChildNotAddedToBookRecyclerForNms(200L)) {

                   Log.d("error", "failed to add views to : bookRecycler");
                   return;
                }

                // 얻은 교재에 해당하는 교재 버튼을 얻는다.
                LinearLayout bookNameTextContainer = (LinearLayout) (bookRecycler.getChildAt(index));
                TextView bookNameText = (TextView) bookNameTextContainer.getChildAt(0);

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {

                        // 교재 버튼에 리스너가 설정될 떄까지 0.2초간 대기한다.
                        if (isListenerNotSetToBookNameTextForNms(200L)) {

                            Log.d("error", "failed to set OnClickListener to : bookNameText");
                            return;
                        }

                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                bookNameText.performClick();
                            }
                        });
                    }

                    // 교재 버튼에 리스너가 설정될 떄까지 0.00n초간 대기한다.
                    private boolean isListenerNotSetToBookNameTextForNms(long n) {

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

                            if (!bookNameText.hasOnClickListeners()) continue;

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

            // 교재 리싸이클러에 교재 버튼이 추가되었는지 0.00n초간 확인한다.
            private boolean IsChildNotAddedToBookRecyclerForNms(long n) {

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

                    if (bookRecycler.getChildCount() == 0) continue;

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

    // 얻은 문제의 페이지를 페이지 에딧에 입력한다.
    private void enterPageByProblem(Problem problem) {

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 교재 버튼이 0.2초 이내에 클릭되지 않으면
                if (isClickedBookNullForNms(200L)) {

                    Log.d("error", "failed to initialize : clickedBook");
                    return;
                }

                // 바로 클릭하면 페이지 에딧이 초기화되는 오류가 발생해서 작업을 0.1초 지연시킨다.
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {

                        bookPageEdit.setText(String.valueOf(problem.getPage()));

                        bookPageEdit.onEditorAction(EditorInfo.IME_ACTION_DONE);

                        // 키보드를 보이지 않게 한다.
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(bookPageEdit.getWindowToken(), 0);
                    }
                }, 100L);
            }
        });
    }

    // 교재 버튼이 클릭되었는지 0.00n초간 확인한다.
    private boolean isClickedBookNullForNms(long n) {

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

            if (clickedBook == null) continue;

            return false;
        }

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    // 얻은 문제에 해당하는 문제로 스크롤한다.
    private void scrollToPositionByProblem(Problem problem) {

        // 얻은 문제가 해당 페이지 안에서 몇 째인지를 얻는다.
        Integer[] index = new Integer[1];

        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                // 페이지 에딧에 페이지가 0.2초 이내에 입력되지 않으면
                if (isBookPageEditNullForNms(200L)) {

                    Log.d("error", "bookPageEdit.getText() : null");
                    return;
                }

                // 문제 어댑터에 아이템이 0.2초 이내에 추가되지 않았다면
                if (isItemsEmptyForNms(200L)) {

                    Log.d("error", "problemAdapter.getItems().isEmpty() == true");
                    return;
                }

                List<Problem> problems = problemAdapter.getItems();

                for (Problem p : problems) {

                    if (problem.getId().equals(p.getId())) {
                        index[0] = problems.indexOf(p);
                    }
                }

                // 해당 문제로 스크롤한다.
                LinearLayoutManager linearLayoutManager =
                        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                linearLayoutManager.scrollToPosition(index[0] - 1);

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        problemRecycler.setLayoutManager(linearLayoutManager);
                    }
                });
            }

            // 페이지 에딧에 페이지가 입력되었는지 0.00n초간 확인한다.
            private boolean isBookPageEditNullForNms(long n) {

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

                    if (String.valueOf(bookPageEdit.getText()) == null) continue;

                    return false;
                }

                try {
                    Thread.sleep(15L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // 문제 어댑터에 아이템이 추가되었는지 0.00n초간 확인한다.
            private boolean isItemsEmptyForNms(long n) {

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

                    if (problemAdapter.getItems().isEmpty()) continue;

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