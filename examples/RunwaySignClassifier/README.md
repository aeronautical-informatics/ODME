# Runway Sign Classifier — ODD Example

An Operational Design Domain model for a ground-based ML system that detects and
classifies airport runway signs from a vehicle-mounted camera, based on:

> K. Dmitriev, J. Schumann, I. Bostanov, M. Abdelhamid, F. Holzapfel,
> "Runway Sign Classifier: A DAL C Certifiable Machine Learning System,"
> *2023 IEEE/AIAA 42nd Digital Avionics Systems Conference (DASC)*, Barcelona, 2023.
> DOI: [10.1109/DASC58513.2023.10311228](https://doi.org/10.1109/DASC58513.2023.10311228)

## Design Rationale

This ODD models the **operating conditions** under which the sign classifier must
function — not the system's internal architecture. DNN hyperparameters
(confidence thresholds, backbone choice, input resolution) are design-space
parameters, not operational conditions, and are therefore excluded from the ODD.

Key modelling decisions:

- **Weather decomposed into Precipitation × Visibility.** The original model used
  monolithic weather categories (Fair/Rain/Snow/Fog) as a single specialization,
  which structurally prevented combinations like rain + low visibility. Separating
  them into independent specialization axes allows all physically plausible
  combinations.

- **Airport selection for geographic diversity.** The original 3 airports (KSFO,
  KBOS, KSAN) were all US coastal, low-elevation facilities. The expanded set
  covers high altitude (KDEN, 5431 ft), tropical climate (KMIA), and subarctic
  conditions (PANC) to exercise the classifier across diverse lighting,
  atmospheric, and sign-background conditions.

- **Sign type dimension added.** A sign classifier's ODD must include *what* it
  classifies. ICAO Annex 14 defines distinct sign categories with different colour
  schemes, sizes, and placement rules — each presenting different visual features
  to the classifier.

- **Degradation conditions added.** Real-world ML failures often stem from input
  degradation (faded signs, dirty lenses, glare) rather than nominal environmental
  variation. Including these conditions makes the ODD more representative of
  field deployment.

## System Entity Structure

```
RunwaySignClassifier
 └─ pSpec (aspect)
     ├─ Environment
     │   ├─ AirportDec (specialization)
     │   │   ├─ KSFO  — San Francisco Intl (37.62°N, 122.38°W, 13 ft)
     │   │   ├─ KBOS  — Boston Logan Intl (42.37°N, 71.01°W, 20 ft)
     │   │   ├─ KSAN  — San Diego Intl (32.73°N, 117.19°W, 17 ft)
     │   │   ├─ KDEN  — Denver Intl (39.86°N, 104.67°W, 5431 ft)
     │   │   ├─ KMIA  — Miami Intl (25.80°N, 80.29°W, 9 ft)
     │   │   └─ PANC  — Anchorage Intl (61.17°N, 149.99°W, 152 ft)
     │   ├─ PrecipitationDec (specialization)
     │   │   ├─ NoPrecipitation  — clear sky, 0 mm/h
     │   │   ├─ Rain             — 1–20 mm/h (WMO light–heavy)
     │   │   ├─ Snow             — 0.5–10 mm/h (WMO snow rates)
     │   │   └─ Hail             — 5–30 mm/h (WMO severe convective)
     │   ├─ VisibilityDec (specialization)
     │   │   ├─ HighVisibility      — 5–15 km (VFR)
     │   │   ├─ ModerateVisibility  — 1.5–5 km (marginal VFR)
     │   │   ├─ LowVisibility       — 0.5–1.5 km (IFR)
     │   │   └─ VeryLowVisibility   — 0.2–0.5 km (CAT II/III)
     │   ├─ TimeOfDayDec (specialization)
     │   │   ├─ Morning    — sun elevation 5°–30°, illuminance 1k–25k lux
     │   │   ├─ Afternoon  — sun elevation 30°–70°, illuminance 25k–100k lux
     │   │   ├─ Dusk       — sun elevation -6°–0°, illuminance 40–1000 lux
     │   │   └─ Dawn       — sun elevation -6°–10°, illuminance 40–5000 lux
     │   └─ SignTypeDec (specialization)
     │       ├─ MandatoryInstruction      — red background, white text (runway hold positions)
     │       ├─ LocationSign              — yellow text on black background
     │       ├─ DirectionSign             — yellow on black with arrows
     │       └─ RunwayDistanceRemaining   — black on white, distance markers
     ├─ Sensor (ground-based vehicle-mounted camera)
     │   ├─ DistanceDec (specialization)
     │   │   ├─ DS10  — 10–12 m from sign
     │   │   ├─ DS12  — 12–14 m from sign
     │   │   └─ DS14  — 14–16 m from sign
     │   ├─ ElevationDec (specialization)
     │   │   ├─ EL10  — camera 1.0–1.3 m AGL
     │   │   ├─ EL13  — camera 1.3–1.6 m AGL
     │   │   └─ EL16  — camera 1.6–1.9 m AGL
     │   └─ LateralOffsetDec (specialization)
     │       ├─ LO00  — 0–0.7 m offset from sign centre
     │       ├─ LO07  — 0.7–1.4 m offset
     │       └─ LO14  — 1.4–2.0 m offset
     └─ Degradation
         ├─ SignConditionDec (specialization)
         │   ├─ GoodCondition      — clean, fully visible sign
         │   ├─ FadedSign          — weathered paint, reduced contrast
         │   ├─ PartiallyOccluded  — partial obstruction (vehicle, equipment, snow bank)
         │   └─ WetSign            — water on sign surface, specular reflections
         └─ SensorConditionDec (specialization)
             ├─ NominalSensor   — clean lens, normal operation
             ├─ WaterDroplets   — lens contamination from rain or spray
             ├─ DirtySensor     — lens contamination from dust or debris
             └─ Overexposed     — direct sunlight glare on lens

Total leaf nodes:  6 + 4 + 4 + 4 + 4 + 3 + 3 + 3 + 4 + 4 = 39
Specialization nodes: 10
PES combinations: 6 × 4 × 4 × 4 × 4 × 3 × 3 × 3 × 4 × 4 = 1,327,104
```

## Variables (ODD Parameters)

### Environment — Airport

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| KSFO | latitude_deg | double | 37.6213 (fixed) | FAA AIP |
| KSFO | longitude_deg | double | -122.379 (fixed) | FAA AIP |
| KSFO | elevation_ft | double | 13.0 (fixed) | FAA AIP |
| KBOS | latitude_deg | double | 42.3656 (fixed) | FAA AIP |
| KBOS | longitude_deg | double | -71.0096 (fixed) | FAA AIP |
| KBOS | elevation_ft | double | 20.0 (fixed) | FAA AIP |
| KSAN | latitude_deg | double | 32.7336 (fixed) | FAA AIP |
| KSAN | longitude_deg | double | -117.19 (fixed) | FAA AIP |
| KSAN | elevation_ft | double | 17.0 (fixed) | FAA AIP |
| KDEN | latitude_deg | double | 39.8561 (fixed) | FAA AIP |
| KDEN | longitude_deg | double | -104.6737 (fixed) | FAA AIP |
| KDEN | elevation_ft | double | 5431.0 (fixed) | FAA AIP |
| KMIA | latitude_deg | double | 25.7959 (fixed) | FAA AIP |
| KMIA | longitude_deg | double | -80.2870 (fixed) | FAA AIP |
| KMIA | elevation_ft | double | 9.0 (fixed) | FAA AIP |
| PANC | latitude_deg | double | 61.1744 (fixed) | FAA AIP |
| PANC | longitude_deg | double | -149.9964 (fixed) | FAA AIP |
| PANC | elevation_ft | double | 152.0 (fixed) | FAA AIP |

### Environment — Precipitation

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| NoPrecipitation | precipitation_mm_h | double | 0.0 (fixed) | Clear sky |
| Rain | precipitation_mm_h | double | [1.0, 20.0] | WMO light–heavy rain |
| Snow | precipitation_mm_h | double | [0.5, 10.0] | WMO snow rates |
| Hail | precipitation_mm_h | double | [5.0, 30.0] | WMO severe convective |

### Environment — Visibility

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| HighVisibility | visibility_m | double | [5000, 15000] | ICAO VFR minimum |
| ModerateVisibility | visibility_m | double | [1500, 5000] | Marginal VFR |
| LowVisibility | visibility_m | double | [500, 1500] | IFR conditions |
| VeryLowVisibility | visibility_m | double | [200, 500] | ICAO CAT II/III |

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

### Environment — Sign Type

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| MandatoryInstruction | background_colour | string | "red" (fixed) | ICAO Annex 14 |
| MandatoryInstruction | text_colour | string | "white" (fixed) | ICAO Annex 14 |
| MandatoryInstruction | sign_height_cm | double | [30, 50] | ICAO Annex 14 Table 5-4 |
| LocationSign | background_colour | string | "black" (fixed) | ICAO Annex 14 |
| LocationSign | text_colour | string | "yellow" (fixed) | ICAO Annex 14 |
| LocationSign | sign_height_cm | double | [20, 40] | ICAO Annex 14 Table 5-4 |
| DirectionSign | background_colour | string | "black" (fixed) | ICAO Annex 14 |
| DirectionSign | text_colour | string | "yellow" (fixed) | ICAO Annex 14 |
| DirectionSign | has_arrow | boolean | true (fixed) | ICAO Annex 14 |
| DirectionSign | sign_height_cm | double | [20, 40] | ICAO Annex 14 Table 5-4 |
| RunwayDistanceRemaining | background_colour | string | "white" (fixed) | ICAO Annex 14 |
| RunwayDistanceRemaining | text_colour | string | "black" (fixed) | ICAO Annex 14 |
| RunwayDistanceRemaining | sign_height_cm | double | [60, 90] | ICAO Annex 14 Table 5-5 |

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

### Degradation — Sign Condition

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| GoodCondition | paint_contrast_pct | double | [80, 100] | Nominal reflectivity |
| FadedSign | paint_contrast_pct | double | [30, 80] | Weathered sign studies |
| FadedSign | age_years | double | [5, 20] | Typical repaint cycle |
| PartiallyOccluded | occlusion_pct | double | [10, 60] | Partial obstruction |
| PartiallyOccluded | occluder_type | string | "vehicle\|equipment\|snow" | Field observation |
| WetSign | specular_reflection_pct | double | [5, 40] | Wet surface reflectance |

### Degradation — Sensor Condition

| Entity | Variable | Type | Range | Source |
|--------|----------|------|-------|--------|
| NominalSensor | lens_transmission_pct | double | [95, 100] | Clean optics |
| WaterDroplets | droplet_coverage_pct | double | [5, 40] | Rain/spray contamination |
| WaterDroplets | lens_transmission_pct | double | [60, 95] | Partial obstruction |
| DirtySensor | dirt_coverage_pct | double | [5, 30] | Dust/debris contamination |
| DirtySensor | lens_transmission_pct | double | [70, 95] | Partial obstruction |
| Overexposed | glare_intensity_lux | double | [50000, 120000] | Direct sun on lens |
| Overexposed | affected_image_pct | double | [10, 60] | Bloom/flare area |

**Total: 72 parameters (30 with continuous ranges for LHS sampling)**

## Sample Scenarios (PES instances)

| Scenario | Airport | Precip | Visibility | Time | Sign | Distance | Elevation | Offset | Sign Cond | Sensor Cond | Risk |
|----------|---------|--------|------------|------|------|----------|-----------|--------|-----------|-------------|------|
| Nominal | KSFO | None | High | Morning | Mandatory | DS10 | EL13 | LO00 | Good | Nominal | Low |
| Winter Ops | KDEN | Snow | Low | Dawn | Location | DS14 | EL10 | LO07 | Wet | WaterDroplets | High |
| Tropical Glare | KMIA | Rain | Moderate | Afternoon | Direction | DS12 | EL16 | LO14 | Good | Overexposed | High |
| Arctic Degraded | PANC | Snow | VeryLow | Dusk | RwyDist | DS14 | EL10 | LO07 | Faded | DirtySensor | Critical |
| Coastal Fog | KBOS | None | VeryLow | Dawn | Mandatory | DS10 | EL13 | LO00 | PartOccl | Nominal | High |
| Dry Clear | KSAN | None | High | Afternoon | Direction | DS12 | EL13 | LO00 | Good | Nominal | Low |

## Test Case Generation

Use **Tools > Generate OD > Generate Test Cases (LHS)** to create test cases
via Latin Hypercube Sampling. The sampler extracts all variable parameters with
continuous ranges and distributes samples uniformly across each dimension while
minimising correlations between parameters.

With 1,327,104 structural PES combinations and 30 continuous parameters, full
enumeration is infeasible. LHS efficiently samples the parameter space — e.g.,
100 LHS samples per selected PES configuration provides good coverage with
manageable test set sizes.

## EASA Traceability

This example maps to EASA AI/ML guidance (Concept Paper Issue 2):

- **W-3.1 (ODD definition)**: Environment (Airport, Precipitation, Visibility,
  TimeOfDay, SignType), Sensor, and Degradation branches define the complete
  operational envelope
- **W-3.2 (Data requirements)**: Variable ranges sourced from ICAO Annex 14,
  WMO, CIE, and FAA AIP — each traceable to its authoritative reference
- **W-4.4 (Verification)**: Scenario enumeration (1,327,104 PES combinations)
  + LHS test case generation for coverage-driven testing
- **Degradation coverage**: SignCondition and SensorCondition branches address
  robustness requirements beyond nominal operating conditions

Maps to DO-178C DAL C objectives (Paper Table III):

- **A-2#4/5**: Low-level requirements → ODD parameters with explicit ranges
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
