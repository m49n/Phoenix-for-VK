package ir.sinasoheili.animated_toast

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class AnimatedToast : Toast {
    private var context: Context
    private var main_view: View? = null
    private val view_resource = R.layout.toast_view
    private var text: String? = null
    private var image_id = -1
    private val duration_animation_textview = 1000

    //CONSTRUCTOR ----------------------------------------------------------------------------------
    constructor(context: Context) : super(context) {
        this.context = context

        //set view
        init_view(context)
        setView(main_view)

        //set text
        this.setText("")

        //set image
        set_image(-1)

        //set duration
        this.duration = LENGTH_LONG
    }

    constructor(context: Context, text: String) : super(context) {
        this.context = context

        //set view
        init_view(context)
        setView(main_view)

        //set text
        this.setText(text)

        //set image
        set_image(-1)

        //set duration
        this.duration = LENGTH_LONG
    }

    constructor(context: Context, text_id: Int) : super(context) {
        this.context = context

        //set view
        init_view(context)
        setView(main_view)

        //set text
        this.setText(text_id)

        //set image
        set_image(-1)

        //set duration
        this.duration = LENGTH_LONG
    }

    constructor(context: Context, text: String, image_src_id: Int) : super(context) {
        this.context = context

        //set view
        init_view(context)
        setView(main_view)

        //set text
        this.setText(text)

        //set image view
        set_image(image_src_id)

        //set duration
        this.duration = LENGTH_LONG
    }

    constructor(context: Context, text_id: Int, image_src_id: Int) : super(context) {
        this.context = context

        //set view
        init_view(context)
        setView(main_view)

        //set text
        this.setText(text_id)

        //set image view
        set_image(image_src_id)

        //set duration
        this.duration = LENGTH_LONG
    }

    //INIT VIEW ------------------------------------------------------------------------------------
    private fun init_view(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        main_view = inflater.inflate(view_resource, null, false)
    }

    //SET TEXT -------------------------------------------------------------------------------------
    override fun setText(s: CharSequence) {
        text = s as String
    }

    override fun setText(resId: Int) {
        text = try {
            context.getString(resId)
        } catch (e: Exception) {
            ""
        }
    }

    //SET IMAGE ------------------------------------------------------------------------------------
    fun set_image(image_res_id: Int) {
        image_id = image_res_id
    }

    //GETTER ---------------------------------------------------------------------------------------
    override fun getView(): View {
        return main_view!!
    }

    //ANIMATION ------------------------------------------------------------------------------------
    private fun anim_scale_imageView() {
        if (main_view != null) {
            val iv = main_view!!.findViewById<ImageView>(R.id.toast_image_view)
            if (image_id == -1) {
                iv.visibility = View.GONE
            } else {
                iv.visibility = View.VISIBLE
                iv.setImageResource(image_id)
                val anim = AnimationUtils.loadAnimation(context, R.anim.scale_image)
                iv.startAnimation(anim)
            }
        }
    }

    private fun anim_textView() {
        if (main_view != null) {
            val tv = main_view!!.findViewById<TextView>(R.id.toast_text_view)
            if (text == null || text!!.isEmpty() == true) {
                tv.visibility = View.GONE
            } else {
                tv.visibility = View.VISIBLE
                val rect = Rect()
                tv.paint.getTextBounds(text, 0, text!!.length, rect)
                val min_width = 0
                val max_width = (rect.width() + tv.textSize).toInt()
                tv.layoutParams.width = min_width
                val vanimator = ValueAnimator.ofInt(0, max_width)
                vanimator.duration = duration_animation_textview.toLong()
                vanimator.startDelay = 500 + 200.toLong() //500 is duration of scale animation for ImageView
                vanimator.addUpdateListener { animation ->
                    tv.layoutParams.width = animation.animatedValue as Int
                    tv.requestLayout()
                }
                vanimator.start()
                Handler().postDelayed({ tv.text = text }, duration_animation_textview + 500 + 200.toLong()) //duration_animation_textview+500+200 are all durartion and delay of TextView animation
            }
        }
    }

    // SET DURATION --------------------------------------------------------------------------------
    override fun setDuration(duration: Int) {
        super.setDuration(LENGTH_LONG)
    }

    //SHOW AND CANCEL ------------------------------------------------------------------------------
    override fun cancel() {
        super.cancel()
    }

    override fun show() {
        anim_scale_imageView()
        anim_textView()
        super.show()
    }
}