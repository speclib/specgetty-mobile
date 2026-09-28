package io.github.mipmip.specgettyondroid.nav

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The route table. Change names and capability ids come from a repository and
 * may contain slashes, spaces and anything else a directory name allows, so
 * every argument is encoded on the way in and decoded on the way out.
 */
object Destinations {

    const val REPO_LIST = "repos"

    const val SCANNER = "scanner"

    const val PROJECT_ARG_REPO = "repoId"
    const val PROJECT_PATTERN = "project/{$PROJECT_ARG_REPO}"

    const val CHANGE_ARG_REPO = "repoId"
    const val CHANGE_ARG_NAME = "changeName"
    const val CHANGE_PATTERN = "change/{$CHANGE_ARG_REPO}/{$CHANGE_ARG_NAME}"

    const val SPEC_ARG_REPO = "repoId"
    const val SPEC_ARG_CAPABILITY = "capability"
    const val SPEC_PATTERN = "spec/{$SPEC_ARG_REPO}/{$SPEC_ARG_CAPABILITY}"

    const val DELTA_ARG_REPO = "repoId"
    const val DELTA_ARG_NAME = "changeName"
    const val DELTA_PATTERN = "delta/{$DELTA_ARG_REPO}/{$DELTA_ARG_NAME}"

    fun project(repoId: String): String = "project/${encode(repoId)}"

    fun change(repoId: String, changeName: String): String =
        "change/${encode(repoId)}/${encode(changeName)}"

    fun spec(repoId: String, capability: String): String =
        "spec/${encode(repoId)}/${encode(capability)}"

    fun delta(repoId: String, changeName: String): String =
        "delta/${encode(repoId)}/${encode(changeName)}"

    fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())
}
