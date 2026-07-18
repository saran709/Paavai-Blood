import 'package:flutter/material.dart';
import 'dart:math' as math;

/// A high-fidelity, interactive statistics widget in Flutter that visualizes
/// a student's personal donation frequency and impact using an elegant line chart.
///
/// Features:
/// 1. Dual modes: Toggle between "Frequency" (donation timeline) and "Impact" (lives saved progression).
/// 2. Custom Draw: Uses [CustomPainter] to render customizable spline curves, background fill gradients,
///    gridlines, and node highlights with zero third-party dependencies.
/// 3. Interactive Focus: Tapping anywhere on the canvas focuses the nearest data point and shows detailed telemetry.
/// 4. Empty State Blueprint Mode: If no histories are logged, displays a gorgeous dashed reference trend
///    to motivate the student.
class StudentDonationStatsWidget extends StatefulWidget {
  final List<DonationHistoryModel> donationHistory;

  const StudentDonationStatsWidget({
    Key? key,
    required this.donationHistory,
  }) : super(key: key);

  @override
  _StudentDonationStatsWidgetState createState() => _StudentDonationStatsWidgetState();
}

class _StudentDonationStatsWidgetState extends State<StudentDonationStatsWidget> {
  bool _isImpactMode = false;
  int _selectedPointIndex = 0;

  @override
  void initState() {
    super.initState();
    _resetSelection();
  }

