package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

import kr.co.problemtimertest.jsonobject.SchoolInfoObject;

public class School {

    // 변수
    private Long id;

    @SerializedName("address")
    private String address;

    @SerializedName("name")
    private String name;

    // toSchool()
    public static School toSchool(SchoolInfoObject.SchoolInfo schoolInfo) {

        if (schoolInfo != null) {

            return new School(
                    null,
                    schoolInfo.adres,
                    schoolInfo.schoolName);
        }

        return null;
    }

    // 생성자
    public School(Long id, String address, String name) {

        this.id = id;
        this.address = address;
        this.name = name;
    }

    // getter

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }
}
