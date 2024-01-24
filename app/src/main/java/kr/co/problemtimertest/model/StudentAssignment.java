package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StudentAssignment {

    // 변수
    private Long assignmentId;
    private String academyName;
    private String bookName;
    private Integer pageFrom;
    private Integer pageTo;
    private String numberFrom;
    private String numberTo;
    private String dueDate;
    private String assignedAt;
    private Float progressRate;

    // 생성자
    public StudentAssignment(Long assignmentId, String academyName, String bookName, Integer pageFrom,
                             Integer pageTo, String numberFrom, String numberTo,
                             LocalDate dueDate, LocalDateTime assignedAt, Float progressRate) {

        this.assignmentId = assignmentId;
        this.academyName = academyName;
        this.bookName = bookName;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.numberFrom = numberFrom;
        this.numberTo = numberTo;
        this.dueDate = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.assignedAt = assignedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        this.progressRate = progressRate;
    }

    // toString()
    @Override
    public String toString() {
        return "StudentAssignment{" +
                "assignmentId=" + assignmentId +
                ", academyName='" + academyName + '\'' +
                ", bookName='" + bookName + '\'' +
                ", pageFrom=" + pageFrom +
                ", pageTo=" + pageTo +
                ", numberFrom='" + numberFrom + '\'' +
                ", numberTo='" + numberTo + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", assignedAt=" + assignedAt +
                ", progressRate=" + progressRate +
                '}';
    }

    // 게터
    public Long getAssignmentId() {
        return assignmentId;
    }

    public String getAcademyName() {
        return academyName;
    }

    public String getBookName() {
        return bookName;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public Integer getPageTo() {
        return pageTo;
    }

    public String getNumberFrom() {
        return numberFrom;
    }

    public String getNumberTo() {
        return numberTo;
    }

    public LocalDate getDueDate() {
        return LocalDate.parse(dueDate);
    }

    public LocalDateTime getAssignedAt() {
        return LocalDateTime.parse(assignedAt);
    }

    public Float getProgressRate() {
        return progressRate;
    }
}
