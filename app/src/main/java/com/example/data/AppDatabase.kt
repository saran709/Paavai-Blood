package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Donor::class,
        BloodRequest::class,
        DonationCamp::class,
        DonationHistory::class,
        UserAccountEntity::class,
        DonorNotification::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodConnectDao(): BloodConnectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bloodconnect_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.bloodConnectDao())
                }
            }
        }

        suspend fun populateDatabase(dao: BloodConnectDao) {
            // Prepopulate some realistic Paavai students, faculty & alumni donors
            val initialDonors = listOf(
                Donor(
                    name = "Saran Ramesh",
                    registerNumber = "22104085",
                    department = "B.E. Computer Science",
                    year = "3rd Year",
                    bloodGroup = "O-",
                    mobileNumber = "+91 9876543210",
                    email = "saranramesh709@paavai.edu.in",
                    location = "Paavai Engineering Campus",
                    weight = 68.5,
                    lastDonationDate = "2026-02-15",
                    userType = "Student",
                    availability = true,
                    totalDonations = 3
                ),
                Donor(
                    name = "Ramesh Kumar",
                    registerNumber = "99402511",
                    department = "MCA",
                    year = "2nd Year",
                    bloodGroup = "A+",
                    mobileNumber = "+91 9443210987",
                    email = "ramesh.k@paavai.edu.in",
                    location = "Namakkal Main Hospital Area",
                    weight = 74.0,
                    lastDonationDate = "2026-04-10",
                    userType = "Student",
                    availability = true,
                    totalDonations = 6
                ),
                Donor(
                    name = "Dr. Priya Selvan",
                    registerNumber = "FAC0532",
                    department = "Bio-Technology",
                    year = "Faculty",
                    bloodGroup = "B+",
                    mobileNumber = "+91 9894123456",
                    email = "priyaselvan@paavai.edu.in",
                    location = "Paavai College Hostel Area",
                    weight = 59.0,
                    lastDonationDate = "2026-01-05",
                    userType = "Faculty",
                    availability = true,
                    totalDonations = 5
                ),
                Donor(
                    name = "Arun Karthik",
                    registerNumber = "23103405",
                    department = "B.Tech Information Tech",
                    year = "2nd Year",
                    bloodGroup = "O+",
                    mobileNumber = "+91 8870123123",
                    email = "arunkarthik@paavai.edu.in",
                    location = "Salem Gate Quarter",
                    weight = 71.0,
                    lastDonationDate = "2026-05-15", // Donated recently! So ineligible for next ~3 months (3 months rule = 90 days, we are currently at may 29, so ineligible!)
                    userType = "Student",
                    availability = false,
                    totalDonations = 1
                ),
                Donor(
                    name = "Sneha Srinivasan",
                    registerNumber = "21102904",
                    department = "B.E. Electronics & Comm",
                    year = "4th Year",
                    bloodGroup = "AB+",
                    mobileNumber = "+91 7373112233",
                    email = "snehars@paavai.edu.in",
                    location = "Paavai Engineering Campus",
                    weight = 52.0,
                    lastDonationDate = "2025-11-20",
                    userType = "Student",
                    availability = true,
                    totalDonations = 4
                ),
                Donor(
                    name = "Vignesh Murugan",
                    registerNumber = "24105080",
                    department = "B.Tech Artificial Intelligence",
                    year = "1st Year",
                    bloodGroup = "A-",
                    mobileNumber = "+91 9965009988",
                    email = "vigneshm@paavai.edu.in",
                    location = "Rasipuram Terminal",
                    weight = 46.0, // Weighs less than 45 or 50? Usually <50kg is ineligible. Let's show 46kg - weight restriction might make last in line or ineligible depends!
                    lastDonationDate = "2025-08-10",
                    userType = "Student",
                    availability = true,
                    totalDonations = 2
                )
            )

            initialDonors.forEach { dao.insertDonor(it) }

            // Prepopulate realistic emergency blood requests
            val initialRequests = listOf(
                BloodRequest(
                    bloodGroup = "O-",
                    unitsRequired = 3,
                    hospitalName = "Paavai Multi Speciality Hospital, Namakkal",
                    patientName = "S. Muthusamy (Road Emergency)",
                    urgencyLevel = "Critical",
                    contactName = "R. Selvam",
                    contactNumber = "+91 9488310293",
                    timestamp = System.currentTimeMillis() - 4 * 3600 * 1000, // 4 hrs ago
                    isFulfilled = false,
                    simulatedAlertsSent = true
                ),
                BloodRequest(
                    bloodGroup = "A+",
                    unitsRequired = 2,
                    hospitalName = "Namakkal Government Headquarters Hospital",
                    patientName = "Baby of Deepa (Paediatric Surgery)",
                    urgencyLevel = "High",
                    contactName = "M. Ramesh (Father)",
                    contactNumber = "+91 9003554411",
                    timestamp = System.currentTimeMillis() - 24 * 3600 * 1000, // 24 hrs ago
                    isFulfilled = true,
                    simulatedAlertsSent = true
                ),
                BloodRequest(
                    bloodGroup = "B+",
                    unitsRequired = 1,
                    hospitalName = "Salem Government Hospital",
                    patientName = "K. Chinnasamy (Cardiac Bypass)",
                    urgencyLevel = "Normal",
                    contactName = "C. Jayakumar",
                    contactNumber = "+91 9955118822",
                    timestamp = System.currentTimeMillis() - 36 * 3600 * 1000,
                    isFulfilled = false,
                    simulatedAlertsSent = false
                )
            )

            initialRequests.forEach { dao.insertRequest(it) }

            // Prepopulate some camps
            val initialCamps = listOf(
                DonationCamp(
                    title = "Annual Paavai Institutions Mega Blood Camp",
                    date = "2026-06-14",
                    time = "09:00 AM - 04:30 PM",
                    venue = "Paavai Golden Jubilee Auditorium",
                    Description = "In collaboration with Salem government blood bank. Special certificates, appreciation badges, and energy drinks provided to all donors.",
                    registeredCount = 42
                ),
                DonationCamp(
                    title = "Emergency Red Cross Cadet Camp",
                    date = "2026-07-02",
                    time = "10:00 AM - 03:00 PM",
                    venue = "Health Center, Paavai Tech Campus",
                    Description = "Organized specifically for faculty members and technical staff to bolster Rare Blood reserves.",
                    registeredCount = 15
                )
            )

            initialCamps.forEach { dao.insertCamp(it) }

            // Prepopulate some histories (for leaderboards & dashboard)
            val initialHistory = listOf(
                DonationHistory(
                    donorName = "Ramesh Kumar",
                    donorRegisterNumber = "99402511",
                    date = "2026-04-10",
                    bloodGroup = "A+",
                    unitsDonated = 1,
                    hospitalName = "Namakkal GH Office"
                ),
                DonationHistory(
                    donorName = "Saran Ramesh",
                    donorRegisterNumber = "22104085",
                    date = "2026-02-15",
                    bloodGroup = "O-",
                    unitsDonated = 1,
                    hospitalName = "Paavai Hospital"
                ),
                DonationHistory(
                    donorName = "Dr. Priya Selvan",
                    donorRegisterNumber = "FAC0532",
                    date = "2026-01-05",
                    bloodGroup = "B+",
                    unitsDonated = 1,
                    hospitalName = "Paavai Mega Camp"
                ),
                DonationHistory(
                    donorName = "Sneha Srinivasan",
                    donorRegisterNumber = "21102904",
                    date = "2025-11-20",
                    bloodGroup = "AB+",
                    unitsDonated = 2,
                    hospitalName = "Salem HQ Camp"
                ),
                DonationHistory(
                    donorName = "Vignesh Murugan",
                    donorRegisterNumber = "24105080",
                    date = "2025-08-10",
                    bloodGroup = "A-",
                    unitsDonated = 1,
                    hospitalName = "Muthusamy Memorial Red Cross"
                )
            )

            initialHistory.forEach { dao.insertHistory(it) }

            // Prepopulate a single default admin user account (matching the credentials from ViewModel)
            val defaultAccounts = listOf(
                UserAccountEntity(
                    email = "admin@paavai.edu.in",
                    password = "blood@123",
                    name = "Administrator",
                    registerNumber = "ADM001",
                    role = "Admin"
                )
            )

            defaultAccounts.forEach { dao.insertUserAccount(it) }
        }
    }
}
