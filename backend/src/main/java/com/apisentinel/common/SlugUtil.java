package com.apisentinel.common;

import java.security.SecureRandom;
import java.util.Locale;

public final class SlugUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private SlugUtil() {
    }

    public static String from(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "project";
        }
        return slug + "-" + Integer.toHexString(RANDOM.nextInt(0x10000));
    }
}
