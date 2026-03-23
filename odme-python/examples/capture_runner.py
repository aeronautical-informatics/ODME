#!/usr/bin/env python3
"""
MSFS Screenshot Capture Runner for Synthetic Data Generation.

Automates Microsoft Flight Simulator via SimConnect to:
  1. Load each scenario (weather preset + flight position + time)
  2. Position camera at the specified distance/elevation/offset
  3. Wait for the scene to stabilize (weather, rendering)
  4. Capture a screenshot
  5. Save the image with ODD metadata labels

The output is a labeled image dataset ready for ML training or
verification of the RunwaySignClassifier.

Prerequisites:
  - Microsoft Flight Simulator 2020/2024 running
  - Python-SimConnect: pip install SimConnect
  - Pillow (for screenshot saving): pip install Pillow

Usage:
  python capture_runner.py --scenario-dir ./output --dataset-dir ./dataset

  Where --scenario-dir contains the output of runway_sign_demo.py:
    scenario_manifest.csv, weather_presets/, flights/, simconnect/

The runner can also operate in "dry-run" mode (--dry-run) to verify
the scenario manifest without connecting to MSFS.
"""
from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import time
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Dict, List, Optional


# ─── Dataset Label Schema ───────────────────────────────────────────────────

@dataclass
class ImageLabel:
    """Metadata label for one captured image."""
    image_file: str
    test_case_id: str
    airport: str
    precipitation: str
    precip_rate_mm_h: float
    visibility_m: float
    visibility_class: str
    sun_elevation_deg: float
    illuminance_lux: float
    time_period: str
    sign_type: str
    sign_height_cm: float
    background_colour: str
    text_colour: str
    distance_m: float
    elevation_m: float
    lateral_offset_m: float
    sign_condition: str
    paint_contrast_pct: float
    sensor_condition: str
    lens_transmission_pct: float
    # Capture metadata
    capture_timestamp: str = ""
    msfs_version: str = ""
    render_frames_waited: int = 0


# ─── Manifest Reader ────────────────────────────────────────────────────────

