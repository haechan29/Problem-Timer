package kr.co.problemtimertest.listener;

public interface TimerInterface {

    void sleep(long milliSecond);
    void changeRes(int status, int button);
    void saveTimeRecord(long millisecond);
}
