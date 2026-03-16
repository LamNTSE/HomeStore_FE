package com.example.productmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class UserAdapter extends ArrayAdapter<JSONObject> {

    public interface UserActionListener {
        void onEditUser(JSONObject user);
        void onDeleteUser(JSONObject user);
        void onViewUser(JSONObject user);
    }

    private final JSONArray users;
    private final UserActionListener listener;

    public UserAdapter(Context context, JSONArray users, UserActionListener listener) {
        super(context, R.layout.item_user);
        this.users = users;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return users.length();
    }

    @Override
    public JSONObject getItem(int position) {
        return users.optJSONObject(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
        }

        JSONObject user = getItem(position);
        if (user == null) return convertView;

        TextView tvName = convertView.findViewById(R.id.tvUserName);
        TextView tvEmail = convertView.findViewById(R.id.tvUserEmail);
        TextView tvRole = convertView.findViewById(R.id.tvUserRole);
        ImageView btnEdit = convertView.findViewById(R.id.btnEditUser);
        ImageView btnDelete = convertView.findViewById(R.id.btnDeleteUser);

        tvName.setText(user.optString("fullName", ""));
        tvEmail.setText(user.optString("email", ""));

        String role = user.optString("role", "Customer");
        tvRole.setText(role);

        if ("Admin".equalsIgnoreCase(role)) {
            tvRole.setBackgroundColor(0xFFE53935);
        } else {
            tvRole.setBackgroundColor(0xFF4CAF50);
        }

        btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditUser(user);
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteUser(user);
        });

        convertView.setOnClickListener(v -> {
            if (listener != null) listener.onViewUser(user);
        });

        return convertView;
    }
}
