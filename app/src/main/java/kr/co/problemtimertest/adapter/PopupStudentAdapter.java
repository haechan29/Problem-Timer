package kr.co.problemtimertest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.time.format.DateTimeFormatter;
import java.util.List;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.jsonobject.SchoolInfoObject;
import kr.co.problemtimertest.model.Student;
import kr.co.problemtimertest.service.ConversionService;

public class PopupStudentAdapter extends BaseAdapter {

    private List<Student> items;

    public PopupStudentAdapter(List<Student> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : -1;
    }

    @Override
    public Student getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View rootView = inflater.inflate(R.layout.item_dropdown_student, viewGroup, false);

        Student item = items.get(position);

        // 학년
        TextView gradeText = (TextView) rootView.findViewById(R.id.text_grade);
        gradeText.setText(ConversionService.gradeToStr(item.getGrade()));

        // 학생 이름
        TextView studentNameText = (TextView) rootView.findViewById(R.id.text_student_name);
        studentNameText.setText(item.getName());

        // 가입일
        TextView registeredAtText = (TextView) rootView.findViewById(R.id.text_registered_at);
        registeredAtText.setText(item.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 가입")));

        return rootView;
    }
}
