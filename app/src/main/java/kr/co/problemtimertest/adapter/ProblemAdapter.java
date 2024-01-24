package kr.co.problemtimertest.adapter;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.listener.SetProblemRecyclerInterface;
import kr.co.problemtimertest.model.Problem;
import kr.co.problemtimertest.service.TimerService;

public class ProblemAdapter extends RecyclerView.Adapter<ProblemAdapter.ViewHolder> {

    private List<Problem> items = new ArrayList<>();

    private SetProblemRecyclerInterface setProblemRecyclerInterface;

    // ThreadPool 관련 변수
    private ExecutorService threadPool = new ThreadPoolExecutor(
            3,                      // 코어 스레드 개수
            100,                // 최대 스레드 개수
            120L,                  // 놀고 있는 시간
            TimeUnit.SECONDS,                  // 놀고 있는 시간 단위
            new SynchronousQueue<Runnable>()); // 작업 큐

    private Handler handler = new Handler();

    static class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout timerContainer;
        private LinearLayout isSolvedContainer;
        private LinearLayout recordContainer;
        private TextView problemNumberText;
        private TextView timeText;
        private Button startBtn;
        private Button stopBtn;
        private TextView answerText;

        public ViewHolder(View itemView) {

            super(itemView);

            // 변수를 설정한다.
            initializeVariables();
        }

        public void setItem(Problem item) {

            problemNumberText.setText(String.valueOf(item.getNumber()));
            timeText.setText(TimerService.convertToTimestamp(item.getTimeRecord() * 1000));
            answerText.setText(item.getAnswer());
        }

        private void initializeVariables() {

            timerContainer = (LinearLayout) itemView.findViewById(R.id.container_timer);
            isSolvedContainer = (LinearLayout) itemView.findViewById(R.id.container_is_solved);
            problemNumberText = (TextView) itemView.findViewById(R.id.text_problem_number);
            timeText = (TextView) itemView.findViewById(R.id.text_time);
            startBtn = (Button) itemView.findViewById(R.id.btn_start);
            stopBtn = (Button) itemView.findViewById(R.id.btn_stop);
            answerText = (TextView) itemView.findViewById(R.id.text_answer);

            recordContainer = (LinearLayout) itemView.findViewById(R.id.container_record);
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_problem, viewGroup, false);

        return new ViewHolder(itemView);
    }

    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        Problem item = items.get(position);

        viewHolder.setItem(item);

        setProblemRecyclerInterface.setProblemRecycler(viewHolder, position);

        return;
    }

    public int getItemCount() {
        return items.size();
    }

    public void addItem(Problem item) {
        items.add(item);
    }

    public void setItems(ArrayList<Problem> items) {
        this.items = items;
    }

    public Problem getItem(int position) {
        return items.get(position);
    }

    public List<Problem> getItems() {
        return items;
    }

    public void clearItems() { items.clear(); }

    public void setSetProblemRecyclerInterface(SetProblemRecyclerInterface setProblemRecyclerInterface) {
        this.setProblemRecyclerInterface = setProblemRecyclerInterface;
    }
}
