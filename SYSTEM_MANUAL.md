# PAAVAI BLOODCONNECT – SYSTEM ARCHITECTURE & ENTERPRISE MANUAL
## Campus Blood Emergency Management System (Paavai Institutions)

---

## 1. System Architecture

The enterprise architecture of **Paavai BloodConnect** is designed to scale for over 20,000 active users, encompassing students, faculty, club coordinators, NSS/NCC/YRC/Scouts volunteers, and super administrators. It employs a modern **REST API-Driven decoupled client-server architecture**.

### 1.1 Architectural Blueprint (High-Level)

```
+-----------------------------------------------------------------------------------------+
|                                CLIENTS / FRONTEND SERVICES                              |
|                                                                                         |
|   +---------------------------------------+     +-----------------------------------+   |
|   |   Android Mobile App (Kotlin/Compose) |     |  Responsive Admin Dashboard       |   |
|   |   & Cross-Platform Client (Flutter)   |     |  (Vue/React or Laravel Blade)     |   |
|   +-------------------+-------------------+     +-----------------+-----------------+   |
+-----------------------|-------------------------------------------|---------------------+
                        |                                           |
                        +---------------------+---------------------+
                                              | HTTPS Requests (JWT Bearer Token)
                                              v
+-----------------------------------------------------------------------------------------+
|                                    GATEWAY & SECURITY                                   |
|                                                                                         |
|        +------------------------------------------------------------------------+       |
|        | Nginx Reverse Proxy / Load Balancer (SSL Termination, Rate Limiting)   |       |
|        +-----------------------------------+------------------------------------+       |
+--------------------------------------------|--------------------------------------------+
                                             v
+-----------------------------------------------------------------------------------------+
|                                  APPLICATION SVR (LARAVEL)                              |
|                                                                                         |
|   +---------------------------------------------------------------------------------+   |
|   |  REST API Engines (Routing, Middleware, Controllers, Policies)                  |   |
|   |                                                                                 |   |
|   |  * Auth (JWT, OAuth)        * Matching Engine (Vico Recommendation)             |   |
|   |  * Request Processor        * Prediction Module (Priority Engine)               |   |
|   |  * Camp & Registry Manager  * Notification Broadcaster (SMS/Email/Push)       |   |
|   +-------------------+------------------------------------+------------------------+   |
+-----------------------|------------------------------------|----------------------------+
                        | Queries / ORM (Eloquent)           | Transactions / Sync
                        v                                    v
+--------------------------------------------------+ +------------------------------------+
|                DATABASE LAYER (MYSQL)            | |       CLOUD REPLICA / HOOKS        |
|                                                  | |                                    |
|   +------------------------------------------+   | |   +----------------------------+   |
|   | Master Database Schema                 |   | |   | Supabase Sync Engine       |   |
|   | - 14 fully normalized relations          |   | |   | (Real-time Mirroring)      |   |
|   | - Custom indexing on keys                |   | |   +----------------------------+   |
|   | - Stored procedures & Audit triggers     |   | |                                    |
|   +------------------------------------------+   | |                                    |
+--------------------------------------------------+ +------------------------------------+
```

### 1.2 Data Synchronization & Offline-First Strategy
To support connectivity drops inside campus basements or remote rural camps, the mobile application leverages an **Offline-First Room DB synchronization pipeline** paired with the **Supabase Real-time Cloud API**.
- **Local Cache:** All active blood requests, registered donor directories, and personal records are written to a localized SQLite/Room structure.
- **Supabase Cloud Sync:** The client synchronizes record states using RESTful transactions. If the cloud database is reachable, mutations sync immediately; otherwise, mutations queue locally and replay upon recovery.

---

## 2. Use Case Diagram

The system coordinates privileges across 5 distinct logical actors: **Donor**, **Requester**, **Volunteer**, **Coordinator**, and **Super Admin**.

