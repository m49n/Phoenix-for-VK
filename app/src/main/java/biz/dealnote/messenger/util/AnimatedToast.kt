package biz.dealnote.messenger.util;

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import biz.dealnote.messenger.R


class AnimatedToast
(private var context: Context) : Toast(context) {
    private var main_view: View? = null
    private val view_resource = R.layout.toast_view
    private var text: String? = null
    private var image: Drawable? = null

    init {
        init_view(context)
        setView(main_view)
        this.setText("")
        set_image(null)
        this.duration = LENGTH_LONG
    }

    private fun init_view(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        main_view = inflater.inflate(view_resource, null, false)
    }

    override fun setText(s: CharSequence?) {
        text = s as String
    }

    override fun setText(resId: Int) {
        text = try {
            context.getString(resId)
        } catch (e: Exception) {
            ""
        }
    }

    fun set_image(image_p: Bitmap?) {
        image = BitmapDrawable(context.getResources(), image_p)
    }

    fun set_image(@DrawableRes image_p: Int) {
        image = context.getDrawable(image_p);
    }

    override fun getView(): View {
        return main_view!!
    }

    private fun anim_scale_imageView() {
        if (main_view != null) {
            val iv = main_view!!.findViewById<ImageView>(R.id.toast_image_view)
            if (image == null) {
                iv.visibility = View.GONE
            } else {
                iv.visibility = View.VISIBLE
                iv.setImageDrawable(image)
                val anim = AnimationUtils.loadAnimation(context, R.anim.scale_image)
                iv.startAnimation(anim)
            }
        }
    }

    private fun anim_textView() {
        if (main_view != null) {
            val tv = main_view!!.findViewById<TextView>(R.id.toast_text_view)
            if (text == null || text!!.isEmpty()) {
                tv.visibility = View.GONE
            } else {
                tv.visibility = View.VISIBLE
                Handler().postDelayed({ tv.text = text }, 500 + 200.toLong())
            }
        }
    }

    override fun show() {
        anim_scale_imageView()
        anim_textView()
        super.show()
    }
}
