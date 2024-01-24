package kr.co.problemtimertest.api;

import java.util.List;

import kr.co.problemtimertest.model.Academy;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Book;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.model.Student;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StudentAssignmentApi {

    // 특정 학생에게 할당된 과제 리스트를 얻는다.
    // -> 과제 탭의 범위, 기한을 얻는다.
    @GET("/student/{id}/assignments")
    Call<List<Assignment>> listAssignments(@Path("id") Long fromStudentId);

    // 특정 선생님이 소속된 학원을 얻는다.
    // -> 과제 탭의 학원을 얻는다.
    @GET("/teacher/{id}/academy")
    Call<Academy> readAcademy(@Path("id") Long fromTeacherId);

    /*
    // AssignmentTeacherController
    // 특정 bookId에 해당하는 교재를 얻는다.
    // -> 과제 탭의 교재 이름을 얻는다.
    @GET("/book/{id}")
    Call<Book> read(@Path("id") Long fromBookId);
    */

    /*
    // AssignmentTeacherController
    // 특정 학생의 특정 과제에 대한 진행률을 계산한다.
    // -> 과제 탭의 진행률을 얻는다.
    @GET("/student/{studentId}/assignment/{assignmentId}/progress")
    Call<Float> count(
            @Path("studentId") Long ofStudentId,
            @Path("assignmentId") Long ofAssignmentId);
    */

    // 특정 학생이 특정 과제 범위 안의 문제들 중에서 풀지 않은 첫 문제를 얻는다.
    // -> 과제 탭을 클릭하면 문제 풀기 페이지의 해당 문제로 이동한다.
    @GET("/student/{studentId}/assignment/{assignmentId}/problem-to-solve")
    Call<Problem> read(
            @Path("studentId") Long fromStudentId,
            @Path("assignmentId") Long fromAssignmentId);
}
