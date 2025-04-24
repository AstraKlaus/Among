package com.amongus.bot.utils;

import com.amongus.bot.models.Config;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

/**
 * Handles security-related functionality like encryption, code verification,
 * and preventing information leakage.
 */
public class SecurityManager {
    private static final Logger log = LoggerFactory.getLogger(SecurityManager.class);
    
    // AES encryption key used for encoding role information
    private final SecretKey encryptionKey;
    
    // Map of user IDs to confirmation codes with expiration time
    private final Map<Long, Map.Entry<String, Long>> confirmationCodes = new ConcurrentHashMap<>();
    
    // Scheduler for cleaning up expired confirmation codes
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final Random random = new SecureRandom();
    private final Map<Long, String> confirmationCodesMap = new HashMap<>();
    private final Map<Long, Instant> confirmationTimestamps = new HashMap<>();
    
    /**
     * Creates a new security manager.
     */
    public SecurityManager() {
        this.encryptionKey = generateEncryptionKey();
        
        // Schedule cleanup task
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredCodes,
                1, 1, TimeUnit.MINUTES
        );
    }
    
    /**
     * Generates an AES encryption key.
     */
    private SecretKey generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate encryption key: {}", e.getMessage(), e);
            // Fallback to a hardcoded key (not secure for production!)
            byte[] keyBytes = new byte[32]; // 256 bits
            for (int i = 0; i < keyBytes.length; i++) {
                keyBytes[i] = (byte) i;
            }
            return new SecretKeySpec(keyBytes, "AES");
        }
    }
    
    /**
     * Encodes a role ID into a callback data string.
     * This helps hide the actual role from players who might see others' screens.
     */
    public String encodeRoleCallback(String roleId) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            byte[] encrypted = cipher.doFinal(roleId.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encode role callback: {}", e.getMessage(), e);
            // Fallback to a simple obfuscation
            return "role_" + Math.abs(roleId.hashCode());
        }
    }
    
    /**
     * Decodes a callback data string back to a role ID.
     */
    public String decodeRoleCallback(String encodedCallback) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedCallback);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            log.error("Failed to decode role callback: {}", e.getMessage(), e);
            // Try to handle the fallback encoding
            if (encodedCallback.startsWith("role_")) {
                return encodedCallback.substring(5).equals(String.valueOf(Math.abs("impostor".hashCode())))
                        ? "impostor" : "crewmate";
            }
            return "crewmate"; // Default fallback
        }
    }
    
    /**
     * Generates a confirmation code for a user.
     * Used for confirming critical actions like killing a player.
     */
    public String generateConfirmationCode(long userId) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < Config.CONFIRMATION_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        
        String confirmationCode = code.toString();
        confirmationCodesMap.put(userId, confirmationCode);
        confirmationTimestamps.put(userId, Instant.now());
        
        return confirmationCode;
    }
    
    /**
     * Verifies a confirmation code for a user.
     */
    public boolean verifyConfirmationCode(long userId, String code) {
        String storedCode = confirmationCodesMap.get(userId);
        Instant timestamp = confirmationTimestamps.get(userId);
        
        if (storedCode == null || timestamp == null) {
            return false;
        }
        
        // Check if the code is expired (10 minutes)
        if (timestamp.plusSeconds(600).isBefore(Instant.now())) {
            confirmationCodesMap.remove(userId);
            confirmationTimestamps.remove(userId);
            return false;
        }
        
        // Check if the code matches
        boolean isValid = storedCode.equals(code);
        
        // Remove the code after verification attempt
        if (isValid) {
            confirmationCodesMap.remove(userId);
            confirmationTimestamps.remove(userId);
        }
        
        return isValid;
    }
    
    /**
     * Checks if a user is authorized to perform actions.
     * In a real implementation, this would check against a database of authorized users.
     * 
     * @param userId The ID of the user to check
     * @return true if the user is authorized, false otherwise
     */
    public boolean isUserAuthorized(long userId) {
        // For simplicity, all users are authorized
        return true;
    }
    
    /**
     * Cleans up expired confirmation codes.
     */
    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        
        confirmationCodes.entrySet().removeIf(entry -> {
            long expiryTime = entry.getValue().getValue();
            return now > expiryTime;
        });
    }
    
    /**
     * Shut down the cleanup scheduler.
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
    }
} 