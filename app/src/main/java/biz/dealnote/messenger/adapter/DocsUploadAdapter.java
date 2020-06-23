package biz.dealnote.messenger.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.holder.IdentificableHolder;
import biz.dealnote.messenger.adapter.holder.SharedHolders;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.view.CircleRoadProgress;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class DocsUploadAdapter extends RecyclerView.Adapter<DocsUploadAdapter.Holder> {

    private static final int ERROR_COLOR = Color.parseColor("#ff0000");
    private static int idGenerator;
    private final SharedHolders<Holder> sharedHolders;

    private final ActionListener actionListener;
    private List<Upload> data;

    public DocsUploadAdapter(Context context, List<Upload> data, ActionListener actionListener) {
        this.data = data;
        this.actionListener = actionListener;
        this.sharedHolders = new SharedHolders<>(false);
    }

    private static int generateNextHolderId() {
        idGenerator++;
        return idGenerator;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.doc_upload_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        Upload upload = data.get(position);
        sharedHolders.put(position, holder);

        boolean inProgress = upload.getStatus() == Upload.STATUS_UPLOADING;
        holder.progress.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
        if (inProgress) {
            holder.progress.changePercentage(upload.getProgress());
        } else {
            holder.progress.changePercentage(0);
        }

        @ColorInt
        int titleColor = holder.status.getTextColors().getDefaultColor();

        switch (upload.getStatus()) {
            case Upload.STATUS_UPLOADING:
                String precentText = upload.getProgress() + "%";
                holder.status.setText(precentText);
                break;
            case Upload.STATUS_CANCELLING:
                holder.status.setText(R.string.cancelling);
                break;
            case Upload.STATUS_QUEUE:
                holder.status.setText(R.string.in_order);
                break;
            case Upload.STATUS_ERROR:
                holder.status.setText(R.string.error);
                titleColor = ERROR_COLOR;
                break;
        }

        holder.status.setTextColor(titleColor);
        holder.title.setText(String.valueOf(upload.getFileUri()));
        holder.buttonDelete.setOnClickListener(v -> actionListener.onRemoveClick(upload));
    }

    public void changeUploadProgress(int position, int progress, boolean smoothly) {
        Holder holder = sharedHolders.findOneByEntityId(position);
        if (nonNull(holder)) {
            String precentText = progress + "%";
            holder.status.setText(precentText);

            if (smoothly) {
                holder.progress.changePercentageSmoothly(progress);
            } else {
                holder.progress.changePercentage(progress);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<Upload> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public interface ActionListener {
        void onRemoveClick(Upload upload);
    }

    static class Holder extends RecyclerView.ViewHolder implements IdentificableHolder {

        TextView title;
        TextView status;
        View buttonDelete;
        CircleRoadProgress progress;
        //ImageView image;

        Holder(View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.title);
            this.status = itemView.findViewById(R.id.status);
            this.buttonDelete = itemView.findViewById(R.id.progress_root);
            this.progress = itemView.findViewById(R.id.progress_view);
            //this.image = (ImageView) itemView.findViewById(R.id.image);

            itemView.setTag(generateNextHolderId());
        }

        @Override
        public int getHolderId() {
            return (int) itemView.getTag();
        }
    }

}
