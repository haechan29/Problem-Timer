package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class BookUnit2 {

    // 변수
    private Long id;

    @SerializedName("unit_2_name")
    private String unit2Name;

    @SerializedName("unit_2_start_page")
    private Integer unit2StartPage;

    @SerializedName("unit_2_start_number")
    private String unit2StartNumber;

    @SerializedName("unit_2_end_page")
    private Integer unit2EndPage;

    @SerializedName("unit_2_end_number")
    private String unit2EndNumber;

    @SerializedName("book_id")
    private Long bookId;

    @SerializedName("unit_1_number")
    private Integer unit1Number;

    @SerializedName("unit_2_number")
    private Integer unit2Number;

    // toString()
    @Override
    public String toString() {
        return "BookUnit2{" +
                "id=" + id +
                ", unit2Name='" + unit2Name + '\'' +
                ", unit2StartPage=" + unit2StartPage +
                ", unit2StartNumber='" + unit2StartNumber + '\'' +
                ", unit2EndPage=" + unit2EndPage +
                ", unit2EndNumber='" + unit2EndNumber + '\'' +
                ", bookId=" + bookId +
                ", unit1Number=" + unit1Number +
                ", unit2Number=" + unit2Number +
                '}';
    }

    // 게터
    public Long getId() {
        return id;
    }

    public String getUnit2Name() {
        return unit2Name;
    }

    public Integer getUnit2StartPage() {
        return unit2StartPage;
    }

    public String getUnit2StartNumber() {
        return unit2StartNumber;
    }

    public Integer getUnit2EndPage() {
        return unit2EndPage;
    }

    public String getUnit2EndNumber() {
        return unit2EndNumber;
    }

    public Long getBookId() {
        return bookId;
    }

    public Integer getUnit1Number() {
        return unit1Number;
    }

    public Integer getUnit2Number() {
        return unit2Number;
    }
}
