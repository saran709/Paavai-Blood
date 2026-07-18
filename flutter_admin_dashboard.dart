import 'package:flutter/material;
import 'package:google_fonts/google_fonts.dart';

/// ============================================================================
/// PAAVAI BLOODCONNECT - ENTERPRISE RESPONSIVE ADMIN DASHBOARD WIDGET
/// Designed to scale beautifully across Mobile, Tablet, and Desktop screens.
/// Features:
///   1. Dynamic Analytics Cards displaying campus-wide metrics.
///   2. High-Fidelity interactive Blood Group Distribution Chart.
///   3. Domain-Restricted User Directory with strict '@paavai.edu.in' verification.
///   4. Real-time Status Update Action Triggers.
/// ============================================================================

class PaavaiColorTokens {
  static const Color deepMaroon = Color(0xFF6B1B1B);
  static const Color bloodCrimson = Color(0xFFD32F2F);
  static const Color premiumGold = Color(0xFFC5A059);
  static const Color customSlate = Color(0xFF2C3E50);
  static const Color warmLightBG = Color(0xFFF9FBFD);
  static const Color borderLighter = Color(0xFFE2E8F0);
  static const Color textBody = Color(0xFF4A5568);
}

class PaavaiAdminDashboard extends StatefulWidget {
  const PaavaiAdminDashboard({Key? key}) : super(key: key);

  @override
  State<PaavaiAdminDashboard> createState() => _PaavaiAdminDashboardState();
}

class _PaavaiAdminDashboardState extends State<PaavaiAdminDashboard> {
  String selectedMenu = 'Analytics'; // Left rail navigation controller
  String searchQuery = '';
  String filterGroup = 'ALL';
  
