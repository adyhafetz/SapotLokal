package com.sapotlokal.data.model

import android.content.Context
import androidx.annotation.StringRes
import com.sapotlokal.R
import java.text.Normalizer
import java.util.Locale

/**
 * The shop categories a shop owner can pick. The English [value] is what is stored in the database
 * (so existing data and the Explore filter keep working); [label] is what people see, in the language
 * they chose. A category typed by hand before this list existed is shown exactly as it was typed.
 */
enum class ShopCategory(val value: String, @StringRes val labelRes: Int, private vararg val aliases: String) {
    CAFE("Cafe", R.string.cat_cafe, "coffee shop", "kopitiam", "kopi"),
    BAKERY("Bakery", R.string.cat_bakery, "bakeri"),
    RESTAURANT("Restaurant", R.string.cat_restaurant, "restoran", "food", "food & beverage"),
    GROCERY("Grocery", R.string.cat_grocery, "runcit", "minimarket", "supermarket"),
    FLORIST("Florist", R.string.cat_florist, "flower shop"),
    BOOKSTORE("Bookstore", R.string.cat_bookstore, "book store", "kedai buku"),
    SALON("Salon", R.string.cat_salon, "barber", "barbershop"),
    DESSERT("Dessert", R.string.cat_dessert, "sweets", "ice cream"),
    TAILOR("Tailor", R.string.cat_tailor, "tukang jahit"),
    HARDWARE("Hardware", R.string.cat_hardware, "perkakasan"),
    OTHER("Other", R.string.cat_other, "lain-lain");

    companion object {
        private fun normalize(s: String): String =
            Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase(Locale.ROOT)

        /** The category a stored text stands for, or null when it is something custom. */
        fun fromStored(raw: String?): ShopCategory? {
            if (raw.isNullOrBlank()) return null
            val n = normalize(raw)
            return entries.firstOrNull { c ->
                normalize(c.value) == n || c.aliases.any { normalize(it) == n }
            }
        }

        /** Text to show for a stored category, in the current language. */
        fun label(context: Context, raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return fromStored(raw)?.let { context.getString(it.labelRes) } ?: raw.trim()
        }
    }
}
