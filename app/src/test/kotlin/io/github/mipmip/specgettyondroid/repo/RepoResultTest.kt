package io.github.mipmip.specgettyondroid.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepoResultTest {

    @Test
    fun `a success carries its value and no error`() {
        val result: RepoResult<Int> = RepoResult.Success(7)
        assertEquals(7, result.valueOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun `a failure carries its error and no value`() {
        val error = RepoError.Authentication("nope")
        val result: RepoResult<Int> = RepoResult.Failure(error)
        assertEquals(error, result.errorOrNull())
        assertNull(result.valueOrNull())
    }

    @Test
    fun `every error kind carries a message`() {
        val errors = listOf(
            RepoError.Authentication("a"),
            RepoError.Network("b"),
            RepoError.NoOpenSpecProject("c"),
            RepoError.Unknown("d"),
        )
        assertEquals(listOf("a", "b", "c", "d"), errors.map { it.message })
    }
}
