package kr.co.problemtimertest.service;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.listener.TimerInterface;

public class TimerService {

    // status
    public static final int INIT    = 0; //처음
    public static final int RUN     = 1; //실행중
    public static final int PAUSE   = 2; //정지

    private int status = INIT;

    // pushed Btn
    public static final int START = 0;
    public static final int STOP = 1;

    // 경과한 시간
    private long milliSecond = 0;
    private boolean isTimerStopped = true;

    private TimerInterface timerInterface;

    // 스레드풀
    ExecutorService threadPool = new ThreadPoolExecutor(
            3,                       // 코어 스레드 개수
            100,                 // 최대 스레드 개수
            120L,                   // 놀고 있는 시간
            TimeUnit.SECONDS,                   // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>());  // 작업 큐

    // 생성자
    public TimerService(float timeRecord) {

        this.milliSecond = (long) (timeRecord * 1000);
    }

    // startBtn을 눌렀을 때
    public void startBtn() {

        // stop의 값을 변경한다.
        isTimerStopped = !isTimerStopped;

        switch(status) {

            // 맨 처음 startBtn을 누른 경우
            case INIT:

            // 일시정지되어 있던 타이머를 다시 작동시킨 경우
            case PAUSE:

                // 0.1초마다 millisecond의 값을 100만큼 증가시킨다.
                // stop이 false가 되기 전까지 반복한다.
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        while(!isTimerStopped) {

                            // 0.1초간 정지하고, millisecond를 100만큼 증가시킨다.
                            timerInterface.sleep(milliSecond);
                            milliSecond += 100;
                        }
                    }
                });

                // 리소스를 변경한다.
                timerInterface.changeRes(status, START);

                // 상태를 변경한다.
                status = RUN;

                break;

            // 작동하던 타이머를 일시정지시킨 경우
            case RUN:

                // 리소스를 변경한다.
                timerInterface.changeRes(status, START);

                // 상태를 변경한다.
                status = PAUSE;
        }
    }

    // stopBtn을 눌렀을 때
    public void stopBtn() {

        switch (status) {

            // 타이머가 작동하고 있는 상태에서 기록하기 버튼을 누른 경우
            case RUN:

                isTimerStopped = !isTimerStopped;

                // 시간 기록을 저장한다.
                timerInterface.saveTimeRecord(milliSecond);

                // 리소스를 변경한다.
                timerInterface.changeRes(status, STOP);

                // 상태를 변경한다.
                status = PAUSE;

                break;

            // 타이머가 일시정지된 상태에서 초기화하기 버튼을 누른 경우
            case PAUSE:

                milliSecond = 0;

                // 리소스를 변경한다.
                timerInterface.changeRes(status, STOP);

                // 상태를 변경한다.
                status = INIT;
        }
    }

    // 입력된 millisecond를 "hh:mm:ss" 형식의 문자열로 변환하여 반환한다.
    public static String convertToTimestamp(Long milliSecond) {

        if (milliSecond == null) {

            Log.d("error", "\nTimerService.convertToTimeStamp" +
                    "\nmilliSecond가 null입니다.");

            return null;
        }

        // millisecond의 시간, 분, 초를 계산한다.
        long h = milliSecond / 1000 / 60 / 60;
        long m = (milliSecond / 1000 / 60) % 60;
        long s = (milliSecond / 1000) % 60;

        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public void setMilliSecond(long milliSecond) {
        this.milliSecond = milliSecond;
    }

    public void setTimerInterface(TimerInterface timerInterface) {
        this.timerInterface = timerInterface;
    }
}
