#!/usr/bin/env python3
"""
MSFS scenario generator for the RunwaySignClassifier ODD.

Generates a complete test-execution package from ODME's project export:

  1. .WPR weather presets   — visibility, precipitation, clouds, temperature
  2. SimConnect script      — positions aircraft at correct airport, sets time of day
  3. Camera positions JSON  — distance, elevation, lateral offset for each test case
  4. Scenario manifest CSV  — one row per test case with all parameters + generated file paths
  5. results.json           — verdicts fed back to ODME for traceability

MSFS-controllable vs external parameters:

  Directly controllable in MSFS:
    - Airport position (lat/lon/elev)  → SimConnect: AI_WAYPOINT / teleport
    - Precipitation (mm/h)             → .WPR: Precipitations + CloudLayer
    - Visibility (m)                   → .WPR: AerosolDensity (Koschmieder)
    - Time of day / sun elevation      → SimConnect: ZULU_TIME
    - Camera distance / elevation      → SimConnect: drone camera offset
    - Wind (derived from precip)       → .WPR: WindLayer

  External tooling required:
    - Sign type selection              → navigate to correct sign at airport (GPS waypoints)
    - Sign degradation (faded, wet)    → post-processing or custom scenery textures
    - Sensor degradation (droplets)    → image post-processing filters

  The plugin generates everything it can automate and documents the rest
  in the manifest so a human tester or post-processing pipeline can handle it.

Usage (standalone):
    python msfs_runway_sign.py --odme-project export.json --output-dir ./output

Usage (from ODME):
    Tools > Run Python Plugin... > select this file
"""

from __future__ import annotations

import csv
import json
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from odme.cli import setup
from odme.export import write_verdicts


# ---------------------------------------------------------------------------
# Airport database — coordinates and known sign positions
# ---------------------------------------------------------------------------

AIRPORT_DB: Dict[str, Dict] = {
    "KSFO": {
        "icao": "KSFO",
        "name": "San Francisco Intl",
        "lat": 37.6213,
        "lon": -122.3790,
        "elev_ft": 13.0,
        "sign_waypoints": [
            {"label": "RWY 28L Hold", "lat": 37.6155, "lon": -122.3810, "heading": 280},
            {"label": "TWY A/B Junction", "lat": 37.6180, "lon": -122.3830, "heading": 45},
        ],
    },
    "KBOS": {
        "icao": "KBOS",
        "name": "Boston Logan Intl",
        "lat": 42.3656,
        "lon": -71.0096,
        "elev_ft": 20.0,
        "sign_waypoints": [
            {"label": "RWY 22L Hold", "lat": 42.3690, "lon": -71.0120, "heading": 220},
            {"label": "TWY C/D Junction", "lat": 42.3630, "lon": -71.0080, "heading": 130},
        ],
    },
    "KSAN": {
        "icao": "KSAN",
        "name": "San Diego Intl",
        "lat": 32.7336,
        "lon": -117.1900,
        "elev_ft": 17.0,
        "sign_waypoints": [
            {"label": "RWY 27 Hold", "lat": 32.7340, "lon": -117.1950, "heading": 270},
        ],
    },
    "KDEN": {
        "icao": "KDEN",
        "name": "Denver Intl",
        "lat": 39.8561,
        "lon": -104.6737,
        "elev_ft": 5431.0,
        "sign_waypoints": [
            {"label": "RWY 34L Hold", "lat": 39.8490, "lon": -104.6730, "heading": 340},
            {"label": "TWY EE/F Junction", "lat": 39.8530, "lon": -104.6780, "heading": 90},
        ],
    },
    "KMIA": {
        "icao": "KMIA",
        "name": "Miami Intl",
        "lat": 25.7959,
        "lon": -80.2870,
        "elev_ft": 9.0,
        "sign_waypoints": [
            {"label": "RWY 08R Hold", "lat": 25.7930, "lon": -80.2900, "heading": 80},
        ],
    },
    "PANC": {
        "icao": "PANC",
        "name": "Anchorage Intl",
        "lat": 61.1744,
        "lon": -149.9964,
        "elev_ft": 152.0,
        "sign_waypoints": [
            {"label": "RWY 07R Hold", "lat": 61.1720, "lon": -149.9930, "heading": 70},
            {"label": "TWY J/K Junction", "lat": 61.1760, "lon": -150.0010, "heading": 200},
        ],
    },
}

