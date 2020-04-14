package biz.dealnote.messenger.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.ThemeValue;
import biz.dealnote.messenger.settings.Settings;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    private List<ThemeValue> data;

    public ThemeAdapter(List<ThemeValue> data) {
        this.data = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ThemeValue category = data.get(position);

        holder.title.setText(category.name);
        holder.primary.setBackgroundColor(category.color_primary);
        holder.secondary.setBackgroundColor(category.color_secondary);
        holder.selected.setVisibility(Settings.get().ui().getMainThemeKey().equals(category.id) ? View.VISIBLE : View.GONE);
        holder.clicked.setOnClickListener(v -> clickListener.onClick(position, category));
    }

    public interface ClickListener {
        void onClick(int index, ThemeValue value);
    }

    private ClickListener clickListener;

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<ThemeValue> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView primary;
        ImageView secondary;
        ImageView selected;
        ViewGroup clicked;
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            primary = itemView.findViewById(R.id.theme_icon_primary);
            secondary = itemView.findViewById(R.id.theme_icon_secondary);
            selected = itemView.findViewById(R.id.selected);
            clicked = itemView.findViewById(R.id.theme_type);
            title = itemView.findViewById(R.id.item_title);
        }
    }
}
