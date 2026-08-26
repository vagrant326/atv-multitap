package io.github.vagrant326.atvmultitap.settings

import android.content.Context
import androidx.annotation.StringRes
import io.github.vagrant326.atvmultitap.R
import io.github.vagrant326.atvmultitap.ime.CustomKeys
import io.github.vagrant326.atvmultitap.ime.KeyBindings

/**
 * How much of the key mapping the strip spells out.
 *
 * [KEYPAD] is the default, and on this hardware that is not a preference. **Nothing is printed
 * on the remote** — the number keys carry no letters — so without the grid the user is pressing
 * unlabelled buttons and counting taps towards a letter they cannot see. Every phone this method
 * came from had the letters moulded into the keys; a TV remote is the first place multitap has
 * ever run where the mapping is invisible, and the strip is the only surface that can carry it.
 *
 * [STRIP] keeps the run of letters for the key being tapped and drops the grid, which is what is
 * left once the mapping is in the thumb. [OFF] gives the field underneath the whole screen, and
 * loses nothing but the tap count: the letter in progress is in the field either way.
 */
enum class HintMode(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {
    KEYPAD(R.string.hint_keypad, R.string.hint_keypad_description),
    STRIP(R.string.hint_strip, R.string.hint_strip_description),
    OFF(R.string.hint_off, R.string.hint_off_description),
    ;

    fun next(): HintMode = entries[(ordinal + 1) % entries.size]
}

/**
 * How long the same key keeps cycling before a second tap starts a new letter.
 *
 * The one number in multitap that is genuinely a matter of taste, and the only setting here that
 * changes what a press means. It is also the setting people who already type this way arrive
 * with an opinion about, which is reason enough to offer it rather than pick for them.
 *
 * Nobody has to wait it out — the next-letter key ends a letter immediately — so a long setting
 * costs a slow typist nothing and a short one is there for anyone who never uses that key.
 */
enum class LetterTimeout(val millis: Long, @StringRes val labelRes: Int) {
    QUICK(800, R.string.timeout_quick),
    NORMAL(1_200, R.string.timeout_normal),
    RELAXED(1_800, R.string.timeout_relaxed),
    ;

    fun next(): LetterTimeout = entries[(ordinal + 1) % entries.size]
}

/**
 * A function the user can put on a button of their choosing.
 *
 * Three, and two of them are conveniences. The trigger has to be a real key because the keyboard
 * is not on screen when it is needed.
 */
enum class Binding(
    @StringRes val titleRes: Int,
    @StringRes val promptRes: Int,
    @StringRes val fallbackRes: Int,
) {
    /**
     * The only binding the keyboard listens for while it is hidden, which is the mechanism that
     * once left a TV unnavigable — so it is one key, chosen by the user, and unassigned by
     * default. Reserved keys cannot be picked, so the d-pad and the number keys are never at
     * risk.
     */
    TRIGGER(
        R.string.binding_trigger,
        R.string.binding_trigger_prompt,
        R.string.binding_trigger_fallback,
    ),
    DELETE(
        R.string.binding_delete,
        R.string.binding_delete_prompt,
        R.string.binding_delete_fallback,
    ),

    /** Optional twice over: a numeric field turns the digit mode on by itself, and every digit
     *  is also the last stop on its own key's cycle. */
    DIGITS(
        R.string.binding_digits,
        R.string.binding_digits_prompt,
        R.string.binding_digits_fallback,
    ),
}

class Preferences(context: Context) {

    private val store = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** Unassigned by default: nothing is consumed while the keyboard is hidden until asked. */
    var triggerKeyCode: Int
        get() = store.getInt(KEY_TRIGGER_KEYCODE, KeyBindings.NO_KEY)
        set(value) = store.edit().putInt(KEY_TRIGGER_KEYCODE, value).apply()

    var deleteKeyCode: Int
        get() = store.getInt(KEY_DELETE_KEYCODE, KeyBindings.NO_KEY)
        set(value) = store.edit().putInt(KEY_DELETE_KEYCODE, value).apply()

    var digitsKeyCode: Int
        get() = store.getInt(KEY_DIGITS_KEYCODE, KeyBindings.NO_KEY)
        set(value) = store.edit().putInt(KEY_DIGITS_KEYCODE, value).apply()

    /**
     * Defaults to the full grid. The remote has nothing printed on it, so a new user has no way
     * to know which key carries which letters — see [HintMode].
     */
    var hintMode: HintMode
        get() = store.getString(KEY_HINT_MODE, null)
            ?.let { stored -> HintMode.entries.firstOrNull { it.name == stored } }
            ?: HintMode.KEYPAD
        set(value) = store.edit().putString(KEY_HINT_MODE, value.name).apply()

    var timeout: LetterTimeout
        get() = store.getString(KEY_TIMEOUT, null)
            ?.let { stored -> LetterTimeout.entries.firstOrNull { it.name == stored } }
            ?: LetterTimeout.NORMAL
        set(value) = store.edit().putString(KEY_TIMEOUT, value.name).apply()

    val letterTimeout: Long get() = timeout.millis

    val customKeys: CustomKeys
        get() = CustomKeys(triggerKeyCode, deleteKeyCode, digitsKeyCode)

    fun keyCodeFor(binding: Binding): Int = when (binding) {
        Binding.TRIGGER -> triggerKeyCode
        Binding.DELETE -> deleteKeyCode
        Binding.DIGITS -> digitsKeyCode
    }

    fun assign(binding: Binding, keyCode: Int) {
        when (binding) {
            Binding.TRIGGER -> triggerKeyCode = keyCode
            Binding.DELETE -> deleteKeyCode = keyCode
            Binding.DIGITS -> digitsKeyCode = keyCode
        }
    }

    private companion object {
        const val NAME = "multitap"
        const val KEY_TRIGGER_KEYCODE = "trigger_keycode"
        const val KEY_DELETE_KEYCODE = "delete_keycode"
        const val KEY_DIGITS_KEYCODE = "digits_keycode"
        const val KEY_HINT_MODE = "hint_mode"
        const val KEY_TIMEOUT = "letter_timeout"
    }
}
