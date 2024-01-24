package kr.co.problemtimertest.api;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.problemtimertest.model.Academy;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.RecordByProblem;
import kr.co.problemtimertest.model.RecordStatistics;
import kr.co.problemtimertest.model.School;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.model.StudentBook;
import kr.co.problemtimertest.model.Teacher;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LoginApi {

    // 학생을 얻는다.
    // -> 회원가입 시에 특정 이메일을 가진 회원이 존재하는지 확인한다.
    @GET("/student")
    Call<Student> read(@Query("email") String email);

    // 학교를 저장한다.
    @POST("/school")
    Call<School> create(@Body School school);

    // 학생을 저장한다.
    @POST("/student")
    Call<Student> create(@Body Student student);

    // 강사를 얻는다.
    // -> 회원가입 시에 특정 이메일을 가진 강사가 존재하는지 확인한다.
    @GET("/teacher")
    Call<Teacher> readTeacher(@Query("email") String email);

    // 학원을 저장한다.
    @POST("/academy")
    Call<Academy> create(@Body Academy academy);

    // 강사를 저장한다.
    @POST("/teacher")
    Call<Teacher> create(@Body Teacher teacher);
}