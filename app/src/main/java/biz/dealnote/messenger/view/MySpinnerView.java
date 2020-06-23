package biz.dealnote.messenger.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.util.Utils;

public class MySpinnerView extends RelativeLayout {

    private String mHintText;
    @ColorInt
    private int mHintColor;
    @ColorInt
    private int mTextColor;
    private TextView mTextView;

    public MySpinnerView(Context context) {
        this(context, null);
    }

    public MySpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.view_my_spinner, this);

        this.mTextView = findViewById(R.id.text);

        setBackgroundResource(R.drawable.backgroud_rectangle_border);

        ImageView icon = findViewById(R.id.icon);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MySpinnerView);

        try {
            this.mHintText = a.getString(R.styleable.MySpinnerView_spinner_hint);
            this.mHintColor = a.getColor(R.styleable.MySpinnerView_spinner_hint_color, Color.GRAY);
            this.mTextColor = a.getColor(R.styleable.MySpinnerView_spinner_text_color, Color.BLACK);

            int iconColor = a.getColor(R.styleable.MySpinnerView_spinner_icon_color, Color.BLUE);
            Utils.setColorFilter(icon, iconColor);
        } finally {
            a.recycle();
        }

        mTextView.setText(mHintText);
        mTextView.setTextColor(mHintColor);
    }

    public void setIconOnClickListener(View.OnClickListener listener) {
        findViewById(R.id.icon).setOnClickListener(listener);
    }

    public void setValue(String value) {
        if (value != null) {
            mTextView.setText(value);
            mTextView.setTextColor(mTextColor);
        } else {
            mTextView.setText(mHintText);
            mTextView.setTextColor(mHintColor);
        }
    }
}