package kr.co.problemtimertest.listener;

import androidx.recyclerview.widget.RecyclerView;

import kr.co.problemtimertest.adapter.TeacherAssignmentAdapter;
import kr.co.problemtimertest.model.Assignment;
import kr.co.problemtimertest.model.Teacher;

public interface SetTeacherAssignmentRecyclerInterface {

    void setTeacherAssignmentRecycler(RecyclerView.ViewHolder holder, Teacher teacher, int position, TeacherAssignmentAdapter[] adapter);
}
