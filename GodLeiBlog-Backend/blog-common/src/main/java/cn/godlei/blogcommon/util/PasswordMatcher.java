package cn.godlei.blogcommon.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class PasswordMatcher {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private PasswordMatcher() {
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        String candidate = storedPassword.trim();
        if (candidate.isEmpty()) {
            return false;
        }

        if (rawPassword.equals(candidate)) {
            return true;
        }

        if (isBcryptHash(candidate)) {
            try {
                return BCRYPT.matches(rawPassword, candidate);
            } catch (Exception ignored) {
                return false;
            }
        }

        String md5 = digest(rawPassword, "MD5");
        if (candidate.equalsIgnoreCase(md5) || candidate.equalsIgnoreCase("{MD5}" + md5)) {
            return true;
        }

        String sha256 = digest(rawPassword, "SHA-256");
        return candidate.equalsIgnoreCase(sha256) || candidate.equalsIgnoreCase("{SHA-256}" + sha256);
    }

    private static boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private static String digest(String rawPassword, String algorithm) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            byte[] hashed = messageDigest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                builder.append(String.format(Locale.ROOT, "%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unsupported password digest algorithm: " + algorithm, ex);
        }
    }
}
