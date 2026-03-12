package com.example.productmanager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;

/**
 * Singleton that manages real-time SignalR hub connections.
 *
 * Usage:
 *   SignalRManager.getInstance().connectChat(token);
 *   SignalRManager.getInstance().setChatMessageListener(json -> { ... });
 *   SignalRManager.getInstance().disconnectAll(); // on logout
 */
public class SignalRManager {

    private static final String TAG = "SignalRManager";

    // ─── Singleton ───────────────────────────────────────────────────────────
    private static volatile SignalRManager instance;

    public static SignalRManager getInstance() {
        if (instance == null) {
            synchronized (SignalRManager.class) {
                if (instance == null) instance = new SignalRManager();
            }
        }
        return instance;
    }

    private SignalRManager() {}

    // ─── Internal state ───────────────────────────────────────────────────────
    private HubConnection chatConnection;
    private HubConnection orderConnection;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    // ─── Listener interfaces ─────────────────────────────────────────────────
    public interface ChatMessageListener {
        /** Called on the main thread with the received message as a JSON string. */
        void onMessageReceived(String messageJson);
    }

    public interface OrderUpdateListener {
        /** Called on the main thread when an order's status changes. */
        void onOrderStatusUpdated(String orderJson);
    }

    public interface NewOrderListener {
        /** Called on the main thread when a new order is created (admin only). */
        void onNewOrderCreated(String orderJson);
    }

    private ChatMessageListener chatMessageListener;
    private OrderUpdateListener orderUpdateListener;
    private NewOrderListener newOrderListener;

    public void setChatMessageListener(ChatMessageListener listener) {
        this.chatMessageListener = listener;
    }

    public void setOrderUpdateListener(OrderUpdateListener listener) {
        this.orderUpdateListener = listener;
    }

    public void setNewOrderListener(NewOrderListener listener) {
        this.newOrderListener = listener;
    }

    // ─── Chat Hub ────────────────────────────────────────────────────────────

    public void connectChat(String token) {
        if (chatConnection != null
                && chatConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            return;
        }

        String url = ApiConfig.SIGNALR_BASE_URL + "/hubs/chat";
        chatConnection = HubConnectionBuilder.create(url)
                .withAccessTokenProvider(Single.just(token))
                .setHttpClientBuilderCallback(this::applyDevSslConfig)
                .build();

        chatConnection.on("ReceiveMessage", (msg) -> {
            if (chatMessageListener != null) {
                String json = gson.toJson(msg);
                mainHandler.post(() -> chatMessageListener.onMessageReceived(json));
            }
        }, Object.class);

        chatConnection.start()
                .subscribe(
                        () -> Log.d(TAG, "Chat hub connected"),
                        err -> Log.e(TAG, "Chat hub error: " + err.getMessage()));
    }

    public void disconnectChat() {
        if (chatConnection != null) {
            chatConnection.stop().subscribe(
                    () -> Log.d(TAG, "Chat hub disconnected"),
                    err -> Log.e(TAG, "Chat hub stop error: " + err.getMessage()));
            chatConnection = null;
        }
    }

    // ─── Order Hub ───────────────────────────────────────────────────────────

    public void connectOrders(String token) {
        if (orderConnection != null
                && orderConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            return;
        }

        String url = ApiConfig.SIGNALR_BASE_URL + "/hubs/orders";
        orderConnection = HubConnectionBuilder.create(url)
                .withAccessTokenProvider(Single.just(token))
                .setHttpClientBuilderCallback(this::applyDevSslConfig)
                .build();

        orderConnection.on("OrderStatusUpdated", (order) -> {
            if (orderUpdateListener != null) {
                String json = gson.toJson(order);
                mainHandler.post(() -> orderUpdateListener.onOrderStatusUpdated(json));
            }
        }, Object.class);

        orderConnection.on("NewOrderCreated", (order) -> {
            if (newOrderListener != null) {
                String json = gson.toJson(order);
                mainHandler.post(() -> newOrderListener.onNewOrderCreated(json));
            }
        }, Object.class);

        orderConnection.start()
                .subscribe(
                        () -> Log.d(TAG, "Order hub connected"),
                        err -> Log.e(TAG, "Order hub error: " + err.getMessage()));
    }

    public void disconnectOrders() {
        if (orderConnection != null) {
            orderConnection.stop().subscribe(
                    () -> Log.d(TAG, "Order hub disconnected"),
                    err -> Log.e(TAG, "Order hub stop error: " + err.getMessage()));
            orderConnection = null;
        }
    }

    /** Stops both hub connections. Call on user logout. */
    public void disconnectAll() {
        disconnectChat();
        disconnectOrders();
    }

    // ─── SSL helper (dev only – trusts all certs for local HTTPS) ────────────

    private OkHttpClient.Builder applyDevSslConfig(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] c, String a) throws CertificateException {}
                @Override public void checkServerTrusted(X509Certificate[] c, String a) throws CertificateException {}
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustAll}, new java.security.SecureRandom());
            builder.sslSocketFactory(sc.getSocketFactory(), trustAll);
            builder.hostnameVerifier((host, session) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "SSL config failed: " + e.getMessage());
        }
        return builder;
    }
}
