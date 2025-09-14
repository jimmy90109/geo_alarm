import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geo_alarm/l10n/app_localizations.dart';
import 'alarm_edit_screen.dart';
import 'package:permission_handler/permission_handler.dart';
import '../widgets/alarm_list_widget.dart';
import '../widgets/language_btn.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    // request notification permission
    var notificationStatus = await Permission.notification.request();
    if (notificationStatus.isPermanentlyDenied) {
      await openAppSettings();
    }
    // request location permission
    var locationStatus = await Permission.location.request();
    if (locationStatus.isPermanentlyDenied) {
      await openAppSettings();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: _addAlarmBtn(context),
      body: CustomScrollView(
        slivers: [
          _appBar(),
          const AlarmListWidget(),
          // padding at bottom
          SliverToBoxAdapter(
            child: Container(
              height: 80 + MediaQuery.of(context).padding.bottom,
            ),
          ),
        ],
      ),
    );
  }

  Widget _appBar() {
    return SliverAppBar(
      expandedHeight: 200,
      pinned: true,
      floating: true,
      snap: false,
      actions: const [
        Padding(
          padding: EdgeInsets.symmetric(horizontal: 24.0),
          child: LanguageBtn(),
        )
      ],
      flexibleSpace: LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          return FlexibleSpaceBar(
            titlePadding: const EdgeInsets.only(
              left: 24,
              right: 24,
              bottom: 16,
            ),
            title: Align(
              alignment: Alignment.bottomLeft,
              child: Text(
                AppLocalizations.of(context)!.homeTitle,
                style: const TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.normal,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _addAlarmBtn(BuildContext context) {
    return FloatingActionButton(
      child: const Icon(Icons.add),
      onPressed: () {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (context) => const AlarmEditScreen(),
            fullscreenDialog: false,
          ),
        );
      },
    );
  }
}
