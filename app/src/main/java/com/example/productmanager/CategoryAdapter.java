package com.example.productmanager;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class CategoryAdapter extends BaseAdapter {

    private Context context;
    private List<Category> categoryList;

    public CategoryAdapter(Context context, List<Category> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    @Override
    public int getCount() {
        return categoryList == null ? 0 : categoryList.size();
    }

    @Override
    public Category getItem(int position) {
        return categoryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return categoryList.get(position).getCategoryId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView txt = convertView.findViewById(android.R.id.text1);

        Category category = categoryList.get(position);

        txt.setText(category.getCategoryName());

        // Click category -> mở ProductActivity
        convertView.setOnClickListener(v -> {

            Intent intent = new Intent(context, ProductByCategoryActivity.class);
            intent.putExtra("categoryId", category.getCategoryId());
            intent.putExtra("categoryName", category.getCategoryName());

            context.startActivity(intent);
        });

        return convertView;
    }

    public void updateList(List<Category> newList){
        this.categoryList = newList;
        notifyDataSetChanged();
    }
}