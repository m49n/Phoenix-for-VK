package biz.dealnote.messenger.player.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.widget.AppCompatImageButton;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;

public class RepeatButton extends AppCompatImageButton implements OnClickListener {

    public RepeatButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        MusicUtils.cycleRepeat();
        updateRepeatState();
    }

    public void updateRepeatState() {
        switch (MusicUtils.getRepeatMode()) {
            case MusicPlaybackService.REPEAT_ALL:
                setImageDrawable(getResources().getDrawable(R.drawable.repeat, getContext().getTheme()));
                break;
            case MusicPlaybackService.REPEAT_CURRENT:
                setImageDrawable(getResources().getDrawable(R.drawable.repeat_once, getContext().getTheme()));
                break;
            case MusicPlaybackService.REPEAT_NONE:
                setImageDrawable(getResources().getDrawable(R.drawable.repeat_off, getContext().getTheme()));
                break;
            default:
                break;
        }
    }
}
