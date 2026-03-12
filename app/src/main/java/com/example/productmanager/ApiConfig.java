package com.example.productmanager;

public class ApiConfig {
    private ApiConfig() {}

    // Android Emulator -> host machine localhost (https://localhost:7257)
    public static final String BASE_URL = "https://10.0.2.2:7257/api";
    public static final String SIGNALR_BASE_URL = "https://10.0.2.2:7257";
}
