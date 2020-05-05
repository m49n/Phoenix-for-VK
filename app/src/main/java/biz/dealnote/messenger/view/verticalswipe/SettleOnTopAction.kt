package biz.dealnote.messenger.view.verticalswipe

import android.view.View
import androidx.customview.widget.ViewDragHelper
import biz.dealnote.messenger.settings.Settings

/**
 * When view moved downwards, it returns to the initial position.
 * Moves above - takes away from the screen.
 */
@Suppress("unused")
class SettleOnTopAction: PostAction {

    private var originTop: Int = -1

    override fun onViewCaptured(child: View) {
        originTop = child.top
    }

    override fun releasedBelow(helper: ViewDragHelper, diff: Int, child: View): Boolean {
        if(Settings.get().ui().isPhoto_swipe_pos_top_to_bottom)
            return helper.settleCapturedViewAt(child.left, child.height)
        return helper.settleCapturedViewAt(child.left, originTop)
    }

    override fun releasedAbove(helper: ViewDragHelper, diff: Int, child: View): Boolean {
        if(Settings.get().ui().isPhoto_swipe_pos_top_to_bottom)
            return helper.settleCapturedViewAt(child.left, originTop)
        return helper.settleCapturedViewAt(child.left, -child.height)
    }
}
