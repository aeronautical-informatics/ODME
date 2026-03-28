#!/usr/bin/env python3
"""
MSFS Scenario Generator for the RunwaySignClassifier ODD.

Generates complete Microsoft Flight Simulator scenario packages from ODME
test cases, mapping every ODD parameter dimension to simulator-controllable
variables:

  Airport         -> aircraft position (lat/lon/alt in .FLT)
  Precipitation   -> .WPR weather preset
  Visibility      -> .WPR aerosol density (Koschmieder's law)
  Time of Day     -> local time in .FLT (sun-elevation inversion)
  Sign Type       -> sign spawn metadata (scenario manifest)
  Sensor          -> camera placement (SimConnect script)
  Degradation     -> post-processing filter metadata

For each test case the plugin produces:
  weather_presets/<TC>.WPR     – MSFS weather preset XML
  flights/<TC>.FLT             – MSFS flight file (position + time)
  simconnect/<TC>.py           – SimConnect camera placement script
  scenario_manifest.csv        – master index of all test cases
  results.json                 – verdict feed-back to ODME

Usage (standalone):
    python runway_sign_msfs.py --odme-project export.json --output-dir ./output

Usage (from ODME):
    Tools > Run Python Plugin... > select this file
"""
from __future__ import annotations

import csv
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional

# Add parent so the odme package can be found when run from the examples dir
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from odme.cli import setup
from odme.export import write_verdicts


# ─── Airport database ───────────────────────────────────────────────────────

@dataclass
class Airport:
    icao: str
    lat: float
    lon: float
    elev_ft: float
    # Taxiway start position (approximate, near a sign-rich area)
    taxi_lat: float
    taxi_lon: float
    taxi_hdg: float  # heading on taxiway, degrees true


AIRPORTS: Dict[str, Airport] = {
    "KSFO": Airport("KSFO", 37.6213, -122.379, 13.0,
                    37.6155, -122.3820, 300.0),
    "KBOS": Airport("KBOS", 42.3656, -71.0096, 20.0,
                    42.3630, -71.0060, 40.0),
    "KSAN": Airport("KSAN", 32.7336, -117.190, 17.0,
                    32.7320, -117.1880, 275.0),
    "KDEN": Airport("KDEN", 39.8561, -104.674, 5431.0,
                    39.8500, -104.6700, 170.0),
    "KMIA": Airport("KMIA", 25.7959, -80.2870, 9.0,
                    25.7930, -80.2830, 90.0),
    "PANC": Airport("PANC", 61.1744, -149.996, 152.0,
                    61.1710, -149.9900, 180.0),
}


# ─── MSFS .WPR Weather Preset Template ──────────────────────────────────────

WPR_TEMPLATE = """\
<?xml version="1.0" encoding="utf-8"?>
<SimBase.Document Type="WeatherPreset" version="1,4">
  <Descr>AceXML Document</Descr>
  <WeatherPreset.Preset>
    <Name>{name}</Name>
    <Image>Custom</Image>
    <Description>Auto-generated from ODME RunwaySignClassifier ODD</Description>
    <IsAltitudeAMGL>False</IsAltitudeAMGL>
    <MSLPressure>{pressure_pa}</MSLPressure>
    <MSLTemperature>{temperature_c:.1f}</MSLTemperature>
    <AerosolDensity>{aerosol_density:.6f}</AerosolDensity>
    <Pollution>0</Pollution>
    <Precipitations>{precip_mm_h:.2f}</Precipitations>
    <SnowCover>{snow_cover}</SnowCover>
    <ThunderstormIntensity>{thunderstorm}</ThunderstormIntensity>
    <CloudLayer>
      <CloudLayerDensity>{cloud_density:.2f}</CloudLayerDensity>
      <CloudLayerCoverage>{cloud_coverage:.2f}</CloudLayerCoverage>
      <CloudLayerAltitudeBot>{cloud_base_m}</CloudLayerAltitudeBot>
      <CloudLayerAltitudeTop>{cloud_top_m}</CloudLayerAltitudeTop>
      <CloudLayerScattering>{cloud_scattering:.2f}</CloudLayerScattering>
    </CloudLayer>
    <WindLayer>
      <WindLayerAltitude>500</WindLayerAltitude>
      <WindLayerAngle>{wind_dir}</WindLayerAngle>
      <WindLayerSpeed>{wind_speed_kts}</WindLayerSpeed>
    </WindLayer>
  </WeatherPreset.Preset>
</SimBase.Document>"""


