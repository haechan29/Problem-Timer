package kr.co.problemtimertest.model;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeacherAssignment {

    // 변수
    private String bookName;
    private Integer pageFrom;
    private Integer pageTo;
    private String numberFrom;
    private String numberTo;
    private String dueDate;
    private String assignedAt;
    private String classroomName;
    private Integer grade;
    private Integer numberOfStudent;
    private Boolean assignedToAll;

    // equals()
    @Override
    public boolean equals(@Nullable Object obj) {

        if (obj instanceof TeacherAssignment) {

            return bookName == null ?
                    ((TeacherAssignment) obj).getBookName() == null :
                    bookName.equals(((TeacherAssignment) obj).getBookName()) &&

                    pageFrom == null ?
                    ((TeacherAssignment) obj).getPageFrom() == null :
                    pageFrom.equals(((TeacherAssignment) obj).getPageFrom()) &&

                    pageTo == null ?
                    ((TeacherAssignment) obj).getPageTo() == null :
                    pageTo.equals(((TeacherAssignment) obj).getPageTo()) &&

                    numberFrom == null ?
                    ((TeacherAssignment) obj).getNumberFrom() == null :
                    numberFrom.equals(((TeacherAssignment) obj).getNumberFrom()) &&

                    numberTo == null ?
                    ((TeacherAssignment) obj).getNumberTo() == null :
                    numberTo.equals(((TeacherAssignment) obj).getNumberTo()) &&

                    dueDate == null ?
                    ((TeacherAssignment) obj).getDueDate() == null :
                    dueDate.equals(((TeacherAssignment) obj).getDueDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) &&

                    assignedAt == null ?
                    ((TeacherAssignment) obj).getAssignedAt() == null :
                    assignedAt.equals(((TeacherAssignment) obj).getAssignedAt()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))) &&

                    classroomName == null ?
                    ((TeacherAssignment) obj).getClassroomName() == null :
                    classroomName.equals(((TeacherAssignment) obj).getClassroomName()) &&

                    grade == null ?
                    ((TeacherAssignment) obj).getGrade() == null :
                    grade.equals(((TeacherAssignment) obj).getGrade()) &&

                    numberOfStudent == null ?
                    ((TeacherAssignment) obj).getNumberOfStudent() == null :
                    numberOfStudent.equals(((TeacherAssignment) obj).getNumberOfStudent()) &&

                    assignedToAll == null ?
                    ((TeacherAssignment) obj).isAssignedToAll() == null :
                    assignedToAll.equals(((TeacherAssignment) obj).isAssignedToAll());
        }

        return false;
    }

    // 생성자
    public TeacherAssignment(String bookName, Integer pageFrom, Integer pageTo,
                             String numberFrom, String numberTo, LocalDate dueDate, LocalDateTime assignedAt,
                             String classroomName, Integer grade, Integer numberOfStudent, Boolean assignedToAll) {
        this.bookName = bookName;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.numberFrom = numberFrom;
        this.numberTo = numberTo;
        this.dueDate = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.assignedAt = assignedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        this.classroomName = classroomName;
        this.grade = grade;
        this.numberOfStudent = numberOfStudent;
        this.assignedToAll = assignedToAll;
    }

    // toString()
    @Override
    public String toString() {
        return "TeacherAssignment{" +
                "bookName='" + bookName + '\'' +
                ", pageFrom=" + pageFrom +
                ", pageTo=" + pageTo +
                ", numberFrom='" + numberFrom + '\'' +
                ", numberTo='" + numberTo + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", assignedAt='" + assignedAt + '\'' +
                ", classroomName='" + classroomName + '\'' +
                ", grade='" + grade + '\'' +
                ", numberOfStudent=" + numberOfStudent +
                ", assignedToAll=" + assignedToAll +
                '}';
    }

    // 게터
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

    public String getClassroomName() {
        return classroomName;
    }

    public Integer getGrade() {
        return grade;
    }

    public Integer getNumberOfStudent() {
        return numberOfStudent;
    }

    public Boolean isAssignedToAll() {
        return assignedToAll;
    }
}
