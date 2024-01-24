package kr.co.problemtimertest.model;

import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Assignment {

    // 변수
    private Long id;

    @SerializedName("book_id")
    private Long bookId;

    @SerializedName("typed_book_name")
    private String typedBookName;

    @SerializedName("page_from")
    private Integer pageFrom;

    @SerializedName("page_to")
    private Integer pageTo;

    @SerializedName("number_from")
    private String numberFrom;

    @SerializedName("number_to")
    private String numberTo;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("assigned_at")
    private String assignedAt;

    @SerializedName("classroom_name")
    private String classroomName;

    @SerializedName("student_id")
    private Long studentId;

    @SerializedName("teacher_id")
    private Long teacherId;

    // 생성자
    public Assignment(Long id, Long bookId, String typedBookName, Integer pageFrom, Integer pageTo,
                      String numberFrom, String numberTo, LocalDate dueDate, LocalDateTime assignedAt,
                      String classroomName, Long studentId, Long teacherId) {

        this.id = id;
        this.bookId = bookId;
        this.typedBookName = typedBookName;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.numberFrom = numberFrom;
        this.numberTo = numberTo;
        this.dueDate = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.assignedAt = assignedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        this.classroomName = classroomName;
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    // toString()
    @Override
    public String toString() {
        return "Assignment{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", typedBookName=" + typedBookName +
                ", pageFrom=" + pageFrom +
                ", pageTo=" + pageTo +
                ", numberFrom='" + numberFrom + '\'' +
                ", numberTo='" + numberTo + '\'' +
                ", dueDate=" + dueDate +
                ", assignedAt=" + assignedAt +
                ", classroomName='" + classroomName + '\'' +
                ", studentId=" + studentId +
                ", teacherId=" + teacherId +
                '}';
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTypedBookName() {
        return typedBookName;
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

    public String getClassroomName() {
        return classroomName;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getTeacherId() {
        return teacherId;
    }
}
