package kr.co.problemtimertest.impl;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.adapter.BookAdapter;
import kr.co.problemtimertest.api.ManagementApi;
import kr.co.problemtimertest.api.StatisticsApi;
import kr.co.problemtimertest.api.StudentAssignmentApi;
import kr.co.problemtimertest.api.TeacherAssignmentApi;
import kr.co.problemtimertest.api.TimerApi;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.StudentBook;
import kr.co.problemtimertest.service.RetrofitService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AddBookDialogUtil {

    private float dp;

    private Student student;
    private BookAdapter bookAdapter;

    private Dialog addBookDialog;

    private Button radioElementaryBtn;
    private Button radioMiddleBtn;
    private Button radioHighBtn;

    private LinearLayout cancelContainer, addContainer;
    private TextView cancelText, addText;

    private RadioGroup bookSchoolRadioGroup;
    private RadioGroup bookElementarySubjectRadioGroup;
    private RadioGroup bookMiddleSubjectRadioGroup;
    private RadioGroup bookHighSubjectRadioGroup;
    private RadioGroup bookNameRadioGroup;

    private ConstraintLayout bookElementarySubjectContainer;
    private ConstraintLayout bookMiddleSubjectContainer;
    private ConstraintLayout bookHighSubjectContainer;
    private ConstraintLayout bookNameContainer;

    private Integer checkedSchool;
    private Integer checkedSubject;

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

    // 과제 추가 대화상자를 반환한다.
    public Dialog getDialog() {
        return addBookDialog;
    }

    public AddBookDialogUtil(Context context, Student student, BookAdapter[] bookAdapter) {

        // 교재 추가 대화상자를 얻는다.
        addBookDialog = getAddBookDialog(context);

        // 변수를 초기화한다.
        initializeVariable(student, bookAdapter);

        // 학교 탭
        setRadioSchoolBtn();

        // checkedSchool, checkedSubject
        setCheckedSchoolAndSubject();

        // 교재 탭
        setBookNameRadioGroup();

        // 취소 버튼
        setCancelBtn();

        // 추가 버튼
        setAddBtn();
    }

    // 교재 추가 대화상자를 반환한다.
    private Dialog getAddBookDialog(Context context) {

        Dialog addBookDialog = new Dialog(context);

        // addBookDialog의 타이틀을 없애고, 리소스를 연결한다.
        addBookDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addBookDialog.setContentView(R.layout.dialog_add_book);

        return addBookDialog;
    }

    // 교재 추가 대화상자 관련 변수를 초기화한다.
    private void initializeVariable(Student student, BookAdapter[] bookAdapter) {

        dp = addBookDialog.getContext().getResources().getDisplayMetrics().density;

        this.student = student;
        this.bookAdapter = bookAdapter[0];

        radioElementaryBtn = (Button) addBookDialog.findViewById(R.id.radio_elementary);
        radioMiddleBtn = (Button) addBookDialog.findViewById(R.id.radio_middle);
        radioHighBtn = (Button) addBookDialog.findViewById(R.id.radio_high);

        cancelContainer = (LinearLayout) addBookDialog.findViewById(R.id.container_cancel);
        addContainer = (LinearLayout) addBookDialog.findViewById(R.id.container_add);

        cancelText = (TextView) addBookDialog.findViewById(R.id.text_cancel);
        addText = (TextView) addBookDialog.findViewById(R.id.text_add);

        bookSchoolRadioGroup = (RadioGroup) addBookDialog.findViewById(R.id.radio_group_book_school);
        bookElementarySubjectRadioGroup = (RadioGroup) addBookDialog.findViewById(R.id.radio_group_book_elementary_subject);
        bookMiddleSubjectRadioGroup = (RadioGroup) addBookDialog.findViewById(R.id.radio_group_book_middle_subject);
        bookHighSubjectRadioGroup = (RadioGroup) addBookDialog.findViewById(R.id.radio_group_book_high_subject);
        bookNameRadioGroup = addBookDialog.findViewById(R.id.radio_group_book_name);

        bookElementarySubjectContainer =
                (ConstraintLayout) addBookDialog.findViewById(R.id.container_book_elementary_subject);
        bookMiddleSubjectContainer =
                (ConstraintLayout) addBookDialog.findViewById(R.id.container_book_middle_subject);
        bookHighSubjectContainer =
                (ConstraintLayout) addBookDialog.findViewById(R.id.container_book_high_subject);
        bookNameContainer = (ConstraintLayout) addBookDialog.findViewById(R.id.container_book_name);
    }

    // 학교 탭의 버튼을 설정한다.
    private void setRadioSchoolBtn() {

        // 학교 탭의 초등 버튼이 클릭되면
        setOnClickListenerToRadioElementaryBtn();

        // 학교 탭의 중등 버튼이 클릭되면
        setOnClickListenerToRadioMiddleBtn();

        // 학교 탭의 고등 버튼이 클릭되면
        setOnClickListenerToRadioHighBtn();
    }

    // 학교 탭의 초등 버튼이 클릭되면
    private void setOnClickListenerToRadioElementaryBtn() {

        radioElementaryBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 초등의 학년 탭이 보이게 하고, 나머지 학년 탭은 보이지 않게 한다.
                bookElementarySubjectContainer.setVisibility(View.VISIBLE);
                bookMiddleSubjectContainer.setVisibility(View.GONE);
                bookHighSubjectContainer.setVisibility(View.GONE);
            }
        });
    }

    // 학교 탭의 중등 버튼이 클릭되면
    private void setOnClickListenerToRadioMiddleBtn() {

        radioMiddleBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 중등의 학년 탭이 보이게 하고, 나머지 학년 탭은 보이지 않게 한다.
                bookElementarySubjectContainer.setVisibility(View.GONE);
                bookMiddleSubjectContainer.setVisibility(View.VISIBLE);
                bookHighSubjectContainer.setVisibility(View.GONE);
            }
        });
    }

    // 학교 탭의 고등 버튼이 클릭되면
    private void setOnClickListenerToRadioHighBtn() {

        radioHighBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 고등의 학년 탭이 보이게 하고, 나머지 학년 탭은 보이지 않게 한다.
                bookElementarySubjectContainer.setVisibility(View.GONE);
                bookMiddleSubjectContainer.setVisibility(View.GONE);
                bookHighSubjectContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    // 선택된 학교와 과목을 설정한다.
    private void setCheckedSchoolAndSubject() {

        // 학교
        setCheckedSchool();

        // 과목
        setCheckedSubject();
    }

    // 선택된 학교를 설정한다.
    private void setCheckedSchool() {

        bookSchoolRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {

                switch (resId) {
                    case R.id.radio_elementary :
                        checkedSchool = 0;
                        break;

                    case R.id.radio_middle :
                        checkedSchool = 1;
                        break;

                    case R.id.radio_high :
                        checkedSchool = 2;
                        break;

                    case -1 :
                        checkedSchool = null;
                }

                checkedSubject = null;

                // 교재 탭을 설정한다.
                setBookTab();
            }
        });
    }

    // 선택된 과목을 설정한다.
    private void setCheckedSubject() {

        bookElementarySubjectRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {

                if (checkedSchool != null) {

                    switch (resId) {

                        case R.id.radio_elementary_1_1 : checkedSubject = 0; break;
                        case R.id.radio_elementary_1_2 : checkedSubject = 1; break;
                        case R.id.radio_elementary_2_1 : checkedSubject = 2; break;
                        case R.id.radio_elementary_2_2 : checkedSubject = 3; break;
                        case R.id.radio_elementary_3_1 : checkedSubject = 4; break;
                        case R.id.radio_elementary_3_2 : checkedSubject = 5; break;
                        case R.id.radio_elementary_4_1 : checkedSubject = 6; break;
                        case R.id.radio_elementary_4_2 : checkedSubject = 7; break;
                        case R.id.radio_elementary_5_1 : checkedSubject = 8; break;
                        case R.id.radio_elementary_5_2 : checkedSubject = 9; break;
                        case R.id.radio_elementary_6_1 : checkedSubject = 10; break;
                        case R.id.radio_elementary_6_2 : checkedSubject = 11; break;
                        case -1 : checkedSubject = null;
                    }
                }

                // 교재 탭을 설정한다.
                setBookTab();
            }
        });

        bookMiddleSubjectRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {

                if (checkedSchool != null) {

                    switch (resId) {

                        case R.id.radio_middle_1_1 : checkedSubject = 0; break;
                        case R.id.radio_middle_1_2 : checkedSubject = 1; break;
                        case R.id.radio_middle_2_1 : checkedSubject = 2; break;
                        case R.id.radio_middle_2_2 : checkedSubject = 3; break;
                        case R.id.radio_middle_3_1 : checkedSubject = 4; break;
                        case R.id.radio_middle_3_2 : checkedSubject = 5; break;
                        case -1 : checkedSubject = null;
                    }
                }

                // 교재 탭을 설정한다.
                setBookTab();
            }
        });

        bookHighSubjectRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {

                if (checkedSchool != null) {

                    switch (resId) {
                        case R.id.radio_high_high_1 : checkedSubject = 0; break;
                        case R.id.radio_high_high_2 : checkedSubject = 1; break;
                        case R.id.radio_high_math_1 : checkedSubject = 2; break;
                        case R.id.radio_high_math_2 : checkedSubject = 3; break;
                        case R.id.radio_high_DnI : checkedSubject = 4; break;
                        case R.id.radio_high_PnS : checkedSubject = 5; break;
                        case R.id.radio_high_Geometry : checkedSubject = 6; break;
                        case -1 : checkedSubject = null;
                    }
                }

                // 교재 탭을 설정한다.
                setBookTab();
            }
        });
    }

    // 교재 탭을 설정한다.
    private void setBookTab() {

        if (checkedSchool != null & checkedSubject != null) {

            // 교재 이름 라디오그룹을 비운다.
            bookNameRadioGroup.removeAllViews();

            threadPool.execute(new Runnable() {

                List<Book> booksSelectable = new ArrayList<>();
                List<Book> booksUnselectable = new ArrayList<>();

                @Override
                public void run() {

                    // 선택 가능한 교재 리스트를 초기화한다.
                    initializeBooksSelectable();

                    // 선택 불가능한 교재 리스트를 초기화한다.
                    initializeBooksUnselectable();

                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {

                            // 선택 가능한 교재 리스트가 null인지 0.2초간 확인한다.
                            if (isBooksSelectableNullForNms(200L)) return;

                            // 선택 불가능한 교재 리스트가 null인지 0.2초간 확인한다.
                            if (isBooksUnselectableNullForNms(200L)) return;

                            handler.post(new Runnable() {

                                @Override
                                public void run() {

                                    for (Book bookSelectable : booksSelectable) {

                                        // 선택 가능한 교재 버튼을 얻는다.
                                        RadioButton bookSelectableRadioBtn = getBookSelectableRadioBtn(bookSelectable);

                                        bookNameRadioGroup.addView(bookSelectableRadioBtn);
                                    }

                                    for (Book bookUnselectable : booksUnselectable) {

                                        RadioButton radioButton = getBookUnselectableRadioBtn(bookUnselectable);

                                        bookNameRadioGroup.addView(radioButton);
                                    }

                                    bookNameContainer.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                }

                // 선택 가능한 교재 리스트를 초기화한다.
                private void initializeBooksSelectable() {

                    Call<List<Book>> callBooksSelectable = timerApi.list(student.getId(), checkedSchool, checkedSubject);

                    callBooksSelectable.enqueue(new Callback<List<Book>>() {

                        @Override
                        public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {

                            if (!response.isSuccessful()) {

                                Log.d("error", "code : " + response.code());
                                return;
                            }

                            booksSelectable = response.body();
                        }

                        @Override
                        public void onFailure(Call<List<Book>> call, Throwable t) {
                            Log.d("error", t.getMessage());
                        }
                    });
                }

                // 선택 불가능한 교재 리스트를 초기화한다.
                private void initializeBooksUnselectable() {

                    Call<List<Book>> callBooksUnselectable = timerApi.listBooksUnselectable(student.getId(), checkedSchool, checkedSubject);

                    callBooksUnselectable.enqueue(new Callback<List<Book>>() {

                        @Override
                        public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {

                            if (!response.isSuccessful()) {

                                Log.d("error", "code : " + response.code());
                                return;
                            }

                            booksUnselectable = response.body();
                        }

                        @Override
                        public void onFailure(Call<List<Book>> call, Throwable t) {
                            Log.d("error", t.getMessage());
                        }
                    });
                }

                // 선택 가능한 교재 리스트가 null인지 0.00n초간 확인한다.
                private boolean isBooksSelectableNullForNms(long n) {

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

                        if (booksSelectable == null) continue;

                        return false;
                    }

                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d("error", "failed to initialize : booksSelectable");
                    return true;
                }

                // 선택 가능한 교재 리스트가 null인지 0.00n초간 확인한다.
                private boolean isBooksUnselectableNullForNms(long n) {

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

                        if (booksUnselectable == null) continue;

                        return false;
                    }

                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d("error", "failed to initialize : booksUnselectable");
                    return true;
                }
            });
        }
    }

    // 선택 가능한 교재 버튼을 반환한다.
    private RadioButton getBookSelectableRadioBtn(Book bookSelectable) {

        RadioButton bookSelectableRadioBtn = new RadioButton(addBookDialog.getContext());

        bookSelectableRadioBtn.setText(bookSelectable.getName());
        bookSelectableRadioBtn.setTag(bookSelectable);

        // 버튼에 BookSelectableBtn 스타일을 적용한다.
        setBookSelectableBtnStyle(bookSelectableRadioBtn);

        return bookSelectableRadioBtn;
    }

    // 버튼에 BookSelectableBtn 스타일을 적용한다.
    private void setBookSelectableBtnStyle(RadioButton bookSelectableRadioBtn) {

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams( // width, height
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp)); // layout_margin
        bookSelectableRadioBtn.setLayoutParams(layoutParams);

        bookSelectableRadioBtn.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp)); // padding
        bookSelectableRadioBtn.setMinWidth((int) (40 * dp));
        bookSelectableRadioBtn.setTextSize(15);                                            // textSize
        bookSelectableRadioBtn.setTextColor(Color.BLACK);                                  // textColor
        bookSelectableRadioBtn.setGravity(Gravity.CENTER);                                 // gravity
        bookSelectableRadioBtn.setBackgroundResource(R.drawable.selector_radio);           // background
        bookSelectableRadioBtn.setButtonDrawable(0);                                       // button = transparent
        bookSelectableRadioBtn.setElevation((int) (5 * dp));                               // elevation

        // textColor = @drawable/selector_text
        bookSelectableRadioBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (compoundButton.isChecked())
                    compoundButton.setTextColor(Color.WHITE);
                else
                    compoundButton.setTextColor(Color.BLACK);
            }
        });
    }

    // 선택 가능한 교재 버튼을 반환한다.
    private RadioButton getBookUnselectableRadioBtn(Book bookUnselectable) {

        RadioButton bookUnselectableRadioBtn = new RadioButton(addBookDialog.getContext());

        bookUnselectableRadioBtn.setText(bookUnselectable.getName());
        bookUnselectableRadioBtn.setTag(bookUnselectable);

        // 버튼에 BookUnSelectableBtn 스타일을 적용한다.
        setBookUnselectableBtnStyle(bookUnselectableRadioBtn);

        return bookUnselectableRadioBtn;
    }

    // 버튼에 BookSelectableBtn 스타일을 적용한다.
    private void setBookUnselectableBtnStyle(RadioButton bookUnselectableRadioBtn) {

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams( // width, height
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp)); // layout_margin
        bookUnselectableRadioBtn.setLayoutParams(layoutParams);

        bookUnselectableRadioBtn.setPadding((int) (5 * dp), (int) (5 * dp), (int) (5 * dp), (int) (5 * dp)); // padding
        bookUnselectableRadioBtn.setMinWidth((int) (40 * dp));
        bookUnselectableRadioBtn.setTextSize(15);                                            // textSize
        bookUnselectableRadioBtn.setTextColor(Color.parseColor("#99000000"));       // textColor
        bookUnselectableRadioBtn.setGravity(Gravity.CENTER);                                 // gravity
        bookUnselectableRadioBtn.setBackgroundResource(R.drawable.selector_radio);           // background
        bookUnselectableRadioBtn.setButtonDrawable(0);                                       // button = transparent
        bookUnselectableRadioBtn.setElevation(5);                                            // elevation

        bookUnselectableRadioBtn.setEnabled(false);                                        // enabled
    }

    // 교재 라디오그룹을 설정한다.
    private void setBookNameRadioGroup() {

        bookNameRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            // 교재 버튼이 클릭된 경우
            // -> 추가 버튼을 활성화한다.
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {

                setEnabledToAddContainer(true);
            }
        });

        bookNameRadioGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {}

            // 교재 탭이 초기화된 경우
            // -> 추가 버튼을 비활성화한다.
            @Override
            public void onChildViewRemoved(View parent, View child) {

                if (bookNameRadioGroup.getChildCount() == 0) {

                    setEnabledToAddContainer(false);
                }
            }
        });
    }

    // 교재 추가 대화상자의 취소 버튼을 설정한다.
    private void setCancelBtn() {

        cancelContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                addBookDialog.dismiss();

                // 교재 추가 대화상자의 변수를 원래 상태로 되돌린다.
                resetVariable();
            }
        });
    }

    // 교재 추가 대화상자의 변수를 원래 상태로 되돌린다.
    private void resetVariable() {

        bookSchoolRadioGroup.clearCheck();
        bookElementarySubjectRadioGroup.clearCheck();
        bookMiddleSubjectRadioGroup.clearCheck();
        bookHighSubjectRadioGroup.clearCheck();
        bookNameRadioGroup.clearCheck();

        checkedSchool = null;
        checkedSubject = null;

        bookElementarySubjectContainer.setVisibility(View.GONE);
        bookMiddleSubjectContainer.setVisibility(View.GONE);
        bookHighSubjectContainer.setVisibility(View.GONE);
        bookNameContainer.setVisibility(View.GONE);

        setEnabledToAddContainer(false);
    }

    // 교재 추가 대화상자의 추가 버튼을 설정한다.
    private void setAddBtn() {

        addContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // 체크한 교재 버튼을 얻는다.
                RadioButton checkedBtn = bookNameRadioGroup.
                        findViewById(bookNameRadioGroup.getCheckedRadioButtonId());

                // 선택한 교재를 얻는다.
                Book bookSelected = (Book) checkedBtn.getTag();

                // 선택한 교재를 학생의 교재 데이터베이스에 추가한다.
                updateBookSelectedToStudent(bookSelected);

                // 선택한 교재를 교재 어댑터에 추가한다.
                addItemToBookAdapter(bookSelected);

                // 교재 추가 대화상자를 없앤다.
                addBookDialog.dismiss();

                // 교재 추가 대화상자의 변수를 원래 상태로 되돌린다.
                resetVariable();
            }
        });
    }

    // 선택한 교재를 학생의 교재 데이터베이스에 추가한다.
    private void updateBookSelectedToStudent(Book bookSelected) {

        Call<StudentBook> call = timerApi.create(student.getId(), bookSelected.getId(),
                LocalDateTime.parse(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(LocalDateTime.now())));

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

    // 선택한 교재를 교재 어댑터에 추가한다.
    private void addItemToBookAdapter(Book bookSelected) {

        // 얻은 교재 어댑터에 선택한 교재를 추가한다.
        bookAdapter.addItem(bookSelected);

        handler.post(new Runnable() {

            @Override
            public void run() {
                // 교재 어댑터를 새로고침한다.
                bookAdapter.notifyDataSetChanged();
            }
        });
    }

    // 교재 추가 버튼의 enabled를 설정한다.
    public void setEnabledToAddContainer(boolean b) {

        if (b) {

            addText.setTextColor(Color.BLACK);
            addContainer.setBackgroundResource(R.drawable.shape_btn_soft_blue);
            addContainer.setEnabled(true);
        }
        else {

            addText.setTextColor(addBookDialog.getContext().getResources().getColor(R.color.disabled, null));
            addContainer.setBackgroundResource(R.drawable.shape_btn_soft_disabled);
            addContainer.setEnabled(false);
        }
    }
}
