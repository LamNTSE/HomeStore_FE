package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

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

        if (SessionManager.isLoggedIn(this)) {
            goToMain();
            return;
        }

        setupGoogleLogin();
        setupEvents();
    }

    private void setupEvents() {

        // ===== LOGIN THƯỜNG =====
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
                    goToMain();
                }

                @Override
                public void onError(String errorMessage) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // ===== GOOGLE LOGIN =====
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
                .requestIdToken(getString(R.string.default_web_client_id)) // phải là WEB CLIENT ID
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_SIGN_IN) return;

        if (data == null) {
            btnGoogleLogin.setEnabled(true);
            Toast.makeText(this, "Google login bị hủy", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            if (account == null) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(this, "Không lấy được tài khoản Google", Toast.LENGTH_SHORT).show();
                return;
            }
            assert account.getIdToken() != null;
            Log.d("ID_TOKEN", account.getIdToken());

            String idToken = account.getIdToken();

            if (idToken == null || idToken.isEmpty()) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(this, "Không lấy được ID Token", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("GOOGLE_ID_TOKEN", idToken);

            // Sign out để tránh auto login lại
            googleSignInClient.signOut();

            loginWithGoogleToServer(idToken);

        } catch (ApiException e) {
            btnGoogleLogin.setEnabled(true);
            Log.e("GOOGLE_LOGIN_ERROR", "Status: " + e.getStatusCode(), e);
            Toast.makeText(this,
                    "Google login thất bại: " + e.getStatusCode(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ===== GỬI TOKEN LÊN SERVER =====
    private void loginWithGoogleToServer(String idToken) {

        ApiClient.googleLogin(this, idToken, new ApiClient.DataCallback<String>() {

            @Override
            public void onSuccess(String token, String message) {

                if (isFinishing() || isDestroyed()) return;

                btnGoogleLogin.setEnabled(true);

                if (token == null || token.trim().isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "Server trả về token rỗng",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d("JWT_TOKEN_FROM_SERVER", token);

                SessionManager.saveToken(LoginActivity.this, token);
                goToMain();
            }

            @Override
            public void onError(String error) {

                if (isFinishing() || isDestroyed()) return;

                btnGoogleLogin.setEnabled(true);

                Log.e("GOOGLE_LOGIN_SERVER_ERROR", error);

                Toast.makeText(LoginActivity.this,
                        error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}