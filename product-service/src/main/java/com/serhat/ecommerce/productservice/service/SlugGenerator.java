package com.serhat.ecommerce.productservice.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Turns a display name into a URL-safe slug, e.g. "Kablosuz Kulaklık" -> "kablosuz-kulaklik".
 *
 * <p>Turkish characters are mapped explicitly before Unicode normalisation, because
 * decomposition alone leaves "ı" and "ş" as unmapped characters that would be stripped and
 * silently collapse distinct names into the same slug.
 */
public final class SlugGenerator {

    private SlugGenerator() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String mapped = input
                .replace("ı", "i").replace("İ", "i")
                .replace("ş", "s").replace("Ş", "s")
                .replace("ğ", "g").replace("Ğ", "g")
                .replace("ü", "u").replace("Ü", "u")
                .replace("ö", "o").replace("Ö", "o")
                .replace("ç", "c").replace("Ç", "c");

        String normalized = Normalizer.normalize(mapped, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
    }

    /**
     * Appends {@code -2}, {@code -3}, ... until the slug is free, so two products with the
     * same name do not collide on the unique slug constraint.
     */
    public static String uniqueSlug(String desired, Predicate<String> exists) {
        String base = slugify(desired);
        if (base.isEmpty()) {
            base = "item";
        }
        String candidate = base;
        int suffix = 2;
        while (exists.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