# Sign type metadata — ICAO Annex 14
SIGN_TYPES = {
    "MandatoryInstruction": {
        "category": "Mandatory Instruction",
        "bg": "red",
        "fg": "white",
        "example_text": "4-22",
        "placement": "runway holding position",
    },
    "LocationSign": {
        "category": "Location",
        "bg": "black",
        "fg": "yellow",
        "example_text": "A",
        "placement": "taxiway identification",
    },
    "DirectionSign": {
        "category": "Direction",
        "bg": "black",
        "fg": "yellow",
        "example_text": "← 27R →",
        "placement": "taxiway intersection",
    },
    "RunwayDistanceRemaining": {
        "category": "Runway Distance Remaining",
        "bg": "white",
        "fg": "black",
        "example_text": "3",
        "placement": "runway edge",
    },
}


# ---------------------------------------------------------------------------
# Physics / conversion helpers
# ---------------------------------------------------------------------------

def visibility_to_aerosol(visibility_m: float) -> float:
    """Koschmieder-law mapping: meteorological visibility → MSFS aerosol density."""
    if visibility_m >= 15000:
        return 0.0
    if visibility_m <= 200:
        return 1.0
    return max(0.0, min(1.0, 1.0 - math.log(visibility_m / 200) / math.log(15000 / 200)))


def precip_to_clouds(precip_mm_h: float, precip_type: str) -> dict:
    """Map precipitation rate and type to MSFS cloud layer parameters."""
    if precip_mm_h <= 0:
        return {"density": 0.1, "coverage": 0.2, "base_m": 3000, "top_m": 5000, "scattering": 0.3}

    if precip_type == "Snow":
        # Lower, thicker cloud base for snow
        if precip_mm_h <= 3:
            return {"density": 0.5, "coverage": 0.8, "base_m": 1200, "top_m": 4000, "scattering": 0.6}
        return {"density": 0.8, "coverage": 0.95, "base_m": 600, "top_m": 3500, "scattering": 0.7}

    if precip_type == "Hail":
        # Convective: tall clouds, high density
        return {"density": 0.9, "coverage": 1.0, "base_m": 400, "top_m": 8000, "scattering": 0.8}

    # Rain
    if precip_mm_h <= 5:
        return {"density": 0.5, "coverage": 0.7, "base_m": 1500, "top_m": 4000, "scattering": 0.5}
    if precip_mm_h <= 15:
        return {"density": 0.7, "coverage": 0.9, "base_m": 800, "top_m": 3500, "scattering": 0.6}
    return {"density": 0.9, "coverage": 1.0, "base_m": 500, "top_m": 3000, "scattering": 0.7}


def sun_elevation_to_zulu_offset(sun_elev_deg: float, latitude: float) -> float:
    """Rough estimate: sun elevation → hours from solar noon.

    Uses the simplified solar geometry equation:
        sin(elevation) = sin(lat)*sin(decl) + cos(lat)*cos(decl)*cos(hour_angle)

    We assume equinox (decl=0) for simplicity, so:
        sin(elev) = cos(lat)*cos(hour_angle)
        hour_angle = acos(sin(elev) / cos(lat))
    """
    lat_rad = math.radians(latitude)
    cos_lat = math.cos(lat_rad)
    if cos_lat == 0:
        return 0.0
    sin_elev = math.sin(math.radians(sun_elev_deg))
    ratio = sin_elev / cos_lat
    ratio = max(-1.0, min(1.0, ratio))
    hour_angle_rad = math.acos(ratio)
    hours_from_noon = math.degrees(hour_angle_rad) / 15.0
    return hours_from_noon


def elev_to_local_hour(sun_elev_deg: float, latitude: float, morning: bool = True) -> int:
    """Convert sun elevation to approximate local hour (0-23)."""
    offset = sun_elevation_to_zulu_offset(sun_elev_deg, latitude)
    if morning:
        hour = 12.0 - offset
    else:
        hour = 12.0 + offset
    return max(0, min(23, int(round(hour))))


def temperature_estimate(sun_elev_deg: float, elev_ft: float, precip_type: str) -> float:
    """Rough temperature estimate from sun elevation, altitude, and precipitation."""
    # Base: 15C at sea level, noon
    base = 15.0 + (sun_elev_deg / 70.0) * 12.0
    # Lapse rate: ~2C per 1000ft
    base -= (elev_ft / 1000.0) * 2.0
    # Snow implies cold
    if precip_type == "Snow":
        base = min(base, -2.0)
    return round(base, 1)


