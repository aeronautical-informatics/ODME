#!/usr/bin/env python3
"""
Generate MSFS .WPR weather preset files from ODME ODD parameters.

This example demonstrates the ODME Python plugin pipeline:
1. ODME exports the project (SES tree, parameters, test cases) as JSON
2. This script reads the JSON via the odme SDK
3. For each test case, it generates an MSFS weather preset (.WPR file)
4. It writes results.json for ODME to import back

Usage (standalone):
    python msfs_weather.py --odme-project export.json --output-dir ./output

Usage (from ODME):
    Tools > Run Python Plugin... > select this file
"""

import sys
from pathlib import Path

# Add parent directory to path so odme package can be imported
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from odme.cli import setup
from odme.export import write_verdicts


WPR_TEMPLATE = """\
<?xml version="1.0" encoding="utf-8"?>
<SimBase.Document Type="WeatherPreset" version="1,4">
  <Descr>AceXML Document</Descr>
  <WeatherPreset.Preset>
    <Name>{name}</Name>
    <Image>Custom</Image>
    <Description>Auto-generated from ODME ODD — {description}</Description>
    <IsAltitudeAMGL>False</IsAltitudeAMGL>
    <MSLPressure>{pressure_pa}</MSLPressure>
    <MSLTemperature>{temperature_c}</MSLTemperature>
    <AerosolDensity>{aerosol_density:.4f}</AerosolDensity>
    <Pollution>0</Pollution>
    <Precipitations>{precip_mm_h:.1f}</Precipitations>
    <SnowCover>{snow_cover}</SnowCover>
    <ThunderstormIntensity>0</ThunderstormIntensity>
    <CloudLayer>
      <CloudLayerDensity>{cloud_density:.2f}</CloudLayerDensity>
      <CloudLayerCoverage>{cloud_coverage:.2f}</CloudLayerCoverage>
      <CloudLayerAltitudeBot>{cloud_base_m}</CloudLayerAltitudeBot>
      <CloudLayerAltitudeTop>{cloud_top_m}</CloudLayerAltitudeTop>
      <CloudLayerScattering>{cloud_scattering:.2f}</CloudLayerScattering>
    </CloudLayer>
    <WindLayer>
      <WindLayerAltitude>500</WindLayerAltitude>
      <WindLayerAngle>270</WindLayerAngle>
      <WindLayerSpeed>{wind_speed_kts}</WindLayerSpeed>
    </WindLayer>
  </WeatherPreset.Preset>
</SimBase.Document>"""


def visibility_to_aerosol(visibility_m: float) -> float:
    """Convert meteorological visibility to MSFS aerosol density.

    MSFS uses aerosol density inversely related to visibility.
    Approximate mapping based on Koschmieder's law.
    """
    if visibility_m >= 15000:
        return 0.0
    if visibility_m <= 200:
        return 1.0
    # Log-linear mapping: ~0 at 15km, ~1 at 200m
    import math
    return max(0.0, min(1.0, 1.0 - math.log(visibility_m / 200) / math.log(15000 / 200)))


def precip_to_cloud(precip_mm_h: float) -> tuple[float, float, int, int]:
    """Map precipitation rate to cloud parameters.

    Returns (density, coverage, base_m, top_m).
    """
    if precip_mm_h <= 0:
        return (0.1, 0.2, 3000, 5000)  # scattered fair-weather clouds
    if precip_mm_h <= 5:
        return (0.5, 0.7, 1500, 4000)  # moderate overcast
    if precip_mm_h <= 15:
        return (0.7, 0.9, 800, 3500)   # heavy overcast
    return (0.9, 1.0, 500, 3000)        # severe


def main():
    project, output_dir = setup()

    print(f"ODME Project: {project.meta.name}")
    print(f"SES tree: {project.ses_tree.name}")
    if project.ses_tree.root:
        leaves = project.ses_tree.root.leaf_nodes()
        print(f"  Leaf nodes: {len(leaves)}")
    print(f"Parameters: {len(project.parameters)}")
    print(f"Test cases: {len(project.test_cases)}")
    print()

    # Build parameter lookup
    param_lookup = {p.qualified_name: p for p in project.parameters}

    # Find weather-related parameters
    vis_params = [p for p in project.parameters if "visibility" in p.name.lower()]
    precip_params = [p for p in project.parameters if "precipitation" in p.name.lower()]

    if not vis_params and not precip_params:
        print("No weather parameters found in ODD model.")
        print("Parameters available:")
        for p in project.parameters:
            print(f"  {p.qualified_name}: [{p.min}, {p.max}]")
        return

    print(f"Weather parameters found:")
    for p in vis_params + precip_params:
        print(f"  {p.qualified_name}: [{p.min}, {p.max}]")
    print()

    # Generate weather presets for each test case
    wpr_dir = output_dir / "weather_presets"
    wpr_dir.mkdir(exist_ok=True)

    verdicts = []
    generated = 0

    for tc in project.test_cases:
        # Find the best visibility and precipitation values for this test case
        vis_m = 10000.0  # default
        precip_mmh = 0.0  # default

        for p in vis_params:
            val = tc.values.get(p.qualified_name)
            if val is not None:
                vis_m = val
                break

        for p in precip_params:
            val = tc.values.get(p.qualified_name)
            if val is not None:
                precip_mmh = val
                break

        # Get sun elevation for temperature estimate
        sun_el = 30.0  # default
        for key, val in tc.values.items():
            if "sun_elevation" in key:
                sun_el = val
                break

        # Map to MSFS weather
        aerosol = visibility_to_aerosol(vis_m)
        cloud_density, cloud_coverage, cloud_base, cloud_top = precip_to_cloud(precip_mmh)
        temperature = 15 + (sun_el / 70.0) * 10  # rough estimate
        wind_speed = max(0, int(precip_mmh * 1.2))
        snow_cover = 1 if precip_mmh > 0 and temperature < 2 else 0

        preset_name = f"ODME_TC_{tc.id:04d}"
        description = f"vis={vis_m:.0f}m precip={precip_mmh:.1f}mm/h sun_el={sun_el:.1f}deg"

        wpr_content = WPR_TEMPLATE.format(
            name=preset_name,
            description=description,
            pressure_pa=101325,
            temperature_c=f"{temperature:.1f}",
            aerosol_density=aerosol,
            precip_mm_h=precip_mmh,
            snow_cover=snow_cover,
            cloud_density=cloud_density,
            cloud_coverage=cloud_coverage,
            cloud_base_m=cloud_base,
            cloud_top_m=cloud_top,
            cloud_scattering=0.5,
            wind_speed_kts=wind_speed,
        )

        wpr_path = wpr_dir / f"{preset_name}.WPR"
        wpr_path.write_text(wpr_content, encoding="utf-8")
        generated += 1

        verdicts.append({
            "testCaseId": f"TC_{tc.id:04d}",
            "scenarioName": "",
            "verdict": "GENERATED",
            "detail": f"Weather preset: {description}",
        })

    # Write results for ODME to import
    if verdicts:
        write_verdicts(verdicts, output_dir)

    print(f"Generated {generated} MSFS weather presets in {wpr_dir}")
    print(f"Results written to {output_dir / 'results.json'}")


if __name__ == "__main__":
    main()
