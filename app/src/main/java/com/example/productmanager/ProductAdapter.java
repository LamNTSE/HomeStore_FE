package com.example.productmanager;

import android.app.AlertDialog;
import android.content.Context;
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

    private Context context;
    private final int layout;
    private List<Product> productList;
    private ProductActionListener listener;
    private boolean isAdmin;

    public ProductAdapter(Context context,
                          int layout,
                          List<Product> productList,
                          ProductActionListener listener,
                          boolean isAdmin) {
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
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return productList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(layout, parent, false);

            holder.txtName = convertView.findViewById(R.id.txtName);
            holder.txtPrice = convertView.findViewById(R.id.txtPrice);
            holder.imgProduct = convertView.findViewById(R.id.imgProduct);
            holder.btnEdit = convertView.findViewById(R.id.btnEditItem);
            holder.btnDelete = convertView.findViewById(R.id.btnDeleteItem);
            holder.btnAddToCart = convertView.findViewById(R.id.btnAddToCartItem);
            holder.layoutInfo = convertView.findViewById(R.id.layoutInfo);

            // 👇 thêm 2 dòng này
            holder.txtDiscount = convertView.findViewById(R.id.txtDiscount);
            holder.layoutRating = convertView.findViewById(R.id.layoutRating);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);

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

        // =============================
        // ADMIN MODE
        // =============================
        if (isAdmin) {

            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnAddToCart.setVisibility(View.GONE);

            // 🔥 Ẩn rating + discount
            if (holder.txtDiscount != null)
                holder.txtDiscount.setVisibility(View.GONE);

            if (holder.layoutRating != null)
                holder.layoutRating.setVisibility(View.GONE);

            holder.imgProduct.setOnClickListener(null);
            holder.layoutInfo.setOnClickListener(null);

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
        }

        // =============================
        // CUSTOMER MODE
        // =============================
        else {

            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnAddToCart.setVisibility(View.VISIBLE);

            // 🔥 Hiển thị lại rating + discount
            if (holder.txtDiscount != null)
                holder.txtDiscount.setVisibility(View.VISIBLE);

            if (holder.layoutRating != null)
                holder.layoutRating.setVisibility(View.VISIBLE);

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
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView txtName, txtPrice;
        ImageView imgProduct;
        ImageButton btnEdit, btnDelete;
        Button btnAddToCart;
        LinearLayout layoutInfo;

        // 👇 thêm 2 dòng này
        TextView txtDiscount;
        LinearLayout layoutRating;
    }
}