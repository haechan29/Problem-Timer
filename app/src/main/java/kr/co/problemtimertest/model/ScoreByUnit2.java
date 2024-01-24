package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class ScoreByUnit2 {

    // 변수
    private Long id;
    private Float score;

    @SerializedName("student_id")
    private Long studentId;

    @SerializedName("book_unit_2_id")
    private Long bookUnit2Id;

    // 생성자
    public ScoreByUnit2(Long id, Float score, Long studentId, Long bookUnit2Id) {

        this.id = id;
        this.score = score;
        this.studentId = studentId;
        this.bookUnit2Id = bookUnit2Id;
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Float getScore() {
        return score;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getBookUnit2Id() {
        return bookUnit2Id;
    }

    // toString()
    @Override
    public String toString() {
        return "ScoreByUnit2{" +
                "id=" + id +
                ", score=" + score +
                ", studentId=" + studentId +
                ", bookUnit2Id=" + bookUnit2Id +
                '}';
    }
}


