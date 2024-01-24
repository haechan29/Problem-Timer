package kr.co.problemtimertest.api;

import java.time.LocalDate;
import java.util.List;

import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.BookUnit1;
import kr.co.problemtimertest.model.BookUnit2;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.ProblemStatistics;
import kr.co.problemtimertest.model.RecordByProblem;
import kr.co.problemtimertest.model.ScoreByUnit2;
import kr.co.problemtimertest.model.ScoreStatistics;
import kr.co.problemtimertest.model.Student;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StatisticsApi {

    // student 테이블에서 학생 아이디를 이용하여 학생의 이름과 학교, 반을 얻는다.
    @GET("/student/{id}")
    Call<Student> read(@Path("id") Long fromStudentId);

    /*
    // student_books 테이블에서 특정 학생의 book 리스트를 얻는다.
    @GET("/student/books")
    Call<List<Book>> list(Query("fromStudentId") Long fromStudentId);
    */

    // 해당 학생의 단원별 점수 리스트를 얻는다.
    @GET("/student/{id}/scores")
    Call<List<ScoreByUnit2>> readScores(
            @Path("id") Long fromStudentId);

    // 해당 학생의 해당 교재의 단원별 점수를 얻고, 그 백분율의 평균을 구한다.
    @GET("/student/{studentId}/book/{bookId}/avgOfPercentages")
    Call<Float> calculate(
            @Path("studentId") Long fromStudentId,
            @Path("bookId") Long fromBookId);

    // 해당 학생의 해당 교재의 단원별 점수 리스트를 얻고, 그 합을 구한다.
    @GET("/student/{studentId}/book/{bookId}/scores")
    Call<Float> calculateScores(
            @Path("studentId") Long fromStudentId,
            @Path("bookId") Long fromBookId);

    // book_units_1 테이블에서 특정 교재의 모든 대단원 엔티티를 얻는다.
    @GET("/book/{bookId}/book-units-1")
    Call<List<BookUnit1>> readBookUnits1(
            @Path("bookId") Long fromBookId);

        // book_units_1 테이블에서 특정 교재의 특정 대단원 엔티티를 얻는다.
    @GET("/book/{bookId}/book-unit-1/{unit1Number}")
    Call<BookUnit1> read(
            @Path("bookId") Long fromBookId,
            @Path("unit1Number") Integer fromUnit1Number);

    // book_units_2 테이블에서 특정 교재의 특정 대단원의 모든 중단원 엔티티를 얻는다.
    @GET("/book/{bookId}/book-unit-1/{unit1Number}/book-units-2")
    Call<List<BookUnit2>> readBookUnits2(
            @Path("bookId") Long fromBookId,
            @Path("unit1Number") Integer fromUnit1Number);

    // book_units_2 테이블에서 특정 교재의 특정 대단원의 특정 중단원 엔티티를 얻는다.
    @GET("/book/{bookId}/book-unit-1/{unit1Number}/book-unit-2/unit2Number")
    Call<BookUnit2> read(
            @Path("bookId") Long fromBookId,
            @Path("unit1Number") Integer fromUnit1Number,
            @Query("unit2Number") Integer fromUnit2Number);

    // 특정 학생의 특정 중단원 점수를 얻는다.
    @GET("/scoreByUnit2")
    Call<Float> read(
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromBookUnit2Id") Long fromBookUnit2Id);

    // 특정 중단원 점수의 통계를 얻는다.
    @GET("/scoreStatistics")
    Call<ScoreStatistics> readScoreStatistics(
            @Query("fromBookUnit2Id") Long fromBookUnit2Id);

    // 특정 중단원의 문제 통계 리스트를 얻는다.
    @GET("/problemStatistics")
    Call<List<ProblemStatistics>> readProblemStatistics(
            @Query("fromStudentId") Long fromStudentId,
            @Query("fromBookUnit2Id") Long fromBookUnit2Id);

    // 특정 중단원에 속하는 문제 리스트를 얻는다.
    @GET("/book-unit-2/{id}/problems")
    Call<List<Problem>> list(
            @Path("id") Long fromBookUnit2Id);

    // 특정 학생의 특정 기간동안 공부한 시간을 얻는다.
    @GET("/student/{id}/study-time")
    Call<Float> read(
            @Path("id") Long ofStudentId,
            @Query("from") LocalDate fromDate,
            @Query("to") LocalDate toDate);
}
