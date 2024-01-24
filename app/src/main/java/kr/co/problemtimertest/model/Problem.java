package kr.co.problemtimertest.model;

import com.google.gson.annotations.SerializedName;

import kr.co.problemtimertest.service.TimerService;

public class Problem {

    // 변수
    private Long id;

    @SerializedName("book_id")
    private Long bookId;

    private Integer page;
    private String number;
    private String answer;

    private Long timeRecord = 0L;

    // 타이머
    private TimerService timerService = new TimerService(0L);

    public void startTimer() {
        timerService.startBtn();
    }

    public void stopTimer() {
        timerService.stopBtn();
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public Integer getPage() {
        return page;
    }

    public String getNumber() {
        return number;
    }

    public String getAnswer() {
        return answer;
    }

    public Long getTimeRecord() {
        return timeRecord;
    }

    public TimerService getTimerService() {
        return timerService;
    }

    // toString()
    @Override
    public String toString() {
        return "Problem{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", page=" + page +
                ", number='" + number + '\'' +
                ", answer='" + answer + '\'' +
                ", timeRecord=" + timeRecord +
                ", timerService=" + timerService +
                '}';
    }
}
