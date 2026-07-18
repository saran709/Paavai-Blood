import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'dart:math' as math;

/// ============================================================================
/// PAAVAI BLOODCONNECT - HIGH-SCALE STUDENT & FACULTY DONOR REGISTRY (FLUTTER)
/// Features:
///   1. Fully strongly-typed Donor Model supporting either Student or Faculty.
///   2. Location Tracking Telemetry (Coordinates, Campus Blocks, Live Distances).
///   3. Advanced Multi-Criteria Filtering (Blood Group matching, User Type, Query).
///   4. Real-time Campus Radar Location tracking system simulated via Canvas.
///   5. Premium Material 3 themed UI with crisp interactive gestures.
/// ============================================================================

// --- THEME COLOR TOKENS ---
class PaavaiColors {
  static const Color deepMaroon = Color(0xFF6B1B1B);
  static const Color bloodCrimson = Color(0xFFD32F2F);
  static const Color premiumGold = Color(0xFFC5A059);
  static const Color slateNavy = Color(0xFF1E293B);
  static const Color bgOffWhite = Color(0xFFF8FAFC);
  static const Color cardOutline = Color(0xFFE2E8F0);
  static const Color textDark = Color(0xFF0F172A);
  static const Color textLight = Color(0xFF64748B);
  static const Color successGreen = Color(0xFF10B981);
}

// --- DONOR MODEL REPRESENTATION ---
enum DonorType { student, faculty }

class DonorModel {
  final String id;
  final String name;
  final String email;
  final String registerNumber;
  final String bloodGroup;
  final DonorType userType;
  final String departmentOrDesignation;
  final String yearOrCabin;
  final String phoneNumber;
  final bool isAvailable;
  
  // Location Tracking Telemetry
  final String campusBlock; // e.g. "Main Block", "South Block", "PG Block"
  final double latitude;    // Mock latitude centered around Paavai Engineering Campus
  final double longitude;   // Mock longitude mapped relative to emergency center
  final double distanceInKm; // Live computed/simulated distance
  final DateTime lastDonationDate;

  const DonorModel({
    required this.id,
    required this.name,
    required this.email,
    required this.registerNumber,
    required this.bloodGroup,
    required this.userType,
    required this.departmentOrDesignation,
    required this.yearOrCabin,
    required this.phoneNumber,
    required this.isAvailable,
    required this.campusBlock,
    required this.latitude,
    required this.longitude,
    required this.distanceInKm,
    required this.lastDonationDate,
  });

  // Calculate elapsed days since the last eligible lifespan donation
  int get daysSinceLastDonation {
    return DateTime.now().difference(lastDonationDate).inDays;
  }

  // A donor is only fit for immediate mobilization if available and elapsed standard 90-day cooldown
  bool get isMedicallyEligible {
    return isAvailable && daysSinceLastDonation >= 90;
  }
}

// --- FLUTTER REGISTRY WIDGET ---
class PaavaiDonorRegistryScreen extends StatefulWidget {
  const PaavaiDonorRegistryScreen({Key? key}) : super(key: key);

  @override
  State<PaavaiDonorRegistryScreen> createState() => _PaavaiDonorRegistryScreenState();
}

class _PaavaiDonorRegistryScreenState extends State<PaavaiDonorRegistryScreen> with SingleTickerProviderStateMixin {
  // Controller state trackers
  String _searchQuery = '';
  String _selectedBloodGroup = 'ALL';
  String _selectedUserType = 'ALL'; // 'ALL', 'STUDENT', 'FACULTY'
  bool _onlyEligible = false;
  
  late AnimationController _radarAnimationController;
  
