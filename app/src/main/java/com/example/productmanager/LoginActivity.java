package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin, btnGoogleLogin;
    TextView txtRegister;

    GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        txtRegister = findViewById(R.id.txtRegister);

        // Auto login nếu đã có token
        if (SessionManager.isLoggedIn(this)) {
            redirectByRole(SessionManager.getToken(this));
            return;
        }

        setupGoogleLogin();
        setupEvents();
    }

    private void setupEvents() {

        btnLogin.setOnClickListener(view -> {

            String email = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);

            ApiClient.login(this, email, password, new ApiClient.DataCallback<String>() {
                @Override
                public void onSuccess(String token, String message) {
                    btnLogin.setEnabled(true);
                    SessionManager.saveToken(LoginActivity.this, token);
                    redirectByRole(token);
                }

                @Override
                public void onError(String errorMessage) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnGoogleLogin.setOnClickListener(v -> {
            btnGoogleLogin.setEnabled(false);
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        txtRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void setupGoogleLogin() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_SIGN_IN) return;

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            String idToken = account.getIdToken();
            googleSignInClient.signOut();

            loginWithGoogleToServer(idToken);

        } catch (Exception e) {
            btnGoogleLogin.setEnabled(true);
            Toast.makeText(this, "Google login thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithGoogleToServer(String idToken) {

        ApiClient.googleLogin(this, idToken, new ApiClient.DataCallback<String>() {

            @Override
            public void onSuccess(String token, String message) {

                btnGoogleLogin.setEnabled(true);

                if (token == null || token.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "Server trả về token rỗng",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String role = getRoleFromToken(token);

                // ❌ Chặn admin login bằng Google
                if ("Admin".equalsIgnoreCase(role)) {
                    Toast.makeText(LoginActivity.this,
                            "Admin không được đăng nhập bằng Google",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                SessionManager.saveToken(LoginActivity.this, token);
                redirectByRole(token);
            }

            @Override
            public void onError(String error) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== PHÂN QUYỀN =====
    private void redirectByRole(String token) {
        String role = getRoleFromToken(token);

        if ("Admin".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, AdminHomeActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }

    private String getRoleFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";

            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);

            // Thử nhiều key khác nhau
            if (json.has("role"))
                return json.getString("role");

            if (json.has("roles"))
                return json.getString("roles");

            if (json.has("http://schemas.microsoft.com/ws/2008/06/identity/claims/role"))
                return json.getString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role");

            return "";

        } catch (Exception e) {
            return "";
        }
    }
    private void goToMain() {
        String token = SessionManager.getToken(this);

        if (token == null || token.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String role = getRoleFromToken(token);

        if ("Admin".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, AdminHomeActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }
}