  // High-scale list representing 20,000+ users database mock matching the Paavai domain rule
  final List<Map<String, String>> campusUsers = [
    {
      'name': 'Ramesh Kumar K',
      'email': 'ramesh.k@paavai.edu.in',
      'role': 'Faculty Donor',
      'group': 'O+',
      'status': 'Eligible',
      'registerNo': 'FAC042'
    },
    {
      'name': 'Saran Ramesh',
      'email': 'saranramesh709@paavai.edu.in',
      'role': 'Super Admin',
      'group': 'O-',
      'status': 'Eligible',
      'registerNo': 'ADM001'
    },
    {
      'name': 'Priya Selvan',
      'email': 'priyaselvan@paavai.edu.in',
      'role': 'NSS Volunteer Coordinator',
      'group': 'B+',
      'status': 'Eligible',
      'registerNo': 'NSS099'
    },
    {
      'name': 'Arun Karthik',
      'email': 'arunkarthik@paavai.edu.in',
      'role': 'Student Donor',
      'group': 'A-',
      'status': 'In-Cooldown',
      'registerNo': '22104085'
    },
    {
      'name': 'Sneha RS',
      'email': 'snehars@paavai.edu.in',
      'role': 'YRC volunteer',
      'group': 'AB+',
      'status': 'Eligible',
      'registerNo': '22104099'
    }
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: PaavaiColorTokens.warmLightBG,
      body: LayoutBuilder(
        builder: (context, constraints) {
          // Check screen size classification
          if (constraints.maxWidth >= 1024) {
            // Desktop Split Layout with left static side navigational drawer rail
            return Row(
              children: [
                _buildSidebar(isDesktop: true),
                const VerticalDivider(width: 1, color: PaavaiColorTokens.borderLighter),
                Expanded(
                  child: _buildMainWorkspace(),
                ),
              ],
            );
          } else if (constraints.maxWidth >= 600) {
            // Medium sized screen display (Tablets, Foldables) with narrow NavigationRail
            return Row(
              children: [
                _buildNavigationRail(),
                const VerticalDivider(width: 1, color: PaavaiColorTokens.borderLighter),
                Expanded(
                  child: _buildMainWorkspace(),
                ),
              ],
            );
          } else {
            // Mobile viewport using standard slide drawer context
            return Scaffold(
              appBar: AppBar(
                title: Text(
                  'PAAVAI BLOODCONNECT',
                  style: GoogleFonts.spaceGrotesk(
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                    fontSize: 16,
                  ),
                ),
                backgroundColor: PaavaiColorTokens.deepMaroon,
                centerTitle: true,
              ),
              drawer: Drawer(
                child: _buildSidebar(isDesktop: false),
              ),
              body: _buildMainWorkspace(),
            );
          }
        },
      ),
    );
  }

  // 1. Sidebar Navigational Panel
  Widget _buildSidebar({required bool isDesktop}) {
    return Container(
      width: 260,
      color: Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            color: PaavaiColorTokens.deepMaroon,
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(6),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.shield_outlined, color: PaavaiColorTokens.premiumGold, size: 28),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'PAAVAI',
                        style: GoogleFonts.spaceGrotesk(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 1.2,
                        ),
                      ),
                      Text(
                        'BLOODCONNECT',
                        style: GoogleFonts.spaceGrotesk(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: PaavaiColorTokens.premiumGold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          _buildSidebarItem(icon: Icons.dashboard, title: 'Analytics'),
          _buildSidebarItem(icon: Icons.people_alt, title: 'User Management'),
          _buildSidebarItem(icon: Icons.healing, title: 'Emergency Requests'),
          _buildSidebarItem(icon: Icons.event, title: 'Camp Schedules'),
          const Spacer(),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                const Icon(Icons.dns, size: 16, color: PaavaiColorTokens.bloodCrimson),
                const SizedBox(width: 8),
                Text(
                  'Zone DB Active (20k scale)',
                  style: GoogleFonts.jetbrainsMono(fontSize: 10, color: PaavaiColorTokens.textBody),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  Widget _buildSidebarItem({required IconData icon, required String title}) {
    final isSelected = selectedMenu == title;
    return InkWell(
      onTap: () {
        setState(() {
          selectedMenu = title;
        });
      },
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected ? PaavaiColorTokens.deepMaroon.withOpacity(0.08) : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(icon, color: isSelected ? PaavaiColorTokens.deepMaroon : Colors.grey[600], size: 20),
            const SizedBox(width: 12),
            Text(
              title,
              style: GoogleFonts.inter(
                fontSize: 13,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                color: isSelected ? PaavaiColorTokens.deepMaroon : Colors.grey[800],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // 2. NavigationRail representation for Medium/Tablet Screen ratios
  Widget _buildNavigationRail() {
    return NavigationRail(
      selectedIndex: selectedMenu == 'Analytics' ? 0 : 1,
      onDestinationSelected: (idx) {
        setState(() {
          selectedMenu = idx == 0 ? 'Analytics' : 'User Management';
        });
      },
      labelType: NavigationRailLabelType.all,
      destinations: const [
        NavigationRailDestination(
          icon: Icon(Icons.dashboard_outlined),
          selectedIcon: Icon(Icons.dashboard, color: PaavaiColorTokens.deepMaroon),
          label: Text('Analytics'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.people_alt_outlined),
          selectedIcon: Icon(Icons.people_alt, color: PaavaiColorTokens.deepMaroon),
          label: Text('Directory'),
        ),
      ],
    );
  }

  // 3. Central Application View Space Dispatcher
  Widget _buildMainWorkspace() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    selectedMenu.toUpperCase(),
                    style: GoogleFonts.spaceGrotesk(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: PaavaiColorTokens.customSlate,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      const CircleAvatar(backgroundColor: Colors.green, radius: 4),
                      const SizedBox(width: 6),
                      Text(
                        'Master LDAP Synchronizer Active',
                        style: GoogleFonts.inter(fontSize: 11, color: Colors.grey[600]),
                      )
                    ],
                  ),
                ],
              ),
              Image.network(
                'https://images.unsplash.com/photo-1544005313-94ddf0286df2?fit=crop&w=48&h=48',
                errorBuilder: (_, __, ___) => const CircleAvatar(child: Icon(Icons.admin_panel_settings)),
              )
            ],
          ),
          const SizedBox(height: 24),
          if (selectedMenu == 'Analytics') _buildAnalyticsView(),
          if (selectedMenu == 'User Management') _buildUserManagementView(),
        ],
      ),
    );
  }

  // 4. Analytics Panel Layout Widgets
  Widget _buildAnalyticsView() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Grid cards displaying current metrics
        LayoutBuilder(builder: (context, constraints) {
          int crossAxisCount = constraints.maxWidth > 800 ? 3 : 1;
          return GridView.count(
            crossAxisCount: crossAxisCount,
            childAspectRatio: 3.2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 16,
            mainAxisSpacing: 16,
            children: [
              _buildStatCard(
                title: 'TOTAL REGISTERED DONORS',
                value: '18,492',
                detail: '@paavai.edu.in students & staff',
                icon: Icons.verified,
                color: Colors.blueAccent,
              ),
              _buildStatCard(
                title: 'ACTIVE SOS EMERGENCY CRISIS',
                value: '4 RUNS',
                detail: 'NSS/NCC mobilization targets',
                icon: Icons.notification_important,
                color: PaavaiColorTokens.bloodCrimson,
              ),
              _buildStatCard(
                title: 'LIVES REGISTERED AS SAVED',
                value: '422 SAVED',
                detail: 'Approved clinical registrations',
                icon: Icons.favorite,
                color: Colors.pink,
              ),
            ],
          );
        }),
        const SizedBox(height: 32),
        Text(
          'CAMPUS BLOOD GROUP DISTRIBUTION MATRIX',
          style: GoogleFonts.spaceGrotesk(fontWeight: FontWeight.bold, fontSize: 14, color: PaavaiColorTokens.customSlate),
        ),
        const SizedBox(height: 12),
        // Visualization mock chart representation
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: PaavaiColorTokens.borderLighter),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Distribution among active campus volunteers (NSS/NCC/YRC):',
                style: GoogleFonts.inter(fontSize: 12, color: Colors.grey[600]),
              ),
              const SizedBox(height: 24),
              _buildProgressBarMetric(label: 'O+ Positive', percentage: 0.38, color: PaavaiColorTokens.bloodCrimson, count: '7,026'),
              _buildProgressBarMetric(label: 'O- Negative (Rare Emergency Match)', percentage: 0.08, color: PaavaiColorTokens.premiumGold, count: '1,479'),
              _buildProgressBarMetric(label: 'A+ Positive', percentage: 0.24, color: PaavaiColorTokens.customSlate, count: '4,438'),
              _buildProgressBarMetric(label: 'B+ Positive', percentage: 0.20, color: Colors.green, count: '3,698'),
              _buildProgressBarMetric(label: 'AB+ / AB- Critical Coaxial', percentage: 0.10, color: Colors.orange, count: '1,849'),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildStatCard({
    required String title,
    required String value,
    required String detail,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: PaavaiColorTokens.borderLighter),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.01),
            blurRadius: 10,
            offset: const Offset(0, 4),
          )
        ],
      ),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: color.withOpacity(0.06),
            radius: 26,
            child: Icon(icon, color: color, size: 28),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  title,
                  style: GoogleFonts.inter(fontSize: 9, fontWeight: FontWeight.bold, letterSpacing: 0.5, color: Colors.grey[500]),
                ),
                const SizedBox(height: 4),
                Text(
                  value,
                  style: GoogleFonts.spaceGrotesk(fontSize: 20, fontWeight: FontWeight.bold, color: PaavaiColorTokens.customSlate),
                ),
                Text(
                  detail,
                  style: GoogleFonts.inter(fontSize: 10, color: Colors.grey[600]),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  Widget _buildProgressBarMetric({
    required String label,
    required double percentage,
    required Color color,
    required String count,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label, style: GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.bold, color: PaavaiColorTokens.customSlate)),
              Text('$count registered (${(percentage * 100).toInt()}%)', style: GoogleFonts.jetbrainsMono(fontSize: 11, color: Colors.grey[600])),
            ],
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: percentage,
              minHeight: 10,
              backgroundColor: Colors.grey[100],
              color: color,
            ),
          )
        ],
      ),
    );
  }

  // 5. Restricted User Management Directory List Layout with Strict domain locking checks
  Widget _buildUserManagementView() {
    final filteredList = campusUsers.where((user) {
      final nameMatches = user['name']!.toLowerCase().contains(searchQuery.toLowerCase());
      final rollMatches = user['registerNo']!.toLowerCase().contains(searchQuery.toLowerCase());
      final bgMatches = filterGroup == 'ALL' || user['group'] == filterGroup;
      return (nameMatches || rollMatches) && bgMatches;
    }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                onChanged: (val) {
                  setState(() {
                    searchQuery = val;
                  });
                },
                decoration: InputDecoration(
                  prefixIcon: const Icon(Icons.search, size: 20),
                  hintText: 'Search dynamic registry by Roll Number or Name...',
                  hintStyle: GoogleFonts.inter(fontSize: 12),
                  filled: true,
                  fillColor: Colors.white,
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: PaavaiColorTokens.borderLighter)),
                  focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: PaavaiColorTokens.deepMaroon)),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: PaavaiColorTokens.borderLighter),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: filterGroup,
                  items: ['ALL', 'O+', 'O-', 'B+', 'A-', 'AB+']
                      .map((val) => DropdownMenuItem(value: val, child: Text('Filter: $val', style: GoogleFonts.inter(fontSize: 12))))
                      .toList(),
                  onChanged: (val) {
                    setState(() {
                      filterGroup = val ?? 'ALL';
                    });
                  },
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: PaavaiColorTokens.borderLighter),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                color: PaavaiColorTokens.deepMaroon.withOpacity(0.02),
                child: Row(
                  children: [
                    const Icon(Icons.domain_verification, size: 18, color: Colors.green),
                    const SizedBox(width: 8),
                    Text(
                      'STRICT DOMAIN MANDATE ENFORCED: @paavai.edu.in ONLY',
                      style: GoogleFonts.jetbrainsMono(fontWeight: FontWeight.bold, fontSize: 11, color: Colors.green[800]),
                    )
                  ],
                ),
              ),
              const Divider(height: 1, color: PaavaiColorTokens.borderLighter),
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: filteredList.length,
                separatorBuilder: (_, __) => const Divider(height: 1, color: PaavaiColorTokens.borderLighter),
                itemBuilder: (context, index) {
                  final user = filteredList[index];
                  // Defensive verification rule asserting correctness of domain input
                  final isDomainAuthenticated = user['email']!.endsWith('@paavai.edu.in');

                  return Padding(
                    padding: const EdgeInsets.all(20),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Row(
                            children: [
                              CircleAvatar(
                                backgroundColor: isDomainAuthenticated ? Colors.green.withOpacity(0.1) : Colors.red.withOpacity(0.1),
                                child: Text(user['group']!, style: GoogleFonts.spaceGrotesk(color: PaavaiColorTokens.bloodCrimson, fontWeight: FontWeight.bold, fontSize: 15)),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      user['name']!,
                                      style: GoogleFonts.inter(fontWeight: FontWeight.bold, fontSize: 13, color: PaavaiColorTokens.customSlate),
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'RollNo: ${user['registerNo']} • ${user['role']}',
                                      style: GoogleFonts.inter(fontSize: 11, color: Colors.grey[600]),
                                    ),
                                    Text(
                                      user['email']!,
                                      style: GoogleFonts.jetbrainsMono(fontSize: 10, color: isDomainAuthenticated ? Colors.green[700] : Colors.red),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                              decoration: BoxDecoration(
                                color: user['status'] == 'Eligible' ? Colors.green.withOpacity(0.08) : Colors.amber.withOpacity(0.08),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                user['status']!,
                                style: GoogleFonts.inter(
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                  color: user['status'] == 'Eligible' ? Colors.green[700] : Colors.amber[800],
                                ),
                              ),
                            ),
                            const SizedBox(width: 16),
                            IconButton(
                              icon: const Icon(Icons.settings_outlined, size: 20),
                              onPressed: () {
                                _showUserActionBottomSheet(context, user);
                              },
                            )
                          ],
                        )
                      ],
                    ),
                  );
                },
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _showUserActionBottomSheet(BuildContext context, Map<String, String> user) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Manage Registry Access',
                style: GoogleFonts.spaceGrotesk(fontSize: 16, fontWeight: FontWeight.bold, color: PaavaiColorTokens.customSlate),
              ),
              const SizedBox(height: 6),
              Text(
                'Account: ${user['name']} (${user['email']})',
                style: GoogleFonts.inter(fontSize: 12, color: Colors.grey[600]),
              ),
              const SizedBox(height: 18),
              ListTile(
                leading: const Icon(Icons.toggle_on_outlined, color: Colors.green),
                title: Text('Toggle Availability Cooldown', style: GoogleFonts.inter(fontSize: 13)),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Cooldown toggled for student ${user['registerNo']}.')),
                  );
                },
              ),
              ListTile(
                leading: const Icon(Icons.admin_panel_settings_outlined, color: PaavaiColorTokens.deepMaroon),
                title: Text('Promote to NSS Coordinator', style: GoogleFonts.inter(fontSize: 13)),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('${user['name']} promoted successfully.')),
                  );
                },
              ),
              ListTile(
                leading: const Icon(Icons.delete_outline, color: Colors.red),
                title: Text('Revoke Registry Access', style: GoogleFonts.inter(fontSize: 13, color: Colors.red)),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Verification privileges revoked lock safe.')),
                  );
                },
              ),
            ],
          ),
        );
      },
    );
  }
}
