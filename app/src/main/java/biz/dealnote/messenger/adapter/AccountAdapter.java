package biz.dealnote.messenger.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.Account;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.ViewUtils;

import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.Holder> {

    private Context context;
    private List<Account> data;
    private Transformation transformation;
    private Callback callback;

    public AccountAdapter(Context context, List<Account> items, Callback callback) {
        this.context = context;
        this.data = items;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
        this.callback = callback;
    }

    @NotNull
    @Override
    public Holder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_account, parent, false));
    }

    @NotNull
    public Account getByPosition(int position) {
        return data.get(position);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull Holder holder, int position) {
        final Account account = data.get(position);

        Owner owner = account.getOwner();

        if (Objects.isNull(owner)) {
            holder.firstName.setText(String.valueOf(account.getId()));
            ViewUtils.displayAvatar(holder.avatar, transformation, null, Constants.PICASSO_TAG);
        } else {
            holder.firstName.setText(owner.getFullName());
            ViewUtils.displayAvatar(holder.avatar, transformation, owner.getMaxSquareAvatar(), Constants.PICASSO_TAG);
        }

        if (account.getId() < 0) {
            holder.lastName.setText("club" + Math.abs(account.getId()));
        } else {
            User user = (User) owner;

            if (Objects.nonNull(user) && nonEmpty(user.getDomain())) {
                holder.lastName.setText("@" + user.getDomain());
            } else {
                holder.lastName.setText("@id" + account.getId());
            }
        }

        boolean isCurrent = account.getId() == Settings.get()
                .accounts()
                .getCurrent();

        holder.active.setVisibility(isCurrent ? View.VISIBLE : View.INVISIBLE);
        holder.account.setOnClickListener(v -> callback.onClick(account));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface Callback {
        void onClick(Account account);
    }

    public static class Holder extends RecyclerView.ViewHolder {

        TextView firstName;
        TextView lastName;
        ImageView avatar;
        ImageView active;
        View account;

        public Holder(View itemView) {
            super(itemView);
            firstName = itemView.findViewById(R.id.first_name);
            lastName = itemView.findViewById(R.id.last_name);
            avatar = itemView.findViewById(R.id.avatar);
            active = itemView.findViewById(R.id.active);
            account = itemView.findViewById(R.id.account_select);
        }
    }
}