# ─── MSFS .FLT Flight File Template ─────────────────────────────────────────

FLT_TEMPLATE = """\
[SimVars.0]
Latitude={latitude}
Longitude={longitude}
Altitude=+{altitude_ft:.6f}
Heading={heading}
OnGround=true

[DateTimeSeason]
Year={year}
Day={day_of_year}
Hours={hours}
Minutes={minutes}
Seconds=0

[Weather]
WeatherPresetFile={wpr_filename}

[Sim.0]
Sim=Cessna Skyhawk G1000 Asobo
"""


# ─── SimConnect Camera Script Template ───────────────────────────────────────

SIMCONNECT_TEMPLATE = '''\
#!/usr/bin/env python3
"""
SimConnect camera placement for test case {tc_id}.

Places the external camera at the specified distance, elevation, and
lateral offset relative to the current aircraft position, simulating
the ground vehicle's camera viewing a runway sign.

Requires: Python SimConnect (pip install SimConnect)
"""
from SimConnect import SimConnect, AircraftRequests, AircraftEvents

# Camera parameters from ODD test case
DISTANCE_M = {distance_m:.2f}
ELEVATION_M = {elevation_m:.2f}
LATERAL_OFFSET_M = {lateral_offset_m:.2f}

# Sign metadata
SIGN_TYPE = "{sign_type}"
SIGN_HEIGHT_CM = {sign_height_cm:.1f}
BACKGROUND_COLOUR = "{background_colour}"
TEXT_COLOUR = "{text_colour}"

# Degradation
SIGN_CONDITION = "{sign_condition}"
SENSOR_CONDITION = "{sensor_condition}"
PAINT_CONTRAST_PCT = {paint_contrast_pct:.1f}
LENS_TRANSMISSION_PCT = {lens_transmission_pct:.1f}


def main():
    sc = SimConnect()
    aq = AircraftRequests(sc)

    # Read current aircraft position
    lat = aq.get("PLANE_LATITUDE")
    lon = aq.get("PLANE_LONGITUDE")
    alt = aq.get("PLANE_ALTITUDE")
    hdg = aq.get("PLANE_HEADING_DEGREES_TRUE")

    print(f"Aircraft at {{lat:.6f}}, {{lon:.6f}}, alt={{alt:.0f}} ft, hdg={{hdg:.1f}}")
    print(f"Camera: {{DISTANCE_M:.1f}}m forward, {{ELEVATION_M:.2f}}m AGL, "
          f"{{LATERAL_OFFSET_M:.2f}}m lateral offset")
    print(f"Sign: {{SIGN_TYPE}} ({{BACKGROUND_COLOUR}}/{{TEXT_COLOUR}}, "
          f"{{SIGN_HEIGHT_CM:.0f}}cm)")
    print(f"Degradation: sign={{SIGN_CONDITION}}, sensor={{SENSOR_CONDITION}}")

    # Calculate camera view angles
    # Vertical angle to sign centre (sign at ~1m above ground)
    sign_centre_m = 1.0  # approximate sign centre height AGL
    vert_angle = math.atan2(sign_centre_m - ELEVATION_M, DISTANCE_M)
    print(f"Vertical look angle: {{math.degrees(vert_angle):.1f}} deg")

    # NOTE: Actual camera placement requires SimConnect camera API
    # or custom WASM module. This script documents the required
    # placement for each test case.


if __name__ == "__main__":
    import math
    main()
'''


# ─── Physics / Mapping Functions ────────────────────────────────────────────

