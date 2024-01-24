package kr.co.problemtimertest.api;

import java.util.List;

import kr.co.problemtimertest.model.Classroom;
import kr.co.problemtimertest.model.School;
import kr.co.problemtimertest.model.Student;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ManagementApi {

    // 특정 선생님이 담당하는 수업 리스트를 얻는다.
    @GET("/teacher/{id}/classrooms")
    Call<List<Classroom>> list(@Path("id") Long fromTeacherId);

    // 학생 아이디 리스트를 이용해 학생 리스트를 얻는다.
    @GET("/students")
    Call<List<Student>> list(
            @Query("ids") List<Long> fromStudentIds);

    // 학교 이름을 이용하여 학교 리스트를 얻는다.
    // -> 강사가 학생을 추가할 때 학생을 검색하는 데에 사용한다.
    @GET("/schools")
    Call<List<School>> listSchools(
            @Query("name") String fromName);

    // 학생 이름을 이용해 학생 리스트를 얻는다.
    @GET("/students/name/{name}")
    Call<List<Student>> list(
            @Path("name") String fromName);

    // 수업 엔티티를 저장한다.
    // -> 특정 학생을 특정 강사의 학생으로 추가한다.
    @POST("/classroom")
    Call<Classroom> create(
            @Body Classroom classroom);

    // 수업을 삭제한다.
    // -> 특정 학생을 특정 강사의 학생 리스트에서 삭제한다.
    @DELETE("/classroom/{name}/teacher/{teacherId}/student/{studentId}")
    Call<Classroom> delete(
            @Path(value = "name") String name,
            @Path(value = "teacherId") Long teacherId,
            @Path(value = "studentId") Long studentId);
}