# ---------------------------------------------------------------------------
# .WPR weather preset template
# ---------------------------------------------------------------------------

WPR_TEMPLATE = """\
<?xml version="1.0" encoding="utf-8"?>
<SimBase.Document Type="WeatherPreset" version="1,4">
  <Descr>AceXML Document</Descr>
  <WeatherPreset.Preset>
    <Name>{name}</Name>
    <Image>Custom</Image>
    <Description>{description}</Description>
    <IsAltitudeAMGL>False</IsAltitudeAMGL>
    <MSLPressure>{pressure_pa}</MSLPressure>
    <MSLTemperature>{temperature_c}</MSLTemperature>
    <AerosolDensity>{aerosol:.4f}</AerosolDensity>
    <Pollution>0</Pollution>
    <Precipitations>{precip_mm_h:.1f}</Precipitations>
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
      <WindLayerSpeed>{wind_kts}</WindLayerSpeed>
    </WindLayer>
  </WeatherPreset.Preset>
</SimBase.Document>"""

# ---------------------------------------------------------------------------
# SimConnect Python script template
# ---------------------------------------------------------------------------

SIMCONNECT_SCRIPT = """\
#!/usr/bin/env python3
\"\"\"SimConnect automation for {scenario_name}.

Generated by ODME RunwaySignClassifier plugin.
Requires: Python-SimConnect (pip install SimConnect)

This script:
  1. Loads the weather preset (.WPR)
  2. Teleports to the airport sign position
  3. Sets the correct time of day
  4. Configures the drone camera for the test case
  5. Captures a screenshot for ML inference
\"\"\"

import time

SCENARIO = {scenario_json}


def main():
    try:
        from SimConnect import SimConnect, AircraftRequests, AircraftEvents
    except ImportError:
        print("SimConnect not available — printing scenario parameters instead.")
        import json
        print(json.dumps(SCENARIO, indent=2))
        return

    sm = SimConnect()
    aq = AircraftRequests(sm)
    ae = AircraftEvents(sm)

    # 1. Teleport to airport sign position
    pos = SCENARIO["position"]
    print(f"Teleporting to {{pos['airport']}} — {{pos['sign_label']}}")
    aq.set("PLANE_LATITUDE", pos["latitude"])
    aq.set("PLANE_LONGITUDE", pos["longitude"])
    aq.set("PLANE_ALTITUDE", pos["altitude_ft"])
    aq.set("PLANE_HEADING_DEGREES_TRUE", pos["heading"])
    time.sleep(1)

    # 2. Set time of day
    tod = SCENARIO["time_of_day"]
    print(f"Setting local time to {{tod['local_hour']:02d}}:00")
    ae.find("ZULU_HOURS_SET").value = tod["zulu_hour"]
    time.sleep(0.5)

    # 3. Configure drone camera
    cam = SCENARIO["camera"]
    print(f"Camera: distance={{cam['distance_m']:.1f}}m, "
          f"elevation={{cam['elevation_m']:.1f}}m, "
          f"offset={{cam['lateral_offset_m']:.1f}}m")
    # Drone camera positioning via SimConnect is limited;
    # in practice, use the external camera XML or Drone mode offsets
    # These values are recorded for manual drone positioning
    time.sleep(0.5)

    # 4. Wait for weather to settle, then capture
    print("Waiting for weather to stabilise...")
    time.sleep(3)

    print(f"Scenario ready: {{SCENARIO['name']}}")
    print(f"  Weather preset: {{SCENARIO['wpr_file']}}")
    print(f"  Sign type: {{SCENARIO['sign_type']['category']}}")
    print(f"  Degradation: sign={{SCENARIO['degradation']['sign']}}, "
          f"sensor={{SCENARIO['degradation']['sensor']}}")
    print()
    print("Capture screenshot now (or run automated capture pipeline).")

    sm.exit()
    print("Done.")


if __name__ == "__main__":
    main()
"""


# ---------------------------------------------------------------------------
# Parameter extraction from test case values
# ---------------------------------------------------------------------------

