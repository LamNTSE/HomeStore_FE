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
    }

    private Context context;
    private int layout;
    private List<CartItem> list;
    private CartActionListener listener;

    public CartAdapter(Context context, int layout, List<CartItem> list, CartActionListener listener) {
        this.context = context;
        this.layout = layout;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getCount() { return list.size(); }
    @Override
    public Object getItem(int position) { return list.get(position); }
    @Override
    public long getItemId(int position) { return 0; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(layout, null);

        TextView tvName = view.findViewById(R.id.tvCartName);
        TextView tvPrice = view.findViewById(R.id.tvCartPrice);
        ImageView img = view.findViewById(R.id.imgCart);
        ImageButton btnRemove = view.findViewById(R.id.btnRemoveFromCart);

        CartItem item = list.get(position);
        tvName.setText(item.getProductName());
        tvPrice.setText(String.format("%.2f USD x %d = %.2f USD",
                item.getPrice(), item.getQuantity(), item.getSubTotal()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(img);
        } else {
            img.setImageResource(R.mipmap.ic_launcher);
        }

        btnRemove.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Xác nhận xóa");
            builder.setMessage("Bạn có chắc muốn xóa '" + item.getProductName() + "' khỏi giỏ hàng?");

            builder.setPositiveButton("Có", (dialog, which) -> {
                if (listener != null) {
                    listener.onRemoveCartItem(item);
                }
            });

            builder.setNegativeButton("Không", null);
            builder.show();
        });

        return view;
    }
}