def visibility_to_aerosol(visibility_m: float) -> float:
    """Convert meteorological visibility to MSFS aerosol density.

    Uses Koschmieder's law: V = 3.912 / sigma_ext
    MSFS aerosol density maps ~linearly to extinction coefficient.
    """
    if visibility_m >= 15000:
        return 0.0
    if visibility_m <= 200:
        return 1.0
    return max(0.0, min(1.0,
        1.0 - math.log(visibility_m / 200) / math.log(15000 / 200)))


def precip_to_weather(precip_mm_h: float, is_snow: bool, is_hail: bool):
    """Map precipitation rate to cloud/weather parameters.

    Returns dict with cloud_density, cloud_coverage, cloud_base_m,
    cloud_top_m, cloud_scattering, wind_speed_kts, thunderstorm.
    """
    if precip_mm_h <= 0:
        return dict(cloud_density=0.10, cloud_coverage=0.20,
                    cloud_base_m=3000, cloud_top_m=5000,
                    cloud_scattering=0.3, wind_speed_kts=5,
                    thunderstorm=0)
    if precip_mm_h <= 5:
        return dict(cloud_density=0.50, cloud_coverage=0.70,
                    cloud_base_m=1500, cloud_top_m=4000,
                    cloud_scattering=0.5, wind_speed_kts=10,
                    thunderstorm=0)
    if precip_mm_h <= 15:
        return dict(cloud_density=0.70, cloud_coverage=0.90,
                    cloud_base_m=800, cloud_top_m=3500,
                    cloud_scattering=0.6, wind_speed_kts=18,
                    thunderstorm=1 if is_hail else 0)
    # Severe
    return dict(cloud_density=0.90, cloud_coverage=1.00,
                cloud_base_m=500, cloud_top_m=3000,
                cloud_scattering=0.7, wind_speed_kts=25,
                thunderstorm=1 if is_hail else 0)


def sun_elevation_to_time(sun_elev_deg: float, latitude: float,
                          day_of_year: int = 80) -> tuple:
    """Approximate local solar time for a desired sun elevation.

    Uses the solar declination for the given day and solves for hour angle.
    Returns (hours, minutes) in local solar time.

    For twilight (negative elevation), returns pre-sunrise or post-sunset time.
    """
    # Solar declination (Spencer's formula, simplified)
    decl = 23.45 * math.sin(math.radians(360 / 365 * (day_of_year - 81)))
    decl_rad = math.radians(decl)
    lat_rad = math.radians(latitude)

    # sin(elev) = sin(lat)*sin(decl) + cos(lat)*cos(decl)*cos(hour_angle)
    sin_elev = math.sin(math.radians(sun_elev_deg))
    cos_ha = (sin_elev - math.sin(lat_rad) * math.sin(decl_rad)) / (
        math.cos(lat_rad) * math.cos(decl_rad) + 1e-10)
    cos_ha = max(-1.0, min(1.0, cos_ha))
    hour_angle = math.degrees(math.acos(cos_ha))

    # Convert hour angle to time (morning = before noon, afternoon = after)
    if sun_elev_deg < 15:
        # Use morning side (before solar noon)
        solar_time = 12.0 - hour_angle / 15.0
    else:
        # Use afternoon side
        solar_time = 12.0 + hour_angle / 15.0

    solar_time = max(0.0, min(23.99, solar_time))
    hours = int(solar_time)
    minutes = int((solar_time - hours) * 60)
    return hours, minutes


def estimate_temperature(sun_elev: float, airport: Airport,
                         is_snow: bool) -> float:
    """Rough ambient temperature estimate for MSFS.

    Based on solar elevation and airport latitude/altitude.
    """
    # Base temperature from solar elevation
    base = 5.0 + (sun_elev / 70.0) * 20.0

    # Altitude lapse rate: ~2 deg C per 1000 ft
    alt_correction = -airport.elev_ft / 1000.0 * 2.0

    # Latitude correction: cooler at higher latitudes
    lat_correction = -(abs(airport.lat) - 35) * 0.3

    temp = base + alt_correction + lat_correction
    if is_snow:
        temp = min(temp, 0.0)  # Force sub-zero for snow
    return temp


# ─── Parameter Extraction ───────────────────────────────────────────────────

