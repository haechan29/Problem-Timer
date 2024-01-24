package kr.co.problemtimertest.service;

import android.os.Handler;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import kr.co.problemtimertest.R;

public class ConversionService {

    public static Integer zToRank(Float z) {

        if      (z == null || z < 0.26F) { return null; }
        else if (z < 0.74F) { return 4; }
        else if (z < 1.23F) { return 3; }
        else if (z < 2.65F) { return 2; }
        else                { return 1; }
    }

    public static Integer scoreToRank(Float score) {

        if      (score - (1F/6F) < 0.001F) { return null; }
        else if (score - (2F/6F) < 0.001F) { return 4; }
        else if (score - (3F/6F) < 0.001F) { return 3; }
        else if (score - (4F/6F) < 0.001F) { return 2; }
        else                               { return 1; }
    }

    public static Float zToPercentage(Float z) {

        if (z == null || z < 0.26F) { return null; }
        else if (z < 0.28F) { return 0.397F; }
        else if (z < 0.31F) { return 0.389F; }
        else if (z < 0.34F) { return 0.378F; }
        else if (z < 0.36F) { return 0.366F; }
        else if (z < 0.39F) { return 0.359F; }
        else if (z < 0.42F) { return 0.348F; }
        else if (z < 0.44F) { return 0.337F; }
        else if (z < 0.47F) { return 0.330F; }
        else if (z < 0.50F) { return 0.319F; }
        else if (z < 0.53F) { return 0.308F; }
        else if (z < 0.56F) { return 0.298F; }
        else if (z < 0.59F) { return 0.287F; }
        else if (z < 0.62F) { return 0.277F; }
        else if (z < 0.65F) { return 0.267F; }
        else if (z < 0.68F) { return 0.257F; }
        else if (z < 0.71F) { return 0.248F; }
        else if (z < 0.74F) { return 0.238F; }
        else if (z < 0.78F) { return 0.229F; }
        else if (z < 0.81F) { return 0.217F; }
        else if (z < 0.85F) { return 0.209F; }
        else if (z < 0.88F) { return 0.197F; }
        else if (z < 0.92F) { return 0.189F; }
        else if (z < 0.96F) { return 0.178F; }
        else if (z < 1.00F) { return 0.168F; }
        else if (z < 1.04F) { return 0.158F; }
        else if (z < 1.09F) { return 0.149F; }
        else if (z < 1.13F) { return 0.137F; }
        else if (z < 1.18F) { return 0.129F; }
        else if (z < 1.23F) { return 0.119F; }
        else if (z < 1.29F) { return 0.109F; }
        else if (z < 1.35F) { return 0.985F; }
        else if (z < 1.41F) { return 0.885F; }
        else if (z < 1.48F) { return 0.793F; }
        else if (z < 1.56F) { return 0.694F; }
        else if (z < 1.65F) { return 0.594F; }
        else if (z < 1.76F) { return 0.495F; }
        else if (z < 1.82F) { return 0.392F; }
        else if (z < 1.89F) { return 0.344F; }
        else if (z < 1.96F) { return 0.294F; }
        else if (z < 2.06F) { return 0.250F; }
        else if (z < 2.11F) { return 0.197F; }
        else if (z < 2.17F) { return 0.174F; }
        else if (z < 2.24F) { return 0.150F; }
        else if (z < 2.33F) { return 0.125F; }
        else if (z < 2.41F) { return 0.099F; }
        else if (z < 2.51F) { return 0.080F; }
        else if (z < 2.58F) { return 0.060F; }
        else if (z < 2.65F) { return 0.049F; }
        else if (z < 2.75F) { return 0.040F; }
        else if (z < 2.88F) { return 0.030F; }
        else if (z < 3.08F) { return 0.020F; }
        else                { return 0.010F; }
    }

    public static String gradeToStr(Integer subject) {

        switch (subject) {
            case 0: return "초등 1학년";
            case 1: return "초등 2학년";
            case 2: return "초등 3학년";
            case 3: return "초등 4학년";
            case 4: return "초등 5학년";
            case 5: return "초등 6학년";
            case 6: return "중등 1학년";
            case 7: return "중등 2학년";
            case 8: return "중등 3학년";
            case 9: return "고등 1학년";
            case 10: return "고등 2학년";
            case 11: return "고등 3학년";
            default: return null;
        }
    }

    public static Integer strToGrade(String str) {

        switch (str) {
            case "초등 1학년": return 0;
            case "초등 2학년": return 1;
            case "초등 3학년": return 2;
            case "초등 4학년": return 3;
            case "초등 5학년": return 4;
            case "초등 6학년": return 5;
            case "중등 1학년": return 6;
            case "중등 2학년": return 7;
            case "중등 3학년": return 8;
            case "고등 1학년": return 9;
            case "고등 2학년": return 10;
            case "고등 3학년": return 11;
            default: return null;
        }
    }
}