  @override
  void didUpdateWidget(covariant StudentDonationStatsWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.donationHistory.length != oldWidget.donationHistory.length) {
      _resetSelection();
    }
  }

  void _resetSelection() {
    setState(() {
      final pointsCount = widget.donationHistory.isEmpty ? 4 : widget.donationHistory.length;
      _selectedPointIndex = pointsCount - 1;
    });
  }

  List<ChartPointData> _getChartPoints() {
    if (widget.donationHistory.isEmpty) {
      // Blueprint model trendline representation
      return [
        ChartPointData(label: 'Term 1', yValue: 0.0, description: 'Blueprint Initial state'),
        ChartPointData(label: 'Term 2', yValue: 3.0, description: 'Projected Target: 1st Donation'),
        ChartPointData(label: 'Term 3', yValue: 6.0, description: 'Projected Target: 2nd Donation'),
        ChartPointData(label: 'Term 4', yValue: 9.0, description: 'Target Milestone: 3 Donations'),
      ];
    } else {
      // Sort chronologically
      final sortedList = List<DonationHistoryModel>.from(widget.donationHistory)
        ..sort((a, b) => a.date.compareTo(b.date));

      double cumulativeUnits = 0.0;
      return List<ChartPointData>.generate(sortedList.length, (index) {
        final history = sortedList[index];
        cumulativeUnits += history.unitsDonated;

        // In frequency mode, we display the sequence number.
        // In impact mode, we show cumulative lives saved (units * 3)
        final yVal = _isImpactMode ? (cumulativeUnits * 3.0) : (index + 1).toDouble();

        // Format Date label
        String dateLabel = history.date;
        try {
          final parts = history.date.split('-');
          if (parts.length == 3) {
            final year = parts[0].substring(math.max(0, parts[0].length - 2));
            final month = int.tryParse(parts[1]) ?? 1;
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            final monthStr = (month >= 1 && month <= 12) ? months[month - 1] : 'M';
            dateLabel = "$monthStr, '$year";
          }
        } catch (_) {}

        return ChartPointData(
          label: dateLabel,
          yValue: yVal,
          description: _isImpactMode
              ? 'Units: ${cumulativeUnits.toInt()} (${(cumulativeUnits * 3).toInt()} Lives Saved)'
              : 'Donation #${index + 1} at ${history.hospitalName}',
        );
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final points = _getChartPoints();
    final activePoint = points.asMap().containsKey(_selectedPointIndex) ? points[_selectedPointIndex] : null;

    // Paavai Theme Colors
    const deepMaroon = Color(0xFF800000);
    const bloodCrimson = Color(0xFFC00000);
    const lightGold = Color(0xFFECC844);
    const textDark = Color(0xFF1E293B);
    const lightSlate = Color(0xFF64748B);
    const cardBorder = Color(0xFFE2E8F0);
    const successGreen = Color(0xFF10B981);

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.0),
        border: Border.all(color: cardBorder, width: 1.0),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 10.0,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Category title and interactive selector
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'PERSONAL IMPACT ANALYTICS',
                      style: TextStyle(
                        fontSize: 10.0,
                        fontWeight: FontWeight.bold,
                        color: lightGold,
                        letterSpacing: 0.5,
                      ),
                    ),
                    const SizedBox(height: 2.0),
                    Text(
                      _isImpactMode ? 'Cumulative Community Impact' : 'Donation Frequency & Timeline',
                      style: const TextStyle(
                        fontSize: 14.0,
                        fontWeight: FontWeight.bold,
                        color: textDark,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8.0),
              // Segmented Toggle
              Container(
                decoration: BoxDecoration(
                  color: cardBorder.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(8.0),
                ),
                padding: const EdgeInsets.all(2.0),
                child: Row(
                  children: [
                    _toggleButton(
                      label: 'Frequency',
                      isSelected: !_isImpactMode,
                      onTap: () {
                        setState(() {
                          _isImpactMode = false;
                        });
                      },
                    ),
                    _toggleButton(
                      label: 'Impact',
                      isSelected: _isImpactMode,
                      onTap: () {
                        setState(() {
                          _isImpactMode = true;
                        });
                      },
                    ),
                  ],
                ),
              )
            ],
          ),
          const SizedBox(height: 20.0),

          // Main Line Chart Canvas Wrapper
          LayoutBuilder(
            builder: (context, constraints) {
              return GestureDetector(
                onTapDown: (details) {
                  final RenderBox box = context.findRenderObject() as RenderBox;
                  final localOffset = box.globalToLocal(details.globalPosition);
                  final double chartWidth = constraints.maxWidth;
                  final double stepX = points.length > 1 ? chartWidth / (points.length - 1) : chartWidth;
                  
                  // Map local X gesture offset to closest node index
                  int closestIdx = (localOffset.dx / stepX).round();
                  closestIdx = closestIdx.clamp(0, points.length - 1);
                  setState(() {
                    _selectedPointIndex = closestIdx;
                  });
                },
                child: SizedBox(
                  width: double.infinity,
                  height: 130.0,
                  child: CustomPaint(
                    painter: DonationChartPainter(
                      points: points,
                      selectedIdx: _selectedPointIndex,
                      isBlueprint: widget.donationHistory.isEmpty,
                      crimsonColor: bloodCrimson,
                      maroonColor: deepMaroon,
                      greenColor: successGreen,
                      slateColor: lightSlate,
                      borderColor: cardBorder,
                    ),
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: 8.0),

          // X-Axis Labels Row
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: List.generate(points.length, (index) {
              final isSelected = index == _selectedPointIndex;
              return GestureDetector(
                onTap: () {
                  setState(() {
                    _selectedPointIndex = index;
                  });
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 4.0, horizontal: 2.0),
                  child: Text(
                    points[index].label,
                    style: TextStyle(
                      fontSize: 9.0,
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      color: isSelected ? deepMaroon : lightSlate,
                    ),
                  ),
                ),
              );
            }),
          ),
          const Divider(height: 24.0, thickness: 0.5),

          // Interactive Focus Info Panel Details
          if (activePoint != null) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(
                      _isImpactMode ? Icons.favorite : Icons.show_chart,
                      size: 16.0,
                      color: _isImpactMode ? successGreen : bloodCrimson,
                    ),
                    const SizedBox(width: 6.0),
                    Text(
                      _isImpactMode
                          ? 'Estimated Impact: ~${activePoint.yValue.toInt()} Lives Saved'
                          : 'Donation Event #${activePoint.yValue.toInt()}: Verified',
                      style: const TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.bold,
                        color: textDark,
                      ),
                    ),
                  ],
                ),
                Container(
                  decoration: BoxDecoration(
                    color: widget.donationHistory.isEmpty ? bloodCrimson.withOpacity(0.08) : Colors.emerald.withOpacity(0.08),
                    borderRadius: BorderRadius.circular(4.0),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 6.0, vertical: 2.0),
                  child: Text(
                    widget.donationHistory.isEmpty ? 'Blueprint View' : activePoint.label,
                    style: TextStyle(
                      fontSize: 9.0,
                      fontWeight: FontWeight.bold,
                      color: widget.donationHistory.isEmpty ? deepMaroon : successGreen,
                    ),
                  ),
                ),
              ],
            ),
          ],

          if (widget.donationHistory.isEmpty) ...[
            const SizedBox(height: 8.0),
            const Text(
              '💡 Ideal Trend Model Displayed. Each formal blood donation yields 1 Unit (saving up to 3 local patients). Start logging your historical donations within the registry to render actual personal telemetry live!',
              style: TextStyle(
                fontSize: 10.0,
                color: lightSlate,
                height: 1.4,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _toggleButton({
    required String label,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFF800000) : Colors.transparent,
          borderRadius: BorderRadius.circular(6.0),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 9.5,
            fontWeight: FontWeight.bold,
            color: isSelected ? Colors.white : const Color(0xFF64748B),
          ),
        ),
      ),
    );
  }
}

/// Custom Canvas Painter to render the donation statistics curve
class DonationChartPainter extends CustomPainter {
  final List<ChartPointData> points;
  final int selectedIdx;
  final bool isBlueprint;
  final Color crimsonColor;
  final Color maroonColor;
  final Color greenColor;
  final Color slateColor;
  final Color borderColor;

