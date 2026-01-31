package com.app.summa.data.repository

import com.app.summa.data.local.IdentityDao
import com.app.summa.data.model.Identity
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class IdentityRepositoryTest {

    private val identityDao: IdentityDao = mockk(relaxed = true)
    private val knowledgeRepository: KnowledgeRepository = mockk(relaxed = true)
    private val identityRepository = IdentityRepositoryImpl(identityDao, knowledgeRepository)

    @Test
    fun `deleteIdentity calls dao deleteIdentity`() = runTest {
        val identity = Identity(
            id = 1,
            name = "Test Identity",
            description = "Test Description"
        )

        identityRepository.deleteIdentity(identity)

        coVerify(exactly = 1) { identityDao.deleteIdentity(identity) }
    }
}
