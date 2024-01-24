package kr.co.problemtimertest.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Student {

    // 변수
    private Long id;
    private String email;
    private String name;
    private Long schoolId;
    private Integer grade;

    @SerializedName("registered_at")
    private String registeredAt;

    // equals()
    @Override
    public boolean equals(@Nullable Object obj) {

        if (obj instanceof Student) {

            return id.equals(((Student) obj).id)
                    && email.equals(((Student) obj).email)
                    && name.equals(((Student) obj).name)
                    && schoolId.equals(((Student) obj).schoolId)
                    && grade.equals(((Student) obj).grade)
                    && registeredAt.equals(((Student) obj).registeredAt);
        }

        return false;
    }

    // 생성자
    public Student(Long id, String email, String name, Long schoolId, Integer grade, LocalDate registeredAt) {

        this.id = id;
        this.email = email;
        this.name = name;
        this.schoolId = schoolId;
        this.grade = grade;
        this.registeredAt = registeredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // toString()
    @Override
    public String toString() {

        return "Student{" +
                "id=" + id +
                ", nickname='" + email + '\'' +
                ", name='" + name + '\'' +
                ", schoolId='" + schoolId + '\'' +
                ", grade=" + grade +
                ", registeredAt=" + registeredAt +
                '}';
    }

    // 게터
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Integer getGrade() {
        return grade;
    }

    public LocalDate getRegisteredAt() {
        return LocalDate.parse(registeredAt);
    }
}
