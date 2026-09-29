package io.github.mipmip.specgettyondroid.repo

sealed interface RepoError {
    val message: String

    data class Authentication(override val message: String) : RepoError

    data class Network(override val message: String) : RepoError

    data class NoOpenSpecProject(override val message: String) : RepoError

    /**
     * The host accepted the credential and refused the repository. Distinct
     * from [Authentication] because the credential is not what is wrong, and
     * telling the person to check it sends them to fix the one thing that is
     * fine.
     */
    data class NoAccessToRepository(override val message: String) : RepoError

    /**
     * The project is real and its content is held somewhere this app cannot
     * reach. Distinct from [NoOpenSpecProject] because there is a project here,
     * and saying there is not sends a person looking for the wrong thing.
     */
    data class PointsElsewhere(override val message: String) : RepoError

    data class Unknown(override val message: String) : RepoError
}

sealed interface RepoResult<out T> {
    data class Success<T>(val value: T) : RepoResult<T>

    data class Failure(val error: RepoError) : RepoResult<Nothing>
}

fun <T> RepoResult<T>.valueOrNull(): T? = (this as? RepoResult.Success<T>)?.value

fun <T> RepoResult<T>.errorOrNull(): RepoError? = (this as? RepoResult.Failure)?.error
