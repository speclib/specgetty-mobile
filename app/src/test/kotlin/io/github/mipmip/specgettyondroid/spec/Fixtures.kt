package io.github.mipmip.specgettyondroid.spec

/**
 * The fixtures are specimens, not inventions. Each was taken from a live spec
 * and copied verbatim from specgetty's `src/ui/testdata/specs/`.
 */
object Fixtures {

    fun read(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/specs/$name.md")) {
            "fixture $name is missing"
        }.bufferedReader().readText()

    fun parse(name: String): SpecTree {
        val result = SpecParser.parse(name, read(name))
        require(result.isSpec) { "$name should be a spec, got ${result.problems}" }
        return result.tree
    }

    val all = listOf(
        "canonical",
        "bare-uppercase-clauses",
        "bold-title-clauses",
        "prose-scenario",
        "mixed-parts",
        "heading-without-prefix",
        "heading-in-fence",
        "empty-scenario",
        "delta-header",
        "no-purpose",
        "requirements-outside",
        "no-requirements",
        "requirement-without-scenario",
    )
}
