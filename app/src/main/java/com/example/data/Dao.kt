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

    @Query("UPDATE blood_requests SET status = :status WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: Int, status: String)

    @Query("UPDATE blood_requests SET donorRating = :donorRating, platformRating = :platformRating, feedbackComment = :comment WHERE id = :requestId")
    suspend fun submitRequestFeedback(requestId: Int, donorRating: Int, platformRating: Int, comment: String)

    @Query("DELETE FROM blood_requests WHERE id = :requestId")
    suspend fun deleteRequestById(requestId: Int)

    // --- CAMPS ---
    @Query("SELECT * FROM donation_camps ORDER BY date ASC")
    fun getAllCamps(): Flow<List<DonationCamp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamp(camp: DonationCamp)

    @Update
    suspend fun updateCamp(camp: DonationCamp)

    @Delete
    suspend fun deleteCamp(camp: DonationCamp)

    @Query("UPDATE donation_camps SET registeredCount = registeredCount + 1 WHERE id = :campId")
    suspend fun incrementCampRegistrationCount(campId: Int)

    // --- HISTORY ---
    @Query("SELECT * FROM donation_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<DonationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: DonationHistory)

    // --- USER ACCOUNTS ---
    @Query("SELECT * FROM user_accounts ORDER BY name ASC")
    fun getAllUserAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts")
    suspend fun getUserAccountsList(): List<UserAccountEntity>

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccountByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE registerNumber = :regNum LIMIT 1")
    suspend fun getUserAccountByRegisterNumber(regNum: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(userAccount: UserAccountEntity)

    @Update
    suspend fun updateUserAccount(userAccount: UserAccountEntity)

    @Delete
    suspend fun deleteUserAccount(userAccount: UserAccountEntity)

    // --- DONOR NOTIFICATIONS ---
    @Query("SELECT * FROM donor_notifications WHERE donorRegisterNumber = :regNum ORDER BY timestamp DESC")
    fun getNotificationsForDonor(regNum: String): Flow<List<DonorNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: DonorNotification): Long

    @Query("UPDATE donor_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("UPDATE donor_notifications SET isRead = 1 WHERE donorRegisterNumber = :regNum")
    suspend fun markAllNotificationsAsRead(regNum: String)

    @Query("DELETE FROM donor_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    @Query("DELETE FROM donor_notifications WHERE donorRegisterNumber = :regNum")
    suspend fun clearNotificationsForDonor(regNum: String)
}
