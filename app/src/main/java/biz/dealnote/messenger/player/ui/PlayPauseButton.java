package biz.dealnote.messenger.player.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.player.util.MusicUtils;

public class PlayPauseButton extends FloatingActionButton implements OnClickListener {

    public PlayPauseButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        MusicUtils.playOrPause();
        updateState();
    }

    public void updateState() {
        if (MusicUtils.isPlaying()) {
            setImageResource(R.drawable.pause);
        } else {
            setImageResource(R.drawable.play);
        }
    }

}
