package com.example.productmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

public class StoreLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvStoreName, tvStoreAddress, tvStorePhone;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_location);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStoreName = findViewById(R.id.tvStoreName);
        tvStoreAddress = findViewById(R.id.tvStoreAddress);
        tvStorePhone = findViewById(R.id.tvStorePhone);

        authToken = SessionManager.getToken(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadStoreLocation();
    }

    private void loadStoreLocation() {
        ApiClient.getStoreLocations(this, authToken, new ApiClient.DataCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data, String message) {
                if (data.length() == 0) {
                    tvStoreName.setText("Chưa có vị trí cửa hàng");
                    tvStoreAddress.setVisibility(View.GONE);
                    tvStorePhone.setVisibility(View.GONE);
                    return;
                }

                JSONObject store = null;
                for (int i = 0; i < data.length(); i++) {
                    JSONObject candidate = data.optJSONObject(i);
                    if (candidate != null && candidate.optBoolean("isActive", false)) {
                        store = candidate;
                        break;
                    }
                }

                if (store == null) {
                    store = data.optJSONObject(0);
                }

                if (store == null) return;

                String name = store.optString("storeName", "Cửa hàng");
                String address = store.optString("address", "");
                String phone = store.optString("phone", "");
                double lat = store.optDouble("latitude", 0);
                double lng = store.optDouble("longitude", 0);

                tvStoreName.setText(name);
                tvStoreAddress.setText(address.isEmpty() ? "Chưa có địa chỉ" : address);

                if (phone.isEmpty()) {
                    tvStorePhone.setVisibility(View.GONE);
                } else {
                    tvStorePhone.setText(phone);
                }

                if (lat != 0 && lng != 0 && mMap != null) {
                    LatLng storeLatLng = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions()
                            .position(storeLatLng)
                            .title(name));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(storeLatLng, 15f));
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(StoreLocationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
