package io.github.vagrant326.atvmultitap.core.bench

import io.github.vagrant326.atvmultitap.core.Cost
import io.github.vagrant326.atvmultitap.core.Simulator
import java.io.File

/**
 * KSPC over the query corpus.
 *
 * One figure, not two, and the same figure on the first day as on the thousandth. That is the
 * whole reason this application exists inside the programme: the other three keyboards each have
 * a cold number and a warm number, and which one you quote decides the argument. This one has
 * neither — the press count for a string is a property of the layout, so the number below is the
 * floor the others have to beat with all their machinery running.
 *
 * The `language` column of the corpus is read only to group the output. Nothing in the cost
 * depends on it: there is no dictionary and no model, so the keyboard has no language state at
 * all, and a query being Polish costs what its diacritics cost and nothing more.
 */
fun main(arguments: Array<String>) {
    val options = arguments.toList().chunked(2).associate { it[0] to it.getOrElse(1) { "" } }
    val queries = File(options["--queries"] ?: "bench/queries-v1.tsv")
    if (!queries.exists()) {
        System.err.println("no query corpus at ${queries.absolutePath}")
        return
    }

    val rows = queries.readLines()
        .drop(1)
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { it.split('\t') }
        .filter { it.size >= 2 }

    val simulator = Simulator()

    println("%-34s %-6s %8s %8s".format("query", "lang", "kspc", "waited"))
    println("-".repeat(60))

    var total = Cost.ZERO
    val byLanguage = mutableMapOf<String, Cost>()

    for (row in rows) {
        val query = row[0].trim()
        val language = row[1].trim().ifEmpty { "pl" }
        val cost = simulator.cost(query)
        total += cost
        byLanguage[language] = (byLanguage[language] ?: Cost.ZERO) + cost

        println(
            "%-34s %-6s %8.4f %8.4f".format(
                query.take(34),
                language,
                cost.kspc,
                cost.waitedKspc,
            )
        )
    }

    println("-".repeat(60))
    for ((language, cost) in byLanguage.entries.sortedBy { it.key }) {
        println("%-34s %-6s %8.4f %8.4f".format("", language, cost.kspc, cost.waitedKspc))
    }
    println("%-34s %-6s %8.4f %8.4f".format("ALL", "", total.kspc, total.waitedKspc))
    println()
    println(
        "%d presses over %d characters in %d words; %d of them moved on from a doubled key, %d characters no key carries"
            .format(total.presses, total.characters, total.words, total.nextLetter, total.unreachable)
    )
    println()
    println("The waited column is the same queries with every doubled key sat out rather than")
    println("pressed through, which is the convention the published figures use.")
    println()
    println("Baselines from docs/00-overview.md §5: multitap 2.0342, Less-Tap 1.5266,")
    println("LetterWise 1.1500, published T9 1.0072 (dictionary words only, running prose).")
}
