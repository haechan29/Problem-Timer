package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class BookUnit1 {

    // 변수
    private Long id;

    @SerializedName("unit_1_name")
    private String unit1Name;

    @SerializedName("unit_1_start_page")
    private Integer unit1StartPage;

    @SerializedName("unit_1_start_number")
    private String unit1StartNumber;

    @SerializedName("unit_1_end_page")
    private Integer unit1EndPage;

    @SerializedName("unit_1_end_number")
    private String unit1EndNumber;

    @SerializedName("book_id")
    private Long bookId;

    @SerializedName("unit_1_number")
    private Integer unit1Number;

    // toString()
    @Override
    public String toString() {
        return "BookUnit1{" +
                "id=" + id +
                ", unit1Name='" + unit1Name + '\'' +
                ", unit1StartPage=" + unit1StartPage +
                ", unit1StartNumber='" + unit1StartNumber + '\'' +
                ", unit1EndPage=" + unit1EndPage +
                ", unit1EndNumber='" + unit1EndNumber + '\'' +
                ", bookId=" + bookId +
                ", unit1Number=" + unit1Number +
                '}';
    }

    // 게터
    public Long getId() {
        return id;
    }

    public String getUnit1Name() {
        return unit1Name;
    }

    public Integer getUnit1StartPage() {
        return unit1StartPage;
    }

    public String getUnit1StartNumber() {
        return unit1StartNumber;
    }

    public Integer getUnit1EndPage() {
        return unit1EndPage;
    }

    public String getUnit1EndNumber() {
        return unit1EndNumber;
    }

    public Long getBookId() {
        return bookId;
    }

    public Integer getUnit1Number() {
        return unit1Number;
    }
}