@dataclass
class ScenarioParams:
    """Extracted ODD parameter values for one test case."""

    tc_id: int
    # Airport
    airport_icao: Optional[str] = None
    latitude: float = 37.6213
    longitude: float = -122.379
    elevation_ft: float = 13.0
    # Precipitation
    precip_type: str = "NoPrecipitation"
    precip_mm_h: float = 0.0
    # Visibility
    visibility_m: float = 10000.0
    visibility_category: str = "HighVisibility"
    # Time of day
    time_category: str = "Morning"
    sun_elevation_deg: float = 20.0
    illuminance_lux: float = 10000.0
    # Sign type
    sign_type: str = "MandatoryInstruction"
    sign_height_cm: float = 40.0
    # Sensor
    distance_m: float = 12.0
    elevation_m: float = 1.3
    lateral_offset_m: float = 0.7
    # Degradation — sign
    sign_condition: str = "GoodCondition"
    paint_contrast_pct: float = 90.0
    occlusion_pct: float = 0.0
    specular_reflection_pct: float = 0.0
    age_years: float = 0.0
    # Degradation — sensor
    sensor_condition: str = "NominalSensor"
    lens_transmission_pct: float = 98.0
    droplet_coverage_pct: float = 0.0
    dirt_coverage_pct: float = 0.0
    glare_intensity_lux: float = 0.0
    affected_image_pct: float = 0.0


def extract_params(tc_id: int, values: Dict[str, float]) -> ScenarioParams:
    """Map qualified parameter names to structured scenario parameters."""
    p = ScenarioParams(tc_id=tc_id)

    for qname, val in values.items():
        parts = qname.split(".")
        if len(parts) != 2:
            continue
        parent, var = parts

        # Airport
        if parent in AIRPORT_DB:
            p.airport_icao = parent
            info = AIRPORT_DB[parent]
            p.latitude = info["lat"]
            p.longitude = info["lon"]
            p.elevation_ft = info["elev_ft"]
            if var == "latitude_deg":
                p.latitude = val
            elif var == "longitude_deg":
                p.longitude = val
            elif var == "elevation_ft":
                p.elevation_ft = val

        # Precipitation
        elif parent in ("NoPrecipitation", "Rain", "Snow", "Hail"):
            p.precip_type = parent
            if var == "precipitation_mm_h":
                p.precip_mm_h = val

        # Visibility
        elif parent in ("HighVisibility", "ModerateVisibility", "LowVisibility", "VeryLowVisibility"):
            p.visibility_category = parent
            if var == "visibility_m":
                p.visibility_m = val

        # Time of day
        elif parent in ("Morning", "Afternoon", "Dusk", "Dawn"):
            p.time_category = parent
            if var == "sun_elevation_deg":
                p.sun_elevation_deg = val
            elif var == "illuminance_lux":
                p.illuminance_lux = val

        # Sign type
        elif parent in SIGN_TYPES:
            p.sign_type = parent
            if var == "sign_height_cm":
                p.sign_height_cm = val

        # Sensor distance
        elif parent in ("DS10", "DS12", "DS14"):
            if var == "distance_m":
                p.distance_m = val

        # Sensor elevation
        elif parent in ("EL10", "EL13", "EL16"):
            if var == "elevation_m":
                p.elevation_m = val

        # Sensor lateral offset
        elif parent in ("LO00", "LO07", "LO14"):
            if var == "lateral_offset_m":
                p.lateral_offset_m = val

        # Sign condition
        elif parent in ("GoodCondition", "FadedSign", "PartiallyOccluded", "WetSign"):
            p.sign_condition = parent
            if var == "paint_contrast_pct":
                p.paint_contrast_pct = val
            elif var == "occlusion_pct":
                p.occlusion_pct = val
            elif var == "specular_reflection_pct":
                p.specular_reflection_pct = val
            elif var == "age_years":
                p.age_years = val

        # Sensor condition
        elif parent in ("NominalSensor", "WaterDroplets", "DirtySensor", "Overexposed"):
            p.sensor_condition = parent
            if var == "lens_transmission_pct":
                p.lens_transmission_pct = val
            elif var == "droplet_coverage_pct":
                p.droplet_coverage_pct = val
            elif var == "dirt_coverage_pct":
                p.dirt_coverage_pct = val
            elif var == "glare_intensity_lux":
                p.glare_intensity_lux = val
            elif var == "affected_image_pct":
                p.affected_image_pct = val

    return p


# ---------------------------------------------------------------------------
# File generators
# ---------------------------------------------------------------------------

