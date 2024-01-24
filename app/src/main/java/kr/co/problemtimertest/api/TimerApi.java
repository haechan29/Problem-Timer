package kr.co.problemtimertest.api;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.RecordByProblem;
import kr.co.problemtimertest.model.RecordStatistics;
import kr.co.problemtimertest.model.ScoreByUnit2;
import kr.co.problemtimertest.model.StudentBook;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TimerApi {

    // 특정 교재의 첫 페이지에 있는 Problem 리스트를 얻는다.
    // -> 특정 교재를 선택했을 때 자동으로 첫 페이지의 문제 리스트를 가져오는 기능
    @GET("/problemsInFirstPage")
    Call<List<Problem>> listProblemsInFirstPage(
            @Query("fromBookId") Long fromBookId);

    // 특정 교재의 특정 페이지에 있는 Problem 리스트를 얻는다.
    // -> 페이지 탭에 페이지를 입력하면 해당 페이지에 있는 문제 리스트를 가져오는 기능
    @GET("/problems")
    Call<List<Problem>> list(
            @Query("fromBookId") Long fromBookId,
            @Query("fromPage") Integer fromPage);

    // 특정 학생이 특정 문제를 푼 기록 리스트를 얻는다.
    // -> 타이머 탭을 클릭했을 때 해당 학생의 문제 기록 리스트를 가져오는 기능
    @GET("/problem/records-by-problem")
    Call<List<RecordByProblem>> list(
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromProblemId") Long fromProblemId);

    // 특정 학생이 추가했던 교재 리스트를 얻는다.
    // -> 교재 탭에 교재를 보여주는 기능
    @GET("/student/{id}/books")
    Call<List<Book>> list(@Path("id") Long fromStudentId);

    // 특정 학년, 특정 과목에 해당하는 교재 리스트를 얻는다. 특정 학생의 교재 리스트에 들어있는 교재는 제외한다.
    // -> 교재를 추가할 때 학년과 과목을 선택하면 선택 가능한 교재를 보여주는 기능
    @GET("/books-selectable")
    Call<List<Book>> list(
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromSchool") Integer fromSchool,
            @Query("fromSubject") Integer fromSubject);

    // 특정 학년, 특정 과목에 해당하는 교재 리스트 중에서 특정 학생이 추가한 적이 있는 교재를 얻는다.
    // -> 교재를 추가할 때 학년과 과목을 선택하면 선택 불가능한 교재를 보여주는 기능
    @GET("/books-unselectable")
    Call<List<Book>> listBooksUnselectable(
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromSchool") Integer fromSchool,
            @Query("fromSubject") Integer fromSubject);

    // 특정 학생이 특정 문제를 푼 기록을 저장한다.
    // -> 타이머 탭의 기록하기 버튼을 누르면 실행된다.
    @POST("/record-by-problem/time-record")
    Call<RecordByProblem> createTimeRecord(@Body RecordByProblem recordByProblem);

    // 특정 학생이 특정 문제를 푼 채점 기록을 저장한다.
    // -> 타이머 탭의 채점하기 버튼을 누르면 실행된다.
    @POST("/record-by-problem/is-solved")
    Call<RecordByProblem> create(@Body RecordByProblem recordByProblem);

    // 특정 학생이 추가한 특정 교재를 저장한다.
    // -> 해당 학생이 교재 추가하기 탭에서 해당 교재를 선택하면 실행된다.
    @POST("/book")
    Call<StudentBook> create(
            @Query("studentId") Long studentId,
            @Query("bookId") Long bookId,
            @Query("addedAt") LocalDateTime addedAt);

    // 특정 문제의 통계를 생성하거나 갱신한다.
    // -> 타이머 탭의 채점하기 버튼을 누르면 실행된다.
    @PATCH("/record-statistics")
    Call<RecordStatistics> update(@Body RecordByProblem recordByProblem);

    // 특정 학생이 특정 문제를 풀었을 때 해당 중단원별 점수를 생성하거나 갱신한다.
    // -> 타이머 탭의 채점하기 버튼을 누르면 실행된다.
    @PATCH("/scoreByProblem")
    Call<Float> calculate(
            @Query("ofTimeRecord") Float ofTimeRecord,
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromProblemId") Long fromProblemId);

    // 특정 학생의 특정 교재를 삭제한다.
    // -> 교재 탭에서 교재를 길게 클릭하면 실행된다.
    @DELETE("/book")
    Call<StudentBook> delete(
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromBookId") Long fromBookId);
}