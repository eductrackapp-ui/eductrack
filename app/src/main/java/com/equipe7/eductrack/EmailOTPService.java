package com.equipe7.eductrack;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * EmailJS OTP Service for EduTrack Authentication
 * Handles sending OTP codes via email for login and password reset
 */
public class EmailOTPService {
    
    private static final String TAG = "EmailOTPService";
    
    // EmailJS Configuration
    private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";
    private static final String SERVICE_ID = "service_yvl11d5";
    private static final String TEMPLATE_ID = "template_zlp263e";
    private static final String USER_ID = "Un7snKzeE4AGeorc-";
    private static final String ACCESS_TOKEN = "IL07jpzJG6LR1S32IfFJy";
    
    private final OkHttpClient client;
    private final Context context;
    
    public EmailOTPService(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }
    
    /**
     * Generate a 6-digit OTP code
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    /**
     * Send OTP via email for login authentication
     */
    public void sendLoginOTP(String userEmail, String userName, String otpCode, OTPCallback callback) {
        sendOTP(userEmail, userName, otpCode, "Login Verification", 
                "Your EduTrack login verification code", callback);
    }
    
    /**
     * Send OTP via email for password reset
     */
    public void sendPasswordResetOTP(String userEmail, String userName, String otpCode, OTPCallback callback) {
        sendOTP(userEmail, userName, otpCode, "Password Reset", 
                "Your EduTrack password reset code", callback);
    }
    
    /**
     * Send OTP via email for admin login
     */
    public void sendAdminLoginOTP(String adminEmail, String adminName, String otpCode, OTPCallback callback) {
        sendOTP(adminEmail, adminName, otpCode, "Admin Login Verification", 
                "Your EduTrack admin login verification code", callback);
    }
    
    /**
     * Core method to send OTP via EmailJS
     */
    private void sendOTP(String toEmail, String userName, String otpCode, String purpose, String subject, OTPCallback callback) {
        try {
            // Create EmailJS payload
            JSONObject emailData = new JSONObject();
            emailData.put("service_id", SERVICE_ID);
            emailData.put("template_id", TEMPLATE_ID);
            emailData.put("user_id", USER_ID);
            emailData.put("accessToken", ACCESS_TOKEN);
            
            // Template parameters
            JSONObject templateParams = new JSONObject();
            templateParams.put("to_email", toEmail);
            templateParams.put("to_name", userName != null ? userName : "User");
            templateParams.put("verification_code", otpCode);
            templateParams.put("purpose", purpose);
            templateParams.put("subject", subject);
            templateParams.put("app_name", "EduTrack");
            templateParams.put("from_name", "EduTrack Team");
            
            emailData.put("template_params", templateParams);
            
            // Create request
            RequestBody body = RequestBody.create(
                    emailData.toString(),
                    MediaType.get("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(EMAILJS_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            // Send async request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to send OTP email", e);
                    if (callback != null) {
                        callback.onFailure("Failed to send OTP email: " + e.getMessage());
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "OTP email sent successfully to: " + toEmail);
                        if (callback != null) {
                            callback.onSuccess("OTP sent successfully to " + toEmail);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to send OTP email. Response: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onFailure("Failed to send OTP email. Please try again.");
                        }
                    }
                    response.close();
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating email payload", e);
            if (callback != null) {
                callback.onFailure("Error preparing email: " + e.getMessage());
            }
        }
    }
    
    /**
     * Validate OTP code format
     */
    public boolean isValidOTPFormat(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }
    
    /**
     * Check if OTP has expired (10 minutes)
     */
    public boolean isOTPExpired(long otpTimestamp) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = 10 * 60 * 1000; // 10 minutes in milliseconds
        return (currentTime - otpTimestamp) > expirationTime;
    }
    
    /**
     * Callback interface for OTP operations
     */
    public interface OTPCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}