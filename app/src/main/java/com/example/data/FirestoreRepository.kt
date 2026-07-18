package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Service repository for managing Donor Profiles in Cloud Firestore.
 * Handles production-grade reads, writes, and advanced safety queries.
 */
class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val donorsCollection = firestore.collection("donors")

    companion object {
        private const val TAG = "FirestoreRepository"
    }

    /**
     * Creates or fully updates a donor profile in Cloud Firestore.
     * Uses registerNumber as the document ID to prevent duplicate profiles per student.
     */
    suspend fun saveDonorProfile(profile: FirestoreDonorProfile): Boolean {
        return try {
            val docId = profile.registerNumber.ifEmpty { profile.email }
            if (docId.isEmpty()) {
                Log.e(TAG, "Cannot save profile: Register number and email are both empty.")
                return false
            }
            
            // Explicitly set the updated timestamp
            val updatedProfile = profile.copy(updatedAt = null) // ServerTimestamp will set this on save
            
            donorsCollection.document(docId).set(updatedProfile).await()
            Log.d(TAG, "Donor profile successfully saved with ID: $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving donor profile to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Updates only the availability flag and latest update timestamp of a donor.
     */
    suspend fun updateAvailability(registerNumber: String, isAvailable: Boolean): Boolean {
        return try {
            donorsCollection.document(registerNumber)
                .update(
                    "availability", isAvailable,
                    "updated_at", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()
            Log.d(TAG, "Availability updated for $registerNumber to $isAvailable")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update availability: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches a donor profile by register number.
     */
    suspend fun getDonorByRegisterNumber(registerNumber: String): FirestoreDonorProfile? {
        return try {
            val snapshot = donorsCollection.document(registerNumber).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(FirestoreDonorProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching donor $registerNumber: ${e.message}", e)
            null
        }
    }

    /**
     * Queries Firestore for all available donors matching a specific blood group.
     * Uses composite query index: blood_group == [bloodGroup] AND availability == true.
     */
    suspend fun queryAvailableDonors(bloodGroup: String): List<FirestoreDonorProfile> {
        return try {
            val snapshot = donorsCollection
                .whereEqualTo("blood_group", bloodGroup)
                .whereEqualTo("availability", true)
                .get()
                .await()
            
            snapshot.toObjects(FirestoreDonorProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying available donors for blood group $bloodGroup: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Advanced Query: Queries available donors who are also clinically eligible.
     * This filters available donors and validates that their recovery interval is satisfied
     * (males: 90 days since last donation, females: 120 days since last donation).
     */
    suspend fun queryEligibleAndAvailableDonors(bloodGroup: String): List<FirestoreDonorProfile> {
        return try {
            // First fetch the set of active, available donors of this blood group
            val candidates = queryAvailableDonors(bloodGroup)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance()

            candidates.filter { donor ->
                // Check weight limit safety threshold
                if (donor.weight < 50.0) return@filter false
                
                // If they've never donated, they are immediately eligible
                if (donor.lastDonationDate.isEmpty()) return@filter true
                
                try {
                    val lastDate = sdf.parse(donor.lastDonationDate) ?: return@filter false
                    val calendar = Calendar.getInstance().apply { time = lastDate }
                    
                    // Determine gender recovery cycle rules (males: 90 days, females: 120 days)
                    val requiredInterval = if (donor.gender.lowercase(Locale.getDefault()) == "female") 120 else 90
                    calendar.add(Calendar.DAY_OF_YEAR, requiredInterval)
                    
                    // Eligible if the cooling period is completed
                    !today.before(calendar)
                } catch (pe: Exception) {
                    Log.e(TAG, "Error parsing donation date for ${donor.name}: ${donor.lastDonationDate}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing combined eligibility query: ${e.message}", e)
            emptyList()
        }
    }
}
