package com.example.productmanager;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OrderActionListener {
        void onConfirm(Order order);
        void onCancel(Order order);
        void onFeedbackClick(Order order);
    }

    private Context context;
    private List<Order> list;
    private boolean isAdminMode;
    private OrderActionListener listener;
    // orderId -> true means order has at least one feedback
    private Map<Integer, Boolean> feedbackStatus = new HashMap<>();

    public OrderAdapter(Context context, List<Order> list, boolean isAdminMode, OrderActionListener listener) {
        this.context = context;
        this.list = list;
        this.isAdminMode = isAdminMode;
        this.listener = listener;
    }

    public void updateFeedbackStatus(Map<Integer, Boolean> map) {
        this.feedbackStatus.clear();
        if (map != null) feedbackStatus.putAll(map);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Order order = list.get(position);

        holder.tvOrderId.setText("Đơn hàng #" + order.getOrderId());
        holder.tvReceiverName.setText("Người nhận: " + nullSafe(order.getReceiverName()));
        holder.tvPhone.setText("SĐT: " + nullSafe(order.getPhone()));
        holder.tvAddress.setText(nullSafe(order.getShippingAddress()));

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvTotalAmount.setText(format.format(order.getTotalAmount()));

        holder.tvPaymentMethod.setText(nullSafe(order.getPaymentMethod()));
        holder.tvCreatedAt.setText(formatDate(order.getCreatedAt()));

        String status = nullSafe(order.getStatus());
        holder.tvOrderStatus.setText(status);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12f);
        bg.setColor(getStatusColor(status));
        holder.tvOrderStatus.setBackground(bg);

        // Reset button visibility
        holder.btnConfirm.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.layoutActions.setVisibility(View.GONE);

        if (isAdminMode) {
            // Admin: Pending -> show "Xác nhận" (to Shipping) + "Huỷ đơn"
            if ("Pending".equals(status)) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnConfirm.setVisibility(View.VISIBLE);
                holder.btnConfirm.setText("Xác nhận");
                holder.btnCancel.setVisibility(View.VISIBLE);
            }
        } else {
            // User: Pending -> show "Huỷ đơn"
            // User: Shipping -> show "Xác nhận nhận hàng" + "Huỷ đơn"
            if ("Pending".equals(status)) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
            } else if ("Shipping".equals(status)) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnConfirm.setVisibility(View.VISIBLE);
                holder.btnConfirm.setText("Xác nhận nhận hàng");
                holder.btnCancel.setVisibility(View.VISIBLE);
            }
        }

        holder.btnConfirm.setOnClickListener(v -> {
            if (listener != null) listener.onConfirm(order);
        });
        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(order);
        });

        // Feedback link: only visible for non-admin, Delivered orders
        if (!isAdminMode && "Delivered".equals(status)) {
            holder.tvFeedbackLink.setVisibility(View.VISIBLE);
            boolean hasFeedback = Boolean.TRUE.equals(feedbackStatus.get(order.getOrderId()));
            holder.tvFeedbackLink.setText(hasFeedback ? "Xem đánh giá" : "Viết đánh giá");
            holder.tvFeedbackLink.setOnClickListener(v -> {
                if (listener != null) listener.onFeedbackClick(order);
            });
        } else {
            holder.tvFeedbackLink.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Confirmed":  return Color.parseColor("#1976D2");
            case "Shipping":   return Color.parseColor("#FF9800");
            case "Delivered":  return Color.parseColor("#4CAF50");
            case "Cancelled":  return Color.parseColor("#F44336");
            default:           return Color.parseColor("#9E9E9E"); // Pending
        }
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            // Lấy phần ngày từ ISO string (yyyy-MM-ddT...)
            return iso.substring(0, Math.min(iso.length(), 10));
        } catch (Exception e) {
            return iso;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvReceiverName, tvPhone,
                tvAddress, tvTotalAmount, tvPaymentMethod, tvCreatedAt, tvFeedbackLink;
        Button btnConfirm, btnCancel;
        LinearLayout layoutActions;

        ViewHolder(View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvReceiverName = itemView.findViewById(R.id.tvReceiverName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            tvFeedbackLink = itemView.findViewById(R.id.tvFeedbackLink);
        }
    }
}
