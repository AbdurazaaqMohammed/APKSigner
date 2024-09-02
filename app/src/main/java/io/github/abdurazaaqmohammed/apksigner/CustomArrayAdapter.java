package io.github.abdurazaaqmohammed.apksigner;
import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Objects;

public class CustomArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int textColor;
    private String highlight;

    public CustomArrayAdapter(Context context, String[] values, int textColor, String highlight) {
        super(context, android.R.layout.simple_list_item_multiple_choice, values);
        this.context = context;
        this.values = values;
        this.textColor = textColor;
        if(!TextUtils.isEmpty(highlight)) this.highlight = highlight;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        String curr = values[position];
        textView.setText(Objects.equals(curr, highlight) ? Html.fromHtml("<b>" + curr + "</b>") : curr);
        textView.setTextColor(textColor);

        return convertView;
    }
}