# Runway Sign Classifier — ODD Example

An Operational Design Domain model for an airborne ML system that detects and
classifies airport runway signs, based on:

> K. Dmitriev, J. Schumann, I. Bostanov, M. Abdelhamid, F. Holzapfel,
> "Runway Sign Classifier: A DAL C Certifiable Machine Learning System,"
> *2023 IEEE/AIAA 42nd Digital Avionics Systems Conference (DASC)*, Barcelona, 2023.

## System Entity Structure

```
RunwaySignClassifier
 └─ pSpec (specialization)
     ├─ Environment
     │   ├─ AirportDec (specialization)
     │   │   ├─ KSFO  — San Francisco International
     │   │   ├─ KBOS  — Boston Logan International
     │   │   └─ KSAN  — San Diego International
     │   ├─ WeatherDec (specialization)
     │   │   ├─ FairWeather
     │   │   ├─ RainyWeather
     │   │   ├─ SnowyWeather
     │   │   └─ FoggyWeather
     │   └─ TimeOfDayDec (specialization)
     │       ├─ Morning
     │       ├─ Afternoon
     │       ├─ Dusk
     │       └─ Dawn
     ├─ Sensor
     │   ├─ DistanceDec (specialization)
     │   │   ├─ DS10  — 10–12 m
     │   │   ├─ DS12  — 12–14 m
     │   │   └─ DS14  — 14–16 m
     │   ├─ ElevationDec (specialization)
     │   │   ├─ EL10  — 1.0–1.3 m AGL
     │   │   ├─ EL13  — 1.3–1.6 m AGL
     │   │   └─ EL16  — 1.6–1.9 m AGL
     │   └─ LateralOffsetDec (specialization)
     │       ├─ LO00  — 0–0.7 m
     │       ├─ LO07  — 0.7–1.4 m
     │       └─ LO14  — 1.4–2.0 m
     └─ SystemArchitecture
         ├─ DNNComponentDec (specialization)
         │   ├─ FasterRCNN  — ResNet-50 backbone, two-stage
         │   └─ YOLOv2      — DarkNet-19 backbone, single-stage
         └─ SafetyMonitor   — IoU-based output comparator

Total leaf nodes:  3 + 4 + 4 + 3 + 3 + 3 + 2 + 1 = 23
Specialization nodes: 6  →  PES combinations: 3 × 4 × 4 × 3 × 3 × 3 × 2 = 2,592
```

## Variables (ODD Parameters)

| Entity | Variable | Type | Range |
|--------|----------|------|-------|
| DS10 | distance_m | double | [10.0, 12.0] |
| DS12 | distance_m | double | [12.0, 14.0] |
| DS14 | distance_m | double | [14.0, 16.0] |
| EL10 | elevation_m | double | [1.0, 1.3] |
| EL13 | elevation_m | double | [1.3, 1.6] |
| EL16 | elevation_m | double | [1.6, 1.9] |
| LO00 | lateral_offset_m | double | [0.0, 0.7] |
| LO07 | lateral_offset_m | double | [0.7, 1.4] |
| LO14 | lateral_offset_m | double | [1.4, 2.0] |
| SafetyMonitor | iou_threshold | double | [0.32, 1.0] |
| FasterRCNN | confidence_threshold | double | [0.95, 1.0] |

## Sample Scenarios (PES instances)

| Scenario | Airport | Weather | Time | Distance | Elevation | Offset | DNN | Risk |
|----------|---------|---------|------|----------|-----------|--------|-----|------|
| Nominal | KSFO | Fair | Morning | DS10 | EL13 | LO00 | FasterRCNN | Low |
| Degraded Visibility | KBOS | Snow | Dusk | DS14 | EL10 | LO07 | YOLOv2 | High |
| Edge Case | KSAN | Fog | Dawn | DS12 | EL16 | LO14 | FasterRCNN | High |
| Wet Conditions | KSFO | Rain | Afternoon | DS12 | EL13 | LO07 | YOLOv2 | Medium |

## EASA Traceability

This example maps to EASA AI/ML guidance (Concept Paper Issue 2):
- **W-3.1 (ODD definition)**: Environment, Sensor, SystemArchitecture branches
- **W-3.2 (Data requirements)**: Table II requirements → SES leaf variables
- **W-4.1 (Learning assurance)**: DNN component specializations with dissimilarity
- **W-4.4 (Verification)**: Scenario enumeration for coverage-driven testing

## Usage

Copy this directory to the ODME working directory and open the project:
```bash
cp -r examples/RunwaySignClassifier .
# Then launch ODME — it will detect the project
```
