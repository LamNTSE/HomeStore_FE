package com.example.productmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends BaseAdapter {

    public interface CartActionListener {
        void onRemoveCartItem(CartItem item);
        void onIncreaseQuantity(CartItem item);
        void onDecreaseQuantity(CartItem item);
    }

    private Context context;
    private int layout;
    private List<CartItem> list;
    private CartActionListener listener;

    public CartAdapter(Context context,
                       int layout,
                       List<CartItem> list,
                       CartActionListener listener) {
        this.context = context;
        this.layout = layout;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // =========================
    // VIEW HOLDER
    // =========================
    static class ViewHolder {
        TextView tvName, tvPrice, tvQuantity;
        ImageView img;
        ImageButton btnRemove, btnPlus, btnMinus;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(layout, parent, false);

            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tvCartName);
            holder.tvPrice = convertView.findViewById(R.id.tvCartPrice);
            holder.tvQuantity = convertView.findViewById(R.id.tvQuantity);
            holder.img = convertView.findViewById(R.id.imgCart);
            holder.btnRemove = convertView.findViewById(R.id.btnRemoveFromCart);
            holder.btnPlus = convertView.findViewById(R.id.btnPlus);
            holder.btnMinus = convertView.findViewById(R.id.btnMinus);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CartItem item = list.get(position);

        // ===== GÁN DỮ LIỆU =====
        holder.tvName.setText(item.getProductName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        holder.tvPrice.setText(
                String.format("%.2f USD", item.getSubTotal())
        );

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.img);
        } else {
            holder.img.setImageResource(R.mipmap.ic_launcher);
        }

        // =========================
        // NÚT TĂNG
        // =========================
        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseQuantity(item);
            }
        });

        // =========================
        // NÚT GIẢM
        // =========================
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                if (listener != null) {
                    listener.onDecreaseQuantity(item);
                }
            }
        });

        // =========================
        // NÚT XÓA
        // =========================
        holder.btnRemove.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Xác nhận xóa");
            builder.setMessage("Bạn có chắc muốn xóa '"
                    + item.getProductName()
                    + "' khỏi giỏ hàng?");

            builder.setPositiveButton("Có", (dialog, which) -> {
                if (listener != null) {
                    listener.onRemoveCartItem(item);
                }
            });

            builder.setNegativeButton("Không", null);
            builder.show();
        });

        return convertView;
    }
}