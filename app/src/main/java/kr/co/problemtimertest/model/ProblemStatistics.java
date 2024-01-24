package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class ProblemStatistics {

    // 변수
    @SerializedName("problem_number")
    private String problemNumber;

    @SerializedName("is_solved")
    private Integer isSolved;

    @SerializedName("correct_answer_rate_avg")
    private Float correctAnswerRateAvg;

    private Float score;

    @SerializedName("student_id")
    private Long studentId;

    @SerializedName("problem_id")
    private Long problemId;

    // 생성자
    public ProblemStatistics(String problemNumber, Integer isSolved, Float correctAnswerRateAvg,
                             Float score, Long studentId, Long problemId) {
        this.problemNumber = problemNumber;
        this.isSolved = isSolved;
        this.correctAnswerRateAvg = correctAnswerRateAvg;
        this.score = score;
        this.studentId = studentId;
        this.problemId = problemId;
    }

    @Override
    public String toString() {
        return "ProblemStatistics{" +
                "problemNumber='" + problemNumber + '\'' +
                ", isSolved=" + isSolved +
                ", correctAnswerRateAvg=" + correctAnswerRateAvg +
                ", score=" + score +
                ", studentId=" + studentId +
                ", problemId=" + problemId +
                '}';
    }

    // 게터
    public String getProblemNumber() {
        return problemNumber;
    }

    public Integer getIsSolved() {
        return isSolved;
    }

    public Float getCorrectAnswerRateAvg() {
        return correctAnswerRateAvg;
    }

    public Float getScore() {
        return score;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getProblemId() {
        return problemId;
    }
}