@dataclass
class ScenarioConfig:
    """All ODD parameters resolved for a single test case."""
    tc_id: int
    # Airport
    airport: Airport
    airport_icao: str
    # Precipitation
    precip_type: str          # NoPrecipitation | Rain | Snow | Hail
    precip_mm_h: float
    # Visibility
    visibility_m: float
    visibility_class: str     # High | Moderate | Low | VeryLow
    # Time of day
    sun_elevation_deg: float
    illuminance_lux: float
    time_period: str          # Morning | Afternoon | Dusk | Dawn
    # Sign type
    sign_type: str
    sign_height_cm: float
    background_colour: str
    text_colour: str
    # Sensor
    distance_m: float
    elevation_m: float
    lateral_offset_m: float
    # Degradation - sign
    sign_condition: str
    paint_contrast_pct: float
    occlusion_pct: float
    specular_reflection_pct: float
    # Degradation - sensor
    sensor_condition: str
    lens_transmission_pct: float


# Mapping from leaf node parent path fragments to parameter roles
AIRPORT_NODES = {"KSFO", "KBOS", "KSAN", "KDEN", "KMIA", "PANC"}
PRECIP_NODES = {"NoPrecipitation", "Rain", "Snow", "Hail"}
VISIBILITY_NODES = {"HighVisibility", "ModerateVisibility",
                    "LowVisibility", "VeryLowVisibility"}
TIME_NODES = {"Morning", "Afternoon", "Dusk", "Dawn"}
SIGN_NODES = {"MandatoryInstruction", "LocationSign",
              "DirectionSign", "RunwayDistanceRemaining"}
DISTANCE_NODES = {"DS10", "DS12", "DS14"}
ELEVATION_NODES = {"EL10", "EL13", "EL16"}
OFFSET_NODES = {"LO00", "LO07", "LO14"}
SIGN_COND_NODES = {"GoodCondition", "FadedSign",
                   "PartiallyOccluded", "WetSign"}
SENSOR_COND_NODES = {"NominalSensor", "WaterDroplets",
                     "DirtySensor", "Overexposed"}

# Default sign properties per sign type
SIGN_DEFAULTS = {
    "MandatoryInstruction":    {"bg": "red",   "fg": "white", "h": 40.0},
    "LocationSign":            {"bg": "black", "fg": "yellow", "h": 30.0},
    "DirectionSign":           {"bg": "black", "fg": "yellow", "h": 30.0},
    "RunwayDistanceRemaining": {"bg": "white", "fg": "black", "h": 75.0},
}

# Default sensor ranges (midpoints)
SENSOR_DEFAULTS = {
    "DS10": 11.0, "DS12": 13.0, "DS14": 15.0,
    "EL10": 1.15, "EL13": 1.45, "EL16": 1.75,
    "LO00": 0.35, "LO07": 1.05, "LO14": 1.70,
}

VISIBILITY_DEFAULTS = {
    "HighVisibility": 10000.0, "ModerateVisibility": 3000.0,
    "LowVisibility": 1000.0, "VeryLowVisibility": 350.0,
}


def get_val(values: Dict[str, float], parent: str, name: str,
            default: float = 0.0) -> float:
    """Look up a parameter value by parent.name, with fallback."""
    key = f"{parent}.{name}"
    return values.get(key, default)


def resolve_node_choice(values: Dict[str, float],
                        node_set: set) -> Optional[str]:
    """Determine which node from a specialization set is active.

    Heuristic: the node whose parameters appear in the test case values.
    """
    counts: Dict[str, int] = {}
    for key in values:
        parent = key.split(".")[0] if "." in key else key
        if parent in node_set:
            counts[parent] = counts.get(parent, 0) + 1
    if counts:
        return max(counts, key=counts.get)
    return None


