import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geo_alarm/l10n/app_localizations.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../providers/alarm_provider.dart';
import '../../data/models/alarm_model.dart';

class AlarmEditScreen extends ConsumerStatefulWidget {
  final AlarmModel? alarm;

  const AlarmEditScreen({super.key, this.alarm});

  @override
  ConsumerState<AlarmEditScreen> createState() => _AlarmEditScreenState();
}

class _AlarmEditScreenState extends ConsumerState<AlarmEditScreen> {
  LatLng? _selectedPosition;
  double _radius = 1000; // default 1km
  bool _isMapLoading = true;
  late String _initialName;

  @override
  void initState() {
    super.initState();
    if (widget.alarm != null) {
      // Initialize with existing alarm data
      _selectedPosition =
          LatLng(widget.alarm!.latitude, widget.alarm!.longitude);
      _radius = widget.alarm!.radius;
      _initialName = widget.alarm!.name;
    } else {
      _initialName = '';
    }
  }

  void _showAlarmNameDialog() {
    final nameController = TextEditingController(text: _initialName);

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(AppLocalizations.of(context)!.alarmName),
          content: TextField(
            controller: nameController,
            decoration: InputDecoration(
              hintText: AppLocalizations.of(context)!.enterAlarmName,
              border: const OutlineInputBorder(),
            ),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text(AppLocalizations.of(context)!.cancel),
            ),
            TextButton(
              child: Text(AppLocalizations.of(context)!.save),
              onPressed: () {
                if (nameController.text.trim().isNotEmpty) {
                  if (widget.alarm != null) {
                    // Update existing alarm
                    final updatedAlarm = widget.alarm!.copyWith(
                      name: nameController.text.trim(),
                      latitude: _selectedPosition!.latitude,
                      longitude: _selectedPosition!.longitude,
                      radius: _radius,
                    );
                    ref
                        .read(alarmListProvider.notifier)
                        .updateAlarm(updatedAlarm);
                  } else {
                    // Add new alarm
                    ref.read(alarmListProvider.notifier).addAlarm(
                          name: nameController.text.trim(),
                          latitude: _selectedPosition!.latitude,
                          longitude: _selectedPosition!.longitude,
                          radius: _radius,
                        );
                  }
                  Navigator.of(context).pop(); // Close dialog
                  Navigator.of(context).pop(); // Close screen
                }
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        title: Text(widget.alarm != null
            ? AppLocalizations.of(context)!.editAlarm
            : AppLocalizations.of(context)!.addAlarm),
      ),
      body: Stack(
        children: [
          // Always render GoogleMap so onMapCreated can be called (hidden initially)
          Positioned.fill(
            child: Stack(
              children: [
                // Background GoogleMap
                _mapWidget(),
                // Bottom rounded rectangle with slider
                Positioned(
                  bottom: 0,
                  left: 24,
                  right: 24,
                  child: SafeArea(
                    minimum: const EdgeInsets.only(bottom: 24),
                    child: _sliderWidget(context),
                  ),
                ),
              ],
            ),
          ),
          // Loading overlay with fade animation
          Positioned.fill(
            child: _loadingWidget(context),
          ),
        ],
      ),
    );
  }

  Widget _mapWidget() {
    return GoogleMap(
      initialCameraPosition: CameraPosition(
        target: _selectedPosition ??
            const LatLng(
                25.034, 121.564), // Use alarm position or default to Taipei 101
        zoom: _selectedPosition != null
            ? 15.0
            : 13.0, // Closer zoom for existing alarms
      ),
      zoomControlsEnabled: false,
      onMapCreated: (GoogleMapController controller) {
        // Wait at least 1 second before showing the map
        Future.delayed(const Duration(seconds: 1), () {
          if (mounted) {
            setState(() {
              _isMapLoading = false;
            });
          }
        });
      },
      onTap: (position) {
        setState(() {
          _selectedPosition = position;
        });
      },
      markers: _selectedPosition == null
          ? {}
          : {
              Marker(
                markerId: const MarkerId('destination'),
                position: _selectedPosition!,
              ),
            },
      circles: _selectedPosition == null
          ? {}
          : {
              Circle(
                circleId: const CircleId('radius'),
                center: _selectedPosition!,
                radius: _radius,
                fillColor: Colors.blueGrey.withValues(alpha: 0.1),
                strokeColor: Colors.blueGrey.withValues(alpha: 0.8),
                strokeWidth: 2,
              ),
            },
    );
  }

  Widget _sliderWidget(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Theme.of(context).scaffoldBackgroundColor,
        borderRadius: const BorderRadius.all(Radius.circular(36)),
        boxShadow: const [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 10,
            spreadRadius: 0,
            offset: Offset(0, -2),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Radius slider
          Text(
            AppLocalizations.of(context)!.radius(_radius.toInt()),
            style: Theme.of(context).textTheme.bodyLarge,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Slider(
            value: _radius,
            min: 100,
            max: 5000,
            divisions: 49,
            padding: EdgeInsets.zero,
            label: '${_radius.round()}m',
            onChanged: (double value) {
              setState(() {
                _radius = value;
              });
            },
          ),
          const SizedBox(height: 16),
          // Save button
          FilledButton(
            onPressed:
                _selectedPosition != null ? () => _showAlarmNameDialog() : null,
            child: Text(AppLocalizations.of(context)!.save),
          )
        ],
      ),
    );
  }

  AnimatedSwitcher _loadingWidget(BuildContext context) {
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 500),
      child: _isMapLoading
          ? Container(
              key: const ValueKey('loading'),
              color: Theme.of(context).scaffoldBackgroundColor,
              child: const Center(
                child: CircularProgressIndicator(),
              ),
            )
          : const SizedBox.shrink(key: ValueKey('empty')),
    );
  }
}
