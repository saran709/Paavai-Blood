package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "donors")
data class Donor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val registerNumber: String,
    val department: String,
    val year: String,  // "1st Year", "2nd Year", "3rd Year", "4th Year", "Faculty", "Alumni"
    val bloodGroup: String, // "A+", "A-", etc.
    val mobileNumber: String,
    val email: String,
    val location: String, // e.g. "Paavai Campus", "Namakkal", "Salem", "Erode"
    val weight: Double,
    val lastDonationDate: String, // "YYYY-MM-DD" or empty
    val userType: String = "Student", // "Student", "Faculty", "Alumni", "Local Volunteer"
    val availability: Boolean = true,
    val totalDonations: Int = 0,
    val gender: String = "Male",
    val dob: String = "2005-01-01",
    val address: String = "Namakkal, Tamil Nadu",
    val emergencyContact: String = "+91 9900998877",
    val profilePhoto: String = ""
) : Serializable

@Entity(tableName = "blood_requests")
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
    val simulatedAlertsSent: Boolean = false
) : Serializable

@Entity(tableName = "donation_camps")
data class DonationCamp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // "YYYY-MM-DD"
    val time: String, // "09:00 AM - 04:00 PM"
    val venue: String, // "Paavai Audi", "Paavai Health Center"
    val Description: String,
    val registeredCount: Int = 0
) : Serializable

@Entity(tableName = "donation_history")
data class DonationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val donorName: String,
    val donorRegisterNumber: String,
    val date: String,
    val bloodGroup: String,
    val unitsDonated: Int = 1,
    val hospitalName: String = "Paavai Blood Camp"
) : Serializable