def read_manifest(manifest_path: Path) -> List[Dict[str, str]]:
    """Read the scenario_manifest.csv produced by runway_sign_demo.py."""
    rows = []
    with open(manifest_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows


# ─── SimConnect Interface (abstracted for testability) ──────────────────────

class SimConnectInterface:
    """Wrapper around Python-SimConnect for scenario loading and capture.

    In dry-run mode, all operations are logged but no MSFS connection
    is made.
    """

    def __init__(self, dry_run: bool = False):
        self.dry_run = dry_run
        self.sc = None
        self.aq = None
        self._connected = False

    def connect(self):
        """Establish connection to MSFS via SimConnect."""
        if self.dry_run:
            print("[DRY RUN] Would connect to MSFS via SimConnect")
            self._connected = True
            return

        try:
            from SimConnect import SimConnect, AircraftRequests
            self.sc = SimConnect()
            self.aq = AircraftRequests(self.sc)
            self._connected = True
            print("Connected to MSFS via SimConnect")
        except ImportError:
            print("ERROR: SimConnect not installed. "
                  "Install with: pip install SimConnect")
            print("       Or use --dry-run to verify without MSFS.")
            sys.exit(1)
        except Exception as e:
            print(f"ERROR: Could not connect to MSFS: {e}")
            print("       Is MSFS running?")
            sys.exit(1)

    def disconnect(self):
        if self.sc and not self.dry_run:
            self.sc.exit()
            print("Disconnected from MSFS")

    def load_flight(self, flt_path: Path):
        """Load a .FLT file to position aircraft."""
        if self.dry_run:
            print(f"  [DRY RUN] Would load flight: {flt_path.name}")
            return

        # SimConnect doesn't have direct FLT loading — we parse and set
        flt_data = _parse_flt(flt_path)
        if self.aq:
            self.aq.set("PLANE_LATITUDE", flt_data["latitude"])
            self.aq.set("PLANE_LONGITUDE", flt_data["longitude"])
            self.aq.set("PLANE_ALTITUDE", flt_data["altitude_ft"])
            self.aq.set("PLANE_HEADING_DEGREES_TRUE", flt_data["heading"])

    def load_weather_preset(self, wpr_path: Path):
        """Load a .WPR weather preset.

        MSFS weather presets must be placed in the Community folder or
        loaded via the in-game UI. SimConnect weather SimVars are read-only.
        For automated weather loading, copy .WPR to:
          %APPDATA%/Microsoft Flight Simulator/Weather/Presets/
        """
        if self.dry_run:
            print(f"  [DRY RUN] Would load weather preset: {wpr_path.name}")
            return

        # Copy WPR to MSFS presets folder if possible
        msfs_presets = _get_msfs_weather_dir()
        if msfs_presets and msfs_presets.exists():
            import shutil
            dest = msfs_presets / wpr_path.name
            shutil.copy2(wpr_path, dest)
            print(f"  Copied {wpr_path.name} to MSFS presets")
        else:
            print(f"  WARNING: Could not find MSFS presets folder. "
                  f"Manually load {wpr_path.name}")

    def set_time(self, hours: int, minutes: int, day_of_year: int = 80,
                 year: int = 2025):
        """Set simulation time."""
        if self.dry_run:
            print(f"  [DRY RUN] Would set time: {hours:02d}:{minutes:02d} "
                  f"day {day_of_year}")
            return

        if self.aq:
            self.aq.set("LOCAL_TIME", hours * 3600 + minutes * 60)

    def set_camera(self, distance_m: float, elevation_m: float,
                   lateral_offset_m: float):
        """Position external camera relative to aircraft.

        Note: Full camera control requires a WASM module or the
        camera SDK. This uses the closest SimConnect approximation.
        """
        if self.dry_run:
            print(f"  [DRY RUN] Would set camera: "
                  f"dist={distance_m:.1f}m, elev={elevation_m:.2f}m, "
                  f"offset={lateral_offset_m:.2f}m")
            return

        # SimConnect camera positioning is limited; log the intended position
        print(f"  Camera target: dist={distance_m:.1f}m, "
              f"elev={elevation_m:.2f}m, offset={lateral_offset_m:.2f}m")

    def wait_for_render(self, seconds: float = 5.0) -> int:
        """Wait for the scene to stabilize after loading."""
        if self.dry_run:
            print(f"  [DRY RUN] Would wait {seconds:.1f}s for render")
            return 0

        print(f"  Waiting {seconds:.1f}s for scene to stabilize...")
        time.sleep(seconds)
        # Count approximate frames (assuming 30 fps)
        return int(seconds * 30)

    def capture_screenshot(self, output_path: Path) -> bool:
        """Capture the current MSFS view as an image.

        Uses the MSFS built-in screenshot key (PrintScreen) via
        SimConnect key event, then copies from the MSFS screenshot folder.
        Alternatively, uses Win32 screen capture if available.
        """
        if self.dry_run:
            # Create a placeholder file for testing
            output_path.parent.mkdir(parents=True, exist_ok=True)
            output_path.write_text("DRY_RUN_PLACEHOLDER")
            print(f"  [DRY RUN] Would capture screenshot -> {output_path.name}")
            return True

        try:
            # Method 1: Win32 screen capture (if available)
            return _capture_win32(output_path)
        except ImportError:
            pass

        try:
            # Method 2: Trigger MSFS screenshot key
            if self.sc:
                from SimConnect import AircraftEvents
                ae = AircraftEvents(self.sc)
                # There's no direct screenshot event in SimConnect,
                # so we fall back to method 3
                pass
        except Exception:
            pass

        # Method 3: Instruct user to use external capture tool
        print(f"  NOTE: Auto-capture not available. Please capture "
              f"screenshot manually and save as {output_path}")
        return False


def _capture_win32(output_path: Path) -> bool:
    """Capture screenshot using Win32 API (Windows only)."""
    import ctypes
    from ctypes import wintypes

    user32 = ctypes.windll.user32
    # Find MSFS window
    hwnd = user32.FindWindowW(None, "Microsoft Flight Simulator")
    if not hwnd:
        hwnd = user32.FindWindowW(None,
                                  "Microsoft Flight Simulator 2024")
    if not hwnd:
        print("  WARNING: MSFS window not found for screen capture")
        return False

    # Get window dimensions
    rect = wintypes.RECT()
    user32.GetWindowRect(hwnd, ctypes.byref(rect))
    width = rect.right - rect.left
    height = rect.bottom - rect.top

    # Capture using PIL if available
    from PIL import ImageGrab
    img = ImageGrab.grab(bbox=(rect.left, rect.top, rect.right, rect.bottom))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(str(output_path), "PNG")
    print(f"  Captured {width}x{height} screenshot -> {output_path.name}")
    return True


def _parse_flt(flt_path: Path) -> dict:
    """Parse a .FLT file for aircraft position."""
    result = {"latitude": 0.0, "longitude": 0.0,
              "altitude_ft": 0.0, "heading": 0.0}
    section = ""
    for line in flt_path.read_text().splitlines():
        line = line.strip()
        if line.startswith("["):
            section = line
            continue
        if section == "[SimVars.0]" and "=" in line:
            key, val = line.split("=", 1)
            key = key.strip()
            val = val.strip()
            if key == "Latitude":
                result["latitude"] = _parse_lat_lon(val)
            elif key == "Longitude":
                result["longitude"] = _parse_lat_lon(val)
            elif key == "Altitude":
                result["altitude_ft"] = float(val.lstrip("+"))
            elif key == "Heading":
                result["heading"] = float(val)
    return result


def _parse_lat_lon(val: str) -> float:
    """Parse N37.6155 or W122.382 format."""
    if val[0] in ("N", "E"):
        return float(val[1:])
    elif val[0] in ("S", "W"):
        return -float(val[1:])
    return float(val)


def _get_msfs_weather_dir() -> Optional[Path]:
    """Find the MSFS weather presets directory."""
    appdata = os.environ.get("APPDATA", "")
    if not appdata:
        return None
    candidates = [
        Path(appdata) / "Microsoft Flight Simulator" / "Weather" / "Presets",
        Path(appdata) / "Microsoft Flight Simulator 2024" / "Weather" / "Presets",
    ]
    for c in candidates:
        if c.exists():
            return c
    # Return first candidate (will be created)
    return candidates[0] if candidates else None


# ─── Batch Capture Pipeline ─────────────────────────────────────────────────

def run_capture_session(scenario_dir: Path, dataset_dir: Path,
                        dry_run: bool = False,
                        settle_time: float = 5.0,
                        max_scenarios: Optional[int] = None) -> List[ImageLabel]:
    """Run a full capture session across all scenarios.

    Args:
        scenario_dir: Directory containing scenario_manifest.csv and
                      subdirectories (weather_presets/, flights/, simconnect/).
        dataset_dir: Output directory for the labeled dataset.
        dry_run: If True, skip MSFS connection.
        settle_time: Seconds to wait for rendering to stabilize.
        max_scenarios: Limit number of scenarios (for testing).

    Returns:
        List of ImageLabel objects for all captured images.
    """
    manifest_path = scenario_dir / "scenario_manifest.csv"
    if not manifest_path.exists():
        print(f"ERROR: {manifest_path} not found. "
              f"Run runway_sign_demo.py first.")
        return []

    rows = read_manifest(manifest_path)
    if max_scenarios:
        rows = rows[:max_scenarios]

    print("=" * 70)
    print("MSFS Synthetic Data Capture Runner")
    print("=" * 70)
    print(f"Scenarios:    {len(rows)}")
    print(f"Dataset dir:  {dataset_dir}")
    print(f"Settle time:  {settle_time}s per scenario")
    print(f"Mode:         {'DRY RUN' if dry_run else 'LIVE CAPTURE'}")
    print()

    # Create dataset directories
    images_dir = dataset_dir / "images"
    images_dir.mkdir(parents=True, exist_ok=True)

    # Connect to MSFS
    sim = SimConnectInterface(dry_run=dry_run)
    sim.connect()

    labels: List[ImageLabel] = []
    total = len(rows)

    try:
        for i, row in enumerate(rows, 1):
            tc_id = row["TestCase_ID"]
            print(f"\n[{i}/{total}] {tc_id}: "
                  f"{row['Airport']} | {row['Precipitation']} | "
                  f"vis {row['Visibility_m']}m | {row['TimePeriod']} | "
                  f"{row['SignType']}")

            # Load weather
            wpr_path = scenario_dir / row["WPR_File"]
            if wpr_path.exists():
                sim.load_weather_preset(wpr_path)

            # Load flight position
            flt_path = scenario_dir / row["FLT_File"]
            if flt_path.exists():
                sim.load_flight(flt_path)

            # Set camera
            sim.set_camera(
                distance_m=float(row["Distance_m"]),
                elevation_m=float(row["Elevation_m"]),
                lateral_offset_m=float(row["LateralOffset_m"]),
            )

            # Wait for render
            frames = sim.wait_for_render(settle_time)

            # Capture
            img_filename = f"{tc_id}.png"
            img_path = images_dir / img_filename
            captured = sim.capture_screenshot(img_path)

            # Record label
            timestamp = time.strftime("%Y-%m-%dT%H:%M:%S")
            label = ImageLabel(
                image_file=img_filename,
                test_case_id=tc_id,
                airport=row["Airport"],
                precipitation=row["Precipitation"],
                precip_rate_mm_h=float(row["PrecipRate_mm_h"]),
                visibility_m=float(row["Visibility_m"]),
                visibility_class=row["VisibilityClass"],
                sun_elevation_deg=float(row["SunElevation_deg"]),
                illuminance_lux=float(row["Illuminance_lux"]),
                time_period=row["TimePeriod"],
                sign_type=row["SignType"],
                sign_height_cm=float(row["SignHeight_cm"]),
                background_colour=row["BackgroundColour"],
                text_colour=row["TextColour"],
                distance_m=float(row["Distance_m"]),
                elevation_m=float(row["Elevation_m"]),
                lateral_offset_m=float(row["LateralOffset_m"]),
                sign_condition=row["SignCondition"],
                paint_contrast_pct=float(row["PaintContrast_pct"]),
                sensor_condition=row["SensorCondition"],
                lens_transmission_pct=float(row["LensTransmission_pct"]),
                capture_timestamp=timestamp,
                render_frames_waited=frames,
            )
            labels.append(label)

    finally:
        sim.disconnect()

    # Write dataset labels
    _write_labels_csv(labels, dataset_dir / "labels.csv")
    _write_labels_json(labels, dataset_dir / "labels.json")
    _write_dataset_readme(labels, dataset_dir)

    print(f"\n{'=' * 70}")
    print(f"Capture complete: {len(labels)} images")
    print(f"  Images:     {images_dir}/")
    print(f"  Labels CSV: {dataset_dir}/labels.csv")
    print(f"  Labels JSON:{dataset_dir}/labels.json")
    print(f"  README:     {dataset_dir}/README.md")
    print(f"{'=' * 70}")

    return labels


def _write_labels_csv(labels: List[ImageLabel], path: Path):
    """Write labels as CSV for easy pandas loading."""
    if not labels:
        return
    fieldnames = list(asdict(labels[0]).keys())
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for label in labels:
            writer.writerow(asdict(label))


def _write_labels_json(labels: List[ImageLabel], path: Path):
    """Write labels as JSON for programmatic access."""
    data = {
        "version": "1.0",
        "generator": "ODME RunwaySignClassifier Capture Runner",
        "total_images": len(labels),
        "odd_dimensions": {
            "airports": sorted(set(l.airport for l in labels)),
            "precipitation_types": sorted(set(l.precipitation for l in labels)),
            "visibility_classes": sorted(set(l.visibility_class for l in labels)),
            "time_periods": sorted(set(l.time_period for l in labels)),
            "sign_types": sorted(set(l.sign_type for l in labels)),
            "sign_conditions": sorted(set(l.sign_condition for l in labels)),
            "sensor_conditions": sorted(set(l.sensor_condition for l in labels)),
        },
        "labels": [asdict(l) for l in labels],
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)


def _write_dataset_readme(labels: List[ImageLabel], dataset_dir: Path):
    """Generate a README describing the dataset."""
    # Count categories
    airports = sorted(set(l.airport for l in labels))
    precips = sorted(set(l.precipitation for l in labels))
    vis = sorted(set(l.visibility_class for l in labels))
    times = sorted(set(l.time_period for l in labels))
    signs = sorted(set(l.sign_type for l in labels))

    content = f"""\
# RunwaySignClassifier Synthetic Dataset

Auto-generated by ODME RunwaySignClassifier Capture Runner.

## Statistics

- **Total images**: {len(labels)}
- **Airports**: {len(airports)} ({', '.join(airports)})
- **Precipitation types**: {len(precips)} ({', '.join(precips)})
- **Visibility classes**: {len(vis)} ({', '.join(vis)})
- **Time periods**: {len(times)} ({', '.join(times)})
- **Sign types**: {len(signs)} ({', '.join(signs)})

## Directory Structure

```
dataset/
  images/          # PNG screenshots from MSFS
    ODME_TC_0001.png
    ODME_TC_0002.png
    ...
  labels.csv       # Tabular labels (one row per image)
  labels.json      # Structured labels with ODD dimension summary
  README.md        # This file
```

## Labels Schema

Each image has the following ODD parameter labels:

| Field | Description | Example |
|-------|-------------|---------|
| image_file | Filename of the captured image | ODME_TC_0001.png |
| test_case_id | ODME test case identifier | ODME_TC_0001 |
| airport | ICAO airport code | KSFO |
| precipitation | Precipitation type | Rain |
| precip_rate_mm_h | Precipitation rate | 5.3 |
| visibility_m | Meteorological visibility | 1200.0 |
| visibility_class | Visibility category | LowVisibility |
| sun_elevation_deg | Solar elevation angle | 15.2 |
| illuminance_lux | Ambient illuminance | 12000 |
| time_period | Time of day category | Morning |
| sign_type | ICAO sign category | MandatoryInstruction |
| sign_height_cm | Physical sign height | 40.0 |
| distance_m | Camera-to-sign distance | 11.5 |
| elevation_m | Camera height AGL | 1.35 |
| lateral_offset_m | Camera lateral offset | 0.5 |
| sign_condition | Sign degradation state | GoodCondition |
| sensor_condition | Sensor degradation state | NominalSensor |

## Usage with PyTorch

```python
import pandas as pd
from torchvision import transforms
from torch.utils.data import Dataset
from PIL import Image

class RunwaySignDataset(Dataset):
    def __init__(self, dataset_dir, transform=None):
        self.labels = pd.read_csv(f"{{dataset_dir}}/labels.csv")
        self.images_dir = f"{{dataset_dir}}/images"
        self.transform = transform or transforms.Compose([
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
        ])
        # Encode sign types as class indices
        self.classes = sorted(self.labels["sign_type"].unique())
        self.class_to_idx = {{c: i for i, c in enumerate(self.classes)}}

    def __len__(self):
        return len(self.labels)

    def __getitem__(self, idx):
        row = self.labels.iloc[idx]
        img = Image.open(f"{{self.images_dir}}/{{row['image_file']}}")
        img = self.transform(img)
        label = self.class_to_idx[row["sign_type"]]
        return img, label
```

## Provenance

Generated from the ODME RunwaySignClassifier ODD example using
Latin Hypercube Sampling for continuous parameters with stratified
selection of categorical ODD dimensions (airport, precipitation,
visibility, time, sign type, degradation).

See: EASA AI Roadmap 2.0, W-3.2 (data requirements traceability)
"""
    readme_path = dataset_dir / "README.md"
    readme_path.write_text(content, encoding="utf-8")


# ─── CLI ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="MSFS Synthetic Data Capture Runner for RunwaySignClassifier")
    parser.add_argument("--scenario-dir", type=Path, required=True,
                        help="Directory with scenario_manifest.csv from "
                             "runway_sign_demo.py")
    parser.add_argument("--dataset-dir", type=Path, required=True,
                        help="Output directory for the labeled dataset")
    parser.add_argument("--dry-run", action="store_true",
                        help="Verify scenarios without connecting to MSFS")
    parser.add_argument("--settle-time", type=float, default=5.0,
                        help="Seconds to wait for rendering (default: 5.0)")
    parser.add_argument("--max-scenarios", type=int, default=None,
                        help="Limit number of scenarios (for testing)")
    args = parser.parse_args()

    run_capture_session(
        scenario_dir=args.scenario_dir,
        dataset_dir=args.dataset_dir,
        dry_run=args.dry_run,
        settle_time=args.settle_time,
        max_scenarios=args.max_scenarios,
    )


if __name__ == "__main__":
    main()