```
                           +---------------------------+
                           |  Paavai BloodConnect System|
                           |                           |
       (Donor) ------------+----> [Register & Login]   |
                           |----> [Update Availability]|
                           |----> [View Blood Requests]|
                           |----> [Accept Request]     |
                           |----> [Download Cert]      |
                           |                           |
     (Requester) ----------+----> [Raise Blood Request]|
                           |----> [Track Responses]    |
                           |----> [Close Request]      |
                           |                           |
     (Volunteer) ----------+----> [Coordinate Donor]   |
 (NSS/NCC/YRC/Scouts)      |----> [Progress Update]    |
                           |----> [Mark Completed]     |
                           |                           |
    (Coordinator) ---------+----> [Manage Volunteers]  |
                           |----> [Assign Emergencies] |
                           |----> [Create Camps/Events]|
                           |                           |
    (Super Admin) ---------+----> [Approve Accounts]   |
                           |----> [System Logs / Audit]|
                           |----> [Broadcast Alerts]   |
                           +---------------------------+
```

---

## 3. Entity-Relationship (ER) Diagram

```
 +-----------------+          +-----------------+          +-------------------+
 |     roles       | 1      * |     users       | *      1 |    departments    |
 |-----------------|----------|-----------------|----------|-------------------|
 | id (PK)         |          | id (PK)         |          | id (PK)           |
 | name            |          | email           |          | name              |
 +-----------------+          | password        |          +-------------------+
                              | role_id (FK)    |
                              | department_id   | 1
                              +--------+--------+
                                       |
                                       | 1     1
                                       +-------------------------+
                                       |                         |
                                       v *                       v *
                             +-------------------+     +------------------+
                             |    volunteers     |     |      donors      |
                             |-------------------|     |------------------|
                             | id (PK)           |     | id (PK)          |
                             | user_id (FK)      |     | user_id (FK)     |
                             | club_id (FK)      |     | blood_group      |
                             +-------------------+     | availability     |
                                                       | last_donation    |
                                                       +--------+---------+
                                                                | 1
                                                                |
                                                                | *
                                                       +--------v---------+
                                                       | donation_history |
                                                       |------------------|
                                                       | id (PK)          |
                                                       | donor_id (FK)    |
                                                       | camp_id (FK_null)|
                                                       | donation_date    |
                                                       +------------------+
```

---

## 4. Normalized Database Schema (MySQL DDL)

Here is the fully normalized structural DDL script establishing clean foreign keys, integrity constraints, indexes, and stored procedures.

```sql
-- Create Database
CREATE DATABASE IF NOT EXISTS paavai_bloodconnect;
USE paavai_bloodconnect;

-- 1. Roles Table
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Departments Table
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Clubs Table (Voluntary Organizations)
CREATE TABLE clubs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. Users Table (Master Accounts Federated with JWT)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    department_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

-- 5. Donors Table (Sourced from Users)
CREATE TABLE donors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    register_number VARCHAR(20) NOT NULL UNIQUE,
    blood_group VARCHAR(5) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    dob DATE NOT NULL,
    last_donation_date DATE NULL,
    availability_status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_donor_bg_avail (blood_group, availability_status)
) ENGINE=InnoDB;

-- 6. Volunteers Table
CREATE TABLE volunteers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    club_id INT NOT NULL,
    volunteer_uid VARCHAR(30) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Blood Requests Table (Crisis Tickets)
CREATE TABLE blood_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_user_id BIGINT NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    hospital_name VARCHAR(255) NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    units_required INT NOT NULL DEFAULT 1,
    emergency_level VARCHAR(20) NOT NULL DEFAULT 'Normal', -- 'Critical', 'High', 'Medium', 'Normal'
    contact_number VARCHAR(15) NOT NULL,
    location VARCHAR(255) NOT NULL,
    required_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Open', -- 'Open', 'In-Transit', 'Completed', 'Closed'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_req_bg_status (blood_group, status)
) ENGINE=InnoDB;

-- 8. Request Responses Table
CREATE TABLE request_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    donor_user_id BIGINT NOT NULL,
    volunteer_user_id BIGINT,
    volunteer_notes TEXT,
    response_status VARCHAR(25) NOT NULL DEFAULT 'Accepted', -- 'Accepted', 'Donated', 'No-Show', 'Cancelled'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (donor_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (volunteer_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 9. Notifications Table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_unread (recipient_user_id, is_read)
) ENGINE=InnoDB;

-- 10. Blood Camps Table
CREATE TABLE blood_camps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255) NOT NULL,
    scheduled_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    coordinator_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coordinator_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 11. Camp Registrations Table
CREATE TABLE camp_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camp_id BIGINT NOT NULL,
    donor_user_id BIGINT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (camp_id) REFERENCES blood_camps(id) ON DELETE CASCADE,
    FOREIGN KEY (donor_user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_camp_donor (camp_id, donor_user_id)
) ENGINE=InnoDB;

-- 12. Donation History Table
CREATE TABLE donation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    camp_id BIGINT NULL,
    donation_date DATE NOT NULL,
    units_donated INT DEFAULT 1,
    location_or_hospital VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    FOREIGN KEY (camp_id) REFERENCES blood_camps(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 13. Certificates Table
CREATE TABLE certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_id BIGINT UNIQUE NOT NULL,
    certificate_url VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donation_id) REFERENCES donation_history(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 14. Audit Logs Table
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(255) NOT NULL,
    table_affected VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Stored Procedure: Auto-update Donor Availability status based on Last Donation Date (3 months cooldown rule)
DELIMITER //
CREATE PROCEDURE UpdateDonorCooldowns()
BEGIN
    UPDATE donors 
    SET availability_status = FALSE 
    WHERE last_donation_date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY);
    
    UPDATE donors 
    SET availability_status = TRUE 
    WHERE last_donation_date IS NULL OR last_donation_date < DATE_SUB(CURDATE(), INTERVAL 90 DAY);
END //
DELIMITER ;
```