def extract_config(tc_id: int, values: Dict[str, float]) -> ScenarioConfig:
    """Extract a ScenarioConfig from test case values."""

    # --- Airport ---
    airport_icao = resolve_node_choice(values, AIRPORT_NODES) or "KSFO"
    airport = AIRPORTS.get(airport_icao, AIRPORTS["KSFO"])

    # --- Precipitation ---
    precip_type = resolve_node_choice(values, PRECIP_NODES) or "NoPrecipitation"
    precip_mm_h = get_val(values, precip_type, "precipitation_mm_h", 0.0)

    # --- Visibility ---
    vis_class = resolve_node_choice(values, VISIBILITY_NODES) or "HighVisibility"
    visibility_m = get_val(values, vis_class, "visibility_m",
                           VISIBILITY_DEFAULTS.get(vis_class, 10000.0))

    # --- Time of Day ---
    time_period = resolve_node_choice(values, TIME_NODES) or "Morning"
    sun_elev = get_val(values, time_period, "sun_elevation_deg", 20.0)
    illuminance = get_val(values, time_period, "illuminance_lux", 10000.0)

    # --- Sign Type ---
    sign_type = resolve_node_choice(values, SIGN_NODES) or "MandatoryInstruction"
    sign_def = SIGN_DEFAULTS.get(sign_type, SIGN_DEFAULTS["MandatoryInstruction"])
    sign_height = get_val(values, sign_type, "sign_height_cm", sign_def["h"])
    bg_colour = sign_def["bg"]
    fg_colour = sign_def["fg"]

    # --- Sensor ---
    dist_node = resolve_node_choice(values, DISTANCE_NODES) or "DS10"
    elev_node = resolve_node_choice(values, ELEVATION_NODES) or "EL13"
    offset_node = resolve_node_choice(values, OFFSET_NODES) or "LO00"
    distance_m = get_val(values, dist_node, "distance_m",
                         SENSOR_DEFAULTS.get(dist_node, 11.0))
    elevation_m = get_val(values, elev_node, "elevation_m",
                          SENSOR_DEFAULTS.get(elev_node, 1.45))
    lateral_offset_m = get_val(values, offset_node, "lateral_offset_m",
                               SENSOR_DEFAULTS.get(offset_node, 0.35))

    # --- Sign Condition ---
    sign_cond = resolve_node_choice(values, SIGN_COND_NODES) or "GoodCondition"
    paint_contrast = get_val(values, sign_cond, "paint_contrast_pct", 90.0)
    occlusion = get_val(values, sign_cond, "occlusion_pct", 0.0)
    specular = get_val(values, sign_cond, "specular_reflection_pct", 0.0)

    # --- Sensor Condition ---
    sensor_cond = resolve_node_choice(values, SENSOR_COND_NODES) or "NominalSensor"
    lens_trans = get_val(values, sensor_cond, "lens_transmission_pct", 98.0)

    return ScenarioConfig(
        tc_id=tc_id,
        airport=airport, airport_icao=airport_icao,
        precip_type=precip_type, precip_mm_h=precip_mm_h,
        visibility_m=visibility_m, visibility_class=vis_class,
        sun_elevation_deg=sun_elev, illuminance_lux=illuminance,
        time_period=time_period,
        sign_type=sign_type, sign_height_cm=sign_height,
        background_colour=bg_colour, text_colour=fg_colour,
        distance_m=distance_m, elevation_m=elevation_m,
        lateral_offset_m=lateral_offset_m,
        sign_condition=sign_cond,
        paint_contrast_pct=paint_contrast,
        occlusion_pct=occlusion,
        specular_reflection_pct=specular,
        sensor_condition=sensor_cond,
        lens_transmission_pct=lens_trans,
    )


# ─── File Generators ────────────────────────────────────────────────────────