def generate_wpr(sp: ScenarioParams, output_dir: Path) -> Path:
    """Generate a .WPR weather preset file."""
    aerosol = visibility_to_aerosol(sp.visibility_m)
    clouds = precip_to_clouds(sp.precip_mm_h, sp.precip_type)
    temp = temperature_estimate(sp.sun_elevation_deg, sp.elevation_ft, sp.precip_type)
    wind_kts = max(0, int(sp.precip_mm_h * 1.2))
    snow_cover = 1 if sp.precip_type == "Snow" and temp < 2 else 0
    thunderstorm = 1 if sp.precip_type == "Hail" else 0

    name = f"ODME_TC_{sp.tc_id:04d}"
    desc = (
        f"ODME RunwaySignClassifier | "
        f"{sp.airport_icao or 'KSFO'} | "
        f"vis={sp.visibility_m:.0f}m | "
        f"precip={sp.precip_type} {sp.precip_mm_h:.1f}mm/h | "
        f"sun_el={sp.sun_elevation_deg:.1f}deg"
    )

    content = WPR_TEMPLATE.format(
        name=name,
        description=desc,
        pressure_pa=101325,
        temperature_c=temp,
        aerosol=aerosol,
        precip_mm_h=sp.precip_mm_h,
        snow_cover=snow_cover,
        thunderstorm=thunderstorm,
        cloud_density=clouds["density"],
        cloud_coverage=clouds["coverage"],
        cloud_base_m=clouds["base_m"],
        cloud_top_m=clouds["top_m"],
        cloud_scattering=clouds["scattering"],
        wind_dir=270,
        wind_kts=wind_kts,
    )

    wpr_dir = output_dir / "weather_presets"
    wpr_dir.mkdir(exist_ok=True)
    path = wpr_dir / f"{name}.WPR"
    path.write_text(content, encoding="utf-8")
    return path


def generate_simconnect_script(sp: ScenarioParams, wpr_path: Path, output_dir: Path) -> Path:
    """Generate a SimConnect automation script for one test case."""
    icao = sp.airport_icao or "KSFO"
    airport = AIRPORT_DB.get(icao, AIRPORT_DB["KSFO"])

    # Pick the first sign waypoint at this airport
    waypoints = airport.get("sign_waypoints", [])
    wp = waypoints[sp.tc_id % len(waypoints)] if waypoints else {
        "label": "Unknown", "lat": airport["lat"], "lon": airport["lon"], "heading": 0,
    }

    is_morning = sp.time_category in ("Morning", "Dawn")
    local_hour = elev_to_local_hour(sp.sun_elevation_deg, sp.latitude, morning=is_morning)
    # Rough UTC offset based on longitude
    utc_offset = round(sp.longitude / 15.0)
    zulu_hour = (local_hour - int(utc_offset)) % 24

    sign_info = SIGN_TYPES.get(sp.sign_type, SIGN_TYPES["MandatoryInstruction"])

    scenario_data = {
        "name": f"TC_{sp.tc_id:04d}_{icao}_{sp.time_category}",
        "test_case_id": sp.tc_id,
        "wpr_file": str(wpr_path.name),
        "position": {
            "airport": icao,
            "sign_label": wp["label"],
            "latitude": wp["lat"],
            "longitude": wp["lon"],
            "altitude_ft": airport["elev_ft"] + 3.0,  # ground level + vehicle height
            "heading": wp["heading"],
        },
        "time_of_day": {
            "category": sp.time_category,
            "sun_elevation_deg": sp.sun_elevation_deg,
            "illuminance_lux": sp.illuminance_lux,
            "local_hour": local_hour,
            "zulu_hour": zulu_hour,
        },
        "camera": {
            "distance_m": sp.distance_m,
            "elevation_m": sp.elevation_m,
            "lateral_offset_m": sp.lateral_offset_m,
        },
        "weather": {
            "visibility_m": sp.visibility_m,
            "visibility_category": sp.visibility_category,
            "precipitation_type": sp.precip_type,
            "precipitation_mm_h": sp.precip_mm_h,
        },
        "sign_type": {
            "category": sign_info["category"],
            "node": sp.sign_type,
            "height_cm": sp.sign_height_cm,
            "background": sign_info["bg"],
            "foreground": sign_info["fg"],
        },
        "degradation": {
            "sign": sp.sign_condition,
            "sign_details": {
                "paint_contrast_pct": sp.paint_contrast_pct,
                "occlusion_pct": sp.occlusion_pct,
                "specular_reflection_pct": sp.specular_reflection_pct,
            },
            "sensor": sp.sensor_condition,
            "sensor_details": {
                "lens_transmission_pct": sp.lens_transmission_pct,
                "droplet_coverage_pct": sp.droplet_coverage_pct,
                "dirt_coverage_pct": sp.dirt_coverage_pct,
                "glare_intensity_lux": sp.glare_intensity_lux,
                "affected_image_pct": sp.affected_image_pct,
            },
        },
    }

    script_name = f"TC_{sp.tc_id:04d}_{icao}.py"
    content = SIMCONNECT_SCRIPT.format(
        scenario_name=scenario_data["name"],
        scenario_json=json.dumps(scenario_data, indent=4),
    )

    scripts_dir = output_dir / "simconnect_scripts"
    scripts_dir.mkdir(exist_ok=True)
    path = scripts_dir / script_name
    path.write_text(content, encoding="utf-8")
    return path


