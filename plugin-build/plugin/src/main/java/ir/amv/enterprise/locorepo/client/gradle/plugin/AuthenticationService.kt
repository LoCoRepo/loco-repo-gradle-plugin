package ir.amv.enterprise.locorepo.client.gradle.plugin

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.RefreshTokenRequest
import com.google.api.client.auth.openidconnect.IdTokenResponse
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.IdTokenProvider
import java.io.ByteArrayInputStream
import java.io.File

class AuthenticationService {
    companion object {
        fun fromServiceAccount(serviceAccount: String): String {
            val credential = GoogleCredentials.fromStream(ByteArrayInputStream(serviceAccount.toByteArray()))
            return when (credential) {
                is IdTokenProvider -> {
                    credential.idTokenWithAudience(
                        "609321703527-7uddmgdd8i0src8i8jlnbu593goj2lek.apps.googleusercontent.com",
                        emptyList()
                    ).tokenValue
                }
                else -> credential.createScoped("https://www.googleapis.com/auth/userinfo.email")
                    .also {
                        it.refresh()
                        it.authenticationType
                    }.accessToken!!.tokenValue
            }
        }

        fun authenticate(): String {
            val dataDirectory = File(System.getProperty("user.home"), ".store/amir")
            val clientId = "609321703527-7uddmgdd8i0src8i8jlnbu593goj2lek.apps.googleusercontent.com"
            val clientSecret = "SKLEKhc5axjyqVn_EHeZZVNo"
            val authorization = AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                NetHttpTransport(),
                GsonFactory(),
                GenericUrl("https://oauth2.googleapis.com/token"),
                ClientParametersAuthentication(clientId, clientSecret),
                clientId,
                "https://accounts.google.com/o/oauth2/auth"
            )
                .setScopes(listOf("https://www.googleapis.com/auth/userinfo.email"))
                .setDataStoreFactory(FileDataStoreFactory(dataDirectory))
                .build()
            // authorize
            val receiver: LocalServerReceiver = LocalServerReceiver.Builder()
                .setHost("localhost")
                .setPort(-1)
                .build()
            val jwt = MyAuthApp(authorization, receiver)
                .fetchJwt()
            return jwt.idToken
        }
    }

    class MyAuthApp(
        flow: AuthorizationCodeFlow,
        receiver: LocalServerReceiver
    ) : AuthorizationCodeInstalledApp(flow, receiver) {
        fun fetchJwt(): IdTokenResponse {
            try {
                val credential: Credential? = flow.loadCredential("user")
                credential?.apply {
                    val response = RefreshTokenRequest(
                        transport,
                        jsonFactory,
                        GenericUrl(tokenServerEncodedUrl),
                        refreshToken
                    )
                        .setClientAuthentication(clientAuthentication)
                        .setRequestInitializer(requestInitializer)
                        .execute()
                    // response doesn't have refresh token!
//                    response.refreshToken = refreshToken
//                    flow.createAndStoreCredential(response, "user")
                    return IdTokenResponse()
                        .setIdToken(response["id_token"] as String)
                        .setAccessToken(response.accessToken)
                        .setExpiresInSeconds(response.expiresInSeconds)
                        .setRefreshToken(response.refreshToken)
                        .setTokenType(response.accessToken)
                }
                // open in browser
                val redirectUri = receiver.redirectUri
                val authorizationUrl: AuthorizationCodeRequestUrl =
                    flow.newAuthorizationUrl().setRedirectUri(redirectUri)
                onAuthorization(authorizationUrl)
                // receive authorization code and exchange it for an access token
                val code = receiver.waitForCode()
                val response = IdTokenResponse.execute(
                    flow
                        .newTokenRequest(code)
                        .setRedirectUri(redirectUri)
                )
                // store credential and return it
                flow.createAndStoreCredential(response, "user")
                return response
            } finally {
                receiver.stop()
            }
        }
    }
}
