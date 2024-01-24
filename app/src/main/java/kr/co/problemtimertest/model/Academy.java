package kr.co.problemtimertest.model;

import kr.co.problemtimertest.jsonobject.AcademyInfoObject;
import kr.co.problemtimertest.jsonobject.SchoolInfoObject;

public class Academy {

    // 변수
    private Long id;
    private Long apiId;
    private String address;
    private String name;

    // toAcademy()
    public static Academy toAcademy(AcademyInfoObject.AcademyInfo academyInfo) {

        if (academyInfo != null) {

            return new Academy(
                    null,
                    Long.parseLong(academyInfo.ACA_ASNUM),
                    academyInfo.FA_RDNMA,
                    academyInfo.ACA_NM);
        }

        return null;
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Long getApiId() {
        return apiId;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    // 생성자
    public Academy(Long id, Long apiId, String address, String name) {

        this.id = id;
        this.apiId = apiId;
        this.address = address;
        this.name = name;
    }
}


