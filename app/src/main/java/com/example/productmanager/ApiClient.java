package com.example.productmanager;

import android.content.Context;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ApiClient {
    public interface DataCallback<T> {
        void onSuccess(T data, String message);
        void onError(String errorMessage);
    }

    private static RequestQueue requestQueue;


    private static RequestQueue getQueue(Context context) {
        if (requestQueue == null) {
            if (ApiConfig.BASE_URL.startsWith("https://")) {
                trustLocalDevCertificate();
            }
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    private static void trustLocalDevCertificate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }

                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception ignored) {
        }
    }

    private static String getErrorMessage(VolleyError error) {
        if (error == null) {
            return "Có lỗi xảy ra";
        }
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String body = new String(error.networkResponse.data);
                JSONObject obj = new JSONObject(body);
                return obj.optString("message", "Request thất bại");
            } catch (Exception ignored) {
            }
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return "Không thể kết nối tới server";
    }

    private static Map<String, String> buildAuthHeader(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    public static void login(Context context, String email, String password, DataCallback<String> callback) {
        String url = ApiConfig.BASE_URL + "/auth/login";
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            callback.onError("Dữ liệu đăng nhập không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    String message = response.optString("message", "");
                    if (!success) {
                        callback.onError(message.isEmpty() ? "Đăng nhập thất bại" : message);
                        return;
                    }

                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        callback.onError("Phản hồi login không hợp lệ");
                        return;
                    }

                    String token = data.optString("token", "");
                    if (token.isEmpty()) {
                        callback.onError("Không nhận được token từ server");
                        return;
                    }

                    callback.onSuccess(token, message);
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader("");
            }
        };

        getQueue(context).add(request);
    }

    public static void register(Context context,
                                String fullName,
                                String email,
                                String password,
                                String phone,
                                String address,
                                DataCallback<String> callback) {

        String url = ApiConfig.BASE_URL + "/auth/register";

        JSONObject body = new JSONObject();
        try {
            body.put("fullName", fullName);
            body.put("email", email);
            body.put("password", password);
            body.put("phone", phone);
            body.put("address", address);
        } catch (JSONException e) {
            callback.onError("Dữ liệu đăng ký không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    String message = response.optString("message", "");

                    if (!success) {
                        callback.onError(message.isEmpty() ? "Đăng ký thất bại" : message);
                        return;
                    }

                    callback.onSuccess(null, message.isEmpty() ? "Đăng ký thành công" : message);
                },
                error -> callback.onError(getErrorMessage(error))) {

            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader("");
            }
        };

        getQueue(context).add(request);
    }

    public static void googleLogin(Context context,
                                   String idToken,
                                   DataCallback<String> callback) {

        String url = ApiConfig.BASE_URL + "/auth/google-login";

        JSONObject body = new JSONObject();
        try {
            body.put("idToken", idToken);
        } catch (JSONException e) {
            callback.onError("Không thể tạo request body");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,

                response -> {

                    boolean success = response.optBoolean("success", false);
                    if (!success) {
                        callback.onError(response.optString("message", "Login thất bại"));
                        return;
                    }

                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        callback.onError("Phản hồi không hợp lệ");
                        return;
                    }

                    String jwt = data.optString("token", "");
                    callback.onSuccess(jwt, "Đăng nhập thành công");
                },

                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader("");
            }
        };

        getQueue(context).add(request);
    }

    public static void getProfile(Context context,
                                  String token,
                                  DataCallback<JSONObject> callback) {

        String url = ApiConfig.BASE_URL + "/auth/profile";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Lỗi tải profile"));
                        return;
                    }

                    JSONObject data = response.optJSONObject("data");
                    callback.onSuccess(data, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {

            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getProducts(Context context, String token, String keyword, DataCallback<List<Product>> callback) {
        String url = ApiConfig.BASE_URL + "/products";
        if (keyword != null && !keyword.trim().isEmpty()) {
            url = ApiConfig.BASE_URL + "/products/search?keyword=" + keyword.trim().replace(" ", "%20");
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải sản phẩm thất bại"));
                        return;
                    }

                    JSONArray data = response.optJSONArray("data");
                    List<Product> products = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj == null) {
                                continue;
                            }
                            products.add(new Product(
                                    obj.optInt("productId"),
                                    obj.optString("productName"),
                                    obj.optString("description"),
                                    obj.optDouble("price", 0),
                                    obj.optString("imageUrl", null)
                            ));
                        }
                    }
                    callback.onSuccess(products, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getProductById(Context context, String token, int productId, DataCallback<Product> callback) {
        String url = ApiConfig.BASE_URL + "/products/" + productId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Không tìm thấy sản phẩm"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        callback.onError("Không có dữ liệu sản phẩm");
                        return;
                    }
                    Product product = new Product(
                            data.optInt("productId"),
                            data.optString("productName"),
                            data.optString("description"),
                            data.optDouble("price", 0),
                            data.optString("imageUrl", null)
                    );
                    callback.onSuccess(product, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void createProduct(Context context, String token, String name, String description,
                                     double price, String imageUrl, DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/products";
        JSONObject body = new JSONObject();
        try {
            body.put("productName", name);
            body.put("description", description);
            body.put("price", price);
            body.put("stockQuantity", 10);
            body.put("imageUrl", imageUrl == null ? JSONObject.NULL : imageUrl);
            body.put("categoryId", 1);
        } catch (JSONException e) {
            callback.onError("Dữ liệu sản phẩm không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Tạo sản phẩm thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Thêm sản phẩm thành công"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void updateProduct(Context context, String token, int productId, String name,
                                     String description, double price, String imageUrl,
                                     DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/products/" + productId;
        JSONObject body = new JSONObject();
        try {
            body.put("productName", name);
            body.put("description", description);
            body.put("price", price);
            body.put("imageUrl", imageUrl == null ? JSONObject.NULL : imageUrl);
        } catch (JSONException e) {
            callback.onError("Dữ liệu sản phẩm không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Cập nhật sản phẩm thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Cập nhật sản phẩm thành công"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void deleteProduct(Context context, String token, int productId, DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/products/" + productId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Xóa sản phẩm thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Xóa sản phẩm thành công"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getCart(Context context, String token, DataCallback<List<CartItem>> callback) {
        String url = ApiConfig.BASE_URL + "/carts";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải giỏ hàng thất bại"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    JSONArray items = data != null ? data.optJSONArray("items") : null;

                    List<CartItem> result = new ArrayList<>();
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject obj = items.optJSONObject(i);
                            if (obj == null) {
                                continue;
                            }
                            result.add(new CartItem(
                                    obj.optInt("cartItemId"),
                                    obj.optInt("productId"),
                                    obj.optString("productName"),
                                    obj.optString("imageUrl", null),
                                    obj.optDouble("price", 0),
                                    obj.optInt("quantity", 1)
                            ));
                        }
                    }

                    callback.onSuccess(result, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void addToCart(Context context, String token, int productId, int quantity, DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/carts/items";
        JSONObject body = new JSONObject();
        try {
            body.put("productId", productId);
            body.put("quantity", quantity);
        } catch (JSONException e) {
            callback.onError("Dữ liệu giỏ hàng không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Thêm giỏ hàng thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Đã thêm vào giỏ hàng"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void removeCartItem(Context context, String token, int cartItemId, DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/carts/items/" + cartItemId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Xóa sản phẩm khỏi giỏ thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Đã xóa khỏi giỏ hàng"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void clearCart(Context context, String token, DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/carts";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Xóa giỏ hàng thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Đã xóa giỏ hàng"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void createOrder(Context context,
                                   String token,
                                   String shippingAddress,
                                   String phone,
                                   String receiverName,
                                   String paymentMethod,
                                   DataCallback<JSONObject> callback) {
        String url = ApiConfig.BASE_URL + "/orders";

        JSONObject body = new JSONObject();
        try {
            body.put("shippingAddress", shippingAddress);
            body.put("phone", phone);
            body.put("receiverName", receiverName);
            body.put("paymentMethod", paymentMethod);
        } catch (JSONException e) {
            callback.onError("Du lieu tao don hang khong hop le");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Tao don hang that bai"));
                        return;
                    }

                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        callback.onError("Khong nhan duoc thong tin don hang");
                        return;
                    }

                    callback.onSuccess(data, response.optString("message", "Tao don hang thanh cong"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void updateCartQuantity(Context context,
                                          String token,
                                          int cartItemId,
                                          int quantity,
                                          DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/Carts/items/" + cartItemId;

        JSONObject body = new JSONObject();
        try {
            body.put("quantity", quantity);
        } catch (JSONException e) {
            callback.onError("Dữ liệu số lượng không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message",
                                "Cập nhật số lượng thất bại"));
                        return;
                    }

                    callback.onSuccess(null,
                            response.optString("message",
                                    "Đã cập nhật số lượng"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        getQueue(context).add(request);
    }

    public static void getVouchers(Context context,
                                   String token,
                                   DataCallback<List<Voucher>> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải voucher thất bại"));
                        return;
                    }

                    JSONArray data = response.optJSONArray("data");
                    List<Voucher> vouchers = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {

                            JSONObject obj = data.optJSONObject(i);
                            if (obj == null) continue;

                            vouchers.add(new Voucher(
                                    obj.optInt("voucherId"),
                                    obj.optString("code"),
                                    obj.optString("discountType"),
                                    obj.optDouble("discountValue"),
                                    obj.optDouble("minOrderValue"),
                                    obj.optInt("maxUsageCount"),
                                    obj.optString("startDate"),
                                    obj.optString("expiryDate"),
                                    obj.optBoolean("isActive")
                            ));
                        }
                    }

                    callback.onSuccess(vouchers, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getVoucherById(Context context,
                                      String token,
                                      int voucherId,
                                      DataCallback<Voucher> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers/" + voucherId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Không tìm thấy voucher"));
                        return;
                    }

                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        callback.onError("Không có dữ liệu voucher");
                        return;
                    }

                    Voucher voucher = new Voucher(
                            data.optInt("voucherId"),
                            data.optString("code"),
                            data.optString("discountType"),
                            data.optDouble("discountValue"),
                            data.optDouble("minOrderValue"),
                            data.optInt("maxUsageCount"),
                            data.optString("startDate"),
                            data.optString("expiryDate"),
                            data.optBoolean("isActive")
                    );

                    callback.onSuccess(voucher, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getVoucherByCode(Context context,
                                        String token,
                                        String code,
                                        DataCallback<Voucher> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers/by-code/" + code;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Voucher không hợp lệ"));
                        return;
                    }

                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        callback.onError("Không có dữ liệu voucher");
                        return;
                    }

                    Voucher voucher = new Voucher(
                            data.optInt("voucherId"),
                            data.optString("code"),
                            data.optString("discountType"),
                            data.optDouble("discountValue"),
                            data.optDouble("minOrderValue"),
                            data.optInt("maxUsageCount"),
                            data.optString("startDate"),
                            data.optString("expiryDate"),
                            data.optBoolean("isActive")
                    );

                    callback.onSuccess(voucher, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void createVoucher(Context context,
                                     String token,
                                     String code,
                                     String discountType,
                                     double discountValue,
                                     double minOrderValue,
                                     int maxUsageCount,
                                     String startDate,
                                     String expiryDate,
                                     boolean isActive,
                                     DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers";

        JSONObject body = new JSONObject();
        try {
            body.put("code", code);
            body.put("discountType", discountType);
            body.put("discountValue", discountValue);
            body.put("minOrderValue", minOrderValue);
            body.put("maxUsageCount", maxUsageCount);
            body.put("startDate", startDate);
            body.put("expiryDate", expiryDate);
            body.put("isActive", isActive);
        } catch (JSONException e) {
            callback.onError("Dữ liệu voucher không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {

                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Tạo voucher thất bại"));
                        return;
                    }

                    callback.onSuccess(null,
                            response.optString("message", "Tạo voucher thành công"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void updateVoucher(Context context,
                                     String token,
                                     int voucherId,
                                     String code,
                                     String discountType,
                                     double discountValue,
                                     double minOrderValue,
                                     int maxUsageCount,
                                     String startDate,
                                     String expiryDate,
                                     boolean isActive,
                                     DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers/" + voucherId;

        JSONObject body = new JSONObject();
        try {
            body.put("code", code);
            body.put("discountType", discountType);
            body.put("discountValue", discountValue);
            body.put("minOrderValue", minOrderValue);
            body.put("maxUsageCount", maxUsageCount);
            body.put("startDate", startDate);
            body.put("expiryDate", expiryDate);
            body.put("isActive", isActive);
        } catch (JSONException e) {
            callback.onError("Dữ liệu voucher không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                response -> {

                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Cập nhật voucher thất bại"));
                        return;
                    }

                    callback.onSuccess(null,
                            response.optString("message", "Cập nhật voucher thành công"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void deleteVoucher(Context context,
                                     String token,
                                     int voucherId,
                                     DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers/" + voucherId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Xóa voucher thất bại"));
                        return;
                    }

                    callback.onSuccess(null,
                            response.optString("message", "Xóa voucher thành công"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getAvailableVouchers(Context context,
                                            String token,
                                            DataCallback<List<Voucher>> callback) {

        String url = ApiConfig.BASE_URL + "/Vouchers/available";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải voucher thất bại"));
                        return;
                    }

                    JSONArray data = response.optJSONArray("data");
                    List<Voucher> vouchers = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {

                            JSONObject obj = data.optJSONObject(i);
                            if (obj == null) continue;

                            vouchers.add(new Voucher(
                                    obj.optInt("voucherId"),
                                    obj.optString("code"),
                                    obj.optString("discountType"),
                                    obj.optDouble("discountValue"),
                                    obj.optDouble("minOrderValue"),
                                    obj.optInt("maxUsageCount"),
                                    obj.optString("startDate"),
                                    obj.optString("expiryDate"),
                                    obj.optBoolean("isActive")
                            ));
                        }
                    }

                    callback.onSuccess(vouchers, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getAllOrders(Context context,
                                    String token,
                                    DataCallback<List<Order>> callback) {

        String url = ApiConfig.BASE_URL + "/orders/all";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải đơn hàng thất bại"));
                        return;
                    }

                    JSONArray data = response.optJSONArray("data");
                    List<Order> orders = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj == null) continue;

                            orders.add(new Order(
                                    obj.optInt("orderId"),
                                    obj.optInt("userId"),
                                    obj.optDouble("totalAmount", 0),
                                    obj.optString("status"),
                                    obj.optString("shippingAddress", null),
                                    obj.optString("phone", null),
                                    obj.optString("receiverName", null),
                                    obj.optString("createdAt", ""),
                                    obj.optString("paymentMethod", null),
                                    obj.optString("paymentStatus", null)
                            ));
                        }
                    }

                    callback.onSuccess(orders, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void getMyOrders(Context context,
                                   String token,
                                   DataCallback<List<Order>> callback) {

        String url = ApiConfig.BASE_URL + "/orders";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải đơn hàng thất bại"));
                        return;
                    }

                    JSONArray data = response.optJSONArray("data");
                    List<Order> orders = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj == null) continue;

                            orders.add(new Order(
                                    obj.optInt("orderId"),
                                    obj.optInt("userId"),
                                    obj.optDouble("totalAmount", 0),
                                    obj.optString("status"),
                                    obj.optString("shippingAddress", null),
                                    obj.optString("phone", null),
                                    obj.optString("receiverName", null),
                                    obj.optString("createdAt", ""),
                                    obj.optString("paymentMethod", null),
                                    obj.optString("paymentStatus", null)
                            ));
                        }
                    }

                    callback.onSuccess(orders, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void updateOrderStatus(Context context,
                                         String token,
                                         int orderId,
                                         String status,
                                         DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/orders/" + orderId + "/status?status=" + status;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                null,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Cập nhật trạng thái thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Cập nhật thành công"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void cancelOrder(Context context,
                                   String token,
                                   int orderId,
                                   DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/orders/" + orderId + "/cancel";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                null,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Huỷ đơn thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Đã huỷ đơn hàng"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    public static void confirmDelivery(Context context,
                                       String token,
                                       int orderId,
                                       DataCallback<Void> callback) {

        String url = ApiConfig.BASE_URL + "/orders/" + orderId + "/confirm-delivery";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                null,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Xác nhận nhận hàng thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Đã xác nhận nhận hàng"));
                },
                error -> callback.onError(getErrorMessage(error))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        getQueue(context).add(request);
    }

    // ============================================================
    // FEEDBACK
    // ============================================================

    private static String optNullableString(JSONObject obj, String key) {
        return obj.isNull(key) ? null : obj.optString(key, null);
    }

    private static Feedback parseFeedback(JSONObject obj) {
        return new Feedback(
                obj.optInt("feedbackId"),
                obj.optInt("userId"),
                obj.optString("userFullName", ""),
                obj.optInt("productId"),
                obj.optString("productName", ""),
                obj.optInt("rating"),
                optNullableString(obj, "comment"),
                optNullableString(obj, "adminReply"),
                optNullableString(obj, "adminReplyAt"),
                obj.optString("createdAt", ""),
                optNullableString(obj, "updatedAt")
        );
    }

    public static void getAllFeedbacks(Context context, String token,
                                       DataCallback<List<Feedback>> callback) {
        String url = ApiConfig.BASE_URL + "/feedbacks";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray data = response.optJSONArray("data");
                    List<Feedback> list = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj != null) list.add(parseFeedback(obj));
                        }
                    }
                    callback.onSuccess(list, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void getMyFeedbacks(Context context, String token,
                                       DataCallback<List<Feedback>> callback) {
        String url = ApiConfig.BASE_URL + "/feedbacks/me";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray data = response.optJSONArray("data");
                    List<Feedback> list = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj != null) list.add(parseFeedback(obj));
                        }
                    }
                    callback.onSuccess(list, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void getFeedbacksByProduct(Context context, int productId,
                                              DataCallback<List<Feedback>> callback) {
        String url = ApiConfig.BASE_URL + "/feedbacks/product/" + productId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray data = response.optJSONArray("data");
                    List<Feedback> list = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj != null) list.add(parseFeedback(obj));
                        }
                    }
                    callback.onSuccess(list, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader("");
            }
        };
        getQueue(context).add(request);
    }

    public static void createFeedback(Context context, String token,
                                       int productId, int rating, String comment,
                                       DataCallback<Feedback> callback) {
        String url = ApiConfig.BASE_URL + "/feedbacks";
        JSONObject body = new JSONObject();
        try {
            body.put("productId", productId);
            body.put("rating", rating);
            body.put("comment", comment != null ? comment : JSONObject.NULL);
        } catch (JSONException e) {
            callback.onError("Dữ liệu đánh giá không hợp lệ");
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Gửi đánh giá thất bại"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    callback.onSuccess(data != null ? parseFeedback(data) : null,
                            response.optString("message", "Đã gửi đánh giá"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void updateFeedback(Context context, String token,
                                       int feedbackId, int rating, String comment,
                                       DataCallback<Feedback> callback) {
        String url = ApiConfig.BASE_URL + "/feedbacks/" + feedbackId;
        JSONObject body = new JSONObject();
        try {
            body.put("rating", rating);
            body.put("comment", comment != null ? comment : JSONObject.NULL);
        } catch (JSONException e) {
            callback.onError("Dữ liệu đánh giá không hợp lệ");
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Cập nhật đánh giá thất bại"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    callback.onSuccess(data != null ? parseFeedback(data) : null,
                            response.optString("message", "Đã cập nhật đánh giá"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void adminReplyFeedback(Context context, String token,
                                           int feedbackId, String reply,
                                           DataCallback<Feedback> callback) {
        String url = ApiConfig.BASE_URL + "/feedbacks/" + feedbackId + "/admin-reply";
        JSONObject body = new JSONObject();
        try {
            body.put("adminReply", reply);
        } catch (JSONException e) {
            callback.onError("Dữ liệu phản hồi không hợp lệ");
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Lưu phản hồi thất bại"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    callback.onSuccess(data != null ? parseFeedback(data) : null,
                            response.optString("message", "Đã lưu phản hồi"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void getOrderById(Context context, String token, int orderId,
                                     DataCallback<List<OrderItem>> callback) {
        String url = ApiConfig.BASE_URL + "/orders/" + orderId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    JSONObject data = response.optJSONObject("data");
                    JSONArray items = data != null ? data.optJSONArray("items") : null;
                    List<OrderItem> list = new ArrayList<>();
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject obj = items.optJSONObject(i);
                            if (obj == null) continue;
                            list.add(new OrderItem(
                                    obj.optInt("orderItemId"),
                                    obj.optInt("productId"),
                                    obj.optString("productName", ""),
                                    obj.optString("imageUrl", null),
                                    obj.optInt("quantity", 1),
                                    obj.optDouble("unitPrice", 0)
                            ));
                        }
                    }
                    callback.onSuccess(list, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    // ===== CHAT API =====

    public static void sendMessage(Context context, String token, int receiverId, String content,
                                   DataCallback<JSONObject> callback) {
        String url = ApiConfig.BASE_URL + "/Chat";
        JSONObject body = new JSONObject();
        try {
            body.put("receiverId", receiverId);
            body.put("content", content);
        } catch (JSONException e) {
            callback.onError("Dữ liệu tin nhắn không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Gửi tin nhắn thất bại"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    callback.onSuccess(data, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void getConversation(Context context, String token, int otherUserId,
                                       DataCallback<JSONArray> callback) {
        String url = ApiConfig.BASE_URL + "/Chat/conversation/" + otherUserId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải cuộc trò chuyện thất bại"));
                        return;
                    }
                    JSONArray data = response.optJSONArray("data");
                    callback.onSuccess(data != null ? data : new JSONArray(),
                            response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void getConversationPartners(Context context, String token,
                                               DataCallback<JSONArray> callback) {
        String url = ApiConfig.BASE_URL + "/Chat/partners";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Tải danh sách chat thất bại"));
                        return;
                    }
                    JSONArray data = response.optJSONArray("data");
                    callback.onSuccess(data != null ? data : new JSONArray(),
                            response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    public static void getAdminUser(Context context, String token,
                                    DataCallback<JSONObject> callback) {
        String url = ApiConfig.BASE_URL + "/Chat/admin";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!response.optBoolean("success", true)) {
                        callback.onError(response.optString("message", "Không tìm thấy admin"));
                        return;
                    }
                    JSONObject data = response.optJSONObject("data");
                    callback.onSuccess(data, response.optString("message", ""));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };
        getQueue(context).add(request);
    }

    // ===== PROFILE UPDATE =====

    public static void updateMyProfile(Context context, String token, String avatarUrl,
                                       DataCallback<Void> callback) {
        String url = ApiConfig.BASE_URL + "/Users/me";
        JSONObject body = new JSONObject();
        try {
            body.put("avatarUrl", avatarUrl);
        } catch (JSONException e) {
            callback.onError("Dữ liệu không hợp lệ");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
                response -> {
                    if (!response.optBoolean("success", false)) {
                        callback.onError(response.optString("message", "Cập nhật thất bại"));
                        return;
                    }
                    callback.onSuccess(null, response.optString("message", "Cập nhật thành công"));
                },
                error -> callback.onError(getErrorMessage(error))) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeader(token);
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        getQueue(context).add(request);
    }
}
