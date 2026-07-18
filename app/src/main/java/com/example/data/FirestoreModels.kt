package com.example.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.Date

/**
 * Firestore-compatible Model representing a Donor Profile.
 *
 * This structure is optimized for Cloud Firestore:
 * - Includes default parameters for no-arg constructor deserialization.
 * - Uses [DocumentId] to map the document ID.
 * - Uses standard Firestore data types (such as Long, Double, Boolean, String, and Date).
 * - Implements [Serializable] for convenient intent/navigation transfers.
 */
data class FirestoreDonorProfile(
    @DocumentId
    val documentId: String = "",

    val name: String = "",
    val registerNumber: String = "",
    val department: String = "",
    val year: String = "", // e.g., "1st Year", "2nd Year", "3rd Year", "4th Year", "Faculty"
    
    @get:PropertyName("blood_group")
    @set:PropertyName("blood_group")
    var bloodGroup: String = "", // e.g., "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"
    
    val mobileNumber: String = "",
    val email: String = "",
    val location: String = "",
    val weight: Double = 0.0,
    
    @get:PropertyName("last_donation_date")
    @set:PropertyName("last_donation_date")
    var lastDonationDate: String = "", // formatted as YYYY-MM-DD
    
    val userType: String = "Student", // "Student", "Faculty", "Local Volunteer"
    
    @get:PropertyName("availability")
    @set:PropertyName("availability")
    var availability: Boolean = true,
    
    val totalDonations: Int = 0,
    val gender: String = "Male",
    val dob: String = "2005-01-01",
    val address: String = "",
    val emergencyContact: String = "",
    val profilePhoto: String = "",
    
    @ServerTimestamp
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Date? = null
) : Serializable
