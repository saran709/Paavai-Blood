package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodConnectDao {

    // --- DONORS ---
    @Query("SELECT * FROM donors ORDER BY name ASC")
    fun getAllDonors(): Flow<List<Donor>>

    @Query("SELECT * FROM donors WHERE registerNumber = :regNum LIMIT 1")
    suspend fun getDonorByRegisterNumber(regNum: String): Donor?

    @Query("SELECT * FROM donors WHERE bloodGroup = :bloodGroup ORDER BY lastDonationDate ASC")
    fun getDonorsByBloodGroup(bloodGroup: String): Flow<List<Donor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonor(donor: Donor)

    @Update
    suspend fun updateDonor(donor: Donor)

    @Delete
    suspend fun deleteDonor(donor: Donor)

    // --- BLOOD REQUESTS ---
    @Query("SELECT * FROM blood_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<BloodRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: BloodRequest): Long

    @Query("UPDATE blood_requests SET isFulfilled = :fulfilled WHERE id = :requestId")
    suspend fun setRequestFulfilled(requestId: Int, fulfilled: Boolean)

    @Query("DELETE FROM blood_requests WHERE id = :requestId")
    suspend fun deleteRequestById(requestId: Int)

    // --- CAMPS ---
    @Query("SELECT * FROM donation_camps ORDER BY date ASC")
    fun getAllCamps(): Flow<List<DonationCamp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamp(camp: DonationCamp)

    @Query("UPDATE donation_camps SET registeredCount = registeredCount + 1 WHERE id = :campId")
    suspend fun incrementCampRegistrationCount(campId: Int)

    // --- HISTORY ---
    @Query("SELECT * FROM donation_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<DonationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: DonationHistory)
}
