package kr.co.problemtimertest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.jsonobject.SchoolInfoObject;
import kr.co.problemtimertest.model.School;

public class PopupSchoolAdapter extends BaseAdapter {

    private List<School> items;

    public PopupSchoolAdapter(List<School> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : -1;
    }

    @Override
    public School getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View rootView = inflater.inflate(R.layout.item_dropdown_school, viewGroup, false);

        // 주소
        TextView addressText = (TextView) rootView.findViewById(R.id.text_address);

        String address = items.get(position).getAddress();
        addressText.setText(address == null ? null : address.split(" ")[0] + " " + address.split(" ")[1]);

        // 학교
        TextView schoolNameText = (TextView) rootView.findViewById(R.id.text_school_name);
        schoolNameText.setText(items.get(position).getName());

        return rootView;
    }
}
