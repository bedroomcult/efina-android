package com.efina.app.data.repository

import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.interceptor.TokenManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

class AuthRepositoryLogoutTest {

    private val api = mockk<EfinaApi>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val repository = AuthRepositoryImpl(api, tokenManager)

    @Test
    fun `logout clears the stored token when the server returns 401`() = runTest {
        coEvery { api.logout() } returns Response.error(401, "{}".toResponseBody())
        repository.logout()
        verify { tokenManager.clearAll() }
    }

    @Test
    fun `logout clears the stored token on success`() = runTest {
        coEvery { api.logout() } returns Response.success(Unit)
        repository.logout()
        verify { tokenManager.clearAll() }
    }

    @Test
    fun `logout clears the stored token when the network throws`() = runTest {
        coEvery { api.logout() } throws java.io.IOException("offline")
        repository.logout()
        verify { tokenManager.clearAll() }
    }

    @Test
    fun `clearSession clears the stored token without a network call`() {
        repository.clearSession()
        verify { tokenManager.clearAll() }
    }
}
