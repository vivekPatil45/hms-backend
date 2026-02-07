package com.hms.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\+\\d{1,3}-\\d{8,10}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber != null && MOBILE_PATTERN.matcher(mobileNumber).matches();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
