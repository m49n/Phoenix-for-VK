package biz.dealnote.messenger.util

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import biz.dealnote.messenger.R
import ir.sinasoheili.animated_toast.AnimatedToast

class PhoenixToast private constructor(context: Context) {
    private val M_context: Context?
    private var duration: Int
    fun setDuration(duration: Int): PhoenixToast {
        this.duration = duration
        return this
    }

    fun showToast(message: String?) {
        if (M_context == null) return
        val t = AnimatedToast(M_context)
        t.duration = duration
        t.set_image(R.mipmap.ic_launcher_round)
        t.setText(message!!)
        t.setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 0)
        t.show()
    }

    fun showToast(@StringRes message: Int, vararg params: Any?) {
        if (M_context == null) return
        showToast(M_context.resources.getString(message, *params))
    }

    fun showToastInfo(message: String?) {
        if (M_context == null) return
        val view = View.inflate(M_context, R.layout.phoenix_toast_info, null)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
        subtitle.text = message
        val toast = Toast(M_context)
        toast.duration = duration
        toast.view = view
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    fun showToastInfo(@StringRes message: Int, vararg params: Any?) {
        if (M_context == null) return
        showToastInfo(M_context.resources.getString(message, *params))
    }

    fun showToastError(message: String?) {
        if (M_context == null) return
        val view = View.inflate(M_context, R.layout.toast_error, null)
        val subtitle = view.findViewById<TextView>(R.id.text)
        subtitle.text = message
        val toast = Toast(M_context)
        toast.duration = duration
        toast.view = view
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    fun showToastError(@StringRes message: Int, vararg params: Any?) {
        if (M_context == null) return
        showToastError(M_context.resources.getString(message, *params))
    }

    fun showToastSecret(Id : Boolean) {
        if (M_context == null) return
        val t = AnimatedToast(M_context)
        t.duration = duration
        if(!Id) {
            t.set_image(R.drawable.secret)
            t.setText("Австрийский художник одобряет)")
        }
        else {
            t.set_image(R.drawable.hleb)
            t.setText("ПОКУШАТЬ ТО ХЛЕБУШКА")
        }
        t.setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 0)
        t.show()
    }

    companion object {
        @JvmStatic
        fun CreatePhoenixToast(context: Context): PhoenixToast {
            return PhoenixToast(context)
        }
    }

    init {
        duration = Toast.LENGTH_SHORT
        M_context = context
    }
}