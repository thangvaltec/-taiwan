package mcv.testfacepass.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.google.easyapp.APPUtils;
import com.google.easyapp.utils.GlideUtils;

import java.util.List;

import butterknife.ButterKnife;
import mcv.testfacepass.R;
import mcv.testfacepass.bean.FaceFeatureBean;

public class FeatureItemAdapter extends RecyclerView.Adapter<FeatureItemAdapter.UserItemViewHolder> {
    private final Context context;
    private final List<FaceFeatureBean> list;

    private OnItemClickListener onItemClickListener;

    public FeatureItemAdapter(Context context, List<FaceFeatureBean> list) {
        this.list = list;
        this.context = context;
    }

    @Override
    public UserItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.item_photo_manage, parent, false);
        return new UserItemViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(UserItemViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final FaceFeatureBean item = list.get(position);
        holder.name.setText(item.name);
        if (item.bitmap!=null){
            holder.photo.setImageBitmap(item.bitmap);
        }
        holder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position,item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, FaceFeatureBean item);
    }

    static class UserItemViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView name;
        ImageView ivDelete;

        UserItemViewHolder(View itemView) {
            super(itemView);
            photo= (ImageView) itemView.findViewById(R.id.photo);
            name= (TextView) itemView.findViewById(R.id.name);
            ivDelete= (ImageView) itemView.findViewById(R.id.ivDelete);
            ButterKnife.bind(this, itemView);
        }
    }
}