---

## 5. API Documentation (REST JSON Contracts)

### 5.1 Route Blueprint: `/api/v1/auth/login` [POST]
* **Request Block:**
```json
{
  "email": "student@paavai.edu.in",
  "password": "securepassword123"
}
```
* **Response Block (Success 200):**
```json
{
  "status": "success",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InN0dWRlbnRAcGFhdmFpLmVkdS5pbiIsInJvbGUiOiJTdHVkZW50In0...",
  "user": {
    "email": "student@paavai.edu.in",
    "role": "Donor",
    "register_number": "22104085"
  }
}
```

### 5.2 Route Blueprint: `/api/v1/requests` [GET] (Blood Requests Directory)
* **Headers:** `Authorization: Bearer eyJhbGci...`
* **Response Block (Success 200):**
```json
[
  {
    "id": 104,
    "patient_name": "Selvamani R",
    "hospital_name": "Namakkal Government Hospital",
    "blood_group": "O-",
    "units_required": 3,
    "emergency_level": "Critical",
    "contact_number": "+919442001552",
    "location": "A-Block General ICU",
    "status": "Open"
  }
]
```

### 5.3 Route Blueprint: `/api/v1/requests/create` [POST]
* **Request Block:**
```json
{
  "patient_name": "Arun Kumar",
  "hospital_name": "Paavai Medical Center",
  "blood_group": "A-",
  "units_required": 2,
  "emergency_level": "High",
  "contact_number": "+919876543210",
  "location": "Ward 4B, Namakkal",
  "required_date": "2026-06-15"
}
```

---

## 6. Directory / Folder Layouts

### 6.1 Kotlin / Android Jetpack Compose Architecture
```
/app/src/main/java/com/example/
│
├── data/
│   ├── AppDatabase.kt         (Local encrypted SQLite Room wrapper)
│   ├── Dao.kt                 (Transaction Queries & SQL Statement declarations)
│   ├── Models.kt              (Entities: Donor, UserAccountEntity, Request, etc.)
│   └── SupabaseClient.kt      (Network Engine & Cloud DB Syncer)
│
├── viewmodel/
│   └── BloodConnectViewModel.kt (Unified state tracker, authentication & logic)
│
└── ui/
    ├── screens/
    │   ├── AdminScreen.kt     (Management, users list & status controls)
    │   ├── DashboardScreen.kt (Personal analytics, Live feeds & SOS broadcasts)
    │   ├── DonorFinderScreen.kt (Dynamic searches & department filtering)
    │   ├── LoginScreen.kt     (Secure JWT authentication gate)
    │   ├── RegistrationScreen.kt (Forms for registering new donors and profiles)
    │   ├── RequestsScreen.kt  (Raise, view, track emergency tickets)
    │   └── RewardsScreen.kt   (Accrued campaign levels and badge rewards)
    │
    └── theme/
        ├── Color.kt           (DeepMaroon, BloodCrimson, WarmSlate & PaavaiGold tokens)
        └── Theme.kt           (Material 3 system integrations)
```