  DonationChartPainter({
    required this.points,
    required this.selectedIdx,
    required this.isBlueprint,
    required this.crimsonColor,
    required this.maroonColor,
    required this.greenColor,
    required this.slateColor,
    required this.borderColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (points.isEmpty) return;

    final double width = size.width;
    final double height = size.height;
    final int ptCount = points.length;

    final double maxVal = points.map((p) => p.yValue).reduce(math.max).clamp(1.0, double.infinity);
    final double stepX = ptCount > 1 ? width / (ptCount - 1) : width;

    // 1. Draw Grid Lines
    final Paint gridPaint = Paint()
      ..color = borderColor.withOpacity(0.4)
      ..strokeWidth = 1.0;

    const int gridLines = 3;
    for (int i = 0; i <= gridLines; i++) {
      final double yCoord = height * (i / gridLines);
      canvas.drawLine(Offset(0, yCoord), Offset(width, yCoord), gridPaint);
    }

    // 2. Map coordinates
    final localPoints = List<Offset>.generate(ptCount, (idx) {
      final double x = ptCount > 1 ? idx * stepX : width / 2.0;
      // standard line scaling
      final double y = height - (points[idx].yValue / maxVal * (height - 30.0)) - 15.0;
      return Offset(x, y);
    });

    // 3. Draw Area under curve (Gradient Fill)
    final Path areaPath = Path()
      ..moveTo(localPoints.first.dx, height)
      ..lineTo(localPoints.first.dx, localPoints.first.dy);

    for (int i = 1; i < localPoints.length; i++) {
      final prev = localPoints[i - 1];
      final curr = localPoints[i];
      final cp1 = Offset(prev.dx + (curr.dx - prev.dx) / 2.0, prev.dy);
      final cp2 = Offset(prev.dx + (curr.dx - prev.dx) / 2.0, curr.dy);
      areaPath.cubicTo(cp1.dx, cp1.dy, cp2.dx, cp2.dy, curr.dx, curr.dy);
    }
    areaPath.lineTo(localPoints.last.dx, height);
    areaPath.close();

    final Paint areaPaint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          crimsonColor.withOpacity(0.12),
          Colors.transparent,
        ],
      ).createShader(Rect.fromLTRB(0, 0, width, height));

    canvas.drawPath(areaPath, areaPaint);

    // 4. Draw Line Stroke
    final Path linePath = Path()..moveTo(localPoints.first.dx, localPoints.first.dy);
    for (int i = 1; i < localPoints.length; i++) {
      final prev = localPoints[i - 1];
      final curr = localPoints[i];
      final cp1 = Offset(prev.dx + (curr.dx - prev.dx) / 2.0, prev.dy);
      final cp2 = Offset(prev.dx + (curr.dx - prev.dx) / 2.0, curr.dy);
      linePath.cubicTo(cp1.dx, cp1.dy, cp2.dx, cp2.dy, curr.dx, curr.dy);
    }

    final Paint strokePaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.5
      ..strokeCap = StrokeCap.round
      ..color = isBlueprint ? slateColor.withOpacity(0.5) : maroonColor;

    canvas.drawPath(linePath, strokePaint);

    // 5. Draw Blueprint Dotted Companion Line if empty state is showing
    if (isBlueprint) {
      final Paint blueprintLinePaint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.2
        ..color = greenColor.withOpacity(0.4);

      // Dash Pattern: Draw incremental line segments representing dotted ideal projection path
      const double dashWidth = 8.0;
      const double dashSpace = 6.0;
      double startX = 0.0;
      final double endX = width;
      final double startY = height - 15.0;
      final double endY = 15.0;

      while (startX < endX) {
        final double ratioStart = startX / width;
        final double ratioEnd = math.min(1.0, (startX + dashWidth) / width);
        final double currYStart = startY + ratioStart * (endY - startY);
        final double currYEnd = startY + ratioEnd * (endY - startY);
        
        canvas.drawLine(
          Offset(startX, currYStart),
          Offset(math.min(width, startX + dashWidth), currYEnd),
          blueprintLinePaint,
        );
        startX += dashWidth + dashSpace;
      }
    }

    // 6. Draw Interactive Knobs/Anchor Nodes
    final Paint activeFillPaint = Paint()..color = maroonColor;
    final Paint accentRingPaint = Paint()
      ..color = const Color(0xFFECC844)
      ..style = PaintingStyle.fill;
    final Paint nodePaint = Paint()..color = crimsonColor.withOpacity(0.85);

    for (int i = 0; i < localPoints.length; i++) {
      final pt = localPoints[i];
      final bool isSelected = i == selectedIdx;

      if (isSelected) {
        // Double radius circle ring overlays
        canvas.drawCircle(pt, 6.0, accentRingPaint);
        canvas.drawCircle(pt, 3.5, activeFillPaint);
      } else {
        canvas.drawCircle(pt, 3.0, nodePaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant DonationChartPainter oldDelegate) {
    return oldDelegate.selectedIdx != selectedIdx ||
        oldDelegate.isBlueprint != isBlueprint ||
        oldDelegate.points != points;
  }
}

/// Plain Dart model class representing a point coordinate
class ChartPointData {
  final String label;
  final double yValue;
  final String description;

  ChartPointData({
    required this.label,
    required this.yValue,
    required this.description,
  });
}

/// Plain Dart model representing history record
class DonationHistoryModel {
  final int id;
  final String date; // "YYYY-MM-DD" style
  final double unitsDonated;
  final String hospitalName;

  DonationHistoryModel({
    required this.id,
    required this.date,
    required this.unitsDonated,
    this.hospitalName = 'Paavai Blood Camp',
  });
}
