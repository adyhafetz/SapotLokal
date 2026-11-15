package com.sapotlokal.util

import android.content.Context
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.sapotlokal.data.model.ShopCategory

/** Fills a shop-category dropdown with the fixed list of categories, shown in the current language. */
object CategoryPicker {

    /**
     * @param initialRaw the category already saved for the shop (shown as it is, translated when it is one of the list)
     * @param onChosen   called with the English value to store when the owner picks one
     */
    fun bind(
        context: Context,
        field: MaterialAutoCompleteTextView,
        initialRaw: String?,
        onChosen: (String) -> Unit
    ) {
        val categories = ShopCategory.entries
        val labels = categories.map { context.getString(it.labelRes) }
        field.setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, labels))
        if (!initialRaw.isNullOrBlank()) field.setText(ShopCategory.label(context, initialRaw), false)
        field.setOnItemClickListener { _, _, position, _ -> onChosen(categories[position].value) }
    }
}
