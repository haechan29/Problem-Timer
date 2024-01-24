package kr.co.problemtimertest.jsonobject;

import java.util.ArrayList;
import java.util.List;

public class SchoolInfoObject {

    public DataSearch dataSearch;

    public static class DataSearch {

        public List<SchoolInfo> content = new ArrayList<>();
    }

    public static class SchoolInfo {

        public String adres;
        public String schoolName;
    }
}
