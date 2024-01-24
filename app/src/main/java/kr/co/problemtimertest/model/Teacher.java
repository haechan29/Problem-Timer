package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

public class Teacher {

    // 변수
    private Long id;
    private String email;
    private String name;

    @SerializedName("academy_id")
    private Long academyId;

    public Teacher(Long id, String email, String name, Long academyId) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.academyId = academyId;
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

    public Long getAcademyId() {
        return academyId;
    }
}
