package com.example.productmanager;

public class ProfileResponse {

    private boolean success;
    private String message;
    private Data data;

    public Data getData() {
        return data;
    }

    public static class Data {
        private int userId;
        private String fullName;
        private String email;
        private String phone;
        private String address;
        private String avatarUrl;

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getAddress() {
            return address;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }
    }
}
