package io.github.mipmip.specgettyondroid.repo

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

/** What a project's configuration says about content held somewhere else. */
sealed interface StoreDeclaration {

    /** `store: <id>`, naming the file it was read from. */
    data class Named(val id: String, val declaredIn: String) : StoreDeclaration

    /** A `store` key that is not a single id. */
    data class Malformed(val declaredIn: String) : StoreDeclaration
}

/**
 * Reads a `store:` declaration, which this app can report and cannot follow.
 *
 * Following one means resolving an id through a registry of local paths on the
 * machine that wrote it. A phone has neither the registry nor the paths, so the
 * pointer is a dead end. Saying which id and which file is the difference
 * between knowing this is the wrong repository to add and believing the app is
 * broken.
 */
object StoreDeclarations {

    fun read(projectDir: File): StoreDeclaration? {
        val file = OpenSpecLayout.configFile(projectDir) ?: return null
        val root = try {
            Yaml(SafeConstructor(LoaderOptions())).load<Any?>(file.readText())
        } catch (_: Exception) {
            return null
        }
        val declared = (root as? Map<*, *>)?.get("store") ?: return null
        val where = "openspec/${file.name}"
        return when {
            declared is String && declared.isNotBlank() -> StoreDeclaration.Named(declared, where)
            declared is String -> StoreDeclaration.Malformed(where)
            else -> StoreDeclaration.Malformed(where)
        }
    }

    /**
     * A declaration matters only when the project keeps nothing of its own.
     * Content present locally outranks a pointer, which is specgetty's rule.
     */
    fun unresolvable(projectDir: File): StoreDeclaration? {
        if (!OpenSpecLayout.isEmpty(projectDir)) return null
        return read(projectDir)
    }

    fun describe(declaration: StoreDeclaration): String = when (declaration) {
        is StoreDeclaration.Named ->
            "This points at the store \"${declaration.id}\", declared in " +
                "${declaration.declaredIn}. Stores are resolved on the machine that " +
                "wrote them, so this app cannot follow it. Add the repository that " +
                "holds the content instead."

        is StoreDeclaration.Malformed ->
            "The store declaration in ${declaration.declaredIn} is not a single id, " +
                "so it cannot be read."
    }
}
