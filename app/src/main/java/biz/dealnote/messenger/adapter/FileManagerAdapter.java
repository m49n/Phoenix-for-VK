package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.FileItem;
import biz.dealnote.messenger.util.ElipseTransformation;

public class FileManagerAdapter extends RecyclerView.Adapter<FileManagerAdapter.Holder> {

    private List<FileItem> data;
    private Context mContext;

    public FileManagerAdapter(Context context, List<FileItem> data) {
        this.data = data;
        this.mContext = context;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final FileItem item = data.get(position);
        holder.icon.setBackgroundResource(item.icon);
        if(!item.directory) {
            PicassoInstance.with()
                    .load("file://" + item.path)
                    .tag(Constants.PICASSO_TAG)
                    .transform(new ElipseTransformation())
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            holder.icon.setBackground(new BitmapDrawable(mContext.getResources(), bitmap));
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    });
        }
        holder.fileName.setText(item.file);
        holder.fileDetails.setText(item.details);
        holder.fileDetails.setVisibility(TextUtils.isEmpty(item.details) ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if(clickListener != null){
                clickListener.onClick(holder.getAdapterPosition(), item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class Holder extends RecyclerView.ViewHolder {

        TextView fileName;
        TextView fileDetails;
        ImageView icon;

        public Holder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.item_file_name);
            fileDetails = itemView.findViewById(R.id.item_file_details);
            icon = itemView.findViewById(R.id.item_file_icon);
        }
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private ClickListener clickListener;

    public interface ClickListener {
        void onClick(int position, FileItem item);
    }
}
