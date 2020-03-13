package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.util.List;
import java.util.regex.Matcher;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.AnswerVKOfficial;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.LinkParser;
import biz.dealnote.messenger.util.ViewUtils;

public class AnswerVKOfficialAdapter extends RecyclerView.Adapter<AnswerVKOfficialAdapter.Holder> {

    private List<AnswerVKOfficial> data;
    private Context context;
    private Transformation transformation;

    public AnswerVKOfficialAdapter(List<AnswerVKOfficial> data, Context context) {
        this.data = data;
        this.context = context;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_answer_official, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final AnswerVKOfficial Page = data.get(position);
        if(Page.header != null) {
            SpannableStringBuilder replace = new SpannableStringBuilder(Page.header);
            Matcher matcher6 = LinkParser.REPLY_URL_PATTERN.matcher(Page.header);
            int n = 0;
            while (matcher6.find()) {
                replace = replace.replace(matcher6.start() - n, matcher6.end() - n, "<a href=\"" + matcher6.group(1) + "\">" + matcher6.group(14) + "</a>");
                n -= 12;
            }
            holder.name.setText(Html.fromHtml(LinkParser.parseLinks(replace).toString()));
        }
        else
            holder.name.setText("");
        if(Page.text != null) {
            SpannableStringBuilder replace = new SpannableStringBuilder(Page.text);
            Matcher matcher6 = LinkParser.REPLY_URL_PATTERN.matcher(Page.text);
            int n = 0;
            while (matcher6.find()) {
                replace = replace.replace(matcher6.start() - n, matcher6.end() - n, "<a href=\"" + matcher6.group(1) + "\">" + matcher6.group(14) + "</a>");
                n -= 12;
            }
            holder.description.setText(Html.fromHtml(LinkParser.parseLinks(replace).toString()));
        }
        else
            holder.description.setText("");
        holder.time.setText(AppTextUtils.getDateFromUnixTime(context, Page.time));
        if(Page.iconURL != null) {
            ViewUtils.displayAvatar(holder.avatar, transformation, Page.iconURL, Constants.PICASSO_TAG);
        }
        else {
            Drawable tr = AppCompatResources.getDrawable(context, R.drawable.online_phoenix);
            tr.setColorFilter(CurrentTheme.getColorPrimary(context), PorterDuff.Mode.MULTIPLY);
            holder.avatar.setImageDrawable(tr);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<AnswerVKOfficial> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    public class Holder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView name;
        TextView description;
        TextView time;

        public Holder(View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.item_friend_avatar);
            name = itemView.findViewById(R.id.item_friend_name);
            name.setMovementMethod(LinkMovementMethod.getInstance());
            description = itemView.findViewById(R.id.item_additional_info);
            description.setMovementMethod(LinkMovementMethod.getInstance());
            time = itemView.findViewById(R.id.item_friend_time);
        }
    }
}