def generate_wpr(cfg: ScenarioConfig, output_dir: Path) -> Path:
    """Generate an MSFS .WPR weather preset file."""
    is_snow = cfg.precip_type == "Snow"
    is_hail = cfg.precip_type == "Hail"
    weather = precip_to_weather(cfg.precip_mm_h, is_snow, is_hail)
    aerosol = visibility_to_aerosol(cfg.visibility_m)
    temp = estimate_temperature(cfg.sun_elevation_deg, cfg.airport, is_snow)
    snow_cover = 1 if is_snow and temp < 2 else 0

    name = f"ODME_TC_{cfg.tc_id:04d}"
    content = WPR_TEMPLATE.format(
        name=name,
        pressure_pa=101325,
        temperature_c=temp,
        aerosol_density=aerosol,
        precip_mm_h=cfg.precip_mm_h,
        snow_cover=snow_cover,
        thunderstorm=weather["thunderstorm"],
        cloud_density=weather["cloud_density"],
        cloud_coverage=weather["cloud_coverage"],
        cloud_base_m=weather["cloud_base_m"],
        cloud_top_m=weather["cloud_top_m"],
        cloud_scattering=weather["cloud_scattering"],
        wind_dir=270,
        wind_speed_kts=weather["wind_speed_kts"],
    )

    wpr_dir = output_dir / "weather_presets"
    wpr_dir.mkdir(exist_ok=True)
    path = wpr_dir / f"{name}.WPR"
    path.write_text(content, encoding="utf-8")
    return path


def generate_flt(cfg: ScenarioConfig, output_dir: Path) -> Path:
    """Generate an MSFS .FLT flight file (position + time)."""
    hours, minutes = sun_elevation_to_time(
        cfg.sun_elevation_deg, cfg.airport.lat)

    # Use March equinox (day 80) for predictable solar geometry
    day_of_year = 80

    name = f"ODME_TC_{cfg.tc_id:04d}"
    content = FLT_TEMPLATE.format(
        latitude=f"N{abs(cfg.airport.taxi_lat):.6f}" if cfg.airport.taxi_lat >= 0
            else f"S{abs(cfg.airport.taxi_lat):.6f}",
        longitude=f"E{abs(cfg.airport.taxi_lon):.6f}" if cfg.airport.taxi_lon >= 0
            else f"W{abs(cfg.airport.taxi_lon):.6f}",
        altitude_ft=cfg.airport.elev_ft + 5,  # slightly above ground
        heading=cfg.airport.taxi_hdg,
        year=2025,
        day_of_year=day_of_year,
        hours=hours,
        minutes=minutes,
        wpr_filename=f"ODME_TC_{cfg.tc_id:04d}.WPR",
    )

    flt_dir = output_dir / "flights"
    flt_dir.mkdir(exist_ok=True)
    path = flt_dir / f"{name}.FLT"
    path.write_text(content, encoding="utf-8")
    return path


def generate_simconnect_script(cfg: ScenarioConfig, output_dir: Path) -> Path:
    """Generate a SimConnect Python script for camera placement."""
    name = f"ODME_TC_{cfg.tc_id:04d}"
    content = SIMCONNECT_TEMPLATE.format(
        tc_id=name,
        distance_m=cfg.distance_m,
        elevation_m=cfg.elevation_m,
        lateral_offset_m=cfg.lateral_offset_m,
        sign_type=cfg.sign_type,
        sign_height_cm=cfg.sign_height_cm,
        background_colour=cfg.background_colour,
        text_colour=cfg.text_colour,
        sign_condition=cfg.sign_condition,
        sensor_condition=cfg.sensor_condition,
        paint_contrast_pct=cfg.paint_contrast_pct,
        lens_transmission_pct=cfg.lens_transmission_pct,
    )

    sc_dir = output_dir / "simconnect"
    sc_dir.mkdir(exist_ok=True)
    path = sc_dir / f"{name}.py"
    path.write_text(content, encoding="utf-8")
    return path


