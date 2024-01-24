package kr.co.problemtimertest.jsonobject;

import java.util.ArrayList;
import java.util.List;

public class AcademyInfoObject {

    public List<AcaInsTiInfo> acaInsTiInfo = new ArrayList<>();

    public static class AcaInsTiInfo {

        public List<AcademyInfo> row = new ArrayList<>();
    }

    public static class AcademyInfo {

        public String ACA_ASNUM; // 학원지정번호
        public String FA_RDNMA; // 주소
        public String ACA_NM; // 학원 이름
    }
}
