package com.equipe7.eductrack;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized Firebase manager to ensure consistent data operations
 * This class standardizes all Firebase operations to use Firestore exclusively
 */
public class FirebaseManager {
    
    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    
    // Private constructor for singleton pattern
    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
    
    // Get singleton instance
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    // Get current Firebase Auth instance
    public FirebaseAuth getAuth() {
        return auth;
    }
    
    // Get current Firestore instance
    public FirebaseFirestore getFirestore() {
        return db;
    }
    
    // Get current user
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    // Check if user is logged in
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
    // Save user data to Firestore
    public Task<Void> saveUserData(String userId, User user) {
        return db.collection("users")
                .document(userId)
                .set(user, SetOptions.merge());
    }
    
    // Save user data with map
    public Task<Void> saveUserData(String userId, Map<String, Object> userData) {
        return db.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge());
    }
    
    // Get user data from Firestore
    public Task<DocumentSnapshot> getUserData(String userId) {
        return db.collection("users")
                .document(userId)
                .get();
    }
    
    // Update user data
    public Task<Void> updateUserData(String userId, Map<String, Object> updates) {
        return db.collection("users")
                .document(userId)
                .update(updates);
    }
    
    // Update specific field
    public Task<Void> updateUserField(String userId, String field, Object value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);
        return updateUserData(userId, updates);
    }
    
    // Sign out user
    public void signOut() {
        auth.signOut();
    }
    
    // Create user document with role-specific data
    public Task<Void> createUserWithRole(String userId, String email, String role, Map<String, Object> additionalData) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);
        userData.put("acceptedTerms", false);
        userData.put("createdAt", System.currentTimeMillis());
        
        // Add any additional role-specific data
        if (additionalData != null) {
            userData.putAll(additionalData);
        }
        
        return saveUserData(userId, userData);
    }
    
    // Check if user has accepted terms
    public Task<DocumentSnapshot> checkTermsAcceptance(String userId) {
        return db.collection("users")
                .document(userId)
                .get();
    }
} 