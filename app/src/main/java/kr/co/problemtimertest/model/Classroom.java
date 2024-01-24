package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class Classroom {

    // 변수
    private String name;

    @SerializedName("teacher_id")
    private Long teacherId;

    @SerializedName("student_id")
    private Long studentId;

    // 생성자
    public Classroom(String name, Long teacherId, Long studentId) {
        this.name = name;
        this.teacherId = teacherId;
        this.studentId = studentId;
    }

    // toString()
    @Override
    public String toString() {
        return "Classroom{" +
                "name='" + name + '\'' +
                ", teacherId=" + teacherId +
                ", studentId=" + studentId +
                '}';
    }

    // 게터
    public String getName() {
        return name;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public Long getStudentId() {
        return studentId;
    }
}