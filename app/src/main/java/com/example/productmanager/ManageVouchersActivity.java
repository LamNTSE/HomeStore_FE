package com.example.productmanager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ManageVouchersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VoucherAdapter adapter;
    private final List<Voucher> voucherList = new ArrayList<>();

    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_vouchers);

        token = SessionManager.getToken(this);

        if (token.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!SessionManager.isAdmin(this)) {
            Toast.makeText(this,
                    "Bạn không có quyền truy cập",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewVouchers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new VoucherAdapter(this, voucherList,
                new VoucherAdapter.VoucherActionListener() {
                    @Override
                    public void onEdit(Voucher voucher) {
                        showVoucherDialog(voucher);
                    }

                    @Override
                    public void onDelete(Voucher voucher) {
                        confirmDelete(voucher);
                    }
                });

        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnAddVoucher)
                .setOnClickListener(v -> showVoucherDialog(null));

        loadVouchers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVouchers();
    }

    private void loadVouchers() {

        ApiClient.getVouchers(this, token,
                new ApiClient.DataCallback<List<Voucher>>() {

                    @Override
                    public void onSuccess(List<Voucher> data, String message) {
                        voucherList.clear();
                        voucherList.addAll(data);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        if (errorMessage.contains("401")
                                || errorMessage.contains("403")) {

                            SessionManager.clear(ManageVouchersActivity.this);
                            startActivity(new Intent(
                                    ManageVouchersActivity.this,
                                    LoginActivity.class));
                            finish();
                            return;
                        }

                        Toast.makeText(ManageVouchersActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDelete(Voucher voucher) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Voucher")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete",
                        (d, w) -> deleteVoucher(voucher.getVoucherId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVoucher(int id) {

        ApiClient.deleteVoucher(this, token, id,
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {
                        Toast.makeText(ManageVouchersActivity.this,
                                message,
                                Toast.LENGTH_SHORT).show();
                        loadVouchers();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        if (errorMessage.contains("401")
                                || errorMessage.contains("403")) {

                            SessionManager.clear(ManageVouchersActivity.this);
                            startActivity(new Intent(
                                    ManageVouchersActivity.this,
                                    LoginActivity.class));
                            finish();
                            return;
                        }

                        Toast.makeText(ManageVouchersActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showVoucherDialog(Voucher voucher) {

        View view = getLayoutInflater()
                .inflate(R.layout.dialog_voucher_form, null);

        TextInputEditText edtCode = view.findViewById(R.id.edtCode);
        Spinner spDiscountType = view.findViewById(R.id.spDiscountType);
        TextInputEditText edtValue = view.findViewById(R.id.edtValue);
        TextInputEditText edtMinOrder = view.findViewById(R.id.edtMinOrder);
        TextInputEditText edtMaxUsage = view.findViewById(R.id.edtMaxUsage);
        TextInputEditText edtStartDate = view.findViewById(R.id.edtStartDate);
        TextInputEditText edtEndDate = view.findViewById(R.id.edtEndDate);
        SwitchMaterial switchActive = view.findViewById(R.id.switchActive);

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        edtStartDate.setFocusable(false);
        edtEndDate.setFocusable(false);

        edtStartDate.setOnClickListener(v ->
                showDatePicker(edtStartDate, startCalendar));

        edtEndDate.setOnClickListener(v ->
                showDatePicker(edtEndDate, endCalendar));

        String[] discountTypes = {"Percent", "Fixed"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                discountTypes
        );
        spDiscountType.setAdapter(spinnerAdapter);

        if (voucher != null) {

            edtCode.setText(voucher.getCode());
            edtValue.setText(String.valueOf(voucher.getDiscountValue()));
            edtMinOrder.setText(String.valueOf(voucher.getMinOrderValue()));
            edtMaxUsage.setText(String.valueOf(voucher.getMaxUsageCount()));
            switchActive.setChecked(voucher.isActive());

            if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
                spDiscountType.setSelection(1);
            }

            // giữ nguyên chuỗi backend trả về (ISO)
            edtStartDate.setText(voucher.getStartDate());
            edtEndDate.setText(voucher.getExpiryDate());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {

            String code = getText(edtCode);
            String discountType = spDiscountType.getSelectedItem().toString();
            String valueStr = getText(edtValue);
            String minOrderStr = getText(edtMinOrder);
            String maxUsageStr = getText(edtMaxUsage);
            boolean active = switchActive.isChecked();

            if (code.isEmpty() || valueStr.isEmpty()
                    || minOrderStr.isEmpty() || maxUsageStr.isEmpty()) {

                Toast.makeText(this,
                        "Please fill all required fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            double value;
            double minOrder;
            int maxUsage;

            try {
                value = Double.parseDouble(valueStr);
                minOrder = Double.parseDouble(minOrderStr);
                maxUsage = Integer.parseInt(maxUsageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this,
                        "Invalid number format",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat isoFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
            endCalendar.set(Calendar.MILLISECOND, 999);

            String start = isoFormat.format(startCalendar.getTime());
            String end = isoFormat.format(endCalendar.getTime());

            Log.d("DATE_DEBUG", start);
            Log.d("DATE_DEBUG", end);
            Log.d("DATE_DEBUG", start);
            Log.d("DATE_DEBUG", end);
            ApiClient.DataCallback<Void> callback =
                    new ApiClient.DataCallback<Void>() {

                        @Override
                        public void onSuccess(Void data, String message) {
                            Toast.makeText(ManageVouchersActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT).show();
                            loadVouchers();
                        }

                        @Override
                        public void onError(String errorMessage) {

                            if (errorMessage.contains("401")
                                    || errorMessage.contains("403")) {

                                SessionManager.clear(ManageVouchersActivity.this);
                                startActivity(new Intent(
                                        ManageVouchersActivity.this,
                                        LoginActivity.class));
                                finish();
                                return;
                            }

                            Toast.makeText(ManageVouchersActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    };

            if (voucher == null) {

                ApiClient.createVoucher(this, token,
                        code,
                        discountType,
                        value,
                        minOrder,
                        maxUsage,
                        start,
                        end,
                        active,
                        callback);

            } else {

                ApiClient.updateVoucher(this, token,
                        voucher.getVoucherId(),
                        code,
                        discountType,
                        value,
                        minOrder,
                        maxUsage,
                        start,
                        end,
                        active,
                        callback);
            }

            dialog.dismiss();
        });

        view.findViewById(R.id.btnCancel)
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDatePicker(TextInputEditText editText,
                                Calendar calendar) {

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    calendar.set(year, month, dayOfMonth);

                    SimpleDateFormat display =
                            new SimpleDateFormat("dd/MM/yyyy",
                                    Locale.getDefault());

                    editText.setText(
                            display.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private String getText(TextInputEditText edt) {
        return edt.getText() == null
                ? ""
                : edt.getText().toString().trim();
    }
}