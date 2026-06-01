package com.nuvio.tv.data.remote.api

import com.nuvio.tv.data.remote.dto.trakt.TraktScrobbleRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// [FORK] Direct scrobble API — sends to custom endpoint, bypasses Trakt auth
interface DirectScrobbleApi {

    @POST("start")
    suspend fun scrobbleStart(
        @Header("X-Api-Key") apiKey: String,
        @Body body: TraktScrobbleRequestDto
    ): Response<Unit>

    @POST("stop")
    suspend fun scrobbleStop(
        @Header("X-Api-Key") apiKey: String,
        @Body body: TraktScrobbleRequestDto
    ): Response<Unit>
}
