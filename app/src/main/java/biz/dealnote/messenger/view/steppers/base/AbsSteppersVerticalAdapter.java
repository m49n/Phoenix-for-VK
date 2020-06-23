package biz.dealnote.messenger.view.steppers.base;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import biz.dealnote.messenger.adapter.holder.SharedHolders;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;

public abstract class AbsSteppersVerticalAdapter<H extends AbsStepsHost> extends RecyclerView.Adapter<AbsStepHolder<H>> {

    private final H mHost;
    private SharedHolders<AbsStepHolder<H>> mSharedHolders;
    private BaseHolderListener mActionListener;

    public AbsSteppersVerticalAdapter(@NonNull H host, @NonNull BaseHolderListener actionListener) {
        this.mHost = host;
        this.mSharedHolders = new SharedHolders<>(false);
        this.mActionListener = actionListener;
    }

    @Override
    public AbsStepHolder<H> onCreateViewHolder(ViewGroup parent, int viewType) {
        return createHolderForStep(parent, mHost, viewType);
    }

    @Nullable
    private AbsStepHolder<H> findHolderByStepIndex(int step) {
        return mSharedHolders.findOneByEntityId(step);
    }

    public abstract AbsStepHolder<H> createHolderForStep(ViewGroup parent, H host, int step);

    @Override
    public void onBindViewHolder(final AbsStepHolder<H> holder, final int position) {
        mSharedHolders.put(position, holder);

        holder.counterText.setText(String.valueOf(position + 1));

        boolean isCurrent = mHost.getCurrentStep() == position;
        boolean isLast = position == getItemCount() - 1;
        boolean isActive = position <= mHost.getCurrentStep();

        int activeColor = CurrentTheme.getColorPrimary(holder.itemView.getContext());
        int inactiveColor = Color.parseColor("#2cb1b1b1");
        int tintColor = isActive ? activeColor : inactiveColor;

        holder.counterRoot.setEnabled(isCurrent);
        Utils.setColorFilter(holder.counterRoot.getBackground(), tintColor);
        holder.contentRoot.setVisibility(isCurrent ? View.VISIBLE : View.GONE);

        holder.line.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

        holder.buttonNext.setText(mHost.getNextButtonText(position));
        holder.buttonNext.setOnClickListener(v -> mActionListener.onNextButtonClick(holder.getBindingAdapterPosition()));

        holder.buttonCancel.setText(mHost.getCancelButtonText(position));
        holder.buttonCancel.setOnClickListener(v -> mActionListener.onCancelButtonClick(holder.getBindingAdapterPosition()));

        holder.titleText.setText(mHost.getStepTitle(position));
        holder.titleText.setTypeface(Typeface.create(isCurrent ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));

        holder.bindInternalStepViews(mHost);
        holder.setNextButtonAvailable(mHost.canMoveNext(position));

        int px16dp = (int) Utils.dpToPx(16f, holder.itemView.getContext());
        holder.itemView.setPadding(0, 0, 0, position == getItemCount() - 1 ? px16dp : 0);
    }

    public void updateNextButtonAvailability(int step) {
        AbsStepHolder<H> holder = findHolderByStepIndex(step);
        if (Objects.nonNull(holder)) {
            holder.setNextButtonAvailable(mHost.canMoveNext(step));
        }
    }

    @Override
    public int getItemCount() {
        return mHost.getStepsCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}