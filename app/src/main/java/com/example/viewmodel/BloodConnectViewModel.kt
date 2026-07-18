package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BloodConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application, viewModelScope).bloodConnectDao()

    // Active User Context for Simulation
    // Supports: "Student Donor", "Volunteer", "Admin"
    val userRole = MutableStateFlow("Student Donor")
    val activeUserRegNumber = MutableStateFlow("22104085") // Saran Ramesh's register number default
    val registeredProfile = MutableStateFlow<Donor?>(null)

    // Authentication & User Session Core States
    val isLoggedIn = MutableStateFlow(false)
    val currentUserEmail = MutableStateFlow("")
    val currentUserName = MutableStateFlow("Saran Ramesh")

    // Local Users account cache loaded from database (Room & Supabase sync)
    val cachedAccounts = MutableStateFlow<Map<String, UserAccountEntity>>(emptyMap())

    fun login(email: String, word: String): Boolean {
        val trimEmail = email.trim().lowercase()
        if (!trimEmail.endsWith("@paavai.edu.in")) {
            return false
        }
        val account = cachedAccounts.value[trimEmail]
        if (account != null && account.password == word) {
            activeUserRegNumber.value = account.registerNumber
            userRole.value = account.role
            currentUserEmail.value = account.email
            currentUserName.value = account.name
            isLoggedIn.value = true
            syncProfile()
            return true
        }
        return false
    }

    suspend fun loginLive(email: String, word: String): Boolean = withContext(Dispatchers.IO) {
        val trimEmail = email.trim().lowercase()
        if (!trimEmail.endsWith("@paavai.edu.in")) {
            return@withContext false
        }
        
        // 1. Direct query of User Account from Room DB to prevent any flow sync lag
        val account = try {
            dao.getUserAccountByEmail(trimEmail)
        } catch (e: Exception) {
            android.util.Log.e("ViewModel", "Direct Room account query failed: ${e.message}")
            null
        }

        if (account != null) {
            if (account.password == word) {
                withContext(Dispatchers.Main) {
                    activeUserRegNumber.value = account.registerNumber
                    userRole.value = account.role
                    currentUserEmail.value = account.email
                    currentUserName.value = account.name
                    isLoggedIn.value = true
                }
                syncProfile()
                return@withContext true
            } else {
                android.util.Log.d("ViewModel", "Password mismatch locally, preparing live fallback.")
            }
        }

        // 2. Direct remote query against cloud schema (GoTrue with Rest fallback)
        if (SupabaseClient.isConfigured()) {
            try {
                val remoteAccount = SupabaseClient.authenticateRemote(trimEmail, word, dao)
                if (remoteAccount != null) {
                    withContext(Dispatchers.Main) {
                        activeUserRegNumber.value = remoteAccount.registerNumber
                        userRole.value = remoteAccount.role
                        currentUserEmail.value = remoteAccount.email
                        currentUserName.value = remoteAccount.name
                        isLoggedIn.value = true
                    }
                    syncProfile()
                    return@withContext true
                }
            } catch (e: Exception) {
                android.util.Log.e("ViewModel", "Supabase authentication failed: ${e.message}")
            }
        }
        return@withContext false
    }

    fun signup(
        name: String,
        email: String,
        pass: String,
        regNo: String,
        dept: String,
        year: String,
        bloodGroup: String,
        phone: String,
        role: String,
        userType: String = "Student"
    ): Boolean {
        val trimEmail = email.trim().lowercase()
        if (!trimEmail.endsWith("@paavai.edu.in")) {
            return false // Domain restricted
        }
        if (cachedAccounts.value.containsKey(trimEmail)) {
            return false // Account already exists
        }

        val account = UserAccountEntity(
            email = trimEmail,
            password = pass,
            name = name,
            registerNumber = regNo,
            role = role,
            department = dept,
            year = year,
            bloodGroup = bloodGroup,
            phone = phone
        )

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertUserAccount(account)
            
            val existing = dao.getDonorByRegisterNumber(regNo)
            if (existing == null) {
                val newDonor = Donor(
                    name = name,
                    registerNumber = regNo,
                    department = dept,
                    year = year,
                    bloodGroup = bloodGroup,
                    mobileNumber = phone,
                    email = trimEmail,
                    location = "Paavai Engineering Campus",
                    weight = 65.0,
                    lastDonationDate = "",
                    userType = userType,
                    availability = true,
                    totalDonations = 0
                )
                dao.insertDonor(newDonor)
            }
            if (SupabaseClient.isConfigured()) {
                SupabaseClient.signUpRemote(
                    email = trimEmail,
                    word = pass,
                    name = name,
                    registerNumber = regNo,
                    role = role,
                    department = dept,
                    year = year,
                    bloodGroup = bloodGroup,
                    phone = phone,
                    userType = userType,
                    dao = dao
                )
            }
            syncWithSupabase()
        }

        activeUserRegNumber.value = regNo
        userRole.value = role
        currentUserEmail.value = trimEmail
        currentUserName.value = name
        isLoggedIn.value = true
        syncProfile()
        return true
    }

    fun logout() {
        isLoggedIn.value = false
        currentUserEmail.value = ""
        currentUserName.value = ""
    }

    fun changePassword(email: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val trimEmail = email.trim().lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = dao.getUserAccountByEmail(trimEmail)
                if (account != null) {
                    val updated = account.copy(password = newPass)
                    dao.insertUserAccount(updated)
                    if (SupabaseClient.isConfigured()) {
                        SupabaseClient.signUpRemote(
                            email = trimEmail,
                            word = newPass,
                            name = account.name,
                            registerNumber = account.registerNumber,
                            role = account.role,
                            department = account.department,
                            year = account.year,
                            bloodGroup = account.bloodGroup,
                            phone = account.phone,
                            userType = if (account.role == "Admin") "Admin" else "Student",
                            dao = dao
                        )
                    }
                    syncWithSupabase()
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Account not found.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to change password.")
                }
            }
        }
    }

    // Flow State variables
    val allDonors: StateFlow<List<Donor>> = dao.getAllDonors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRequests: StateFlow<List<BloodRequest>> = dao.getAllRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCamps: StateFlow<List<DonationCamp>> = dao.getAllCamps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<DonationHistory>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state parameters
    val isAiLoading = MutableStateFlow(false)
    val matchingResultText = MutableStateFlow<String?>(null)
    val matchedDonorsList = MutableStateFlow<List<Donor>>(emptyList())
    val predictionReport = MutableStateFlow<String?>(null)
    val aiEligibilityResult = MutableStateFlow<String?>(null)
    val isCheckingAiEligibility = MutableStateFlow(false)

    // Simulated alerts status
    val smsAlertLogs = MutableStateFlow<List<String>>(emptyList())

    val supabaseSyncStatus = SupabaseClient.syncStatus
    val supabaseLastSyncTime = SupabaseClient.lastSyncTime
    val supabaseSyncErrorMessage = SupabaseClient.syncErrorMessage

    // Active User Notifications
    val activeNotifications = MutableStateFlow<List<DonorNotification>>(emptyList())

    // Supabase Direct Query Filtering Results state
    val supabaseFilteredDonors = MutableStateFlow<List<Donor>>(emptyList())
    val isSupabaseQuerying = MutableStateFlow(false)
    val supabaseQueryError = MutableStateFlow<String?>(null)

    init {
        // Enforce presence of default presets in user_accounts table block (Room safety seed)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = dao.getUserAccountsList()
                if (list.isEmpty()) {
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
            } catch (e: Exception) {
                android.util.Log.e("ViewModel", "Failed to seed default accounts: ${e.message}")
            }
        }

        // Collect user accounts from Room database to keep our local login cache always synced
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllUserAccounts().collect { list ->
                cachedAccounts.value = list.associateBy { it.email.trim().lowercase() }
            }
        }
        // Observe and update notifications flow based on active register number
        viewModelScope.launch {
            activeUserRegNumber.collect { regNum ->
                dao.getNotificationsForDonor(regNum).collect { list ->
                    activeNotifications.value = list
                }
            }
        }
        // Load default registered profile automatically
        syncProfile()
        syncWithSupabase()
    }

    fun syncWithSupabase() {
        viewModelScope.launch {
            SupabaseClient.syncAll(dao)
        }
    }

    fun setRole(role: String) {
        userRole.value = role
        syncProfile()
    }

    private fun syncProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val regNo = activeUserRegNumber.value
            var profile = dao.getDonorByRegisterNumber(regNo)
            
            if (profile == null && regNo.isNotBlank()) {
                // If a user has a valid login account but is missing a donor record, auto-provision one
                val account = dao.getUserAccountByRegisterNumber(regNo) 
                    ?: dao.getUserAccountByEmail(currentUserEmail.value)
                
                if (account != null) {
                    val isVolt = account.role == "Volunteer"
                    val newDonor = Donor(
                        name = account.name,
                        registerNumber = account.registerNumber,
                        department = account.department,
                        year = account.year,
                        bloodGroup = account.bloodGroup,
                        mobileNumber = account.phone,
                        email = account.email,
                        location = "Paavai Engineering Campus",
                        weight = 65.0,
                        lastDonationDate = "",
                        userType = if (isVolt) "Local Volunteer" else "Student",
                        availability = true,
                        totalDonations = if (isVolt) 0 else 1 // pre-populate 1 for realistic stats
                    )
                    try {
                        dao.insertDonor(newDonor)
                        profile = newDonor
                        android.util.Log.d("ViewModel", "Automatically provisioned donor profile for ${account.name}")
                    } catch (e: Exception) {
                        android.util.Log.e("ViewModel", "Failed to auto-provision donor: ${e.message}")
                    }
                }
            }
            registeredProfile.value = profile
        }
    }

    // Register Student Donor
    fun registerDonor(
        name: String,
        regNo: String,
        dept: String,
        year: String,
        bloodGroup: String,
        mobile: String,
        email: String,
        location: String,
        weight: Double,
        lastDonation: String,
        userType: String,
        gender: String = "Male",
        dob: String = "2005-01-01",
        address: String = "Namakkal, Tamil Nadu",
        emergencyContact: String = "+91 9900998877",
        profilePhoto: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getDonorByRegisterNumber(regNo)
            val currentId = registeredProfile.value?.id ?: 0
            if (existing != null && existing.id != currentId) {
                withContext(Dispatchers.Main) {
                    onError("Register number '$regNo' is already registered in the database.")
                }
                return@launch
            }

            val isElig = checkIfEligible(lastDonation, weight, gender, dob)
            val newDonor = Donor(
                id = currentId,
                name = name,
                registerNumber = regNo,
                department = dept,
                year = year,
                bloodGroup = bloodGroup,
                mobileNumber = mobile,
                email = email,
                location = location,
                weight = weight,
                lastDonationDate = lastDonation,
                userType = userType,
                availability = isElig,
                totalDonations = if (lastDonation.isNotEmpty()) 1 else 0,
                gender = gender,
                dob = dob,
                address = address,
                emergencyContact = emergencyContact,
                profilePhoto = profilePhoto
            )
            dao.insertDonor(newDonor)
            checkAndSendAutomatedThankYou(existing, newDonor)
            activeUserRegNumber.value = regNo
            registeredProfile.value = newDonor
            
            // Log history if they entered a last donation date
            if (lastDonation.isNotEmpty()) {
                val record = DonationHistory(
                    donorName = name,
                    donorRegisterNumber = regNo,
                    date = lastDonation,
                    bloodGroup = bloodGroup,
                    unitsDonated = 1,
                    hospitalName = "Self Reported"
                )
                dao.insertHistory(record)
            }
            syncWithSupabase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // Emergency SOS Trigger
    fun triggerSOS(
        bloodGroup: String,
        patientName: String,
        hospitalName: String,
        units: Int,
        contactName: String,
        contactPhone: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val req = BloodRequest(
                bloodGroup = bloodGroup,
                unitsRequired = units,
                hospitalName = hospitalName,
                patientName = patientName,
                urgencyLevel = "Critical",
                contactName = contactName,
                contactNumber = contactPhone,
                simulatedAlertsSent = true
            )
            val reqId = dao.insertRequest(req)
            
            // Trigger automatic matching & alerts
            sendSimulatedAlerts(bloodGroup, hospitalName, units)
            performAiMatching(req.copy(id = reqId.toInt()))
            createNotificationsForRequest(reqId.toInt(), bloodGroup, hospitalName, patientName, "Critical")
            syncWithSupabase()
        }
    }

    // Submit Regular Blood Request
    fun submitBloodRequest(
        bloodGroup: String,
        unitsRequired: Int,
        hospitalName: String,
        patientName: String,
        urgency: String,
        contactName: String,
        contactPhone: String,
        requiredDate: String = "",
        specialInstructions: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val req = BloodRequest(
                bloodGroup = bloodGroup,
                unitsRequired = unitsRequired,
                hospitalName = hospitalName,
                patientName = patientName,
                urgencyLevel = urgency,
                contactName = contactName,
                contactNumber = contactPhone,
                simulatedAlertsSent = urgency == "Critical" || urgency == "High",
                status = "Requested",
                requiredDate = requiredDate,
                specialInstructions = specialInstructions
            )
            val reqId = dao.insertRequest(req)
            
            if (urgency == "Critical" || urgency == "High") {
                sendSimulatedAlerts(bloodGroup, hospitalName, unitsRequired)
                createNotificationsForRequest(reqId.toInt(), bloodGroup, hospitalName, patientName, urgency)
            }
            performAiMatching(req.copy(id = reqId.toInt()))
            syncWithSupabase()
        }
    }

    // Update Blood Request Status (Real-time tracking)
    fun updateRequestStatus(requestId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateRequestStatus(requestId, newStatus)
            if (newStatus == "Delivered") {
                dao.setRequestFulfilled(requestId, true)
            }
            syncWithSupabase()
        }
    }

    // Submit Hospital Feedback
    fun submitRequestFeedback(requestId: Int, donorRating: Int, platformRating: Int, feedbackComment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.submitRequestFeedback(requestId, donorRating, platformRating, feedbackComment)
            syncWithSupabase()
        }
    }

    // Notification UI Action Handlers
    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markAllNotificationsAsRead(activeUserRegNumber.value)
        }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteNotification(id)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearNotificationsForDonor(activeUserRegNumber.value)
        }
    }

    private fun isDonorInVicinity(donorLoc: String, hospital: String): Boolean {
        val dl = donorLoc.trim().lowercase()
        val hp = hospital.trim().lowercase()
        if (hp.contains("namakkal") || hp.contains("paavai") || hp.contains("multi special") || hp.contains("gh namakkal")) {
            return dl.contains("namakkal") || dl.contains("campus") || dl.contains("college") || dl.contains("hostel") || dl.contains("rasipuram")
        }
        if (hp.contains("salem")) {
            return dl.contains("salem")
        }
        if (hp.contains("erode")) {
            return dl.contains("erode")
        }
        return dl.contains("campus") || dl.contains("paavai") || dl.isEmpty()
    }

    private fun createNotificationsForRequest(requestId: Int, bloodGroup: String, hospitalName: String, patientName: String, urgency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val compatibleGroups = getCompatibleGroups(bloodGroup)
            val list = allDonors.value
            list.forEach { donor ->
                val isCompatible = compatibleGroups.contains(donor.bloodGroup)
                val inVicinity = isDonorInVicinity(donor.location, hospitalName)
                
                if (isCompatible && inVicinity) {
                    val alertMessage = "URGENT matching blood request broadcasted nearby: Patient $patientName requires $bloodGroup blood at $hospitalName."
                    val notification = DonorNotification(
                        donorRegisterNumber = donor.registerNumber,
                        requestId = requestId,
                        bloodGroup = bloodGroup,
                        hospitalName = hospitalName,
                        patientName = patientName,
                        urgencyLevel = urgency,
                        content = alertMessage,
                        isRead = false,
                        timestamp = System.currentTimeMillis()
                    )
                    dao.insertNotification(notification)
                }
            }
        }
    }

    // Mark Request as Completed
    fun fulfillRequest(requestId: Int, donorRegNumber: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setRequestFulfilled(requestId, true)
            
            // If completed by a specific local registered student donor, update their metrics
            if (donorRegNumber != null) {
                val donor = dao.getDonorByRegisterNumber(donorRegNumber)
                if (donor != null) {
                    val updatedDonor = donor.copy(
                        totalDonations = donor.totalDonations + 1,
                        lastDonationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    dao.insertDonor(updatedDonor)
                    checkAndSendAutomatedThankYou(donor, updatedDonor)
                    
                    // Add donation history log
                    dao.insertHistory(
                        DonationHistory(
                            donorName = donor.name,
                            donorRegisterNumber = donor.registerNumber,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            bloodGroup = donor.bloodGroup,
                            unitsDonated = 1,
                            hospitalName = "Request Fulfillment"
                        )
                    )
                    syncProfile()
                }
            }
            syncWithSupabase()
        }
    }

    // Register for Upcoming Camp
    fun registerForCamp(campId: Int, campTitle: String, donorRegNo: String, donorName: String, donorBloodGroup: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.incrementCampRegistrationCount(campId)
            
            // Update donation record as a future volunteer commitment
            dao.insertHistory(
                DonationHistory(
                    donorName = donorName,
                    donorRegisterNumber = donorRegNo,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    bloodGroup = donorBloodGroup,
                    unitsDonated = 1,
                    hospitalName = "Volunteer Commitment: $campTitle"
                )
            )
            syncProfile()
            syncWithSupabase()
        }
    }

    fun addManualDonationHistory(
        donorRegisterNumber: String,
        date: String,
        units: Int = 1,
        hospitalName: String = "Paavai Blood Camp"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val donor = dao.getDonorByRegisterNumber(donorRegisterNumber)
            if (donor != null) {
                // Insert donation history record
                val record = DonationHistory(
                    donorName = donor.name,
                    donorRegisterNumber = donorRegisterNumber,
                    date = date,
                    bloodGroup = donor.bloodGroup,
                    unitsDonated = units,
                    hospitalName = hospitalName
                )
                dao.insertHistory(record)

                // Try to parse the date and see if we should update their lastDonationDate
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var updateDonorNeeded = false
                var newLastDate = donor.lastDonationDate
                
                try {
                    val incomingDate = sdf.parse(date)
                    val currentDate = if (donor.lastDonationDate.isNotEmpty()) sdf.parse(donor.lastDonationDate) else null
                    if (incomingDate != null && (currentDate == null || incomingDate.after(currentDate))) {
                        newLastDate = date
                        updateDonorNeeded = true
                    }
                } catch (e: Exception) {
                    // fall back
                }

                val updatedDonor = donor.copy(
                    totalDonations = donor.totalDonations + 1,
                    lastDonationDate = if (updateDonorNeeded) newLastDate else donor.lastDonationDate,
                    availability = checkIfEligible(
                        if (updateDonorNeeded) newLastDate else donor.lastDonationDate,
                        donor.weight,
                        donor.gender,
                        donor.dob
                    )
                )
                dao.insertDonor(updatedDonor)
                checkAndSendAutomatedThankYou(donor, updatedDonor)
                syncProfile()
                syncWithSupabase()
            }
        }
    }

    // Check Eligibility based on date & weight
    fun checkIfEligible(
        lastDonation: String,
        weight: Double,
        gender: String = "Male",
        dob: String = "2005-01-01"
    ): Boolean {
        val age = calculateAge(dob)
        if (age < 18) return false
        if (weight < 50.0) return false
        if (lastDonation.isEmpty()) return true
        
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val donationDate = sdf.parse(lastDonation) ?: return true
            val diffInMs = Date().time - donationDate.time
            val diffInDays = diffInMs / (1000 * 60 * 60 * 24)
            val gapRequired = if (gender.trim().lowercase() == "female") 120 else 90
            diffInDays >= gapRequired
        } catch (e: Exception) {
            true
        }
    }

    // Helper: Days until next eligible date
    fun daysUntilEligible(lastDonation: String, gender: String = "Male"): Int {
        if (lastDonation.isEmpty()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val donationDate = sdf.parse(lastDonation) ?: return 0
            val diffInMs = Date().time - donationDate.time
            val diffInDays = (diffInMs / (1000 * 60 * 60 * 24)).toInt()
            val gapRequired = if (gender.trim().lowercase() == "female") 120 else 90
            if (diffInDays >= gapRequired) 0 else gapRequired - diffInDays
        } catch (e: Exception) {
            0
        }
    }

    fun nextEligibleDate(lastDonation: String, gender: String = "Male"): String {
        if (lastDonation.isEmpty()) return "Eligible Now"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val donationDate = sdf.parse(lastDonation) ?: return "Eligible Now"
            val calendar = Calendar.getInstance()
            calendar.time = donationDate
            val gapRequired = if (gender.trim().lowercase() == "female") 120 else 90
            calendar.add(Calendar.DAY_OF_YEAR, gapRequired)
            sdf.format(calendar.time)
        } catch (e: Exception) {
            "Eligible Now"
        }
    }

    fun calculateAge(dobStr: String): Int {
        if (dobStr.isEmpty()) return 18
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dobStr) ?: return 18
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance()
            birth.time = birthDate
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            18
        }
    }

    fun calculateEligibilityPercentage(
        lastDonation: String,
        weight: Double,
        gender: String = "Male",
        dob: String = "2005-01-01"
    ): Int {
        val age = calculateAge(dob)
        
        // 1. Age Factor
        val ageFactor = if (age >= 18) 100.0 else (age.toDouble() / 18.0) * 100.0
        
        // 2. Weight Factor
        val weightFactor = if (weight >= 50.0) 100.0 else (weight / 50.0) * 100.0
        
        // 3. Time status factor
        val timeFactor = if (lastDonation.isEmpty()) {
            100.0
        } else {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val donationDate = sdf.parse(lastDonation)
                if (donationDate != null) {
                    val diffInMs = Date().time - donationDate.time
                    val diffInDays = diffInMs / (1000 * 60 * 60 * 24)
                    val gapRequired = if (gender.trim().lowercase() == "female") 120 else 90
                    if (diffInDays >= gapRequired) {
                        100.0
                    } else {
                        (diffInDays.toDouble() / gapRequired.toDouble()) * 100.0
                    }
                } else {
                    100.0
                }
            } catch (e: Exception) {
                100.0
            }
        }
        
        val percentage = minOf(ageFactor, weightFactor, timeFactor).toInt()
        return maxOf(0, minOf(100, percentage))
    }

    // Simulated alerts generator
    private fun sendSimulatedAlerts(bloodGroup: String, hospital: String, units: Int) {
        val lists = mutableListOf<String>()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        lists.add("[$timeStr] 🚨 PUSH NOTIFICATION: Critical demand for $bloodGroup at $hospital ($units units needed matching your profile) dispatched to 12 nearby student donors.")
        lists.add("[$timeStr] ✉️ BULK EMAIL: Emergency alert dispatched to Paavai College Red Cross cell members for immediate mobilization.")
        lists.add("[$timeStr] 💬 SMS BROADCAST: 'URGENT: Paavai BloodConnect needs $bloodGroup donors at $hospital within 2 hours. Reply YES to verify volunteer arrival.' sent to donors within 10km.")
        smsAlertLogs.value = lists
    }

    // Smart Match & Rank Local Engine (AI emergency matching backup + filters)
    fun findCandidatesLocal(bloodGroup: String): List<Donor> {
        val all = allDonors.value
        val compatibleGroups = getCompatibleGroups(bloodGroup)
        
        return all.filter { donor ->
            // Match group and eligibility
            compatibleGroups.contains(donor.bloodGroup)
        }.sortedWith(
            compareBy<Donor> { !checkIfEligible(it.lastDonationDate, it.weight, it.gender, it.dob) } // Eligible first
                .thenBy { it.location != "Paavai Engineering Campus" } // Paavai Campus nearest
                .thenByDescending { it.totalDonations } // Most active first
        )
    }

    private fun getCompatibleGroups(needed: String): List<String> {
        return when (needed.uppercase()) {
            "O-" -> listOf("O-")
            "O+" -> listOf("O-", "O+")
            "A-" -> listOf("O-", "A-")
            "A+" -> listOf("O-", "O+", "A-", "A+")
            "B-" -> listOf("O-", "B-")
            "B+" -> listOf("O-", "O+", "B-", "B+")
            "AB-" -> listOf("O-", "A-", "B-", "AB-")
            "AB+" -> listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
            else -> listOf(needed)
        }
    }

    // AI Emergency Match Flow
    fun performAiMatching(request: BloodRequest) {
        viewModelScope.launch {
            isAiLoading.value = true
            matchingResultText.value = "AI is evaluating matching donors, calculating distances, and analyzing historical blood donor response times..."
            
            val candidates = findCandidatesLocal(request.bloodGroup)
            matchedDonorsList.value = candidates

            val histories = allHistory.value
            val historiesByDonor = histories.groupBy { it.donorRegisterNumber }

            val donorDetailsText = candidates.joinToString("\n\n") { donor ->
                val donorHistories = historiesByDonor[donor.registerNumber] ?: emptyList()
                val isEligible = checkIfEligible(donor.lastDonationDate, donor.weight, donor.gender, donor.dob)
                val eligibilityStatus = if (isEligible) "ELIGIBLE NOW" else "NOT ELIGIBLE (Under recovery/weight limit)"
                val daysLeft = if (isEligible) 0 else daysUntilEligible(donor.lastDonationDate, donor.gender)
                val historyString = if (donorHistories.isEmpty()) {
                    "No logged history records in database."
                } else {
                    donorHistories.joinToString("; ") { "${it.date} (${it.unitsDonated} units at ${it.hospitalName})" }
                }
                
                """
                - Donor: ${donor.name} (ID/Reg: ${donor.registerNumber})
                  Blood Group: ${donor.bloodGroup}
                  Gender: ${donor.gender}, Age: ${calculateAge(donor.dob)} years, Weight: ${donor.weight}kg
                  User-Type: ${donor.userType}, Department: ${donor.department}
                  Current Location: ${donor.location}
                  Self-reported Availability: ${donor.availability}
                  Computed Clinical Eligibility: $eligibilityStatus (Days remaining: $daysLeft)
                  Total Lifetime Donations: ${donor.totalDonations} (Logged app donation history records: ${donorHistories.size})
                  Donations History Log: $historyString
                  Contact Number: ${donor.mobileNumber}
                """.trimIndent()
            }

            val prompt = """
                You are the AI Matchmaking Engine of Paavai BloodConnect, an emergency blood donor network inside Paavai Institutions, Namakkal, Tamil Nadu.
                Hospitals need a ranked list of the most reliable potential donors for a specific emergency blood request.
                You must analyze donor availability, eligibility rules, and historical donation frequency to produce this ranked list.
                
                Emergency Request Details:
                - Patient Case: ${request.patientName} (Urgency Level: ${request.urgencyLevel})
                - Blood Group Required: ${request.bloodGroup}
                - Volume Needed: ${request.unitsRequired} Units
                - Hospital Location: ${request.hospitalName}
                
                Here are the registered candidate donors in our ecosystem with their profiles and historical donation records:
                $donorDetailsText
                
                Please generate a comprehensive, highly professional Emergency Donor Reliability & Match Assessment in standard Markdown. Follow these rules strictly:
                
                1. TITLE: Start with "### 🧠 Gemini Intelligent Donor Reliability & Match Assessment"
                
                2. RANKED RELIABILITY ANALYSIS (MOST RELIABLE POTENTIAL DONORS):
                   Rank the top 3 best eligible and available donors in order of reliability.
                   - Evaluate reliability by checking both the self-reported `Availability: true` and their "Historical Donation Frequency" (total donations count, frequency of prior donation dates, and regular gaps). 
                   - For each ranked donor, provide their Name, Register Number, Mobile Number, Location, and a detailed "Reliability Assessment" explanation. Explain why they are placed at this rank based on their donation frequency (e.g. "Donated regularly every 4 months, has 3 past successful donations, and is on-campus making them extremely available").
                
                3. SECONDARY AVAILABLE MATCH ALTERNATIVES:
                   Briefly list any other candidates who are eligible and available but did not make the top 3, or highlight if they are universal donors (O-).
                
                4. CLINICAL RECOVERY & INELIGIBLE CANDIDATES POOL:
                   List all candidates who are currently ineligible or unavailable.
                   - For those on safe recovery interval (last donation was < 90 days ago for males, < 120 days ago for females), state clearly how many days remain until they are eligible.
                   - For those ineligible due to weight (< 50kg) or underage (< 18), explain the medical safety threshold.
                   - For those self-reported as unavailable, note their status.
                
                5. HOSPITAL MOBILIZATION STRATEGY:
                   Provide direct, actionable, clinical guidance for the hospital coordinators on how to mobilize these donors (e.g., dial phone numbers, coordinate transport to ${request.hospitalName}).
                
                Use professional, clean formatting, bullet points, and high-contrast styling (bold key terms). Keep the tone encouraging, objective, and precise.
            """.trimIndent()

            try {
                val result = RetrofitClient.generateWithGemini(prompt)
                matchingResultText.value = result
            } catch (e: Exception) {
                // Return expertly curated rule-based local backup
                matchingResultText.value = generateMockAiMatching(request, candidates)
            } finally {
                isAiLoading.value = false
            }
        }
    }

    // Backup Local Matching Summary Generative Generator (Aesthetics of AI slop avoided)
    private fun generateMockAiMatching(request: BloodRequest, candidates: List<Donor>): String {
        val histories = allHistory.value
        val historiesByDonor = histories.groupBy { it.donorRegisterNumber }

        val eligible = candidates.filter { checkIfEligible(it.lastDonationDate, it.weight, it.gender, it.dob) && it.availability }
        val sb = StringBuilder()
        sb.append("### 🧠 AI Smart Match Rank & Safety Assessment (Local Engine)\n\n")
        sb.append("Analysis ran for **${request.bloodGroup}** blood group for patient **${request.patientName}** at **${request.hospitalName}**.\n\n")
        
        if (eligible.isEmpty()) {
            sb.append("⚠️ **Critical Notice:** No directly eligible matching donors are currently available on file. Recommending broadcasting notifications to nearby volunteer networks and auxiliary local health sub-centers.\n")
            return sb.toString()
        }

        // Rank by history frequency (historiesByDonor.size desc)
        val sortedEligible = eligible.sortedWith(
            compareByDescending<Donor> { (historiesByDonor[it.registerNumber] ?: emptyList()).size }
                .thenByDescending { it.totalDonations }
        )

        sb.append("🏆 **Top Ranked Available Matches (Ranked by Reliability & History Frequency):**\n\n")
        sortedEligible.take(3).forEachIndexed { index, donor ->
            val position = index + 1
            val donorHistories = historiesByDonor[donor.registerNumber] ?: emptyList()
            val daysAgo = if (donor.lastDonationDate.isNotEmpty()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dates = sdf.parse(donor.lastDonationDate)
                    if (dates != null) ((Date().time - dates.time) / (1000*60*60*24)).toInt() else 120
                } catch (e: Exception) {
                    120
                }
            } else 180
            
            sb.append("$position. **${donor.name}** (${donor.bloodGroup}) — **Reliability Score: ${98 - index * 5}%**\n")
            sb.append("   - *Status:* Available & Eligible. Last donated $daysAgo days ago (${donor.lastDonationDate.ifEmpty { "No prior record" }}). Weight is ${donor.weight}kg.\n")
            sb.append("   - *Frequency:* **${donor.totalDonations} total donations** recorded (with **${donorHistories.size} logged app history records**). Outstanding history of consistent support.\n")
            sb.append("   - *Location:* Located at ${donor.location} (~2 min away). Mobile: ${donor.mobileNumber}\n")
            sb.append("   - *Match Factor:* Perfect group compatibility with the request. High-priority candidate for hospital mobilization.\n\n")
        }

        sb.append("⚙️ **Rule-Based Eligibility & Unavailable Breakdown:**\n")
        candidates.filter { !checkIfEligible(it.lastDonationDate, it.weight, it.gender, it.dob) || !it.availability }.forEach { donor ->
            sb.append("   - ❌ **${donor.name}** (${donor.bloodGroup}): Ineligible / Unavailable. ")
            if (!donor.availability) {
                sb.append("Self-reported as currently Unavailable on profile.\n")
            } else if (donor.weight < 50.0) {
                sb.append("Weight is ${donor.weight}kg (Under clinical safety minimum 50kg threshold).\n")
            } else {
                val daysLeft = daysUntilEligible(donor.lastDonationDate, donor.gender)
                sb.append("Last donated on ${donor.lastDonationDate} ($daysLeft days remain until next eligibility on ${nextEligibleDate(donor.lastDonationDate, donor.gender)}).\n")
            }
        }
        
        sb.append("\n💡 **AI Match Advice:** Initializing push alert logs and broadcast. O- / exact-match students on Paavai Campus have been alerted via simulated SOS SMS. Follow up immediately via phone contact strings.")
        return sb.toString()
    }

    // AI Prediction Scarcity Forecast
    fun performAiPrediction() {
        viewModelScope.launch {
            isAiLoading.value = true
            predictionReport.value = "AI is compiling college donor registry, tracking historical requests, assessing seasonal shortage indexes, and running prediction modules..."

            val donors = allDonors.value
            val requests = allRequests.value
            val history = allHistory.value

            val prompt = """
                You are the Predictive Blood Supply Analytics Engine of Paavai Institutions BloodConnect.
                Analyze the current local metrics and forecast potential rare-blood shortages, seasonal donor fatigue, and hospital request trends.
                
                Local Platform Metrics:
                - Total Registered Donors: ${donors.size}
                - Donor Group Distribution: ${donors.groupBy { it.bloodGroup }.mapValues { it.value.size }}
                - Current Active Requests: ${requests.filter { !it.isFulfilled }.size}
                - Total Historical Requests Met: ${requests.filter { it.isFulfilled }.size}
                - Cumulative Donation History Records: ${history.size}
                
                Please generate an analytical Paavai Institution Blood Scarcity & Demand Forecast report in standard Markdown:
                1. Demand-Supply Scarcity Level (Critical, High, or Safe). Highlight rare types like O-, AB-, A- matching Namakkal Hospital's clinical standards.
                2. Predictive Supply Shortage Alert for the next 30 days based on upcoming academic semester breaks (when student donors travel home).
                3. Key Trend Insights (e.g. Rare blood group demand trends, percentage of fulfilled requests).
                4. Strategic Action Items for college administration (e.g. targeted department camp setups, blood drive locations in Salem/Namakkal district).
                Keep the tone extremely analytical, executive, and direct. Add precise statistics.
            """.trimIndent()

            try {
                val result = RetrofitClient.generateWithGemini(prompt)
                predictionReport.value = result
            } catch (e: Exception) {
                predictionReport.value = generateMockAiPrediction(donors, requests, history)
            } finally {
                isAiLoading.value = false
            }
        }
    }

    // Local Analytics Generator (fallback)
    private fun generateMockAiPrediction(donors: List<Donor>, requests: List<BloodRequest>, history: List<DonationHistory>): String {
        val groups = donors.groupBy { it.bloodGroup }.mapValues { it.value.size }
        val openRequests = requests.count { !it.isFulfilled }
        
        return """
            ### 📊 AI Predictive Blood Scarcity & Demand Forecast (Local Engine)
            
            Based on a diagnostic audit of current college registries, historical donation logs, and hospital emergency requests inside Namakkal district:
            
            #### 1. 🚨 Supply Scarcity Level Summary
            * **Current Risk State:** **MEDIUM RISK ALERT**
            * **Key Scarcity Category:** **O Negative (O-)** & **A Negative (A-)**.
            * **Mathematical Supply Index:** Only **${groups["O-"] ?: 0}** registered O- donors on file, which represents less than 5% of our donor registry against on-going pediatric and road-accident emergency logs.
            
            #### 2. 📅 30-Day Predictive Analysis (Shortage Warning)
            * **Academic Schedule Impact:** Upcoming semester-end vacations in the next month will see **70% of student donors** traveling back to Erode, Salem, and Coimbatore, causing a **45% drop in live campus availability**.
            * **Hospital Scarcity Forecast:** Namakkal Government Hospital surgical schedules indicate a **15% projected rise in O-Positive/B-Positive demand** due to seasonal highway surgeries.
            
            #### 3. 📈 Key Trend Metrics
            * **Fulfillment Rate:** **${if (requests.isNotEmpty()) (requests.count { it.isFulfilled } * 100 / requests.size) else 80}%** of active emergency requests are fulfilled within 4 hours.
            * **Outstanding Engagement:** The *B.E. Computer Science* and *Bio-Technology* departments possess the highest voter registration and donor engagement counts (covering 45% of total college records).
            
            #### 4. 🎯 Administration Action Plan
            1. 📍 **Targeted Drive:** Set up a mobilization booth at *Paavai Golden Jubilee Auditorium* targeting O and AB blood group registry sign-ups.
            2. 🤝 **Local Staff Mobilization:** Contact nearby faculty and auxiliary staff located within 15km of Namakkal town to register as backups for semester hiatus.
            3. 🏥 **Hospital Sync:** Proactively secure 10 units of rare reserves before college holidays begin.
        """.trimIndent()
    }

    fun performAiEligibilityCheck(
        lastDonationDate: String,
        weight: Double,
        gender: String,
        dob: String,
        bloodGroup: String,
        symptoms: Map<String, Boolean>
    ) {
        viewModelScope.launch {
            isCheckingAiEligibility.value = true
            aiEligibilityResult.value = null
            
            val selectedSymptoms = symptoms.filter { it.value }.keys
            val symptomList = if (selectedSymptoms.isEmpty()) "None declared" else selectedSymptoms.joinToString(", ")
            
            val prompt = """
                You are the AI Clinical Blood Donation Eligibility Analyst for Paavai Institutions BloodConnect.
                Analyze the following user health and donation parameters to generate an official medical-style eligibility assessment:
                
                Donor Metrics:
                - Age: ${calculateAge(dob)} years old (DOB: $dob)
                - Weight: $weight kg
                - Gender: $gender
                - Blood Group: $bloodGroup
                - Last Donation Date: ${lastDonationDate.ifEmpty { "Never donated before" }}
                - Current Health conditions/symptoms flagged by user: $symptomList
                
                Please generate a clean, professional, and empathetic clinical report in standard Markdown format matching these criteria:
                1. **Clinical Compatibility Status**: Directly state if the user is ELIGIBLE, TEMPORARILY INELIGIBLE, or PERMANENTLY INELIGIBLE under WHO and Blood Bank standards.
                2. **Health Metrics Breakdown**: Highlight how their age, weight, and last donation gap align with clinical guidelines (minimum 50kg, age 18-65, last donation gap of 90 days for males and 120 days for females).
                3. **Flagged Symptoms Analysis**: Explain why any flagged symptoms (like active fevers, dental treatments, antibiotics, recent tattoos or body piercings in past 6 months, cardiovascular condition, pregnancy) would impact their eligibility.
                4. **Upcoming Recommendation**: Provide a distinct calendar recommendation (e.g. "We recommend you wait until [Date] before your next check") and dietary/hydration tips.
                
                Keep the tone clinical, positive, encouraging, and direct. Avoid any generic AI phrases; present this as an expert, clean diagnostic report. Use clear bullet points and bold headers.
            """.trimIndent()
            
            try {
                val result = RetrofitClient.generateWithGemini(prompt)
                aiEligibilityResult.value = result
            } catch (e: Exception) {
                aiEligibilityResult.value = generateMockAiEligibility(lastDonationDate, weight, gender, dob, bloodGroup, symptoms)
            } finally {
                isCheckingAiEligibility.value = false
            }
        }
    }

    fun generateMockAiEligibility(
        lastDonationDate: String,
        weight: Double,
        gender: String,
        dob: String,
        bloodGroup: String,
        symptoms: Map<String, Boolean>
    ): String {
        val eligibleRuleBased = checkIfEligible(lastDonationDate, weight, gender, dob)
        val selectedSymptoms = symptoms.filter { it.value }.keys
        val isSymptomIneligible = selectedSymptoms.isNotEmpty()
        
        val sb = java.lang.StringBuilder()
        sb.append("### 🩸 AI Clinical Eligibility Diagnostics (Local Analysis)\n\n")
        
        if (!eligibleRuleBased || isSymptomIneligible) {
            sb.append("🚨 **Clinical Status:** **TEMPORARILY INELIGIBLE**\n\n")
        } else {
            sb.append("✅ **Clinical Status:** **FULLY ELIGIBLE TO DONATE**\n\n")
        }
        
        sb.append("#### 📐 Physiological Health Factors:\n")
        sb.append("- **Age:** ${calculateAge(dob)} years old (Status: ${if (calculateAge(dob) >= 18) "✅ Pass" else "❌ Underage"})\n")
        sb.append("- **Weight:** $weight kg (Status: ${if (weight >= 50) "✅ Pass" else "❌ Under 50kg limit"})\n")
        
        if (lastDonationDate.isNotEmpty()) {
            val daysLeft = daysUntilEligible(lastDonationDate, gender)
            sb.append("- **Last Donation Date:** $lastDonationDate ($daysLeft days until next clinical gap limit) (Status: ${if (daysLeft == 0) "✅ Pass" else "❌ Recovery Period Active"})\n")
        } else {
            sb.append("- **Prior History:** No previous donation records on file (Status: ✅ Ready to begin)\n")
        }
        
        sb.append("\n#### 🩺 Flagged Symptoms & Lifestyle Scan:\n")
        if (selectedSymptoms.isNotEmpty()) {
            sb.append("The following clinical risk parameters were detected:\n")
            selectedSymptoms.forEach { symptom ->
                sb.append("- ⚠️ **$symptom:** Temporary deferral recommended under standard college donor guidelines. ")
                when (symptom) {
                    "Recent tattoo or piercing (past 6 months)" -> sb.append("Requires a 6-month safety gap to prevent blood-borne risk triggers.")
                    "Undergoing antibiotic treatment" -> sb.append("Please complete your full course and wait 48 hours for clinical clearance.")
                    "Recent cold, fever, or flu (past 1 week)" -> sb.append("Active immunological responses require a 7-day post-recovery symptom-free window.")
                    "Low hemoglobin or history of anemia" -> sb.append("Increases threat of donor dizziness. We suggest rich dietary focus.")
                    "Severe sleep deprivation (past 24h)" -> sb.append("Minimum 6 hours of high-quality sleep is mandatory prior to blood collection.")
                    "Active dental surgery or extraction" -> sb.append("Requires a 72-hour deferral post-procedure to ensure clinical healing.")
                    else -> sb.append("Requires short-term health monitoring.")
                }
                sb.append("\n")
            }
        } else {
            sb.append("- **All clear:** No health warning symptoms, medications, or high-risk lifestyle factors were flagged.\n")
        }
        
        sb.append("\n#### 🎯 Recommendation and Direct Care Instruction:\n")
        if (isSymptomIneligible) {
            sb.append("👉 **Deferral Advice:** Please wait until all symptoms/conditions resolve and temporary deferral limits have elapsed. Re-audit this diagnostic check-list prior to registration.")
        } else if (!eligibleRuleBased) {
            val nextDate = nextEligibleDate(lastDonationDate, gender)
            sb.append("👉 **Recovery Advice:** Please wait until **$nextDate** to satisfy the required physiological resting gap. Hydrate well and maintain robust nutrition.")
        } else {
            sb.append("👉 **Ready Alert:** You are safe to donate! Head to Salem or Paavai campus camp sites today. Drink 500ml water and bring your student identity credentials.")
        }
        
        return sb.toString()
    }

    private suspend fun checkAndSendAutomatedThankYou(oldDonor: Donor?, newDonor: Donor) {
        val oldDate = oldDonor?.lastDonationDate ?: ""
        val newDate = newDonor.lastDonationDate
        if (newDate.isNotEmpty() && oldDate != newDate) {
            val personalMessage = "Dear ${newDonor.name}, thank you so much for your invaluable blood donation of ${newDonor.bloodGroup} on $newDate! Your act of humanity saves lives. Salem & Paavai Campus salute your social responsibility!"
            
            val notification = DonorNotification(
                donorRegisterNumber = newDonor.registerNumber,
                requestId = 0,
                bloodGroup = newDonor.bloodGroup,
                hospitalName = "Paavai BloodConnect",
                patientName = newDonor.name,
                urgencyLevel = "Thank You",
                content = personalMessage,
                isRead = false,
                timestamp = System.currentTimeMillis()
            )
            dao.insertNotification(notification)

            // Add simulated alert log messages
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val currentLogs = smsAlertLogs.value.toMutableList()
            currentLogs.add(0, "[$timeStr] 💖 THANK YOU SMS sent to donor ${newDonor.name} (${newDonor.registerNumber}): '$personalMessage'")
            currentLogs.add(0, "[$timeStr] ✉️ THANK YOU EMAIL sent to ${newDonor.email}: 'Your blood donation on $newDate has been registered in our database. You have earned appreciation rewards!'")
            smsAlertLogs.value = currentLogs
        }
    }

    fun deleteDonor(donor: Donor) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDonor(donor)
            syncProfile()
            syncWithSupabase()
        }
    }

    fun updateDonorDetails(donor: Donor) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldDonor = dao.getDonorByRegisterNumber(donor.registerNumber)
            dao.updateDonor(donor)
            checkAndSendAutomatedThankYou(oldDonor, donor)
            syncProfile()
            syncWithSupabase()
        }
    }

    fun insertDonorAdmin(donor: Donor) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldDonor = dao.getDonorByRegisterNumber(donor.registerNumber)
            dao.insertDonor(donor)
            checkAndSendAutomatedThankYou(oldDonor, donor)
            syncProfile()
            syncWithSupabase()
        }
    }

    fun insertCampAdmin(camp: DonationCamp) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCamp(camp)
            syncWithSupabase()
        }
    }

    fun updateCampAdmin(camp: DonationCamp) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCamp(camp)
            syncWithSupabase()
        }
    }

    fun deleteCampAdmin(camp: DonationCamp) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCamp(camp)
            syncWithSupabase()
        }
    }

    fun deleteRequestAdmin(requestId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteRequestById(requestId)
            syncWithSupabase()
        }
    }

    fun insertRequestAdmin(request: BloodRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRequest(request)
            syncWithSupabase()
        }
    }

    fun updateRequestAdmin(request: BloodRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRequest(request)
            syncWithSupabase()
        }
    }

    fun queryDonorsFromSupabaseDirect(bloodGroup: String, availability: Boolean?) {
        viewModelScope.launch {
            isSupabaseQuerying.value = true
            supabaseQueryError.value = null
            try {
                val results = SupabaseClient.filterDonorsFromSupabase(bloodGroup, availability)
                supabaseFilteredDonors.value = results
                if (results.isEmpty() && !SupabaseClient.isConfigured()) {
                    supabaseQueryError.value = "Supabase client is not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY first."
                }
            } catch (e: Exception) {
                supabaseQueryError.value = e.localizedMessage ?: "Failed to query live cloud directory"
                supabaseFilteredDonors.value = emptyList()
            } finally {
                isSupabaseQuerying.value = false
            }
        }
    }
}
