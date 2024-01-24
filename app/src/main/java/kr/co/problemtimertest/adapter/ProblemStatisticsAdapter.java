package kr.co.problemtimertest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kr.co.problemtimertest.listener.OnItemClickListener;
import kr.co.problemtimertest.R;
import kr.co.problemtimertest.model.ProblemStatistics;
import kr.co.problemtimertest.service.ConversionService;

public class ProblemStatisticsAdapter extends RecyclerView.Adapter<ProblemStatisticsAdapter.ViewHolder> {

    List<ProblemStatistics> items = new ArrayList<>();

    private OnItemClickListener onContainerClickListener;

    private boolean isHowToViewSwitchChecked;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout problemRecordByProblemContainer;
        private TextView problemNumberText;
        private TextView isSolvedText;
        private TextView correctAnswerRateAvgText;
        private TextView correctAnswerRateAvgUnitText;
        private TextView scoreUnitText;
        private TextView scoreText;

        public ViewHolder(View itemView, OnItemClickListener onContainerClickListener) {

            super(itemView);

            // 변수를 초기화한다.
            initializeVariables(itemView);

            // container가 클릭되면 수행된다.
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    onContainerClickListener.onItemClick(ViewHolder.this, view, position);
                }
            });
        }

        // 변수를 초기화한다.
        private void initializeVariables(View itemView) {

            problemRecordByProblemContainer =
                    (LinearLayout) itemView.findViewById(R.id.container_problem_record_by_problem);

            problemNumberText = (TextView) itemView.findViewById(R.id.text_problem_number);
            isSolvedText = (TextView) itemView.findViewById(R.id.text_is_solved);
            correctAnswerRateAvgUnitText = (TextView) itemView.findViewById(R.id.text_correct_answer_rate_avg_unit);
            correctAnswerRateAvgText = (TextView) itemView.findViewById(R.id.text_correct_answer_rate_avg);
            scoreUnitText = (TextView) itemView.findViewById(R.id.text_score_unit);
            scoreText = (TextView) itemView.findViewById(R.id.text_score);
        }

        public void setItem(ProblemStatistics item) {

            // 문제 번호 텍스트
            problemNumberText.setText(String.valueOf(item.getProblemNumber()));

            // 채점 기록 텍스트
            if (item.getIsSolved() == null) {
                isSolvedText.setText(null);
            }
            else {

                switch (item.getIsSolved()) {

                    /*
                    case 0: problemRecordByProblemContainer.setBackgroundResource(R.color.right); break;
                    case 1: problemRecordByProblemContainer.setBackgroundResource(R.color.wrong); break;
                    case 2: problemRecordByProblemContainer.setBackgroundResource(R.color.question);
                    */

                    case 0: isSolvedText.setText("⭕"); break;
                    case 1: isSolvedText.setText("❌"); break;
                    case 2: isSolvedText.setText("⭐");
                }
            }

            // 평균 정답률 텍스트
            Float correctAnswerRate = item.getCorrectAnswerRateAvg();

            if (correctAnswerRate == null) {

                correctAnswerRateAvgText.setText("-");
                correctAnswerRateAvgUnitText.setVisibility(View.GONE);
            }
            else {

                correctAnswerRateAvgText.setText(String.format("%4.1f", correctAnswerRate));
                correctAnswerRateAvgUnitText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_problem_statistics, viewGroup, false);

        // 뷰의 너비를 화면의 1/5으로 설정한다.
        itemView.setLayoutParams(new LinearLayout.LayoutParams(
                (int) ((float) viewGroup.getWidth() / (float) 5F),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        return new ViewHolder(itemView, onContainerClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        ProblemStatistics item = items.get(position);
        viewHolder.setItem(item);

        // 점수 텍스트를 설정한다.
        setScoreText(item, viewHolder, position);

        return;
    }

    private void setScoreText(ProblemStatistics item, ViewHolder viewHolder, int position) {

        Float score = item.getScore();
        TextView scoreUnitText = viewHolder.itemView.findViewById(R.id.text_score_unit);
        TextView scoreText = viewHolder.itemView.findViewById(R.id.text_score);

        // 점수 보기 모드에서는
        if (!isHowToViewSwitchChecked) {

            scoreUnitText.setText("점");

            if (score == null) {

                scoreText.setText("-");
                scoreUnitText.setVisibility(View.GONE);
            }
            else {

                scoreText.setText(String.format("%3.1f", item.getScore()));
                scoreUnitText.setVisibility(View.VISIBLE);
            }
        }
        // 등급 보기 모드에서는
        else {

            scoreUnitText.setText("등급");

            if (score == null) {

                scoreText.setText("-");
                scoreUnitText.setVisibility(View.GONE);
            }
            else {

                Integer rank = ConversionService.scoreToRank(score);

                if (rank != null) {

                    scoreText.setText(String.valueOf(rank));
                    scoreUnitText.setVisibility(View.VISIBLE);
                }
                else {

                    scoreText.setText("-");
                    scoreUnitText.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(ProblemStatistics item) {
        items.add(item);
    }

    public ProblemStatistics getItem(int position) {
        return items.get(position);
    }

    public List<ProblemStatistics> getItems() {
        return items;
    }

    public void clearItems() {
        items.clear();
    }

    public void setOnContainerClickListener(OnItemClickListener onContainerClickListener) {
        this.onContainerClickListener = onContainerClickListener;
    }

    public void setHowToViewSwitchChecked(boolean howToViewSwitchChecked) {
        isHowToViewSwitchChecked = howToViewSwitchChecked;
    }
}
