package io.github.mipmip.specgettyondroid.spec

import java.io.File

/**
 * specgetty's own OpenSpec project, vendored into test resources. A real
 * project that tracks its own work in OpenSpec is the specimen `BRIEFING.md`
 * names, rather than anything this project could invent.
 */
object Corpus {

    val root: File by lazy {
        val url = requireNotNull(javaClass.getResource("/specgetty")) {
            "the vendored specgetty corpus is missing"
        }
        File(url.toURI())
    }

    val specsDir: File get() = File(root, "specs")

    val changesDir: File get() = File(root, "changes")

    val archiveDir: File get() = File(changesDir, "archive")

    /** Every `openspec/specs/<capability>/spec.md`. */
    fun mainSpecs(): List<File> =
        specsDir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { File(it, "spec.md").takeIf(File::isFile) }
            .sortedBy { it.parentFile.name }

    /** Every delta `spec.md` under a change, active or archived. */
    fun deltaSpecs(): List<File> =
        changesDir.walkTopDown()
            .filter { it.isFile && it.name == "spec.md" && it.parentFile.parentFile?.name == "specs" }
            .sortedBy { it.path }
            .toList()

    fun capabilityOf(deltaFile: File): String = deltaFile.parentFile.name
}
