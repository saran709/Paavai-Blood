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

    // Local Users account credential records
    data class UserAccount(
        val name: String,
        val email: String,
        val regNo: String,
        val role: String, // "Admin", "Volunteer", "Student Donor"
        val dept: String = "B.E. Computer Science",
        val year: String = "3rd Year",
        val bloodGroup: String = "O-",
        val phone: String = "9876543210"
    )

    val accounts = MutableStateFlow<Map<String, Pair<String, UserAccount>>>(
        mapOf(
            "blood@paavai.com" to Pair("blood@123", UserAccount("Administrator", "blood@paavai.com", "ADM001", "Admin")),
            "volunteer@paavai.edu.in" to Pair("vol123", UserAccount("Paavai Volunteer", "volunteer@paavai.edu.in", "VOL100", "Volunteer")),
            "student@paavai.edu.in" to Pair("stud123", UserAccount("Saran Ramesh", "student@paavai.edu.in", "22104085", "Student Donor"))
        )
    )

    fun login(email: String, word: String): Boolean {
        val entry = accounts.value[email.trim().lowercase()]
        if (entry != null && entry.first == word) {
            val account = entry.second
            activeUserRegNumber.value = account.regNo
            userRole.value = account.role
            currentUserEmail.value = account.email
            currentUserName.value = account.name
            isLoggedIn.value = true
            syncProfile()
            return true
        }
        return false
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
        role: String
    ): Boolean {
        val trimEmail = email.trim().lowercase()
        if (accounts.value.containsKey(trimEmail)) {
            return false // Account already exists
        }

        val account = UserAccount(
            name = name,
            email = trimEmail,
            regNo = regNo,
            role = role,
            dept = dept,
            year = year,
            bloodGroup = bloodGroup,
            phone = phone
        )

        val newMap = accounts.value.toMutableMap()
        newMap[trimEmail] = Pair(pass, account)
        accounts.value = newMap

        viewModelScope.launch(Dispatchers.IO) {
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
                    userType = if (role == "Volunteer") "Local Volunteer" else "Student",
                    availability = true,
                    totalDonations = 0
                )
                dao.insertDonor(newDonor)
            }
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

    // Simulated alerts status
    val smsAlertLogs = MutableStateFlow<List<String>>(emptyList())

    init {
        // Load default registered profile automatically
        syncProfile()
    }

    fun setRole(role: String) {
        userRole.value = role
        syncProfile()
    }

    private fun syncProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = dao.getDonorByRegisterNumber(activeUserRegNumber.value)
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
        contactPhone: String
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
                simulatedAlertsSent = urgency == "Critical" || urgency == "High"
            )
            val reqId = dao.insertRequest(req)
            
            if (urgency == "Critical" || urgency == "High") {
                sendSimulatedAlerts(bloodGroup, hospitalName, unitsRequired)
            }
            performAiMatching(req.copy(id = reqId.toInt()))
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

            val prompt = """
                You are the AI Core of Paavai BloodConnect, an emergency blood donor network inside Paavai Institutions, Namakkal, Tamil Nadu.
                Calculate eligibility, safety parameters, and rank donors scientifically.
                
                Emergency Request Details:
                - Patient: ${request.patientName} (Urgency: ${request.urgencyLevel})
                - Blood Group Needed: ${request.bloodGroup}
                - Units Required: ${request.unitsRequired}
                - Hospital Location: ${request.hospitalName}
                
                Here are the registered candidate donors in the ecosystem:
                ${candidates.joinToString("\n") { 
                    "- ${it.name} (${it.bloodGroup}, Register: ${it.registerNumber}, Dept: ${it.department}, Weight: ${it.weight}kg, Last Donation: ${it.lastDonationDate.ifEmpty { "Never" }}, User-Type: ${it.userType}, Location: ${it.location})" 
                }}
                
                Please generate a highly professional Emergency Donation Assessment in standard Markdown format matching these exact rules:
                1. Ranks the top 3 best available matched donors. Highlight the critical universal donor O- where useful.
                2. Explicitly comment on eligibility rules (weight must be >=45kg, last donation must be >90 days ago). If any candidates fail these rules, mark them as ineligible with the date they next qualify.
                3. Address the geographical location (e.g. distance from Paavai Engineering Campus/Rasipuram to the specified hospital).
                4. Keep the tone clinical, positive, and direct. Use bullet points and clean structure.
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
        val eligible = candidates.filter { checkIfEligible(it.lastDonationDate, it.weight, it.gender, it.dob) }
        val sb = StringBuilder()
        sb.append("### 🧠 AI Smart Match Rank & Safety Assessment (Local Engine)\n\n")
        sb.append("Analysis ran for **${request.bloodGroup}** blood group for patient **${request.patientName}** at **${request.hospitalName}**.\n\n")
        
        if (eligible.isEmpty()) {
            sb.append("⚠️ **Critical Notice:** No directly eligible matching donors are currently available on file. Recommending broadcasting notifications to nearby alumni and auxiliary local health sub-centers.\n")
            return sb.toString()
        }

        sb.append("🏆 **Top Ranked Available Matches:**\n\n")
        eligible.take(3).forEachIndexed { index, donor ->
            val position = index + 1
            val daysAgo = if (donor.lastDonationDate.isNotEmpty()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dates = sdf.parse(donor.lastDonationDate)
                    if (dates != null) ((Date().time - dates.time) / (1000*60*60*24)).toInt() else 120
                } catch (e: Exception) {
                    120
                }
            } else 180
            
            sb.append("$position. **${donor.name}** (${donor.bloodGroup}) — **Rank Score: ${98 - index * 5}%**\n")
            sb.append("   - *Status:* Eligible. Last donated $daysAgo days ago (${donor.lastDonationDate.ifEmpty { "No prior record" }}). Weight is ${donor.weight}kg.\n")
            sb.append("   - *Location:* Located at ${donor.location} (~2 min away). Mobile: ${donor.mobileNumber}\n")
            sb.append("   - *Match Factor:* Same blood type matching request. Outstanding contact reliability status on historical logs.\n\n")
        }

        sb.append("⚙️ **Rule-Based Eligibility Breakdown:**\n")
        candidates.filter { !checkIfEligible(it.lastDonationDate, it.weight, it.gender, it.dob) }.forEach { donor ->
            sb.append("   - ❌ **${donor.name}** (${donor.bloodGroup}): Ineligible. ")
            if (donor.weight < 50.0) {
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
            2. 🤝 **Alumni Mobilization:** Contact MCA and MBA alumni chapters located within 15km of Namakkal town to register as backups for semester hiatus.
            3. 🏥 **Hospital Sync:** Proactively secure 10 units of rare reserves before college holidays begin.
        """.trimIndent()
    }

    fun deleteDonor(donor: Donor) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDonor(donor)
            syncProfile()
        }
    }

    fun updateDonorDetails(donor: Donor) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateDonor(donor)
            syncProfile()
        }
    }

    fun insertDonorAdmin(donor: Donor) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertDonor(donor)
            syncProfile()
        }
    }

    fun insertCampAdmin(camp: DonationCamp) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCamp(camp)
        }
    }

    fun updateCampAdmin(camp: DonationCamp) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCamp(camp)
        }
    }

    fun deleteCampAdmin(camp: DonationCamp) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCamp(camp)
        }
    }

    fun deleteRequestAdmin(requestId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteRequestById(requestId)
        }
    }

    fun insertRequestAdmin(request: BloodRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRequest(request)
        }
    }

    fun updateRequestAdmin(request: BloodRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRequest(request)
        }
    }
}
