package com.example.productmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {

    public interface VoucherActionListener {
        void onEdit(Voucher voucher);
        void onDelete(Voucher voucher);
    }

    // ⭐ thêm interface chọn voucher
    public interface VoucherSelectListener {
        void onSelect(Voucher voucher);
    }

    private Context context;
    private List<Voucher> list;
    private VoucherActionListener listener;

    // ⭐ biến mới
    private VoucherSelectListener selectListener;
    private boolean isSelectMode = false;

    // constructor cũ (admin)
    public VoucherAdapter(Context context,
                          List<Voucher> list,
                          VoucherActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    // ⭐ constructor mới (cart chọn voucher)
    public VoucherAdapter(Context context,
                          List<Voucher> list,
                          VoucherSelectListener selectListener) {

        this.context = context;
        this.list = list;
        this.selectListener = selectListener;
        this.isSelectMode = true;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Voucher v = list.get(position);

        holder.txtCode.setText(v.getCode());

        NumberFormat format =
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
            holder.txtDiscount.setText(String.format("Giảm %s%%", v.getDiscountValue()));
        } else {
            holder.txtDiscount.setText(String.format("Giảm %s", format.format(v.getDiscountValue())));
        }

        if (v.isActive()) {
            holder.txtStatus.setText("Active");
            holder.txtStatus.setBackgroundResource(R.drawable.status_bg);
        } else {
            holder.txtStatus.setText("Inactive");
            holder.txtStatus.setBackgroundResource(R.drawable.status_bg_inactive);
        }

        // ⭐ chế độ chọn voucher (cart)
        if (isSelectMode) {

            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(view -> {
                if (selectListener != null) {
                    selectListener.onSelect(v);
                }
            });

            return;
        }

        // ========================
        // CODE CŨ GIỮ NGUYÊN
        // ========================

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, VoucherDetailActivity.class);
            intent.putExtra("voucherId", v.getVoucherId());
            context.startActivity(intent);
        });

        // ⭐ thêm kiểm tra null để tránh crash
        holder.btnEdit.setOnClickListener(view -> {
            if (listener != null) {
                listener.onEdit(v);
            }
        });

        holder.btnDelete.setOnClickListener(view -> {
            if (listener != null) {
                new AlertDialog.Builder(context)
                        .setTitle("Confirm")
                        .setMessage("Delete this voucher?")
                        .setPositiveButton("Yes",
                                (d, w) -> listener.onDelete(v))
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtCode, txtDiscount, txtStatus;
        Button btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);

            txtCode = itemView.findViewById(R.id.txtCode);
            txtDiscount = itemView.findViewById(R.id.txtDiscount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}