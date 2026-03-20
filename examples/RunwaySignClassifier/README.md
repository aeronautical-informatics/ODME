# Runway Sign Classifier — ODD Example

An Operational Design Domain model for an airborne ML system that detects and
classifies airport runway signs, based on:

> K. Dmitriev, J. Schumann, I. Bostanov, M. Abdelhamid, F. Holzapfel,
> "Runway Sign Classifier: A DAL C Certifiable Machine Learning System,"
> *2023 IEEE/AIAA 42nd Digital Avionics Systems Conference (DASC)*, Barcelona, 2023.
> DOI: [10.1109/DASC58513.2023.10311228](https://doi.org/10.1109/DASC58513.2023.10311228)

## System Entity Structure

```
RunwaySignClassifier
 └─ pSpec (aspect)
     ├─ Environment
     │   ├─ AirportDec (specialization)
     │   │   ├─ KSFO  — San Francisco International (37.62°N, 122.38°W, 13 ft)
     │   │   ├─ KBOS  — Boston Logan International (42.37°N, 71.01°W, 20 ft)
     │   │   └─ KSAN  — San Diego International (32.73°N, 117.19°W, 17 ft)
     │   ├─ WeatherDec (specialization)
     │   │   ├─ FairWeather   — visibility 5–15 km, no precipitation
     │   │   ├─ RainyWeather  — visibility 1–5 km, precipitation 1–20 mm/h
     │   │   ├─ SnowyWeather  — visibility 0.5–5 km, precipitation 0.5–10 mm/h
     │   │   └─ FoggyWeather  — visibility 0.2–2 km, no precipitation
     │   └─ TimeOfDayDec (specialization)
     │       ├─ Morning    — sun elevation 5°–30°, illuminance 1k–25k lux
     │       ├─ Afternoon  — sun elevation 30°–70°, illuminance 25k–100k lux
     │       ├─ Dusk       — sun elevation -6°–0°, illuminance 40–1000 lux
     │       └─ Dawn       — sun elevation -6°–10°, illuminance 40–5000 lux
     ├─ Sensor
     │   ├─ DistanceDec (specialization)
     │   │   ├─ DS10  — 10–12 m from sign
     │   │   ├─ DS12  — 12–14 m from sign
     │   │   └─ DS14  — 14–16 m from sign
     │   ├─ ElevationDec (specialization)
     │   │   ├─ EL10  — camera 1.0–1.3 m AGL
     │   │   ├─ EL13  — camera 1.3–1.6 m AGL
     │   │   └─ EL16  — camera 1.6–1.9 m AGL
     │   └─ LateralOffsetDec (specialization)
     │       ├─ LO00  — 0–0.7 m offset from sign center
     │       ├─ LO07  — 0.7–1.4 m offset
     │       └─ LO14  — 1.4–2.0 m offset
     └─ SystemArchitecture
         ├─ DNNComponentDec (specialization)
         │   ├─ FasterRCNN  — ResNet-50, 50 layers, two-stage, 600–1200 px input
         │   └─ YOLOv2      — DarkNet-19, 19 layers, single-stage, 224–448 px input
         └─ SysArchDec (aspect)
             └─ SafetyMonitor  — IoU divergence comparator

Total leaf nodes:  3 + 4 + 4 + 3 + 3 + 3 + 2 + 1 = 23
Specialization nodes: 7  →  PES combinations: 3 × 4 × 4 × 3 × 3 × 3 × 2 = 2,592
```

## Variables (ODD Parameters)

### Environment — Airport

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| KSFO | latitude_deg | double | 37.6213 (fixed) | Paper Table II |
| KSFO | longitude_deg | double | -122.379 (fixed) | Paper Table II |
| KSFO | elevation_ft | double | 13.0 (fixed) | FAA AIP |
| KBOS | latitude_deg | double | 42.3656 (fixed) | Paper Table II |
| KBOS | longitude_deg | double | -71.0096 (fixed) | Paper Table II |
| KBOS | elevation_ft | double | 20.0 (fixed) | FAA AIP |
| KSAN | latitude_deg | double | 32.7336 (fixed) | Paper Table II |
| KSAN | longitude_deg | double | -117.19 (fixed) | Paper Table II |
| KSAN | elevation_ft | double | 17.0 (fixed) | FAA AIP |

### Environment — Weather

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| FairWeather | visibility_m | double | [5000, 15000] | WMO VFR/IFR categories |
| FairWeather | precipitation_mm_h | double | 0.0 (fixed) | Clear sky |
| RainyWeather | visibility_m | double | [1000, 5000] | WMO light-heavy rain |
| RainyWeather | precipitation_mm_h | double | [1.0, 20.0] | WMO rain rates |
| SnowyWeather | visibility_m | double | [500, 5000] | WMO snow visibility |
| SnowyWeather | precipitation_mm_h | double | [0.5, 10.0] | WMO snow rates |
| FoggyWeather | visibility_m | double | [200, 2000] | ICAO CAT I–IIIC fog |
| FoggyWeather | precipitation_mm_h | double | 0.0 (fixed) | Fog only |

### Environment — Time of Day

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| Morning | sun_elevation_deg | double | [5.0, 30.0] | Solar geometry |
| Morning | illuminance_lux | double | [1000, 25000] | CIE Standard |
| Afternoon | sun_elevation_deg | double | [30.0, 70.0] | Solar geometry |
| Afternoon | illuminance_lux | double | [25000, 100000] | CIE Standard |
| Dusk | sun_elevation_deg | double | [-6.0, 0.0] | Civil twilight |
| Dusk | illuminance_lux | double | [40, 1000] | CIE twilight |
| Dawn | sun_elevation_deg | double | [-6.0, 10.0] | Civil twilight |
| Dawn | illuminance_lux | double | [40, 5000] | CIE twilight |

### Sensor (from Paper Table II)

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| DS10 | distance_m | double | [10.0, 12.0] | Paper Table II |
| DS12 | distance_m | double | [12.0, 14.0] | Paper Table II |
| DS14 | distance_m | double | [14.0, 16.0] | Paper Table II |
| EL10 | elevation_m | double | [1.0, 1.3] | Paper Table II |
| EL13 | elevation_m | double | [1.3, 1.6] | Paper Table II |
| EL16 | elevation_m | double | [1.6, 1.9] | Paper Table II |
| LO00 | lateral_offset_m | double | [0.0, 0.7] | Paper Table II |
| LO07 | lateral_offset_m | double | [0.7, 1.4] | Paper Table II |
| LO14 | lateral_offset_m | double | [1.4, 2.0] | Paper Table II |

### System Architecture — DNN Components

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| FasterRCNN | backbone | string | "ResNet50" (fixed) | Paper Sec II-E |
| FasterRCNN | input_resolution_px | int | [600, 1200] | torchvision defaults |
| FasterRCNN | confidence_threshold | double | [0.5, 0.99] | Paper Sec II-E |
| FasterRCNN | iou_threshold | double | [0.3, 0.99] | Paper Sec II-E |
| FasterRCNN | num_layers | int | 50 (fixed) | ResNet-50 |
| YOLOv2 | backbone | string | "DarkNet19" (fixed) | Paper Sec II-E |
| YOLOv2 | input_resolution_px | int | [224, 448] | DarkNet-19 range |
| YOLOv2 | confidence_threshold | double | [0.5, 0.99] | Paper Sec II-E |
| YOLOv2 | anchor_h_px | int | 15 (fixed) | MATLAB toolbox |
| YOLOv2 | num_layers | int | 19 (fixed) | DarkNet-19 |

### System Architecture — Safety Monitor

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| SafetyMonitor | iou_divergence_threshold | double | [0.1, 0.5] | Paper Sec II-E, Beta(5.88,3.01) |
| SafetyMonitor | availability_target_pct | double | [90.0, 99.0] | Paper Sec II-E |

**Total: 43 parameters (21 with continuous ranges for LHS sampling)**

## Sample Scenarios (PES instances)

| Scenario | Airport | Weather | Time | Distance | Elevation | Offset | DNN | Risk |
|----------|---------|---------|------|----------|-----------|--------|-----|------|
| Nominal | KSFO | Fair | Morning | DS10 | EL13 | LO00 | FasterRCNN | Low |
| Degraded Visibility | KBOS | Snow | Dusk | DS14 | EL10 | LO07 | YOLOv2 | High |
| Edge Case | KSAN | Fog | Dawn | DS12 | EL16 | LO14 | FasterRCNN | High |
| Wet Conditions | KSFO | Rain | Afternoon | DS12 | EL13 | LO07 | YOLOv2 | Medium |

## Test Case Generation

Use **Tools > Generate OD > Generate Test Cases (LHS)** to create test cases
via Latin Hypercube Sampling. The sampler extracts all variable parameters with
continuous ranges and distributes samples uniformly across each dimension while
minimizing correlations between parameters.

Example: generating 10 LHS samples produces a CSV file with columns for each
of the 21 continuous parameters (see `RunwaySignClassifier_LHS_10.csv`).

## EASA Traceability

This example maps to EASA AI/ML guidance (Concept Paper Issue 2):
- **W-3.1 (ODD definition)**: Environment, Sensor, SystemArchitecture branches
- **W-3.2 (Data requirements)**: Paper Table II requirements → SES leaf variables with ranges
- **W-4.1 (Learning assurance)**: DNN component specializations with dissimilarity requirements
- **W-4.4 (Verification)**: Scenario enumeration (2,592 PES combinations) + LHS test case generation for coverage-driven testing

Maps to DO-178C DAL C objectives (Paper Table III):
- **A-2#4/5**: Low-level requirements → DNN model parameters with explicit ranges
- **RSC-A1–A8**: Architecture requirements → SystemArchitecture branch dissimilarity
- **MB.A-4#MB14–16**: Simulation/test correctness → traceable data requirements

## Usage

1. Copy this directory to the ODME working directory:
   ```bash
   cp -r examples/RunwaySignClassifier .
   ```
2. Launch ODME and use **File > Open** to select `RunwaySignClassifier`
3. Use **Tools > Generate OD** to view the ODD table with all parameters
4. Use **Generate Test Cases (LHS)** to produce test vectors
5. Use **Scenario Modelling** to prune individual PES instances