### 6.2 Full-Stack Flutter Client Architecture
```
/lib/
│
├── main.dart                  (Main entry point & dynamic system router)
├── core/
│   ├── constants/             (Network endpoints, styling tokens)
│   ├── theme/                 (Material 3 clinical system templates)
│   └── utils/                 (Encrypted Secure Storage)
│
├── data/
│   ├── models/                (Type declaration JSON parsers)
│   ├── providers/             (State management & JWT handlers)
│   └── services/              (REST API client / Axios equivalent)
│
└── presentation/
    ├── screens/               (Client views: Dash, Auth, Finder, Request Admin)
    └── widgets/               (Asymmetric charts & beautiful progress tracking bars)
```

### 6.3 Backend Laravel Architecture
```
/app/
│
├── Http/
│   ├── Controllers/
│   │   ├── AuthController.php       (JWT Issuance, password resets)
│   │   ├── BloodRequestController.php (Raises & broadcasts SOS)
│   │   ├── CampController.php       (Manages campus campaigns)
│   │   └── DonorController.php      (Filters active, verified members)
│   │
│   └── Middleware/
│       ├── JWTAuthenticate.php               (Decodes header token validation)
│       ├── RoleAuthorize.php                 (Enforces Admin, Club, and User structures)
│       └── RestrictPaavaiDomainMiddleware.php (Strict domain validation for login & registrations)
│
└── Models/
    ├── User.php
    ├── Donor.php
    ├── Volunteer.php
    ├── BloodRequest.php
    └── Camp.php
```

#### Enforcing Campus Domain Restrictions via Middleware
To protect resources and enforce high-scale performance, register the `RestrictPaavaiDomainMiddleware` globally or target specific authentication request groups in `app/Http/Kernel.php`:

```php
protected $middlewareGroups = [
    'api' => [
        \App\Http\Middleware\RestrictPaavaiDomainMiddleware::class,
        \Illuminate\Routing\Middleware\ThrottleRequests::class.':api',
        \Illuminate\Routing\Middleware\SubstituteBindings::class,
    ],
];
```

---

## 7. Wireframe System Schematics

### 7.1 Mobile View Wireframe: Active Dashboard Screen
```
+--------------------------------------------------------+
| [=] PAAVAI BLOODCONNECT                     (🔔) (🧑‍💼) |
+--------------------------------------------------------+
|  Welcome back, Ramesh Kumar!                           |
|  Category: Faculty • O+                                 |
+--------------------------------------------------------+
|                                                        |
|   +------------------------------------------------+   |
|   |   ❤️ ACCRUED DONATION RATING: LEVEL Gold        |   |
|   |   Rank 12 on campus • 6 lives saved             |   |
|   +------------------------------------------------+   |
|                                                        |
|   +------------------------------------------------+   |
|   |  LIVE EMERGENCY CRISIS STREAM  - 1 Active Ticket|   |
|   |  - O- Needed - Gov General Hospital (Critical)  |   |
|   |  [ ACCEPT DONATION RUN ]    [ GET DIRECTIONS ] |   |
|   +------------------------------------------------+   |
|                                                        |
|   [🎨] NEW REQUEST    [🔍] FIND DONORS    [🎁] REWARDS   |
+--------------------------------------------------------+
```

