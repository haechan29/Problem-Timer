package kr.co.problemtimertest.model;

import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

public class StudentBook {

    // 변수
    @SerializedName("student_id")
    private Long studentId;

    @SerializedName("book_id")
    private Long bookId;

    @SerializedName("added_at")
    private LocalDateTime addedAt;

    // 생성자
    public StudentBook(Long studentId, Long bookId, LocalDateTime addedAt) {

        this.studentId = studentId;
        this.bookId = bookId;
        this.addedAt = addedAt;
    }

    // 게터
    public Long getStudentId() {
        return studentId;
    }

    public Long getBookId() {
        return bookId;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    // toString()
    @Override
    public String toString() {
        return "StudentBook{" +
                "studentId=" + studentId +
                ", bookId=" + bookId +
                ", addedAt=" + addedAt +
                '}';
    }
}
