package com.portalapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PremiumApi(private val baseUrl: String = "https://consumer-api.premiumportal.id") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    companion object {
        private const val TAG = "PremiumApi"
    }

    /**
     * Get Netflix cookies from the API using the access token
     */
    fun getNetflixCookies(accessToken: String, onResult: (Result<List<CookieItem>>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/extensions/get-cookies")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API call failed", e)
                onResult(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "API error $response: $body")
                    onResult(Result.failure(Exception("API error: ${response.code}")))
                    return
                }

                try {
                    val json = JSONObject(body ?: "{}")
                    val data = json.optJSONObject("data") ?: json
                    val cookies = data.optJSONArray("cookies") ?: JSONArray()

                    val items = mutableListOf<CookieItem>()
                    for (i in 0 until cookies.length()) {
                        val c = cookies.getJSONObject(i)
                        items.add(CookieItem(
                            domain = c.optString("domain", ".netflix.com"),
                            name = c.optString("name", ""),
                            value = c.optString("value", ""),
                            path = c.optString("path", "/"),
                            secure = c.optBoolean("secure", true),
                            httpOnly = c.optBoolean("httpOnly", true)
                        ))
                    }
                    onResult(Result.success(items))
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error", e)
                    onResult(Result.failure(e))
                }
            }
        })
    }

    /**
     * Get items/credentials list
     */
    fun getItems(accessToken: String, type: String = "netflix", onResult: (Result<List<JSONObject>>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/items/creds")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    onResult(Result.failure(Exception("API error: ${response.code}")))
                    return
                }
                try {
                    val json = JSONObject(body ?: "{}")
                    val data = json.optJSONArray("data") ?: json.optJSONArray("items") ?: JSONArray()
                    val items = mutableListOf<JSONObject>()
                    for (i in 0 until data.length()) {
                        items.add(data.getJSONObject(i))
                    }
                    onResult(Result.success(items))
                } catch (e: Exception) {
                    onResult(Result.failure(e))
                }
            }
        })
    }
}

data class CookieItem(
    val domain: String,
    val name: String,
    val value: String,
    val path: String = "/",
    val secure: Boolean = true,
    val httpOnly: Boolean = true
)
