package com.example.productmanager;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductAdapter extends BaseAdapter {
    public interface ProductActionListener {
        void onDeleteProduct(Product product);
        void onAddToCart(Product product);
    }

    private MainActivity context;
    private int layout;
    private List<Product> productList;
    private ProductActionListener listener;
    private boolean isAdmin;

    public ProductAdapter(MainActivity context, int layout, List<Product> productList, ProductActionListener listener, boolean isAdmin) {
        this.context = context;
        this.layout = layout;
        this.productList = productList;
        this.listener = listener;
        this.isAdmin = isAdmin;
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int i) {
        return productList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(layout, null);

            holder.txtName = view.findViewById(R.id.txtName);
            holder.txtPrice = view.findViewById(R.id.txtPrice);
            holder.imgProduct = view.findViewById(R.id.imgProduct);
            holder.btnEdit = view.findViewById(R.id.btnEditItem);
            holder.btnDelete = view.findViewById(R.id.btnDeleteItem);
            holder.btnAddToCart = view.findViewById(R.id.btnAddToCartItem);
            holder.layoutInfo = view.findViewById(R.id.layoutInfo);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Product product = productList.get(i);

        holder.txtName.setText(product.getName());
        holder.txtPrice.setText(String.format("%.2f USD", product.getPrice()));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.mipmap.ic_launcher);
        }

        View.OnClickListener showDetail = v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("id", product.getId());
            context.startActivity(intent);
        };
        holder.imgProduct.setOnClickListener(showDetail);
        holder.layoutInfo.setOnClickListener(showDetail);

        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCart(product);
            }
        });

        if (isAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Xác nhận xóa");
            builder.setMessage("Bạn có chắc muốn xóa " + product.getName() + "?");

            builder.setPositiveButton("Có", (dialog, which) -> {
                if (listener != null) {
                    listener.onDeleteProduct(product);
                }
            });

            builder.setNegativeButton("Không", null);
            builder.show();
        });

        return view;
    }

    private static class ViewHolder {
        TextView txtName, txtPrice;
        ImageView imgProduct;
        ImageButton btnEdit, btnDelete;
        Button btnAddToCart;
        LinearLayout layoutInfo;
    }
}