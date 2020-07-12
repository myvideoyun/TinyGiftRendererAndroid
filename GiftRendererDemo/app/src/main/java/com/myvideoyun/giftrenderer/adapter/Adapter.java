package com.myvideoyun.giftrenderer.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myvideoyun.giftrenderer.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<ViewHolder> {

    /**
     * mode 0 show effect
     * mode 1 show beautify
     * mode 2 show style
     * mode 3 show douyin
     */
    private int mode = 0;

    private List<Object[]> effectDataList;

    private List<Object[]> styleDataList;

    private List<Object[]> beautifyDataList;

    private List<Object[]> douyinDataList;

    private OnItemClickListener onItemClickListener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(viewGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder effectViewHolder, int i) {
        Object[] data = new Object[]{"", "", ""};
        if (mode == 0) {
            data = effectDataList.get(i);
        } else if (mode == 1) {
            data = beautifyDataList.get(i);
        } else if (mode == 2) {
            data = styleDataList.get(i);
        } else if (mode == 3) {
            data = douyinDataList.get(i);
        }

        if (data[0] instanceof Integer) {
            effectViewHolder.onBindViewHolder((Integer) data[0], (String) data[1]);
        } else if (data[0] instanceof String) {
            effectViewHolder.onBindViewHolder((String) data[0], (String) data[1]);
        }

        Object[] finalData = data;
        effectViewHolder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(mode, finalData[2]);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mode == 0) {
            if (effectDataList == null || effectDataList.size() == 0) {
                return 0;
            }
            return effectDataList.size();
        } else if (mode == 1) {
            if (beautifyDataList == null || beautifyDataList.size() == 0) {
                return 0;
            }
            return beautifyDataList.size();
        } else if (mode == 2) {
            if (styleDataList == null || styleDataList.size() == 0) {
                return 0;
            }
            return styleDataList.size();
        } else if (mode == 3) {
            if (douyinDataList == null || douyinDataList.size() == 0) {
                return 0;
            }
            return douyinDataList.size();
        }
        return 0;
    }


    public void refreshEffectData(List<Object[]> dataList) {
        if (this.effectDataList == null) {
            this.effectDataList = new ArrayList<>();
        }
        this.effectDataList.clear();
        this.effectDataList.add(new Object[]{R.mipmap.ban, "无", ""});
        this.effectDataList.addAll(dataList);

        notifyDataSetChanged();
    }

    public void refreshBeautifyData(List<Object[]> dataList) {
        if (this.beautifyDataList == null) {
            this.beautifyDataList = new ArrayList<>();
        }
        this.beautifyDataList.clear();
        this.beautifyDataList.addAll(dataList);

        notifyDataSetChanged();
    }

    public void refreshStyleData(List<Object[]> dataList) {
        if (this.styleDataList == null) {
            this.styleDataList = new ArrayList<>();
        }
        this.styleDataList.clear();
        this.styleDataList.addAll(dataList);

        notifyDataSetChanged();
    }

    public void refreshDouyinData(List<Object[]> dataList) {
        if (this.douyinDataList == null) {
            this.douyinDataList = new ArrayList<>();
        }
        this.douyinDataList.clear();
        this.douyinDataList.addAll(dataList);

        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setMode(int mode) {
        this.mode = mode;

        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(int mode, Object data);
    }
}