def generate_manifest(configs: List[ScenarioConfig], output_dir: Path) -> Path:
    """Write a master CSV manifest of all generated scenarios."""
    path = output_dir / "scenario_manifest.csv"
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow([
            "TestCase_ID", "Airport", "Precipitation", "PrecipRate_mm_h",
            "Visibility_m", "VisibilityClass", "SunElevation_deg",
            "Illuminance_lux", "TimePeriod",
            "SignType", "SignHeight_cm", "BackgroundColour", "TextColour",
            "Distance_m", "Elevation_m", "LateralOffset_m",
            "SignCondition", "PaintContrast_pct",
            "SensorCondition", "LensTransmission_pct",
            "WPR_File", "FLT_File", "SimConnect_Script",
        ])
        for cfg in configs:
            tc_name = f"ODME_TC_{cfg.tc_id:04d}"
            writer.writerow([
                tc_name, cfg.airport_icao,
                cfg.precip_type, f"{cfg.precip_mm_h:.1f}",
                f"{cfg.visibility_m:.0f}", cfg.visibility_class,
                f"{cfg.sun_elevation_deg:.1f}", f"{cfg.illuminance_lux:.0f}",
                cfg.time_period,
                cfg.sign_type, f"{cfg.sign_height_cm:.0f}",
                cfg.background_colour, cfg.text_colour,
                f"{cfg.distance_m:.1f}", f"{cfg.elevation_m:.2f}",
                f"{cfg.lateral_offset_m:.2f}",
                cfg.sign_condition, f"{cfg.paint_contrast_pct:.0f}",
                cfg.sensor_condition, f"{cfg.lens_transmission_pct:.0f}",
                f"weather_presets/{tc_name}.WPR",
                f"flights/{tc_name}.FLT",
                f"simconnect/{tc_name}.py",
            ])
    return path


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    project, output_dir = setup()

    print("=" * 60)
    print("MSFS Scenario Generator — RunwaySignClassifier ODD")
    print("=" * 60)
    print(f"Project:     {project.meta.name}")
    print(f"Parameters:  {len(project.parameters)}")
    print(f"Test cases:  {len(project.test_cases)}")

    if project.ses_tree.root:
        leaves = project.ses_tree.root.leaf_nodes()
        print(f"Leaf nodes:  {len(leaves)}")
    print()

    if not project.test_cases:
        print("No test cases found. Use Tools > Generate OD > Generate Test "
              "Cases (LHS) in ODME first.")
        return

    # List available parameters
    print("ODD parameters found:")
    by_parent: Dict[str, List[str]] = {}
    for p in project.parameters:
        by_parent.setdefault(p.parent_node, []).append(
            f"{p.name} [{p.min}..{p.max}]")
    for parent, params in sorted(by_parent.items()):
        print(f"  {parent}: {', '.join(params)}")
    print()

    # Generate scenarios
    configs: List[ScenarioConfig] = []
    verdicts: list = []

    for tc in project.test_cases:
        cfg = extract_config(tc.id, tc.values)
        configs.append(cfg)

        # Generate all output files
        wpr_path = generate_wpr(cfg, output_dir)
        flt_path = generate_flt(cfg, output_dir)
        sc_path = generate_simconnect_script(cfg, output_dir)

        detail = (f"{cfg.airport_icao} | {cfg.precip_type} "
                  f"{cfg.precip_mm_h:.1f}mm/h | vis {cfg.visibility_m:.0f}m | "
                  f"sun {cfg.sun_elevation_deg:.1f}° | {cfg.sign_type} | "
                  f"{cfg.sign_condition} | {cfg.sensor_condition}")

        verdicts.append({
            "testCaseId": f"TC_{tc.id:04d}",
            "scenarioName": f"ODME_TC_{tc.id:04d}",
            "verdict": "GENERATED",
            "detail": detail,
        })

        print(f"  TC_{tc.id:04d}: {detail}")

    # Write manifest
    manifest_path = generate_manifest(configs, output_dir)

    # Write verdicts for ODME
    if verdicts:
        write_verdicts(verdicts, output_dir)

    print()
    print(f"Generated {len(configs)} complete MSFS scenarios:")
    print(f"  Weather presets: {output_dir}/weather_presets/*.WPR")
    print(f"  Flight files:    {output_dir}/flights/*.FLT")
    print(f"  SimConnect:      {output_dir}/simconnect/*.py")
    print(f"  Manifest:        {manifest_path}")
    print(f"  Verdicts:        {output_dir}/results.json")
    print()
    print("To use in MSFS:")
    print("  1. Copy .WPR files to MSFS weather presets folder")
    print("  2. Load .FLT file to position aircraft at the airport")
    print("  3. Run SimConnect script to set camera for sign capture")


if __name__ == "__main__":
    main()
