package kr.co.problemtimertest.api;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Student;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TeacherAssignmentApi {

    // 특정 선생님이 할당한 과제 리스트를 얻는다.
    // -> 과제 탭의 범위, 기한을 얻는다.
    @GET("/teacher/{id}/assignments")
    Call<List<Assignment>> listAssignments(@Path("id") Long fromTeacherId);

    /*
    StatisticsApi
    // 특정 아이디에 해당하는 학생 엔티티를 얻는다.
    // -> 과제 탭의 대상을 얻는다.
    @GET("/student/{id}")
    Call<StudentDto> read(@Path("id") Long fromStudentId);
    */

    // 특정 bookId에 해당하는 교재를 얻는다.
    // -> 과제 탭의 교재를 얻는다.
    @GET("/book/{id}")
    Call<Book> read(@Path("id") Long fromBookId);

    // 특정 학생이 특정 과제를 하는데 걸린 시간을 얻는다.
    // -> 과제 기록 탭의 걸린 시간을 얻는다.
    @GET("/study-time")
    Call<Float> read(
            @Query("studentId") Long ofStudentId,
            @Query("assignmentId") Long ofAssignmentId);

    // 특정 학생의 특정 과제에 대한 진행률을 계산한다.
    // -> 과제 기록 탭의 진행률을 얻는다.
    @GET("/student/{studentId}/assignment/{assignmentId}/progress")
    Call<Float> count(
            @Path("studentId") Long ofStudentId,
            @Path("assignmentId") Long ofAssignmentId);

    // 특정 assignmentId에 해당하는 과제를 가져온다.
    // -> 과제를 수정할 때 과제 출제 대화상자에 채울 기존의 과제 내용을 얻는다.
    @GET("/assignment/{id}")
    Call<Assignment> readAssignment(
            @Path("id") Long fromAssignmentId);

    /*
    // 특정 선생님이 담당하는 수업 리스트를 얻는다.
    // -> 과제를 출제할 때 실행되고, 수업 리스트를 얻는다.
    ManagementController
    @GET("/teacher/{id}/classrooms")
    Call<List<Classroom>> list(@Path("id") Long fromTeacherId);
    */

    // 특정 선생님이 담당하는 학생 리스트를 얻는다.
    // -> 과제를 출제할 때 실행되고, 학년 리스트, 학생 이름 리스트를 얻는다.
    @GET("/teacher/{id}/students")
    Call<List<Student>> listStudents(@Path("id") Long fromTeacherId);

    // 특정 선생님의 특정 수업을 듣는 학생 리스트를 얻는다.
    // -> 과제 만들기 화면에서 특정 반 버튼을 클릭하면 실행된다.
    @GET("/classroom/{name}/teacher/{id}/students")
    Call<List<Student>> list(
            @Path("name") String fromClassroomName,
            @Path("id") Long fromTeacherId);

    /*
    // TimerController
    // 특정 학생이 추가했던 교재 리스트를 얻는다.
    // -> 과제를 출제할 때 교재 리스트를 얻는다.
    @GET("/student/{id}/books")
    Call<List<Book>> list(@Query("studentId") Long fromStudentId)
    */

    // 특정 assignmentId에 해당하는 과제를 수정한다.
    @PATCH("/assignment/{id}")
    Call<Assignment> update(
            @Path("id") Long fromAssignmentId,
            @Body Assignment assignment);

    // 특정 id에 해당하는 과제를 삭제한다.
    @DELETE("/assignment/{id}")
    Call<Assignment> delete(
            @Path("id") Long fromAssignmentId);
}
