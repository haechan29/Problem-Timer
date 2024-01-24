package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;

public class RecordByProblem {

    // 변수
    private Long id;

    @SerializedName("student_id")
    private Long studentId;

    @SerializedName("problem_id")
    private Long problemId;

    @SerializedName("time_record")
    private Float timeRecord;

    @SerializedName("recorded_at")
    private LocalDate recordedAt;

    @SerializedName("is_solved")
    private Integer isSolved;

    // 생성자
    public RecordByProblem(Long id, Long studentId, Long problemId, Float timeRecord, LocalDate recordedAt, Integer isSolved) {
        this.id = id;
        this.studentId = studentId;
        this.problemId = problemId;
        this.timeRecord = timeRecord;
        this.recordedAt = recordedAt;
        this.isSolved = isSolved;
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public Float getTimeRecord() {
        return timeRecord;
    }

    public LocalDate getRecordedAt() {
        return recordedAt;
    }

    public Integer getIsSolved() {
        return isSolved;
    }

    // toString()
    @Override
    public String toString() {
        return "RecordByProblem{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", problemId=" + problemId +
                ", timeRecord=" + timeRecord +
                ", recordedAt=" + recordedAt +
                ", isSolved=" + isSolved +
                '}';
    }
}
