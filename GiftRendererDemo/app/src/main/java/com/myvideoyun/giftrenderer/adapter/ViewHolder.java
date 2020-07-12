package com.myvideoyun.giftrenderer.adapter;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.myvideoyun.giftrenderer.R;

public class ViewHolder extends RecyclerView.ViewHolder {
    private ImageView imageView;
    private TextView textView;

    public ViewHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_item, parent, false));

        initUI();
    }

    private void initUI() {
        imageView = itemView.findViewById(R.id.item_iv);
        textView = itemView.findViewById(R.id.item_tv);
    }

    public void onBindViewHolder(String imagePath, String text) {
        imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        textView.setText(text);
    }

    public void onBindViewHolder(int imageRes, String text) {
        imageView.setImageResource(imageRes);
        textView.setText(text);
    }

}
