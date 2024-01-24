package kr.co.problemtimertest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import kr.co.problemtimertest.R;
import kr.co.problemtimertest.jsonobject.AcademyInfoObject;

public class AcademyAdapter extends BaseAdapter {

    private List<AcademyInfoObject.AcademyInfo> items;

    public AcademyAdapter(List<AcademyInfoObject.AcademyInfo> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : -1;
    }

    @Override
    public AcademyInfoObject.AcademyInfo getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View rootView = inflater.inflate(R.layout.item_dropdown_academy, viewGroup, false);

        // 주소
        TextView adresText = (TextView) rootView.findViewById(R.id.text_address);

        String adres = items.get(position).FA_RDNMA;
        adresText.setText(adres == null ? null : adres.split(" ")[0] + " " + adres.split(" ")[1]);

        // 학원
        TextView academyNameText = (TextView) rootView.findViewById(R.id.text_academy_name);
        academyNameText.setText(items.get(position).ACA_NM);

        return rootView;
    }
}
