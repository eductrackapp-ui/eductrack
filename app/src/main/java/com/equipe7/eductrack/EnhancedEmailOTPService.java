package com.equipe7.eductrack;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Enhanced Email OTP Service using JavaMail API and Firestore
 * This service provides reliable email delivery and secure OTP management
 */
public class EnhancedEmailOTPService {
    
    private static final String TAG = "EnhancedEmailOTPService";
    
    // OTP Configuration
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes
    private static final int MAX_ATTEMPTS_PER_EMAIL = 3;
    private static final long RATE_LIMIT_WINDOW = 60 * 1000; // 1 minute
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final SecureRandom secureRandom;
    
    public EnhancedEmailOTPService(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Generate a secure OTP code
     */
    public String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        String otpCode = otp.toString();
        Log.d(TAG, "Generated OTP: " + otpCode); // Remove in production
        return otpCode;
    }
    
    /**
     * Send OTP with rate limiting and enhanced error handling
     */
    public void sendOTP(String email, String userName, OTPCallback callback) {
        Log.d(TAG, "Sending OTP to: " + email);
        
        // Check rate limiting first
        checkRateLimit(email, new RateLimitCallback() {
            @Override
            public void onAllowed() {
                // Generate and store OTP
                String otpCode = generateOTP();
                storeOTPInFirestore(email, otpCode, new FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // Send email
                        sendEmailAsync(email, userName, otpCode, callback);
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        callback.onFailure("Failed to store OTP: " + error);
                    }
                });
            }
            
            @Override
            public void onRateLimited(long waitTime) {
                callback.onFailure("Trop de tentatives. Veuillez attendre " + (waitTime / 1000) + " secondes.");
            }
        });
    }
    
    /**
     * Verify OTP code
     */
    public void verifyOTP(String email, String inputCode, OTPCallback callback) {
        Log.d(TAG, "Verifying OTP for: " + email);
        
        firestore.collection("otps")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure("Code non trouvé ou expiré");
                        return;
                    }
                    
                    String storedCode = documentSnapshot.getString("code");
                    Long timestamp = documentSnapshot.getLong("timestamp");
                    Boolean used = documentSnapshot.getBoolean("used");
                    
                    // Validate OTP
                    if (storedCode == null || !storedCode.equals(inputCode)) {
                        callback.onFailure("Code incorrect");
                        return;
                    }
                    
                    if (used != null && used) {
                        callback.onFailure("Ce code a déjà été utilisé");
                        return;
                    }
                    
                    if (timestamp == null || isOTPExpired(timestamp)) {
                        callback.onFailure("Le code a expiré");
                        return;
                    }
                    
                    // Mark as used
                    documentSnapshot.getReference().update("used", true)
                            .addOnSuccessListener(aVoid -> callback.onSuccess("Code vérifié avec succès"))
                            .addOnFailureListener(e -> callback.onFailure("Erreur lors de la validation"));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error during verification", e);
                    callback.onFailure("Erreur de vérification");
                });
    }
    
    /**
     * Check rate limiting for email sending
     */
    private void checkRateLimit(String email, RateLimitCallback callback) {
        firestore.collection("rate_limits")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    long currentTime = System.currentTimeMillis();
                    
                    if (!documentSnapshot.exists()) {
                        // First request, allow and create record
                        createRateLimitRecord(email, currentTime);
                        callback.onAllowed();
                        return;
                    }
                    
                    Long lastRequest = documentSnapshot.getLong("lastRequest");
                    Long attempts = documentSnapshot.getLong("attempts");
                    
                    if (lastRequest == null) lastRequest = 0L;
                    if (attempts == null) attempts = 0L;
                    
                    // Reset attempts if window has passed
                    if (currentTime - lastRequest > RATE_LIMIT_WINDOW) {
                        attempts = 0L;
                    }
                    
                    if (attempts >= MAX_ATTEMPTS_PER_EMAIL) {
                        long waitTime = RATE_LIMIT_WINDOW - (currentTime - lastRequest);
                        if (waitTime > 0) {
                            callback.onRateLimited(waitTime);
                            return;
                        }
                        attempts = 0L; // Reset if wait time is over
                    }
                    
                    // Update rate limit record
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastRequest", currentTime);
                    updates.put("attempts", attempts + 1);
                    
                    documentSnapshot.getReference().update(updates);
                    callback.onAllowed();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Rate limit check failed", e);
                    callback.onAllowed(); // Allow on error to not block users
                });
    }
    
    /**
     * Create rate limit record
     */
    private void createRateLimitRecord(String email, long currentTime) {
        Map<String, Object> rateLimitData = new HashMap<>();
        rateLimitData.put("email", email);
        rateLimitData.put("lastRequest", currentTime);
        rateLimitData.put("attempts", 1);
        
        firestore.collection("rate_limits")
                .document(email)
                .set(rateLimitData);
    }
    
    /**
     * Store OTP in Firestore with enhanced data
     */
    private void storeOTPInFirestore(String email, String otpCode, FirestoreCallback callback) {
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("code", otpCode);
        otpData.put("timestamp", System.currentTimeMillis());
        otpData.put("used", false);
        otpData.put("email", email);
        otpData.put("expiresAt", System.currentTimeMillis() + OTP_EXPIRY_TIME);
        
        firestore.collection("otps")
                .document(email)
                .set(otpData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "OTP stored successfully for: " + email);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to store OTP", e);
                    callback.onFailure(e.getMessage());
                });
    }
    
    /**
     * Send email asynchronously using EmailJS API (more reliable than direct SMTP on Android)
     */
    private void sendEmailAsync(String toEmail, String userName, String otpCode, OTPCallback callback) {
        executorService.execute(() -> {
            try {
                // Create EmailJS payload
                JSONObject emailData = new JSONObject();
                emailData.put("service_id", "service_yvl11d5");
                emailData.put("template_id", "template_zlp263e");
                emailData.put("user_id", "Un7snKzeE4AGeorc-");
                emailData.put("accessToken", "IL07jpzJG6LR1S32IfFJy");
                
                // Template parameters
                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email", toEmail);
                templateParams.put("to_name", userName != null ? userName : "User");
                templateParams.put("verification_code", otpCode);
                templateParams.put("subject", EmailConfig.EMAIL_SUBJECT);
                templateParams.put("message", EmailConfig.formatEmailTemplate(userName, otpCode));
                
                emailData.put("template_params", templateParams);
                
                // Create HTTP request
                RequestBody body = RequestBody.create(
                        emailData.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                        .url("https://api.emailjs.com/api/v1.0/email/send")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "EduTrack-Android/1.0")
                        .build();
                
                // Send async request
                OkHttpClient client = new OkHttpClient();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Network failure sending email", e);
                        mainHandler.post(() -> callback.onFailure("Erreur réseau: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseBody = response.body() != null ? response.body().string() : "";
                        
                        Log.d(TAG, "EmailJS Response Code: " + response.code());
                        Log.d(TAG, "EmailJS Response Body: " + responseBody);
                        
                        mainHandler.post(() -> {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Email sent successfully to: " + toEmail);
                                callback.onSuccess("Code envoyé avec succès");
                            } else {
                                Log.e(TAG, "EmailJS API Error: " + response.code() + " - " + responseBody);
                                callback.onFailure("Erreur d'envoi: " + response.code());
                            }
                        });
                        
                        response.close();
                    }
                });
                
            } catch (JSONException e) {
                Log.e(TAG, "Error creating email payload", e);
                mainHandler.post(() -> callback.onFailure("Erreur de configuration: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending email", e);
                mainHandler.post(() -> callback.onFailure("Erreur inattendue: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Check if OTP has expired
     */
    public boolean isOTPExpired(long timestamp) {
        long currentTime = System.currentTimeMillis();
        boolean expired = (currentTime - timestamp) > OTP_EXPIRY_TIME;
        Log.d(TAG, "OTP expired check: " + expired + " (age: " + (currentTime - timestamp) / 1000 + "s)");
        return expired;
    }
    
    /**
     * Clean up expired OTPs (call periodically)
     */
    public void cleanupExpiredOTPs() {
        long currentTime = System.currentTimeMillis();
        
        firestore.collection("otps")
                .whereLessThan("expiresAt", currentTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Cleaning up " + queryDocumentSnapshots.size() + " expired OTPs");
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to cleanup expired OTPs", e));
    }
    
    /**
     * Validate OTP format
     */
    public boolean isValidOTPFormat(String otp) {
        return otp != null && otp.matches("\\d{" + OTP_LENGTH + "}");
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Callback interfaces
    public interface OTPCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
    
    private interface FirestoreCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    private interface RateLimitCallback {
        void onAllowed();
        void onRateLimited(long waitTime);
    }
}