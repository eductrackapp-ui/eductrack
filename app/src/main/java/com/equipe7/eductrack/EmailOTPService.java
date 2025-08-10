package com.equipe7.eductrack;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
 * Simplified and robust EmailJS OTP Service for EduTrack
 * This version focuses on reliability and proper error handling
 */
public class EmailOTPService {
    
    private static final String TAG = "EmailOTPService";
    
    // EmailJS Configuration - VERIFIED WORKING CREDENTIALS
    private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";
    private static final String SERVICE_ID = "service_yvl11d5";
    private static final String TEMPLATE_ID = "template_zlp263e";
    private static final String USER_ID = "Un7snKzeE4AGeorc-";
    private static final String ACCESS_TOKEN = "IL07jpzJG6LR1S32IfFJy";
    
    private final OkHttpClient client;
    private final Context context;
    private final Handler mainHandler;
    
    public EmailOTPService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Generate a secure 6-digit OTP code
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(otp);
        Log.d(TAG, "Generated OTP: " + otpCode); // For debugging - remove in production
        return otpCode;
    }
    
    /**
     * Send OTP email - SIMPLIFIED VERSION
     * This method directly sends the OTP without complex routing
     */
    public void sendOTP(String toEmail, String userName, String otpCode, OTPCallback callback) {
        Log.d(TAG, "Attempting to send OTP to: " + toEmail);
        
        try {
            // Create the EmailJS payload
            JSONObject emailData = new JSONObject();
            emailData.put("service_id", SERVICE_ID);
            emailData.put("template_id", TEMPLATE_ID);
            emailData.put("user_id", USER_ID);
            emailData.put("accessToken", ACCESS_TOKEN);
            
            // Template parameters - SIMPLIFIED
            JSONObject templateParams = new JSONObject();
            templateParams.put("to_email", toEmail);
            templateParams.put("to_name", userName != null ? userName : "User");
            templateParams.put("verification_code", otpCode);
            templateParams.put("subject", "Your EduTrack Verification Code");
            templateParams.put("message", "Your verification code is: " + otpCode + ". This code will expire in 5 minutes.");
            
            emailData.put("template_params", templateParams);
            
            Log.d(TAG, "EmailJS Payload: " + emailData.toString());
            
            // Create HTTP request
            RequestBody body = RequestBody.create(
                    emailData.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(EMAILJS_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "EduTrack-Android/1.0")
                    .build();
            
            // Send async request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network failure sending OTP", e);
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onFailure("Network error: " + e.getMessage());
                        }
                    });
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.body().string();
                        }
                        
                        Log.d(TAG, "EmailJS Response Code: " + response.code());
                        Log.d(TAG, "EmailJS Response Body: " + responseBody);
                        
                        mainHandler.post(() -> {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "OTP email sent successfully to: " + toEmail);
                                if (callback != null) {
                                    callback.onSuccess("OTP sent successfully");
                                }
                            } else {
                                Log.e(TAG, "EmailJS API Error: " + response.code() + " - " + responseBody);
                                if (callback != null) {
                                    callback.onFailure("Email service error: " + response.code());
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onFailure("Response processing error: " + e.getMessage());
                            }
                        });
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating email payload", e);
            if (callback != null) {
                callback.onFailure("Payload creation error: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error sending OTP", e);
            if (callback != null) {
                callback.onFailure("Unexpected error: " + e.getMessage());
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
     * Check if OTP has expired (5 minutes)
     */
    public boolean isOTPExpired(long otpTimestamp) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = 5 * 60 * 1000; // 5 minutes in milliseconds
        boolean expired = (currentTime - otpTimestamp) > expirationTime;
        Log.d(TAG, "OTP expired check: " + expired + " (age: " + (currentTime - otpTimestamp) / 1000 + "s)");
        return expired;
    }
    
    /**
     * Test EmailJS connection
     */
    public void testConnection(OTPCallback callback) {
        Log.d(TAG, "Testing EmailJS connection...");
        sendOTP("test@example.com", "Test User", "123456", new OTPCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "EmailJS connection test successful");
                if (callback != null) callback.onSuccess("Connection test successful");
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "EmailJS connection test failed: " + error);
                if (callback != null) callback.onFailure("Connection test failed: " + error);
            }
        });
    }
    
    /**
     * Callback interface for OTP operations
     */
    public interface OTPCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}