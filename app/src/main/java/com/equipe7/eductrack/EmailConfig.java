package com.equipe7.eductrack;

/**
 * Secure Email Configuration for EduTrack OTP System
 * This class manages email credentials securely
 */
public class EmailConfig {
    
    // Gmail SMTP Configuration
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
    public static final boolean SMTP_AUTH = true;
    public static final boolean SMTP_STARTTLS = true;
    
    // Email credentials - In production, these should be stored securely
    // For now, using a dedicated Gmail account for the app
    private static final String EMAIL_USERNAME = "eductrack.noreply@gmail.com";
    private static final String EMAIL_PASSWORD = "your_app_password_here"; // App-specific password
    
    // Email templates
    public static final String EMAIL_SUBJECT = "Code de vérification EduTrack";
    
    public static final String EMAIL_TEMPLATE = 
        "<!DOCTYPE html>" +
        "<html>" +
        "<head>" +
        "    <meta charset='UTF-8'>" +
        "    <style>" +
        "        body { font-family: Arial, sans-serif; background-color: " + "#f5f5f5" + "; margin: 0; padding: 20px; }" +
        "        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
        "        .header { text-align: center; margin-bottom: 30px; }" +
        "        .logo { color: " + "#6B46C1" + "; font-size: 24px; font-weight: bold; }" +
        "        .otp-code { background-color: " + "#6B46C1" + "; color: white; font-size: 32px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; margin: 20px 0; letter-spacing: 5px; }" +
        "        .message { color: " + "#333" + "; line-height: 1.6; margin: 20px 0; }" +
        "        .warning { color: " + "#e74c3c" + "; font-size: 14px; margin-top: 20px; }" +
        "        .footer { text-align: center; margin-top: 30px; color: " + "#666" + "; font-size: 12px; }" +
        "    </style>" +
        "</head>" +
        "<body>" +
        "    <div class='container'>" +
        "        <div class='header'>" +
        "            <div class='logo'>📚 EduTrack</div>" +
        "        </div>" +
        "        <div class='message'>" +
        "            <p>Bonjour <strong>{USER_NAME}</strong>,</p>" +
        "            <p>Voici votre code de vérification pour EduTrack :</p>" +
        "        </div>" +
        "        <div class='otp-code'>{OTP_CODE}</div>" +
        "        <div class='message'>" +
        "            <p>Ce code est valide pendant <strong>5 minutes</strong>.</p>" +
        "            <p>Si vous n'avez pas demandé ce code, ignorez ce message.</p>" +
        "        </div>" +
        "        <div class='warning'>" +
        "            ⚠️ Ne partagez jamais ce code avec personne." +
        "        </div>" +
        "        <div class='footer'>" +
        "            <p>© 2024 EduTrack - Système de gestion éducative</p>" +
        "        </div>" +
        "    </div>" +
        "</body>" +
        "</html>";
    
    /**
     * Get email username (should be retrieved from secure storage in production)
     */
    public static String getEmailUsername() {
        // In production, retrieve from Android Keystore or encrypted preferences
        return EMAIL_USERNAME;
    }
    
    /**
     * Get email password (should be retrieved from secure storage in production)
     */
    public static String getEmailPassword() {
        // In production, retrieve from Android Keystore or encrypted preferences
        return EMAIL_PASSWORD;
    }
    
    /**
     * Format email template with user data
     */
    public static String formatEmailTemplate(String userName, String otpCode) {
        return EMAIL_TEMPLATE
                .replace("{USER_NAME}", userName != null ? userName : "Utilisateur")
                .replace("{OTP_CODE}", otpCode);
    }
    
    /**
     * Validate email configuration
     */
    public static boolean isConfigurationValid() {
        return !EMAIL_USERNAME.isEmpty() && 
               !EMAIL_PASSWORD.equals("your_app_password_here") &&
               !EMAIL_PASSWORD.isEmpty();
    }
}