package com.example.productmanager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends BaseAdapter {

    public interface ProductActionListener {
        // =============================
        // CLICK PRODUCT → VIEW DETAIL
        // =============================
        void onProductClick(Product product);

        void onDeleteProduct(Product product);
        void onAddToCart(Product product);
    }

    @SuppressLint("DefaultLocale")
    private String formatVND(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private final Context context;
    private final int layout;
    private List<Product> productList;
    private ProductActionListener listener;
    private final boolean isAdmin;

    // Cache tránh gọi API nhiều lần
    private final Map<Integer, Double> ratingCache = new HashMap<>();
    private final Map<Integer, Integer> soldCache = new HashMap<>();

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

        // Load sold 1 lần khi adapter khởi tạo
        if (!isAdmin) {
            loadSold();
        }
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

            holder.txtDiscount = convertView.findViewById(R.id.txtDiscount);
            holder.layoutRating = convertView.findViewById(R.id.layoutRating);

            holder.txtRating = convertView.findViewById(R.id.txtRating);
            holder.txtSold = convertView.findViewById(R.id.txtSold);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);

        holder.txtName.setText(product.getName());
        holder.txtPrice.setText(formatVND(product.getPrice()));

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
        // LOAD RATING + SOLD
        // =============================

        if (!isAdmin) {

            int productId = product.getId();

            // Rating
            if (ratingCache.containsKey(productId)) {
                holder.txtRating.setText("⭐ " + ratingCache.get(productId));
            } else {
                loadRating(productId, holder.txtRating);
            }

            // Sold
            if (soldCache.containsKey(productId)) {
                holder.txtSold.setText(" | Đã bán " + soldCache.get(productId));
            } else {
                holder.txtSold.setText(" | Đã bán 0");
            }
        }

        // =============================
        // ADMIN MODE
        // =============================

        if (isAdmin) {

            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnAddToCart.setVisibility(View.GONE);

            if (holder.txtDiscount != null)
                holder.txtDiscount.setVisibility(View.GONE);

            if (holder.layoutRating != null)
                holder.layoutRating.setVisibility(View.GONE);

            // CLICK CARD → VIEW DETAIL
            View.OnClickListener showDetail = v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            };

            holder.imgProduct.setOnClickListener(showDetail);
            holder.layoutInfo.setOnClickListener(showDetail);

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

    // =============================
    // LOAD RATING
    // =============================

    private void loadRating(int productId, TextView txtRating) {

        ApiClient.getFeedbacksByProduct(context, productId,
                new ApiClient.DataCallback<List<Feedback>>() {

                    @Override
                    public void onSuccess(List<Feedback> feedbacks, String message) {

                        double avg = 0;

                        if (feedbacks != null && !feedbacks.isEmpty()) {

                            int sum = 0;

                            for (Feedback f : feedbacks) {
                                sum += f.getRating();
                            }

                            avg = (double) sum / feedbacks.size();
                        }

                        avg = Math.round(avg * 10.0) / 10.0;

                        ratingCache.put(productId, avg);

                        txtRating.setText("⭐ " + avg);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        txtRating.setText("⭐ 0.0");
                    }
                });
    }

    // =============================
    // LOAD SOLD
    // =============================

    private void loadSold() {

        ApiClient.getProductSold(context,
                response -> {

                    try {

                        JSONArray data = response.getJSONArray("data");

                        for (int i = 0; i < data.length(); i++) {

                            JSONObject obj = data.getJSONObject(i);

                            int productId = obj.getInt("productId");
                            int sold = obj.getInt("sold");

                            soldCache.put(productId, sold);

                            Log.d("SOLD", productId + " - " + sold);
                        }

                        notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                },
                error -> {
                    error.printStackTrace();
                }
        );
    }

    private static class ViewHolder {
        TextView txtName, txtPrice;
        ImageView imgProduct;
        ImageButton btnEdit, btnDelete;
        Button btnAddToCart;
        LinearLayout layoutInfo;

        TextView txtDiscount;
        LinearLayout layoutRating;

        TextView txtRating;
        TextView txtSold;
    }
}