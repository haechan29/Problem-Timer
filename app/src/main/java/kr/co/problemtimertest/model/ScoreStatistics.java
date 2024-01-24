package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class ScoreStatistics {

    // 변수
    private Long id;
    private Float avg;
    private Float sd;

    @SerializedName("book_unit_2_id")
    private Long bookUnit2Id;

    // 게터
    public Long getId() {
        return id;
    }

    public Float getAvg() {
        return avg;
    }

    public Float getSd() {
        return sd;
    }

    public Long getBookUnit2Id() {
        return bookUnit2Id;
    }
}
