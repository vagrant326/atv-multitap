package io.github.vagrant326.atvmultitap.core

/**
 * Which run each number key carries.
 *
 * Not a fourth way of typing but the same one: a key is tapped until what you want appears, and
 * the only thing this changes is what is on the key. That is the whole reason the symbols came
 * out this way rather than as a grid the d-pad walks — the strip and the keypad legend already
 * draw "what is on the active key" and "what is on every key", so both work on a different run
 * with no new drawing and no second way of choosing anything.
 *
 * Distinct from the digit mode, which is not a layer: there the row stops cycling altogether and
 * every key is the one digit printed on it.
 */
enum class Layer { LETTERS, SYMBOLS }
