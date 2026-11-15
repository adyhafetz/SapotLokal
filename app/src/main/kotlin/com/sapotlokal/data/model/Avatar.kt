package com.sapotlokal.data.model

import com.sapotlokal.R

/**
 * The 5 optional profile avatars. A customer's choice is stored as the id at /users/{uid}/avatar.
 * No avatar chosen = the field is absent, and screens fall back to the person's initials.
 */
enum class AvatarDef(val id: String, val label: String, val drawableRes: Int) {
    CAT("cat", "Cat", R.drawable.avatar_cat),
    DOG("dog", "Dog", R.drawable.avatar_dog),
    PANDA("panda", "Panda", R.drawable.avatar_panda),
    BROWN_BEAR("brown_bear", "Brown bear", R.drawable.avatar_bear),
    CAPYBARA("capybara", "Capybara", R.drawable.avatar_capybara);

    companion object {
        fun fromId(id: String?): AvatarDef? = entries.firstOrNull { it.id == id }
    }
}
