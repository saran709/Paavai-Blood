package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

enum class SupabaseSyncStatus {
    NOT_CONFIGURED,
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY

    private val _syncStatus = MutableStateFlow(SupabaseSyncStatus.IDLE)
    val syncStatus: StateFlow<SupabaseSyncStatus> = _syncStatus

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    init {
        if (!isConfigured()) {
            _syncStatus.value = SupabaseSyncStatus.NOT_CONFIGURED
        }
    }

    fun isConfigured(): Boolean {
        return supabaseUrl.isNotEmpty() && 
               supabaseKey.isNotEmpty() && 
               supabaseUrl != "MY_SUPABASE_URL_PLACEHOLDER" && 
               supabaseKey != "MY_SUPABASE_ANON_KEY_PLACEHOLDER" &&
               supabaseUrl.startsWith("http")
    }

    /**
     * Executes two-way synchronization between Room and Supabase.
     */
    suspend fun syncAll(dao: BloodConnectDao) = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.d(TAG, "Supabase not configured, skipping sync.")
            _syncStatus.value = SupabaseSyncStatus.NOT_CONFIGURED
            return@withContext
        }

        _syncStatus.value = SupabaseSyncStatus.SYNCING
        _syncErrorMessage.value = null

        try {
            // 1. Sync Donors
            syncDonors(dao)

            // 2. Sync Blood Requests
            syncBloodRequests(dao)

            // 3. Sync Donation Camps
            syncDonationCamps(dao)

            // 4. Sync Donation History
            syncDonationHistory(dao)

            // 5. Sync User Accounts
            syncUserAccounts(dao)

            // Success Update
            _syncStatus.value = SupabaseSyncStatus.SUCCESS
            val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            _lastSyncTime.value = currentTime
            Log.d(TAG, "Supabase synchronization completed successfully at $currentTime")
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync failed: ${e.message}", e)
            _syncStatus.value = SupabaseSyncStatus.ERROR
            _syncErrorMessage.value = e.localizedMessage ?: "Unknown cloud network error"
        }
    }

    private suspend fun syncDonors(dao: BloodConnectDao) {
        val donorAdapter = moshi.adapter<List<Donor>>(Types.newParameterizedType(List::class.java, Donor::class.java))
        
        // A. Fetch remote donors
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/donors?select=*")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer $supabaseKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val remoteDonors = donorAdapter.fromJson(body)
                    remoteDonors?.forEach { remote ->
                        val local = dao.getDonorByRegisterNumber(remote.registerNumber)
                        if (local == null) {
                            dao.insertDonor(remote.copy(id = 0)) // insert new local
                        } else {
                            // Update local copy with remote values (prefer remote for sync)
                            dao.updateDonor(remote.copy(id = local.id))
                        }
                    }
                }
            } else {
                if (response.code == 404) {
                    throw IllegalStateException("Table 'donors' not found in Supabase. Please ensure your Supabase database has been initialized with the 'donors' table.")
                }
                throw IllegalStateException("Failed to fetch Donors: ${response.code} ${response.message}")
            }
        }

        // B. Push local donors that are newer or all simple upsert
        val localDonorsFlow = dao.getAllDonors()
        val localDonors = localDonorsFlow.firstOrNull() ?: emptyList()
        if (localDonors.isNotEmpty()) {
            val jsonToPush = donorAdapter.toJson(localDonors.map { it.copy(id = 0) }) // remove local primary keys, let supabase handle or upsert based on registerNumber
            val postBody = jsonToPush.toRequestBody(jsonMediaType)
            val pushRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/donors")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates") // upsert based on unique constraint (e.g., registerNumber)
                .post(postBody)
                .build()

            okHttpClient.newCall(pushRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Local donor push returned code: ${response.code}")
                }
            }
        }
    }

    private suspend fun syncBloodRequests(dao: BloodConnectDao) {
        val requestAdapter = moshi.adapter<List<BloodRequest>>(Types.newParameterizedType(List::class.java, BloodRequest::class.java))

        // A. Fetch remote requests
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/blood_requests?select=*")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer $supabaseKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val remoteRequests = requestAdapter.fromJson(body)
                    remoteRequests?.forEach { remote ->
                        // Since we are using standard ID, we overwrite locally by remote ID
                        // Let's make sure it's inserted cleanly
                        dao.insertRequest(remote)
                    }
                }
            } else {
                if (response.code == 404) {
                    throw IllegalStateException("Table 'blood_requests' not found in Supabase. Please initialize your database tables.")
                }
                throw IllegalStateException("Failed to fetch Blood Requests: ${response.code}")
            }
        }

        // B. Push local requests
        val localRequestsFlow = dao.getAllRequests()
        val localRequests = localRequestsFlow.firstOrNull() ?: emptyList()
        if (localRequests.isNotEmpty()) {
            val jsonToPush = requestAdapter.toJson(localRequests)
            val postBody = jsonToPush.toRequestBody(jsonMediaType)
            val pushRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/blood_requests")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(postBody)
                .build()

            okHttpClient.newCall(pushRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Local requests push returned: ${response.code}")
                }
            }
        }
    }

    private suspend fun syncDonationCamps(dao: BloodConnectDao) {
        val campAdapter = moshi.adapter<List<DonationCamp>>(Types.newParameterizedType(List::class.java, DonationCamp::class.java))

        // A. Fetch remote camps
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/donation_camps?select=*")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer $supabaseKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val remoteCamps = campAdapter.fromJson(body)
                    remoteCamps?.forEach { remote ->
                        dao.insertCamp(remote)
                    }
                }
            } else {
                if (response.code == 404) {
                    throw IllegalStateException("Table 'donation_camps' not found in Supabase.")
                }
                throw IllegalStateException("Failed to fetch Camps: ${response.code}")
            }
        }

        // B. Push local camps
        val localCampsFlow = dao.getAllCamps()
        val localCamps = localCampsFlow.firstOrNull() ?: emptyList()
        if (localCamps.isNotEmpty()) {
            val jsonToPush = campAdapter.toJson(localCamps)
            val postBody = jsonToPush.toRequestBody(jsonMediaType)
            val pushRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/donation_camps")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(postBody)
                .build()

            okHttpClient.newCall(pushRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Local camps push returned: ${response.code}")
                }
            }
        }
    }

    private suspend fun syncDonationHistory(dao: BloodConnectDao) {
        val historyAdapter = moshi.adapter<List<DonationHistory>>(Types.newParameterizedType(List::class.java, DonationHistory::class.java))

        // A. Fetch remote history
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/donation_history?select=*")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer $supabaseKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val remoteHistory = historyAdapter.fromJson(body)
                    remoteHistory?.forEach { remote ->
                        dao.insertHistory(remote)
                    }
                }
            } else {
                if (response.code == 404) {
                    throw IllegalStateException("Table 'donation_history' not found in Supabase.")
                }
                throw IllegalStateException("Failed to fetch History: ${response.code}")
            }
        }

        // B. Push local history
        val localHistoryFlow = dao.getAllHistory()
        val localHistory = localHistoryFlow.firstOrNull() ?: emptyList()
        if (localHistory.isNotEmpty()) {
            val jsonToPush = historyAdapter.toJson(localHistory)
            val postBody = jsonToPush.toRequestBody(jsonMediaType)
            val pushRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/donation_history")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(postBody)
                .build()

            okHttpClient.newCall(pushRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Local history push returned: ${response.code}")
                }
            }
        }
    }

    private suspend fun syncUserAccounts(dao: BloodConnectDao) {
        val accountAdapter = moshi.adapter<List<UserAccountEntity>>(Types.newParameterizedType(List::class.java, UserAccountEntity::class.java))

        // A. Fetch remote user accounts
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_accounts?select=*")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer $supabaseKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val remoteAccounts = accountAdapter.fromJson(body)
                    remoteAccounts?.forEach { remote ->
                        val local = dao.getUserAccountByEmail(remote.email)
                        if (local == null) {
                            dao.insertUserAccount(remote)
                        } else {
                            // Update local copy with remote database credentials if different
                            if (local != remote) {
                                dao.updateUserAccount(remote)
                            }
                        }
                    }
                }
            } else {
                if (response.code == 404) {
                    throw IllegalStateException("Table 'user_accounts' not found in Supabase. Please ensure your Supabase database has been initialized with the 'user_accounts' table.")
                }
                throw IllegalStateException("Failed to fetch User Accounts: ${response.code} ${response.message}")
            }
        }

        // B. Push local user accounts
        val localAccounts = dao.getUserAccountsList()
        if (localAccounts.isNotEmpty()) {
            val jsonToPush = accountAdapter.toJson(localAccounts)
            val postBody = jsonToPush.toRequestBody(jsonMediaType)
            val pushRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_accounts")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(postBody)
                .build()

            okHttpClient.newCall(pushRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Local user accounts push returned: ${response.code}")
                }
            }
        }
    }

    /**
     * Queries the 'donors' table from Supabase using filter parameters.
     */
    suspend fun filterDonorsFromSupabase(bloodGroup: String, availability: Boolean?): List<Donor> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.d(TAG, "Supabase not configured, returning empty list.")
            return@withContext emptyList()
        }

        val donorAdapter = moshi.adapter<List<Donor>>(Types.newParameterizedType(List::class.java, Donor::class.java))

        val params = mutableListOf<String>()
        params.add("select=*")
        if (bloodGroup.isNotEmpty() && bloodGroup != "All") {
            val encodedBg = java.net.URLEncoder.encode("eq.$bloodGroup", "UTF-8")
            params.add("bloodGroup=$encodedBg")
        }
        if (availability != null) {
            params.add("availability=eq.$availability")
        }

        val query = params.joinToString("&")
        val url = "$supabaseUrl/rest/v1/donors?$query"

        val request = Request.Builder()
            .url(url)
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer $supabaseKey")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val list = donorAdapter.fromJson(body)
                        return@withContext list ?: emptyList()
                    }
                } else {
                    Log.e(TAG, "Supabase filtering returned code: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase query: ${e.message}", e)
        }
        return@withContext emptyList()
    }
}
