package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class RecordStatistics {

    // 변수
    private Long id;

    @SerializedName("time_record_avg")
    private Float timeRecordAvg;

    @SerializedName("time_record_sd")
    private Float timeRecordSd;

    @SerializedName("correct_answer_rate_avg")
    private Float correctAnswerRateAvg;

    @SerializedName("difficulty")
    private Float difficulty;

    @SerializedName("problem_id")
    private Long problemId;

    // 생성자
    public RecordStatistics(
            Long id,
            Float timeRecordAvg,
            Float timeRecordSd,
            Float correctAnswerRateAvg,
            Float difficulty,
            Long problemId) {

        this.id = id;
        this.timeRecordAvg = timeRecordAvg;
        this.timeRecordSd = timeRecordSd;
        this.correctAnswerRateAvg = correctAnswerRateAvg;
        this.difficulty = difficulty;
        this.problemId = problemId;
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Float getTimeRecordAvg() {
        return timeRecordAvg;
    }

    public Float getTimeRecordSd() {
        return timeRecordSd;
    }

    public Float getCorrectAnswerRateAvg() {
        return correctAnswerRateAvg;
    }

    public Float getDifficulty() {
        return difficulty;
    }

    public Long getProblemId() {
        return problemId;
    }

    // toString()
    @Override
    public String toString() {
        return "RecordStatistics{" +
                "id=" + id +
                ", timeRecordAvg=" + timeRecordAvg +
                ", timeRecordSd=" + timeRecordSd +
                ", correctAnswerRateAvg=" + correctAnswerRateAvg +
                ", difficulty=" + difficulty +
                ", problemId=" + problemId +
                '}';
    }
}