### 7.2 Web Administration Portal: Desktop Analysis View
```
+========================================================================================+
|  PAAVAI ADMIN PORTAL     [Dashboard] [Donors] [Requests] [Volunteers] [Logs] [Settings] |
+========================================================================================+
|                                                                                        |
|  CAMPUS LIFESAVING SCORES: Analytics Feed                                              |
|                                                                                        |
|   +-------------------------+  +-------------------------+  +-----------------------+  |
|   |     TOTAL REGISTERED    |  |     ACTIVE SOS RUNS     |  |   COMPLETED DONATIONS_ |  |
|   |       1,842 Members     |  |       4 Crisis Tickets  |  |       124 Lifesaved   |  |
|   +-------------------------+  +-------------------------+  +-----------------------+  |
|                                                                                        |
|  RECENT EMERGENCY CRISIS REGISTRY                                                      |
|   ID   Patient Name   Group   Hospital             Priority   Assigned Club   Status   |
|   #42  Arun Kumar     A-      Paavai Med Center    High       NSS Volunteers  In-Transit|
|   #43  Meera Roy      O+      Namakkal Gov Hosp    Critical   YRC Officers    Open     |
|                                                                                        |
+========================================================================================+
```

---

## 8. Integrated Smart Logic (Algorithms)

### 8.1 Emergency Priority Prediction Algorithm
```kotlin
enum class PriorityLevel { CRITICAL, HIGH, MEDIUM, NORMAL }

fun predictPriority(units: Int, bg: String, hoursRemaining: Int): PriorityLevel {
    val rareGroups = listOf("O-", "AB-", "A-", "B-")
    val score = (units * 2.5) + (if (rareGroups.contains(bg)) 4.0 else 1.0) + (if (hoursRemaining < 3) 5.5 else 1.0)
    
    return when {
         score >= 10.0 -> PriorityLevel.CRITICAL
         score >= 7.5  -> PriorityLevel.HIGH
         score >= 4.5  -> PriorityLevel.MEDIUM
         else          -> PriorityLevel.NORMAL
    }
}
```

### 8.2 Smart Donor Matching System
```kotlin
fun findMatchingDonors(allDonors: List<Donor>, targetBg: String): List<Donor> {
    return allDonors.filter { donor ->
        donor.availabilityStatus && 
        isCompatible(donor.bloodGroup, targetBg) &&
        hasPassedCooldown(donor.lastDonationDate)
    }.sortedWith(
        compareBy<Donor> { it.weight < 50.0 } // Discard if underweight
        .thenBy { it.lastDonationDate }      // Prioritize longest cooldowns
    )
}
```

---

## 9. Testing & Quality Assurance Plan

### 9.1 Robolectric Unit Unit-Testing Scope
The platform tests all critical user flows inside a local JVM database sandbox without needing physical emulators.
- **Login Verification Tests:** Checks local database cache validation and password encryption.
- **Coordinating SOS Actions:** Ensures correct status changes when volunteer or requester marks emergency closed.

### 9.2 Roborazzi Snapshot Verification
UI screens (like `DashboardScreen` and `RegistrationForm`) utilize screenshot testing to verify alignment and theme uniformity.

---

## 10. Deployment Strategy

### 10.1 Server Infrastructure Requirements
* **Operating System:** Ubuntu Server 22.04 LTS
* **Processor Architecture:** Multi-core virtual CPU (x86_64)
* **RAM / Disk Space:** 8 GB RAM / 80 GB SSD (For storing large multi-variable application logs)

### 10.2 Production Step-by-Step Installation
1. **Clone Source Directory & Setup Environment Variables:**
   Create and finalize the custom `.env` options on the host machine.
2. **Launch Application Gateway Stack:**
   Register SSL configurations on Nginx.
3. **Migrate Cloud Databases:**
   Perform migration commands on relational MySQL or Supabase schema tables.
4. **Compile Application APK:**
   Generate customized APKs specifying college server endpoints:
   ```bash
   gradle assembleDebug
   ```

---

## 11. Interactive User Manual

### 11.1 Logging into the Safe Campus Terminal
* Enter your approved Paavai institutional email (either `student@paavai.edu.in` or staff equivalent).
* Input your 6-character encrypted security password.

### 11.2 Tracking an Emergency Request
1. Transition to the **Requests Tab** using the bottom navigation menu.
2. View active queues tagged with visual emergency flags (e.g. Red for Critical, Yellow for High).
3. If acting as an administrator, tap the ticket to update coordinates, contact regional hospitals, or dispatch NSS volunteers for action.