def generate_batch_runner(scenario_scripts: List[Path], output_dir: Path) -> Path:
    """Generate a master batch runner that executes all SimConnect scripts."""
    lines = [
        "#!/usr/bin/env python3",
        '"""Run all generated SimConnect scenario scripts in sequence.',
        "",
        "Generated by ODME RunwaySignClassifier plugin.",
        "Requires: Python-SimConnect (pip install SimConnect)",
        '"""',
        "",
        "import subprocess",
        "import sys",
        "import time",
        "from pathlib import Path",
        "",
        "",
        f"SCRIPTS = {json.dumps([s.name for s in scenario_scripts], indent=4)}",
        "",
        "",
        "def main():",
        "    scripts_dir = Path(__file__).parent / 'simconnect_scripts'",
        "    passed = 0",
        "    failed = 0",
        "",
        "    for script in SCRIPTS:",
        "        path = scripts_dir / script",
        "        print(f'\\n{'='*60}')",
        "        print(f'Running: {script}')",
        "        print(f'{'='*60}')",
        "",
        "        result = subprocess.run(",
        "            [sys.executable, str(path)],",
        "            capture_output=False,",
        "            timeout=120,",
        "        )",
        "",
        "        if result.returncode == 0:",
        "            passed += 1",
        "        else:",
        "            failed += 1",
        "            print(f'FAILED: {script} (exit code {result.returncode})')",
        "",
        "        # Pause between scenarios for MSFS to stabilise",
        "        time.sleep(5)",
        "",
        "    print(f'\\n{'='*60}')",
        "    print(f'Results: {passed} passed, {failed} failed, {len(SCRIPTS)} total')",
        "    print(f'{'='*60}')",
        "",
        "",
        'if __name__ == "__main__":',
        "    main()",
    ]

    path = output_dir / "run_all.py"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    project, output_dir = setup()

    print(f"ODME Project: {project.meta.name}")
    print(f"SES tree:     {project.ses_tree.name}")
    if project.ses_tree.root:
        leaves = project.ses_tree.root.leaf_nodes()
        print(f"  Leaf nodes: {len(leaves)}")
    print(f"Parameters:   {len(project.parameters)}")
    print(f"Test cases:   {len(project.test_cases)}")
    print(f"Scenarios:    {len(project.scenarios)}")
    if project.coverage:
        print(f"ODD coverage: {project.coverage.coverage_percent:.1f}%")
    print()

    if not project.test_cases:
        print("No test cases in project export. Generate test cases in ODME first")
        print("(Scenario Manager > Generate Samples).")
        return

    # Extract structured parameters for each test case
    all_params: List[ScenarioParams] = []
    for tc in project.test_cases:
        sp = extract_params(tc.id, tc.values)
        all_params.append(sp)

    # Generate files
    verdicts = []
    manifest_rows = []
    scenario_scripts = []

    for sp in all_params:
        print(f"  TC_{sp.tc_id:04d}: {sp.airport_icao or 'KSFO'} / "
              f"{sp.precip_type} / vis={sp.visibility_m:.0f}m / "
              f"{sp.time_category} / {sp.sign_type} / "
              f"sign={sp.sign_condition} sensor={sp.sensor_condition}")

        # 1. Weather preset
        wpr_path = generate_wpr(sp, output_dir)

        # 2. SimConnect script
        sc_path = generate_simconnect_script(sp, wpr_path, output_dir)
        scenario_scripts.append(sc_path)

        # 3. Manifest row
        manifest_rows.append({
            "TestCase_ID": sp.tc_id,
            "Airport": sp.airport_icao or "KSFO",
            "Precipitation": sp.precip_type,
            "Precipitation_mm_h": sp.precip_mm_h,
            "Visibility_m": sp.visibility_m,
            "Visibility_Category": sp.visibility_category,
            "TimeOfDay": sp.time_category,
            "SunElevation_deg": sp.sun_elevation_deg,
            "Illuminance_lux": sp.illuminance_lux,
            "SignType": sp.sign_type,
            "SignHeight_cm": sp.sign_height_cm,
            "Distance_m": sp.distance_m,
            "CameraElevation_m": sp.elevation_m,
            "LateralOffset_m": sp.lateral_offset_m,
            "SignCondition": sp.sign_condition,
            "PaintContrast_pct": sp.paint_contrast_pct,
            "Occlusion_pct": sp.occlusion_pct,
            "SensorCondition": sp.sensor_condition,
            "LensTransmission_pct": sp.lens_transmission_pct,
            "WPR_File": wpr_path.name,
            "SimConnect_Script": sc_path.name,
            "MSFS_Controllable": "YES" if sp.sign_condition == "GoodCondition" and sp.sensor_condition == "NominalSensor" else "PARTIAL",
        })

        # 4. Verdict
        detail_parts = [
            f"airport={sp.airport_icao or 'KSFO'}",
            f"vis={sp.visibility_m:.0f}m",
            f"precip={sp.precip_type}({sp.precip_mm_h:.1f}mm/h)",
            f"time={sp.time_category}(sun={sp.sun_elevation_deg:.1f}deg)",
            f"sign={sp.sign_type}",
            f"cam=({sp.distance_m:.1f}m,{sp.elevation_m:.1f}m,{sp.lateral_offset_m:.1f}m)",
            f"deg_sign={sp.sign_condition}",
            f"deg_sensor={sp.sensor_condition}",
        ]
        verdicts.append({
            "testCaseId": f"TC_{sp.tc_id:04d}",
            "scenarioName": f"{sp.airport_icao or 'KSFO'}_{sp.time_category}_{sp.precip_type}",
            "verdict": "GENERATED",
            "detail": " | ".join(detail_parts),
        })

    # 5. Write scenario manifest CSV
    manifest_path = output_dir / "scenario_manifest.csv"
    if manifest_rows:
        with open(manifest_path, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=manifest_rows[0].keys())
            writer.writeheader()
            writer.writerows(manifest_rows)

    # 6. Write camera positions JSON
    camera_data = []
    for sp in all_params:
        icao = sp.airport_icao or "KSFO"
        airport = AIRPORT_DB.get(icao, AIRPORT_DB["KSFO"])
        waypoints = airport.get("sign_waypoints", [])
        wp = waypoints[sp.tc_id % len(waypoints)] if waypoints else None
        camera_data.append({
            "test_case_id": sp.tc_id,
            "airport": icao,
            "sign_waypoint": wp,
            "camera": {
                "distance_m": sp.distance_m,
                "elevation_m": sp.elevation_m,
                "lateral_offset_m": sp.lateral_offset_m,
            },
        })
    cam_path = output_dir / "camera_positions.json"
    with open(cam_path, "w", encoding="utf-8") as f:
        json.dump(camera_data, f, indent=2)

    # 7. Write batch runner
    runner_path = generate_batch_runner(scenario_scripts, output_dir)

    # 8. Write results for ODME
    if verdicts:
        write_verdicts(verdicts, output_dir)

    # Summary
    print()
    print(f"Generated {len(all_params)} test scenarios:")
    print(f"  Weather presets:     {output_dir}/weather_presets/*.WPR")
    print(f"  SimConnect scripts:  {output_dir}/simconnect_scripts/*.py")
    print(f"  Batch runner:        {runner_path}")
    print(f"  Scenario manifest:   {manifest_path}")
    print(f"  Camera positions:    {cam_path}")
    print(f"  Results (verdicts):  {output_dir}/results.json")

    # Automation coverage report
    fully_auto = sum(1 for r in manifest_rows if r["MSFS_Controllable"] == "YES")
    partial = len(manifest_rows) - fully_auto
    print()
    print(f"MSFS automation coverage:")
    print(f"  Fully automatable:  {fully_auto}/{len(manifest_rows)} "
          f"(nominal sign + sensor conditions)")
    print(f"  Partial (manual):   {partial}/{len(manifest_rows)} "
          f"(degradation requires post-processing or custom scenery)")


if __name__ == "__main__":
    main()
