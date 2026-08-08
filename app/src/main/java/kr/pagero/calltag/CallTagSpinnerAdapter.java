package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/** Spinner의 기본 흰색 시스템 행 대신 콜태그 다크 행을 사용한다. */
public final class CallTagSpinnerAdapter extends ArrayAdapter<String> {
    public CallTagSpinnerAdapter(Context context, List<String> values) {
        super(context, android.R.layout.simple_spinner_item, values);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return row(position, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return row(position, true);
    }

    private View row(int position, boolean dropdown) {
        TextView view = new TextView(getContext());
        String value = getItem(position);
        view.setText(value == null ? "" : value);
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(14f);
        view.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(14), 0, dp(14), 0);
        view.setBackgroundResource(dropdown
                ? R.drawable.bg_clickable_row : R.drawable.bg_secondary_button);
        view.setMinHeight(dp(dropdown ? 52 : 48));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
