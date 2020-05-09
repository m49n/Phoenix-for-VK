package biz.dealnote.messenger.adapter;

import android.content.Context;
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

import java.util.regex.Matcher;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.AnswerVKOfficial;
import biz.dealnote.messenger.model.AnswerVKOfficialList;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.LinkParser;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;

public class AnswerVKOfficialAdapter extends RecyclerView.Adapter<AnswerVKOfficialAdapter.Holder> {

    private static final int DIV_DISABLE = 0;
    private static final int DIV_TODAY = 1;
    private static final int DIV_YESTERDAY = 2;
    private static final int DIV_THIS_WEEK = 3;
    private static final int DIV_OLD = 4;
    private AnswerVKOfficialList data;
    private Context context;
    private Transformation transformation;
    private long mStartOfToday;
    private ClickListener clickListener;

    public AnswerVKOfficialAdapter(AnswerVKOfficialList data, Context context) {
        this.data = data;
        this.context = context;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
        this.mStartOfToday = Utils.startOfTodayMillis();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_answer_official, parent, false));
    }

    private void LoadIcon(@NonNull final Holder holder, final AnswerVKOfficial Page, boolean isSmall) {
        if (!isSmall)
            holder.avatar.setOnClickListener(v -> {
            });
        if (Page.iconURL != null) {
            if (isSmall) {
                holder.small.setVisibility(View.VISIBLE);
                ViewUtils.displayAvatar(holder.small, transformation, Page.iconURL, Constants.PICASSO_TAG);
            } else {
                holder.small.setVisibility(View.INVISIBLE);
                ViewUtils.displayAvatar(holder.avatar, transformation, Page.iconURL, Constants.PICASSO_TAG);
            }
        } else {
            int IconRes = GetIconResByType(Page.iconType);

            Drawable tr = AppCompatResources.getDrawable(context, IconRes);
            if (IconRes == R.drawable.phoenix) {
                assert tr != null;
                Utils.setColorFilter(tr, CurrentTheme.getColorPrimary(context));
            }
            if (isSmall) {
                holder.small.setVisibility(View.VISIBLE);
                holder.small.setImageDrawable(tr);
            } else {
                holder.small.setVisibility(View.INVISIBLE);
                holder.avatar.setImageDrawable(tr);
            }
        }
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private int getDivided(long messageDateJavaTime, Long previousMessageDateJavaTime) {
        int stCurrent = getStatus(messageDateJavaTime);
        if (previousMessageDateJavaTime == null) {
            return stCurrent;
        } else {
            int stPrevious = getStatus(previousMessageDateJavaTime);
            if (stCurrent == stPrevious) {
                return DIV_DISABLE;
            } else {
                return stCurrent;
            }
        }
    }

    private int getStatus(long time) {
        if (time >= mStartOfToday) {
            return DIV_TODAY;
        }

        if (time >= mStartOfToday - 86400000) {
            return DIV_YESTERDAY;
        }

        if (time >= mStartOfToday - 864000000) {
            return DIV_THIS_WEEK;
        }

        return DIV_OLD;
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final AnswerVKOfficial Page = data.items.get(position);
        AnswerVKOfficial previous = position == 0 ? null : data.items.get(position - 1);

        long lastMessageJavaTime = Page.time * 1000;
        int headerStatus = getDivided(lastMessageJavaTime, previous == null ? null : previous.time * 1000);

        switch (headerStatus) {
            case DIV_DISABLE:
                holder.mHeaderRoot.setVisibility(View.GONE);
                break;
            case DIV_OLD:
                holder.mHeaderRoot.setVisibility(View.VISIBLE);
                holder.mHeaderTitle.setText(R.string.dialog_day_older);
                break;
            case DIV_TODAY:
                holder.mHeaderRoot.setVisibility(View.VISIBLE);
                holder.mHeaderTitle.setText(R.string.dialog_day_today);
                break;
            case DIV_YESTERDAY:
                holder.mHeaderRoot.setVisibility(View.VISIBLE);
                holder.mHeaderTitle.setText(R.string.dialog_day_yesterday);
                break;
            case DIV_THIS_WEEK:
                holder.mHeaderRoot.setVisibility(View.VISIBLE);
                holder.mHeaderTitle.setText(R.string.dialog_day_ten_days);
                break;
        }


        holder.small.setVisibility(View.INVISIBLE);
        if (Page.header != null) {
            SpannableStringBuilder replace = new SpannableStringBuilder(Html.fromHtml(Page.header));
            holder.name.setText(LinkParser.parseLinks(context, replace), TextView.BufferType.SPANNABLE);

            Matcher matcher = LinkParser.MENTIONS_AVATAR_PATTERN.matcher(Page.header);
            if (matcher.find()) {
                String Type = matcher.group(1);
                int Id = Integer.parseInt(matcher.group(2));
                if (Type.equals("event") || Type.equals("club") || Type.equals("public"))
                    Id *= -1;
                String icn = data.getAvatar(Id);
                if (icn != null) {
                    PicassoInstance.with()
                            .load(icn)
                            .tag(Constants.PICASSO_TAG)
                            .placeholder(R.drawable.background_gray)
                            .transform(transformation)
                            .into(holder.avatar);
                    int finalId = Id;
                    holder.avatar.setOnClickListener(v -> clickListener.openOwnerWall(finalId));
                    LoadIcon(holder, Page, true);
                } else {
                    PicassoInstance.with().cancelRequest(holder.avatar);
                    LoadIcon(holder, Page, false);
                }
            } else {
                PicassoInstance.with().cancelRequest(holder.avatar);
                LoadIcon(holder, Page, false);
            }
        } else {
            holder.name.setText("");
            LoadIcon(holder, Page, false);
        }
        if (Page.text != null) {

            SpannableStringBuilder replace = new SpannableStringBuilder(Html.fromHtml(Page.text));
            holder.description.setText(LinkParser.parseLinks(context, replace), TextView.BufferType.SPANNABLE);
        } else
            holder.description.setText("");
        holder.time.setText(AppTextUtils.getDateFromUnixTime(context, Page.time));
    }

    private int GetIconResByType(String IconType) {
        if (IconType == null)
            return R.drawable.phoenix;
        if (IconType.equals("suggested_post_published")) {
            return R.drawable.ic_feedback_suggested_post_published;
        }
        if (IconType.equals("transfer_money_cancelled")) {
            return R.drawable.ic_feedback_transfer_money_cancelled;
        }
        if (IconType.equals("invite_game")) {
            return R.drawable.ic_feedback_invite_game;
        }
        if (IconType.equals("cancel")) {
            return R.drawable.ic_feedback_cancel;
        }
        if (IconType.equals("follow")) {
            return R.drawable.ic_feedback_follow;
        }
        if (IconType.equals("repost")) {
            return R.drawable.ic_feedback_repost;
        }
        if (IconType.equals("story_reply")) {
            return R.drawable.ic_feedback_story_reply;
        }
        if (IconType.equals("photo_tag")) {
            return R.drawable.ic_feedback_photo_tag;
        }
        if (IconType.equals("invite_group_accepted")) {
            return R.drawable.ic_feedback_invite_group_accepted;
        }
        if (IconType.equals("ads")) {
            return R.drawable.ic_feedback_ads;
        }
        if (IconType.equals("like")) {
            return R.drawable.ic_feedback_like;
        }
        if (IconType.equals("live")) {
            return R.drawable.ic_feedback_live;
        }
        if (IconType.equals("poll")) {
            return R.drawable.ic_feedback_poll;
        }
        if (IconType.equals("wall")) {
            return R.drawable.ic_feedback_wall;
        }
        if (IconType.equals("friend_found")) {
            return R.drawable.ic_feedback_add;
        }
        if (IconType.equals("event")) {
            return R.drawable.ic_feedback_event;
        }
        if (IconType.equals("reply")) {
            return R.drawable.ic_feedback_reply;
        }
        if (IconType.equals("gift")) {
            return R.drawable.ic_feedback_gift;
        }
        if (IconType.equals("friend_suggest")) {
            return R.drawable.ic_feedback_friend_suggest;
        }
        if (IconType.equals("invite_group")) {
            return R.drawable.ic_feedback_invite_group;
        }
        if (IconType.equals("friend_accepted")) {
            return R.drawable.ic_feedback_friend_accepted;
        }
        if (IconType.equals("mention")) {
            return R.drawable.ic_feedback_mention;
        }
        if (IconType.equals("comment")) {
            return R.drawable.ic_feedback_comment;
        }
        if (IconType.equals("message")) {
            return R.drawable.ic_feedback_message;
        }
        if (IconType.equals("private_post")) {
            return R.drawable.ic_feedback_private_post;
        }
        if (IconType.equals("birthday")) {
            return R.drawable.ic_feedback_birthday;
        }
        if (IconType.equals("invite_app")) {
            return R.drawable.ic_feedback_invite_app;
        }
        if (IconType.equals("new_post")) {
            return R.drawable.ic_feedback_new_post;
        }
        if (IconType.equals("interesting")) {
            return R.drawable.ic_feedback_interesting;
        }
        if (IconType.equals("transfer_money")) {
            return R.drawable.ic_feedback_transfer_money;
        }
        if (IconType.equals("transfer_votes")) {
            return R.drawable.ic_feedback_transfer_votes;
        }
        return R.drawable.phoenix;
    }

    @Override
    public int getItemCount() {
        if (data == null || data.items == null)
            return 0;
        return data.items.size();
    }

    public void setData(AnswerVKOfficialList data) {
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

    public interface ClickListener {
        void openOwnerWall(int owner_id);
    }

    public static class Holder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView name;
        TextView description;
        TextView time;
        ImageView small;
        View mHeaderRoot;
        TextView mHeaderTitle;

        public Holder(View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.item_friend_avatar);
            name = itemView.findViewById(R.id.item_friend_name);
            name.setMovementMethod(LinkMovementMethod.getInstance());
            description = itemView.findViewById(R.id.item_additional_info);
            description.setMovementMethod(LinkMovementMethod.getInstance());
            time = itemView.findViewById(R.id.item_friend_time);
            small = itemView.findViewById(R.id.item_icon);
            mHeaderRoot = itemView.findViewById(R.id.header_root);
            mHeaderTitle = itemView.findViewById(R.id.header_title);
        }
    }
}
