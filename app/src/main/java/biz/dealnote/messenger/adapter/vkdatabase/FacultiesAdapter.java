package biz.dealnote.messenger.adapter.vkdatabase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.database.Faculty;

public class FacultiesAdapter extends RecyclerView.Adapter<FacultiesAdapter.Holder> {

    private Context mContext;
    private List<Faculty> mData;
    private Listener mListener;

    public FacultiesAdapter(Context mContext, List<Faculty> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(mContext).inflate(R.layout.item_country, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final Faculty faculty = mData.get(position);
        holder.name.setText(faculty.getTitle());

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onClick(faculty);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public interface Listener {
        void onClick(Faculty faculty);
    }

    public static class Holder extends RecyclerView.ViewHolder {

        TextView name;

        public Holder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
        }
    }
}