package com.winauth.tester;

/**
 * Utility class for Java 8 compatibility.
 */
public class StringUtils {
    
    /**
     * Repeats a string a specified number of times (Java 8 compatible alternative to String.repeat()).
     * 
     * @param str the string to repeat
     * @param count the number of times to repeat
     * @return the repeated string
     */
    public static String repeat(String str, int count) {
        if (str == null || count < 0) {
            throw new IllegalArgumentException("String cannot be null and count must be non-negative");
        }
        if (count == 0 || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
