package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "donors",
    indices = [
        Index(value = ["registerNumber"], unique = true),
        Index(value = ["bloodGroup"]),
        Index(value = ["email"])
    ]
)
data class Donor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val registerNumber: String,
    val department: String,
    val year: String,  // "1st Year", "2nd Year", "3rd Year", "4th Year", "Faculty"
    val bloodGroup: String, // "A+", "A-", etc.
    val mobileNumber: String,
    val email: String,
    val location: String, // e.g. "Paavai Campus", "Namakkal", "Salem", "Erode"
    val weight: Double,
    val lastDonationDate: String, // "YYYY-MM-DD" or empty
    val userType: String = "Student", // "Student", "Faculty", "Local Volunteer"
    val availability: Boolean = true,
    val totalDonations: Int = 0,
    val gender: String = "Male",
    val dob: String = "2005-01-01",
    val address: String = "Namakkal, Tamil Nadu",
    val emergencyContact: String = "+91 9900998877",
    val profilePhoto: String = ""
) : Serializable

@Entity(
    tableName = "blood_requests",
    indices = [
        Index(value = ["bloodGroup"]),
        Index(value = ["hospitalName"]),
        Index(value = ["status"])
    ]
)
data class BloodRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bloodGroup: String,
    val unitsRequired: Int,
    val hospitalName: String,
    val patientName: String,
    val urgencyLevel: String, // "Critical", "High", "Normal"
    val contactName: String,
    val contactNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFulfilled: Boolean = false,
    val simulatedAlertsSent: Boolean = false,
    val status: String = "Requested", // "Requested", "Donor Found", "Blood Collected", "Delivered"
    val requiredDate: String = "",
    val specialInstructions: String = "",
    val donorRating: Int = 0,
    val platformRating: Int = 0,
    val feedbackComment: String = ""
) : Serializable

@Entity(
    tableName = "donation_camps",
    indices = [
        Index(value = ["date"])
    ]
)
data class DonationCamp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // "YYYY-MM-DD"
    val time: String, // "09:00 AM - 04:00 PM"
    val venue: String, // "Paavai Audi", "Paavai Health Center"
    val Description: String,
    val registeredCount: Int = 0
) : Serializable

@Entity(
    tableName = "donation_history",
    indices = [
        Index(value = ["donorRegisterNumber"]),
        Index(value = ["bloodGroup"])
    ]
)
data class DonationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val donorName: String,
    val donorRegisterNumber: String,
    val date: String,
    val bloodGroup: String,
    val unitsDonated: Int = 1,
    val hospitalName: String = "Paavai Blood Camp"
) : Serializable

@Entity(
    tableName = "user_accounts",
    indices = [
        Index(value = ["registerNumber"])
    ]
)
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val password: String,
    val name: String,
    val registerNumber: String,
    val role: String, // "Admin", "Volunteer", "Student Donor"
    val department: String = "B.E. Computer Science",
    val year: String = "3rd Year",
    val bloodGroup: String = "O-",
    val phone: String = "9876543210"
) : Serializable

@Entity(
    tableName = "donor_notifications",
    indices = [
        Index(value = ["donorRegisterNumber"]),
        Index(value = ["requestId"]),
        Index(value = ["isRead"])
    ]
)
data class DonorNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val donorRegisterNumber: String, // Destination registered donor number
    val requestId: Int, // The broadcast request ID
    val bloodGroup: String, // Requested blood group
    val hospitalName: String,
    val patientName: String,
    val urgencyLevel: String,
    val content: String, // Notification message text
    var isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
