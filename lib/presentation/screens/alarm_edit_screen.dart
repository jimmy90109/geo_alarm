import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geo_alarm/l10n/app_localizations.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../providers/alarm_provider.dart';

class AlarmEditScreen extends ConsumerStatefulWidget {
  const AlarmEditScreen({super.key});

  @override
  ConsumerState<AlarmEditScreen> createState() => _AlarmEditScreenState();
}

class _AlarmEditScreenState extends ConsumerState<AlarmEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  LatLng? _selectedPosition;
  double _radius = 1000; // default 1km

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.addEditAlarm),
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: () {
              if (_formKey.currentState!.validate() && _selectedPosition != null) {
                ref.read(alarmListProvider.notifier).addAlarm(
                      name: _nameController.text,
                      latitude: _selectedPosition!.latitude,
                      longitude: _selectedPosition!.longitude,
                      radius: _radius,
                    );
                Navigator.of(context).pop();
              }
            },
          ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: Column(
          children: [
            TextFormField(
              controller: _nameController,
              decoration: InputDecoration(labelText: AppLocalizations.of(context)!.alarmName),
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return AppLocalizations.of(context)!.pleaseEnterName;
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            Text(AppLocalizations.of(context)!.radius(_radius.toInt())),
            Slider(
              value: _radius,
              min: 100,
              max: 5000,
              divisions: 49,
              label: _radius.round().toString(),
              onChanged: (double value) {
                setState(() {
                  _radius = value;
                });
              },
            ),
            Expanded(
              child: GoogleMap(
                initialCameraPosition: const CameraPosition(
                  target: LatLng(25.034, 121.564), // Taipei 101
                  zoom: 11.0,
                ),
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
              ),
            ),
          ],
        ),
      ),
    );
  }
} 