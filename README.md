# atv-multitap

A multitap keyboard for Android TV. Number keys carry the letters, you tap until the letter you
want appears, and nothing is predicted.

Fourth application in the programme, after `atv-letterwise`, `atv-h4` and `atv-t9`. Separate
repository, separate APK, no shared code — see `../docs/00-overview.md` for why.

---

## Why build the slowest method in the programme

Because it is the one people already know, and because it is the control.

`docs/00-overview.md` §5 puts multitap at KSPC 2.0342 against T9's 1.0072, and by that number
alone this application should not exist. §M2 is where it earns its place: **the visual-check rate
is approximately zero.** Every other keyboard here asks the user to look at the screen and confirm
what the keyboard decided — LetterWise on every character, T9 on every word that collides,
H4-Writer until the tree is in the thumb. This one decides nothing. Three taps of `2` is `c`
today, was `c` on a Nokia in 2003, and will be `c` in a year.

That makes it two things at once:

- **A keyboard for people who type this way already.** The muscle memory is thirty years old and
  widely held. A method that needs no learning is worth two-tenths of a keystroke per character to
  the person who has it.
- **The floor the rest of the programme is measured against.** Cold, warm, trained, untrained —
  none of those words mean anything here. There is one number, and a keyboard with a dictionary
  has to beat it while carrying the dictionary's cost in visual checks.

```bash
./gradlew :core:bench
```

## What is different from `atv-t9`

This started as a copy of it, so the differences are the interesting part.

**No dictionary, and therefore almost no application.** No assets, no vocabulary, no user
dictionary, no learning, no languages to enable and nothing to switch. The engine is one class of
about a hundred lines. Half of T9's settings screen existed to make an adaptive keyboard
accountable for what it had remembered; there is nothing here to be accountable for.

**Only the letter in progress belongs to the keyboard.** T9 holds a whole word in the composing
region until it is committed. Here everything before the current letter is already real text in
the field, which is what makes live search filtering, the caret keys and delete behave the way
they do everywhere else — and what makes `OK` submit in one press instead of two.

**The digit is the last stop on every key's cycle.** `2` is `a b c ą ć 2`, the way every phone
this muscle memory came from did it. The digit mode is a shortcut, not the only route, so no field
is untypeable on a remote with nothing assigned.

**Two keys the method has always needed.** `▶` ends the letter in progress, so a doubled letter
costs a press instead of a timeout in the middle of a word. `◀` steps back one letter, so tapping
past `ż` costs one press instead of six more.

## How it types

| Key | What it does |
|---|---|
| `2`–`9` | Tap for the letter. `a b c ą ć 2` on `2`, `w x y z ź ż 9` on `9`. |
| `▶` `▼` | End the letter, so the next tap of the same key starts a new one. |
| `◀` `▲` | Step back one letter, for when you tap past the one you wanted. |
| `OK` | Finish the letter and submit. One press, because nothing here needs accepting. |
| `0` | Finish the letter and add a space. |
| `1` | Cycle `. , - ' & : /`, replacing in place. |
| `BACK` | Drop the letter in progress, then close the keyboard. |
| hold `◀` `▶` | The caret, one word at a time. Only with no letter in progress. |
| hold delete | Delete back to the start of the word. |

The arrows work the cycle only while a letter is in progress. Outside one they belong to whatever
is behind the keyboard — a keyboard that eats the d-pad on a television leaves the whole device
unnavigable, which is not a hypothetical.

Three keys are assignable from settings, captured from the remote rather than chosen from a list:
show-the-keyboard, delete and digits mode. Remotes disagree about which keys exist and about what
they report — the key this project most wanted turned out to be keycode 300.

**Only the trigger is listened for while the keyboard is hidden**, and it is unassigned by
default.

## The Polish letters

`ą ć ę ł ń ó ś ź ż` sit at the end of the run on the key their base letter is on. `ż` is six taps
of `9`, and that is the honest price of this method — T9 gets it in one press because a dictionary
is doing the work, and this keyboard has no dictionary to do it.

They are at the end rather than interleaved on purpose. Position in the run *is* press count, so
interleaving would make `c` cost four presses to buy `ą` a cheaper one. Where they are, the
English letters cost exactly what they cost on a phone.

## What is remembered

Nothing. There is no dictionary, no user word store, no adaptation and no file in the app's
storage that holds anything typed. The privacy argument in `docs/00-overview.md` §3.1 is
structural here rather than promised: there is no code that could write it down.

## The network permission

`INTERNET` and `REQUEST_INSTALL_PACKAGES` are held for one screen: the updater, which runs in its
own process (`:updater`) so that the component handling keystrokes contains no networking code.
Nothing runs unless the user opens that screen and presses something — no background job, no boot
receiver, no poll at keyboard start.

They exist because sideloading has no update channel. They come out when this ships through a
store. See `../docs/00-overview.md` §3.1.

## Building

```bash
docker compose -f ../docker/compose.yaml run --rm dev ./gradlew assembleDevDebug
```

Two flavours, `prod` and `dev`, and deliberately two *applications*: the dev build carries its own
`applicationId` and installs alongside the released one, so an experiment that misbehaves does not
take the working keyboard with it.

## Known gaps

- **No case.** Everything is lowercase, which a television search box does not care about and a
  message field would. Multitap's usual answer is a shift cycle on `#`, and this remote has no `#`.
- **No Less-Tap.** Reordering each key's letters by frequency is the cheap 25% saving on this
  method — `docs/00-overview.md` §5 puts Less-Tap at 1.5266 — and it costs exactly what this
  application is here to avoid: the layout stops being the one in everybody's thumb. If it is ever
  built it belongs behind a setting, off by default, measured against the figure above.
- **`bench/queries-v1.tsv` is 26 real queries.** Small on purpose — see the header of that file —
  and for this keyboard it matters less than for the others: the number is arithmetic over the
  layout rather than an estimate, so a bigger corpus would move it, not sharpen it.

## Licence

MIT. See `LICENSE`.