  // Master Preassigned Dataset (20,000+ students design schema reference)
  final List<DonorModel> _masterDonors = [
    DonorModel(
      id: "DON-01",
      name: "Ramesh Kumar K",
      email: "ramesh.k@paavai.edu.in",
      registerNumber: "FAC-CSE-042",
      bloodGroup: "O+",
      userType: DonorType.faculty,
      departmentOrDesignation: "Professor (CSE)",
      yearOrCabin: "Cabin 302",
      phoneNumber: "+91 9443212345",
      isAvailable: true,
      campusBlock: "Paavai Main Block (Level 3)",
      latitude: 11.4112,
      longitude: 78.1634,
      distanceInKm: 0.12,
      lastDonationDate: DateTime.now().subtract(const Duration(days: 120)),
    ),
    DonorModel(
      id: "DON-02",
      name: "Saran Ramesh",
      email: "saranramesh709@paavai.edu.in",
      registerNumber: "22104085",
      bloodGroup: "O-", // Rare universal donor!
      userType: DonorType.student,
      departmentOrDesignation: "Information Technology",
      yearOrCabin: "4th Year",
      phoneNumber: "+91 8870123512",
      isAvailable: true,
      campusBlock: "PG Block Library",
      latitude: 11.4121,
      longitude: 78.1648,
      distanceInKm: 0.35,
      lastDonationDate: DateTime.now().subtract(const Duration(days: 105)),
    ),
    DonorModel(
      id: "DON-03",
      name: "Priya Selvan",
      email: "priyaselvan@paavai.edu.in",
      registerNumber: "22103212",
      bloodGroup: "B+",
      userType: DonorType.student,
      departmentOrDesignation: "Electronics & Comm.",
      yearOrCabin: "3rd Year",
      phoneNumber: "+91 9942203104",
      isAvailable: true,
      campusBlock: "NSS Central Command Hub",
      latitude: 11.4101,
      longitude: 78.1612,
      distanceInKm: 0.05,
      lastDonationDate: DateTime.now().subtract(const Duration(days: 15)), // In cooldown!
    ),
    DonorModel(
      id: "DON-04",
      name: "Arun Karthik S",
      email: "arunkarthik@paavai.edu.in",
      registerNumber: "22104245",
      bloodGroup: "A-",
      userType: DonorType.student,
      departmentOrDesignation: "Chemical Engineering",
      yearOrCabin: "2nd Year",
      phoneNumber: "+91 7373112233",
      isAvailable: false, // Temporary offline
      campusBlock: "Tech Hostel - Block C",
      latitude: 11.4145,
      longitude: 78.1661,
      distanceInKm: 0.78,
      lastDonationDate: DateTime.now().subtract(const Duration(days: 180)),
    ),
    DonorModel(
      id: "DON-05",
      name: "Dr. Thangaraj M",
      email: "thangaraj.ece@paavai.edu.in",
      registerNumber: "FAC-ECE-105",
      bloodGroup: "AB+",
      userType: DonorType.faculty,
      departmentOrDesignation: "HOD (ECE Dept)",
      yearOrCabin: "ECE Office Wing",
      phoneNumber: "+91 9842711223",
      isAvailable: true,
      campusBlock: "Main Block First Floor",
      latitude: 11.4115,
      longitude: 78.1629,
      distanceInKm: 0.18,
      lastDonationDate: DateTime.now().subtract(const Duration(days: 95)),
    ),
    DonorModel(
      id: "DON-06",
      name: "Sneha RS",
      email: "snehars@paavai.edu.in",
      registerNumber: "22104399",
      bloodGroup: "B+",
      userType: DonorType.student,
      departmentOrDesignation: "Computer Science",
      yearOrCabin: "4th Year",
      phoneNumber: "+91 9965009988",
      isAvailable: true,
      campusBlock: "Paavai MCA Lab B",
      latitude: 11.4109,
      longitude: 78.1638,
      distanceInKm: 0.22,
      lastDonationDate: DateTime.now().subtract(const Duration(days: 200)),
    ),
  ];

  @override
  void initState() {
    super.initState();
    // Continuous spinning radar sweep logic
    _radarAnimationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();
  }

  @override
  void dispose() {
    _radarAnimationController.dispose();
    super.dispose();
  }

