package com.example.productmanager;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AdminStoreLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker currentMarker;

    private TextInputEditText edtStoreName, edtStoreAddress, edtStorePhone;
    private TextView tvCoordinates;
    private Button btnSave;

    private String authToken;
    private int existingLocationId = -1;
    private double selectedLat = 0;
    private double selectedLng = 0;
    private final Handler geocodeHandler = new Handler(Looper.getMainLooper());
    private Runnable geocodeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_store_location);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtStoreName = findViewById(R.id.edtStoreName);
        edtStoreAddress = findViewById(R.id.edtStoreAddress);
        edtStorePhone = findViewById(R.id.edtStorePhone);
        tvCoordinates = findViewById(R.id.tvCoordinates);
        btnSave = findViewById(R.id.btnSaveStore);

        authToken = SessionManager.getToken(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Forward geocode: address text → map pin
        edtStoreAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                geocodeAddress();
                return true;
            }
            return false;
        });

        edtStoreAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!edtStoreAddress.hasFocus()) {
                    return;
                }

                if (geocodeRunnable != null) {
                    geocodeHandler.removeCallbacks(geocodeRunnable);
                }

                geocodeRunnable = () -> geocodeAddress(false);
                geocodeHandler.postDelayed(geocodeRunnable, 800);
            }
        });

        btnSave.setOnClickListener(v -> saveStoreLocation());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Map tap → move pin + reverse geocode
        mMap.setOnMapClickListener(latLng -> {
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
            updateMarker(latLng);
            reverseGeocode(latLng);
        });

        loadExistingStore();
    }

    // ── LOAD EXISTING ───────────────────────────────────────────────────────

    private void loadExistingStore() {
        ApiClient.getStoreLocations(this, authToken, new ApiClient.DataCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data, String message) {
                if (data.length() == 0) {
                    // Default location: Hà Nội
                    LatLng defaultLoc = new LatLng(21.0285, 105.8542);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f));
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

                existingLocationId = store.optInt("locationId", -1);
                edtStoreName.setText(store.optString("storeName", ""));
                edtStoreAddress.setText(store.optString("address", ""));
                edtStorePhone.setText(store.optString("phone", ""));

                selectedLat = store.optDouble("latitude", 0);
                selectedLng = store.optDouble("longitude", 0);

                if (selectedLat != 0 && selectedLng != 0) {
                    LatLng pos = new LatLng(selectedLat, selectedLng);
                    updateMarker(pos);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
                }

                updateCoordinatesText();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AdminStoreLocationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── MAP MARKER ──────────────────────────────────────────────────────────

    private void updateMarker(LatLng latLng) {
        if (currentMarker != null) currentMarker.remove();

        currentMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Vị trí cửa hàng")
                .draggable(true));

        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        updateCoordinatesText();
    }

    private void updateCoordinatesText() {
        tvCoordinates.setText(String.format(Locale.US, "Tọa độ: %.6f, %.6f", selectedLat, selectedLng));
    }

    // ── GEOCODING ───────────────────────────────────────────────────────────

    private void geocodeAddress() {
        geocodeAddress(true);
    }

    private void geocodeAddress(boolean showErrorToast) {
        String addressText = edtStoreAddress.getText() != null
                ? edtStoreAddress.getText().toString().trim() : "";

        if (addressText.length() < 3) {
            if (showErrorToast) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                selectedLat = addr.getLatitude();
                selectedLng = addr.getLongitude();

                LatLng pos = new LatLng(selectedLat, selectedLng);
                updateMarker(pos);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
                updateCoordinatesText();
            } else {
                if (showErrorToast) {
                    Toast.makeText(this, "Không tìm thấy vị trí cho địa chỉ này", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            if (showErrorToast) {
                Toast.makeText(this, "Lỗi geocoding: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void reverseGeocode(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(addr.getAddressLine(i));
                }
                edtStoreAddress.setText(sb.toString());
            }
        } catch (IOException e) {
            // Silently ignore reverse geocoding errors
        }
    }

    // ── SAVE ────────────────────────────────────────────────────────────────

    private void saveStoreLocation() {
        String name = edtStoreName.getText() != null ? edtStoreName.getText().toString().trim() : "";
        String address = edtStoreAddress.getText() != null ? edtStoreAddress.getText().toString().trim() : "";
        String phone = edtStorePhone.getText() != null ? edtStorePhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên cửa hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLat == 0 && selectedLng == 0) {
            Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ hoặc nhập địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("storeName", name);
            body.put("address", address);
            body.put("latitude", selectedLat);
            body.put("longitude", selectedLng);
            if (!phone.isEmpty()) body.put("phone", phone);
        } catch (JSONException e) {
            Toast.makeText(this, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (existingLocationId > 0) {
            // Update existing
            ApiClient.updateStoreLocation(this, authToken, existingLocationId, body,
                    new ApiClient.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data, String message) {
                            Toast.makeText(AdminStoreLocationActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(AdminStoreLocationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Create new
            ApiClient.createStoreLocation(this, authToken, body,
                    new ApiClient.DataCallback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject data, String message) {
                            Toast.makeText(AdminStoreLocationActivity.this, message, Toast.LENGTH_SHORT).show();
                            if (data != null) {
                                existingLocationId = data.optInt("locationId", -1);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(AdminStoreLocationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
