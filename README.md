# GrowthTwin (Android · Kotlin & Jetpack Compose)

GrowthTwin is an offline-first native Android precision-farming companion for Bangladesh agriculture, rewritten in Kotlin, Jetpack Compose, Material Design 3, and Room persistence from the original `ezecon/Growth-Twin` platform.

## Core Modules

1. **Home Overview (`HomeScreen`)**: Hero carousel, active plot health/moisture summary, and quick navigation tiles across all modules.
2. **Crop Advisor (`AdvisorScreen`)**: Bilingual Bangla/English natural-language slot extraction, agronomic rule/rotation/salinity/irrigation checks across 10 Bangladesh crops, 3-scenario (pessimistic, realistic, optimistic) profit modeling, interactive soil pH recalculation, and expert-review submission.
3. **Digital Plot Twin (`PlotTwinScreen`)**: Day-by-day crop water, N/P/K, pH, EC, heat/rain stress, and disease progression simulator with replayable weather, interactive daily advice, Farm Experiment Lab (what-if branch comparisons), zero-permission Photo Picker advisory leaf triage, Bangla/English `TextToSpeech` summary readout, and Room persistence.
4. **Smart Irrigation & Pump Control (`IrrigationScreen`)**: Live soil moisture liquid gauge, pump pipe animation, cooldown/max-run/daily-cap safety rules, auto-schedule with rain skip reasons, and simulation slider (clearly distinguished from ingested device packets).
5. **Sentinel Telemetry & Reality Gap (`SentinelScreen`)**: Fresh/Stale/Suspect sensor quality scenarios, ESP32 capacitive ADC wilting-point/field-capacity calibration, hardware ingestion verification checklist, packet tester, and reality-gap correction chart.
6. **SolarFlow AI Planner (`SolarFlowScreen`)**: Prototype solar-powered drip/pump irrigation scheduler combining soil deficit, 24h rain skip logic, solar PV vs. battery reserve limits, tank volume constraints, and Bangla/English advisories.
7. **Fair Market & DamSathi (`FairMarketScreen`)**: Voice (`RecognizerIntent`) and text price checker across 19 crops and 35+ Bangladesh districts, middleman offer fairness verdict, net-of-transport market comparison (`0.01 BDT/kg/km` Haversine routing), and farmer rights Q&A over offline reference baselines.
8. **Farmers' Trust & Messaging (`TrustAndMessagingScreen`)**: Failure-inclusive cohort profit distribution, IQR, 8-bin histogram, percentile ranking, and offline WhatsApp/SMS link-code simulation.
9. **Expert & Admin Review Console (`ExpertScreen`)**: Local role switcher (`farmer` / `expert` / `admin`) for reviewing flagged low-confidence advisor sessions and uncertain leaf photo submissions.

## Architecture & Persistence

- **UI Layer**: Jetpack Compose + Material 3 with custom dark (`#07100A` / `#A3F20A`) and day themes, `Outfit`, `Noto Sans Bengali`, and `JetBrains Mono` bundled fonts, edge-to-edge insets, and adaptive `NavigationBar`/`ModalNavigationDrawer`/`NavigationRail` layouts.
- **Domain Engines**: Pure-Kotlin agronomic, weather, irrigation, SolarFlow, Sentinel Kalman/gap correction, Fair Market, and Trust aggregation engines.
- **Local Storage**: Room 2.7.0 SQLite (`plots`, `plot_logs`, `plot_events`, `harvest_records`, `advisor_sessions`, `telemetry_readings`) and DataStore Preferences 1.1.7 (`BN`/`EN` language, theme, user role, checklist, and messaging state).