  // --- FILTRATION ENGINE ---
  List<DonorModel> get _filteredDonors {
    return _masterDonors.where((donor) {
      // 1. Text Query checking name, register number or camp location matching
      final matchesQuery = donor.name.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          donor.registerNumber.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          donor.campusBlock.toLowerCase().contains(_searchQuery.toLowerCase());

      // 2. Blood Group checking
      final matchesBlood = _selectedBloodGroup == 'ALL' || donor.bloodGroup == _selectedBloodGroup;

      // 3. User Type restriction
      bool matchesType = true;
      if (_selectedUserType == 'STUDENT') {
        matchesType = donor.userType == DonorType.student;
      } else if (_selectedUserType == 'FACULTY') {
        matchesType = donor.userType == DonorType.faculty;
      }

      // 4. Clinical eligibility (cooldown + availability toggled active)
      final matchesCriteria = !_onlyEligible || donor.isMedicallyEligible;

      return matchesQuery && matchesBlood && matchesType && matchesCriteria;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: PaavaiColors.bgOffWhite,
      appBar: AppBar(
        title: Text(
          'PAAVAI DONOR REGISTRY',
          style: GoogleFonts.spaceGrotesk(
            fontWeight: FontWeight.bold,
            letterSpacing: 0.8,
            fontSize: 16,
            color: Colors.white,
          ),
        ),
        backgroundColor: PaavaiColors.deepMaroon,
        centerTitle: true,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.share, color: Colors.white),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Registry database sync parameters exported successfully.')),
              );
            },
          ),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          if (constraints.maxWidth >= 900) {
            // Desktop horizontal split screen layout (Telemetry on Left, Filter list on Right)
            return Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  flex: 4,
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildRadarCard(),
                        const SizedBox(height: 24),
                        _buildStatsSummaryGrid(isWide: true),
                      ],
                    ),
                  ),
                ),
                const VerticalDivider(width: 1, color: PaavaiColors.cardOutline),
                Expanded(
                  flex: 6,
                  child: _buildListAndFilterSection(),
                ),
              ],
            );
          } else {
            // Mobile stacked UI design flow
            return SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                    child: _buildRadarCard(),
                  ),
                  _buildListAndFilterSection(),
                ],
              ),
            );
          }
        },
      ),
    );
  }

  // --- STATISTICAL VISUAL CHIPS ---
  Widget _buildStatsSummaryGrid({required bool isWide}) {
    final studentsCount = _masterDonors.where((d) => d.userType == DonorType.student).length;
    final facultyCount = _masterDonors.where((d) => d.userType == DonorType.faculty).length;
    final eligibleCount = _masterDonors.where((d) => d.isMedicallyEligible).length;

    return Row(
      children: [
        Expanded(
          child: _buildCounterBadge(
            label: "STUDENTS",
            value: "$studentsCount",
            color: PaavaiColors.slateNavy,
            icon: Icons.school,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _buildCounterBadge(
            label: "FACULTY",
            value: "$facultyCount",
            color: PaavaiColors.premiumGold,
            icon: Icons.badge,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _buildCounterBadge(
            label: "READY DESPATCH",
            value: "$eligibleCount",
            color: PaavaiColors.successGreen,
            icon: Icons.electric_bolt,
          ),
        ),
      ],
    );
  }

  Widget _buildCounterBadge({
    required String label,
    required String value,
    required Color color,
    required IconData icon,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: PaavaiColors.cardOutline),
      ),
      child: Column(
        children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(height: 6),
          Text(
            value,
            style: GoogleFonts.spaceGrotesk(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: PaavaiColors.textDark,
            ),
          ),
          Text(
            label,
            style: GoogleFonts.inter(
              fontSize: 9,
              fontWeight: FontWeight.w600,
              color: PaavaiColors.textLight,
            ),
          )
        ],
      ),
    );
  }

  // --- REAL-TIME RADAR CANVAS CARD ---
  Widget _buildRadarCard() {
    return Card(
      color: PaavaiColors.slateNavy,
      shape: RoundedCornerShape(16),
      elevation: 3,
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            color: Colors.black.withOpacity(0.15),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: const BoxDecoration(
                        color: Colors.red,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'REAL-TIME GPS TELEMETRY',
                      style: GoogleFonts.jetbrainsMono(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
                Text(
                  'RADIUS: 1.0 KM',
                  style: GoogleFonts.jetbrainsMono(
                    fontSize: 10,
                    color: PaavaiColors.premiumGold,
                  ),
                )
              ],
            ),
          ),
          Container(
            height: 220,
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: AnimatedBuilder(
              animation: _radarAnimationController,
              builder: (context, child) {
                return CustomPaint(
                  painter: PaavaiRadarPainter(
                    sweepAngle: _radarAnimationController.value * 2 * math.pi,
                    donors: _filteredDonors,
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      // Focal point center label
                      Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            padding: const EdgeInsets.all(4),
                            decoration: const BoxDecoration(
                              color: PaavaiColors.bloodCrimson,
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(Icons.emergency, color: Colors.white, size: 10),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'SOS CENTER',
                            style: GoogleFonts.spaceGrotesk(
                              color: Colors.white,
                              fontSize: 8,
                              fontWeight: FontWeight.bold,
                            ),
                          )
                        ],
                      )
                    ],
                  ),
                );
              },
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text(
              'Interactive radar charts location proximity of eligible donors relative to Paavai Main Emergency Center. Hover over/tap list below to detail coords.',
              style: GoogleFonts.inter(
                fontSize: 11,
                color: Colors.white.withOpacity(0.7),
                height: 1.5,
              ),
              textAlign: TextAlign.center,
            ),
          )
        ],
      ),
    );
  }

  // --- FILTERS & LIST BUILDER ---
  Widget _buildListAndFilterSection() {
    final list = _filteredDonors;

    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 1. Search Query Box
          TextField(
            onChanged: (val) {
              setState(() {
                _searchQuery = val;
              });
            },
            style: GoogleFonts.inter(fontSize: 13, color: PaavaiColors.textDark),
            decoration: InputDecoration(
              prefixIcon: const Icon(Icons.search, size: 20, color: PaavaiColors.textLight),
              hintText: 'Search registry by name, roll no, block location...',
              hintStyle: GoogleFonts.inter(fontSize: 12, color: PaavaiColors.textLight),
              filled: true,
              fillColor: Colors.white,
              isDense: true,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: PaavaiColors.cardOutline),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: PaavaiColors.deepMaroon, width: 1.5),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // 2. User Type Selectors: Horizontal scroll view
          Row(
            children: [
              _buildTypeChip("ALL ACTORS", "ALL", Icons.groups),
              const SizedBox(width: 8),
              _buildTypeChip("STUDENT", "STUDENT", Icons.school),
              const SizedBox(width: 8),
              _buildTypeChip("FACULTY", "FACULTY", Icons.badge),
            ],
          ),
          const SizedBox(height: 12),

          // 3. Blood Group Filter Row
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                'BLOOD:',
                style: GoogleFonts.spaceGrotesk(
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  color: PaavaiColors.textDark,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: SizedBox(
                  height: 32,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    children: ['ALL', 'O+', 'O-', 'A+', 'A-', 'B+', 'AB+']
                        .map((bg) => _buildBloodFilterChip(bg))
                        .toList(),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // 4. Clinical eligibility constraint toggles
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.verified_user,
                    size: 16,
                    color: _onlyEligible ? PaavaiColors.successGreen : PaavaiColors.textLight,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    'Exclusive Medically Eligible (90 days cooldown)',
                    style: GoogleFonts.inter(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: PaavaiColors.textDark,
                    ),
                  ),
                ],
              ),
              Switch.adaptive(
                value: _onlyEligible,
                activeColor: PaavaiColors.deepMaroon,
                onChanged: (val) {
                  setState(() {
                    _onlyEligible = val;
                  });
                },
              ),
            ],
          ),
          const Divider(height: 24, color: PaavaiColors.cardOutline),

          // 5. Listing counts counter
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'FILTERED REGISTRY LIST (${list.length})',
                style: GoogleFonts.spaceGrotesk(
                  fontSize: 12,
                  fontWeight: FontWeight.extrabold,
                  color: PaavaiColors.textDark,
                ),
              ),
              Text(
                'Domain Encoded: @paavai.edu.in',
                style: GoogleFonts.jetbrainsMono(
                  fontSize: 8,
                  fontWeight: FontWeight.bold,
                  color: PaavaiColors.successGreen,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // 6. ListView builder
          if (list.isEmpty)
            _buildEmptyState()
          else
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: list.length,
              itemBuilder: (context, index) {
                return _buildDonorCard(list[index]);
              },
            ),
        ],
      ),
    );
  }

  Widget _buildTypeChip(String label, String value, IconData icon) {
    final isSelected = _selectedUserType == value;
    return Expanded(
      child: InkWell(
        onTap: () {
          setState(() {
            _selectedUserType = value;
          });
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: isSelected ? PaavaiColors.deepMaroon : Colors.white,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: isSelected ? PaavaiColors.deepMaroon : PaavaiColors.cardOutline,
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 14,
                color: isSelected ? Colors.white : PaavaiColors.textLight,
              ),
              const SizedBox(width: 6),
              Text(
                label,
                style: GoogleFonts.spaceGrotesk(
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                  color: isSelected ? Colors.white : PaavaiColors.textLight,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBloodFilterChip(String bg) {
    final isSelected = _selectedBloodGroup == bg;
    return InkWell(
      onTap: () {
        setState(() {
          _selectedBloodGroup = bg;
        });
      },
      child: Container(
        margin: const EdgeInsets.only(right: 6),
        padding: const EdgeInsets.symmetric(horizontal: 12),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: isSelected ? PaavaiColors.bloodCrimson : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected ? PaavaiColors.bloodCrimson : PaavaiColors.cardOutline,
          ),
        ),
        child: Text(
          bg,
          style: GoogleFonts.spaceGrotesk(
            fontSize: 11,
            fontWeight: FontWeight.bold,
            color: isSelected ? Colors.white : PaavaiColors.textDark,
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Container(
      padding: const EdgeInsets.all(40),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: PaavaiColors.cardOutline),
      ),
      child: Column(
        children: [
          const Icon(Icons.info_outline, size: 40, color: PaavaiColors.bloodCrimson),
          const SizedBox(height: 12),
          Text(
            'No matching donors found.',
            style: GoogleFonts.spaceGrotesk(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: PaavaiColors.textDark,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Try adjusting filters or checking email coordinates parameters.',
            style: GoogleFonts.inter(fontSize: 11, color: PaavaiColors.textLight),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildDonorCard(DonorModel donor) {
    // Determine colors based on status/type
    final isFaculty = donor.userType == DonorType.faculty;
    final isEligible = donor.isMedicallyEligible;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: PaavaiColors.cardOutline),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.01),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: InkWell(
        onTap: () {
          _showDonorProfileDetailsSheet(donor);
        },
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Core user identification row
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CircleAvatar(
                    backgroundColor: PaavaiColors.bloodCrimson.withOpacity(0.08),
                    radius: 20,
                    child: Text(
                      donor.bloodGroup,
                      style: GoogleFonts.spaceGrotesk(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                        color: PaavaiColors.bloodCrimson,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text(
                              donor.name,
                              style: GoogleFonts.inter(
                                fontWeight: FontWeight.extrabold,
                                fontSize: 13,
                                color: PaavaiColors.textDark,
                              ),
                            ),
                            const Spacer(),
                            // Actor Role indicator badge
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: isFaculty
                                    ? PaavaiColors.premiumGold.withOpacity(0.12)
                                    : PaavaiColors.slateNavy.withOpacity(0.07),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                isFaculty ? "FACULTY" : "STUDENT",
                                style: GoogleFonts.spaceGrotesk(
                                  fontSize: 8,
                                  fontWeight: FontWeight.w800,
                                  color: isFaculty ? PaavaiColors.premiumGold : PaavaiColors.slateNavy,
                                ),
                              ),
                            )
                          ],
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${donor.registerNumber} • ${donor.departmentOrDesignation}',
                          style: GoogleFonts.inter(
                            fontSize: 11,
                            color: PaavaiColors.textLight,
                          ),
                        ),
                      ],
                    ),
                  )
                ],
              ),
              const Divider(height: 24, color: PaavaiColors.cardOutline),

              // 2. Real-time Location widget tracking row
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Row(
                      children: [
                        const Icon(
                          Icons.location_on,
                          size: 14,
                          color: PaavaiColors.deepMaroon,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            donor.campusBlock,
                            style: GoogleFonts.inter(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: PaavaiColors.textDark,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    '${donor.distanceInKm.toStringAsFixed(2)} km away',
                    style: GoogleFonts.jetbrainsMono(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: PaavaiColors.deepMaroon,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),

              // 3. Clinical health eligibility status indicators
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 6,
                        height: 6,
                        decoration: BoxDecoration(
                          color: isEligible ? PaavaiColors.successGreen : Colors.amber,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Text(
                        isEligible
                            ? "Eligible for mobilization"
                            : (donor.daysSinceLastDonation < 90
                                ? "In Cooldown (${90 - donor.daysSinceLastDonation} days left)"
                                : "Unavailable"),
                        style: GoogleFonts.inter(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: isEligible ? PaavaiColors.successGreen : Colors.amber[800],
                        ),
                      ),
                    ],
                  ),
                  Text(
                    'Last: ${donor.daysSinceLastDonation}d ago',
                    style: GoogleFonts.inter(
                      fontSize: 10,
                      color: PaavaiColors.textLight,
                    ),
                  )
                ],
              )
            ],
          ),
        ),
      ),
    );
  }

  // --- INTERACTIVE SYSTEM PROFILE BOTTOM SHEET ---
  void _showDonorProfileDetailsSheet(DonorModel donor) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) {
        return Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 40),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Center accent bar
              Center(
                child: Container(
                  width: 48,
                  height: 4,
                  decoration: BoxDecoration(
                    color: PaavaiColors.cardOutline,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Core Heading Info
              Row(
                children: [
                  CircleAvatar(
                    backgroundColor: PaavaiColors.bloodCrimson,
                    radius: 28,
                    child: Text(
                      donor.bloodGroup,
                      style: GoogleFonts.spaceGrotesk(
                        fontWeight: FontWeight.extrabold,
                        fontSize: 20,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          donor.name,
                          style: GoogleFonts.spaceGrotesk(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: PaavaiColors.textDark,
                          ),
                        ),
                        Text(
                          donor.email,
                          style: GoogleFonts.jetbrainsMono(
                            fontSize: 11,
                            color: PaavaiColors.successGreen,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  )
                ],
              ),
              const Divider(height: 32, color: PaavaiColors.cardOutline),

              // Academic/Professional details
              Row(
                children: [
                  Expanded(
                    child: _buildDetailsInfoBlock(
                      label: "REGISTER NO / CABIN",
                      value: donor.registerNumber,
                      icon: Icons.vpn_key,
                    ),
                  ),
                  Expanded(
                    child: _buildDetailsInfoBlock(
                      label: "AFFILIATION",
                      value: donor.userType == DonorType.faculty ? "FACULTY STAFF" : "STUDENT ACTOR",
                      icon: Icons.person_pin,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: _buildDetailsInfoBlock(
                      label: "DEPARTMENT",
                      value: donor.departmentOrDesignation,
                      icon: Icons.domain,
                    ),
                  ),
                  Expanded(
                    child: _buildDetailsInfoBlock(
                      label: "YEAR / ROOM",
                      value: donor.yearOrCabin,
                      icon: Icons.layers,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Location Telemetry details
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: PaavaiColors.slateNavy,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.gps_fixed, color: PaavaiColors.premiumGold, size: 16),
                        const SizedBox(width: 8),
                        Text(
                          "GPS VERIFIED LOCATION COORDS",
                          style: GoogleFonts.spaceGrotesk(
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      donor.campusBlock,
                      style: GoogleFonts.inter(
                        fontSize: 13,
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      "Lat: ${donor.latitude.toStringAsFixed(6)} • Long: ${donor.longitude.toStringAsFixed(6)}  |  ${donor.distanceInKm.toStringAsFixed(2)} km from base center",
                      style: GoogleFonts.jetbrainsMono(
                        fontSize: 9,
                        color: Colors.white.withOpacity(0.7),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // Action buttons row
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        Navigator.pop(context);
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Dialing registered phone: ${donor.phoneNumber}')),
                        );
                      },
                      icon: const Icon(Icons.phone, color: PaavaiColors.deepMaroon),
                      label: Text(
                        "DIAL CONTACT",
                        style: GoogleFonts.spaceGrotesk(
                          fontWeight: FontWeight.bold,
                          color: PaavaiColors.deepMaroon,
                        ),
                      ),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: donor.isMedicallyEligible
                          ? () {
                              Navigator.pop(context);
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  backgroundColor: PaavaiColors.bloodCrimson,
                                  content: Text('SOS Dispatch notification sent to ${donor.name}!'),
                                ),
                              );
                            }
                          : null,
                      icon: const Icon(Icons.flash_on, color: Colors.white),
                      label: Text(
                        "DISPATCH SOS",
                        style: GoogleFonts.spaceGrotesk(
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: PaavaiColors.bloodCrimson,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                    ),
                  )
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildDetailsInfoBlock({
    required String label,
    required String value,
    required IconData icon,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 12, color: PaavaiColors.textLight),
            const SizedBox(width: 4),
            Text(
              label,
              style: GoogleFonts.inter(
                fontSize: 9,
                fontWeight: FontWeight.bold,
                color: PaavaiColors.textLight,
                letterSpacing: 0.5,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: GoogleFonts.inter(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: PaavaiColors.textDark,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );
  }
}

// --- SIMULATED RADAR PAINTER ---
class PaavaiRadarPainter extends CustomPainter {
  final double sweepAngle;
  final List<DonorModel> donors;

  const PaavaiRadarPainter({
    required this.sweepAngle,
    required this.donors,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final double centerX = size.width / 2;
    final double centerY = size.height / 2;
    final double radius = math.min(size.width, size.height) / 2 - 10;
    
    final Paint gridPaint = Paint()
      ..color = Colors.green.withOpacity(0.18)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8;

    // 1. Draw concentric layout grid circles
    canvas.drawCircle(Offset(centerX, centerY), radius, gridPaint);
    canvas.drawCircle(Offset(centerX, centerY), radius * 0.7, gridPaint);
    canvas.drawCircle(Offset(centerX, centerY), radius * 0.4, gridPaint);

    // 2. Draw crosshairs
    canvas.drawLine(Offset(centerX - radius, centerY), Offset(centerX + radius, centerY), gridPaint);
    canvas.drawLine(Offset(centerX, centerY - radius), Offset(centerX, centerY + radius), gridPaint);

    // 3. Draw active visual radar sweeping beam
    final Paint sweepPaint = Paint()
      ..shader = RadialGradient(
        colors: [
          Colors.green.withOpacity(0.35),
          Colors.green.withOpacity(0.0),
        ],
      ).createShader(Rect.fromCircle(center: Offset(centerX, centerY), radius: radius));

    final sweepPath = Path()
      ..moveTo(centerX, centerY)
      ..arcTo(
        Rect.fromCircle(center: Offset(centerX, centerY), radius: radius),
        sweepAngle - 0.4, // Width of sweeping glow
        0.4,
        false,
      )
      ..close();
    canvas.drawPath(sweepPath, sweepPaint);

    // 4. Paint coordinates overlay dots for matching donors
    for (var donor in donors) {
      // Calculate angle and radial offset based on latitude/longitude seed
      final double seedAngle = (donor.latitude + donor.longitude) * 5000 % (2 * math.pi);
      // Map mock distance to pixel ratio (Max distance = 1.0 km)
      final double distanceRatio = donor.distanceInKm.clamp(0.0, 1.0);
      final double donorRadius = distanceRatio * radius;

      final double dotX = centerX + donorRadius * math.cos(seedAngle);
      final double dotY = centerY + donorRadius * math.sin(seedAngle);

      // Determine color based on donor eligibility parameters
      final Color dotColor = donor.isMedicallyEligible
          ? PaavaiColors.bloodCrimson
          : Colors.amber;

      final Paint dotPaint = Paint()
        ..color = dotColor
        ..style = PaintingStyle.fill;

      final Paint glowPaint = Paint()
        ..color = dotColor.withOpacity(0.2)
        ..style = PaintingStyle.fill;

      // Draw radar targets with blinking feedback glows
      canvas.drawCircle(Offset(dotX, dotY), 7.0, glowPaint);
      canvas.drawCircle(Offset(dotX, dotY), 4.5, dotPaint);
    }
  }

  @override
  bool shouldRepaint(covariant PaavaiRadarPainter oldDelegate) {
    return oldDelegate.sweepAngle != sweepAngle || oldDelegate.donors != donors;
  }
}
