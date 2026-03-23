"""Tests for capture_runner — batch capture pipeline."""
from __future__ import annotations

import csv
import json
import os
import tempfile
import unittest
from pathlib import Path

# Add examples dir to path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "examples"))

from capture_runner import (
    ImageLabel,
    SimConnectInterface,
    read_manifest,
    run_capture_session,
    _parse_flt,
    _parse_lat_lon,
)


class TestParseLatLon(unittest.TestCase):

    def test_north(self):
        self.assertAlmostEqual(37.6155, _parse_lat_lon("N37.6155"))

    def test_south(self):
        self.assertAlmostEqual(-33.8688, _parse_lat_lon("S33.8688"))

    def test_east(self):
        self.assertAlmostEqual(122.379, _parse_lat_lon("E122.379"))

    def test_west(self):
        self.assertAlmostEqual(-122.379, _parse_lat_lon("W122.379"))


class TestParseFlt(unittest.TestCase):

    def test_parse_basic_flt(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".FLT",
                                          delete=False) as f:
            f.write("[SimVars.0]\n")
            f.write("Latitude=N37.615500\n")
            f.write("Longitude=W122.382000\n")
            f.write("Altitude=+18.000000\n")
            f.write("Heading=300.0\n")
            f.write("\n[DateTimeSeason]\n")
            f.write("Year=2025\n")
            f.name
        try:
            result = _parse_flt(Path(f.name))
            self.assertAlmostEqual(37.6155, result["latitude"])
            self.assertAlmostEqual(-122.382, result["longitude"])
            self.assertAlmostEqual(18.0, result["altitude_ft"])
            self.assertAlmostEqual(300.0, result["heading"])
        finally:
            os.unlink(f.name)


class TestSimConnectDryRun(unittest.TestCase):

    def test_connect_disconnect(self):
        sim = SimConnectInterface(dry_run=True)
        sim.connect()
        self.assertTrue(sim._connected)
        sim.disconnect()

    def test_wait_returns_zero_frames(self):
        sim = SimConnectInterface(dry_run=True)
        sim.connect()
        frames = sim.wait_for_render(1.0)
        self.assertEqual(0, frames)

    def test_capture_creates_placeholder(self):
        sim = SimConnectInterface(dry_run=True)
        sim.connect()
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "images" / "test.png"
            result = sim.capture_screenshot(path)
            self.assertTrue(result)
            self.assertTrue(path.exists())
            self.assertEqual("DRY_RUN_PLACEHOLDER", path.read_text())


class TestReadManifest(unittest.TestCase):

    def test_reads_csv(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".csv",
                                          delete=False, newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["TestCase_ID", "Airport", "Precipitation"])
            writer.writerow(["TC_0001", "KSFO", "Rain"])
            writer.writerow(["TC_0002", "KBOS", "Snow"])
        try:
            rows = read_manifest(Path(f.name))
            self.assertEqual(2, len(rows))
            self.assertEqual("TC_0001", rows[0]["TestCase_ID"])
            self.assertEqual("KBOS", rows[1]["Airport"])
        finally:
            os.unlink(f.name)


class TestRunCaptureSession(unittest.TestCase):
    """End-to-end dry-run test of the capture pipeline."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.scenario_dir = Path(self.tmpdir) / "scenarios"
        self.dataset_dir = Path(self.tmpdir) / "dataset"
        self.scenario_dir.mkdir()

        # Create a minimal manifest
        manifest = self.scenario_dir / "scenario_manifest.csv"
        with open(manifest, "w", newline="", encoding="utf-8") as f:
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
            writer.writerow([
                "ODME_TC_0001", "KSFO", "Rain", "5.0",
                "3000", "ModerateVisibility", "15.0",
                "10000", "Morning",
                "MandatoryInstruction", "40", "red", "white",
                "11.0", "1.35", "0.50",
                "GoodCondition", "90",
                "NominalSensor", "98",
                "weather_presets/ODME_TC_0001.WPR",
                "flights/ODME_TC_0001.FLT",
                "simconnect/ODME_TC_0001.py",
            ])
            writer.writerow([
                "ODME_TC_0002", "PANC", "Snow", "3.0",
                "400", "VeryLowVisibility", "-2.0",
                "200", "Dusk",
                "LocationSign", "30", "black", "yellow",
                "14.0", "1.15", "1.05",
                "FadedSign", "50",
                "WaterDroplets", "75",
                "weather_presets/ODME_TC_0002.WPR",
                "flights/ODME_TC_0002.FLT",
                "simconnect/ODME_TC_0002.py",
            ])

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmpdir)

    def test_dry_run_produces_dataset(self):
        labels = run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True,
            settle_time=0.0,
        )
        self.assertEqual(2, len(labels))

    def test_labels_csv_created(self):
        run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0)
        csv_path = self.dataset_dir / "labels.csv"
        self.assertTrue(csv_path.exists())
        with open(csv_path) as f:
            reader = csv.DictReader(f)
            rows = list(reader)
        self.assertEqual(2, len(rows))
        self.assertEqual("ODME_TC_0001", rows[0]["test_case_id"])

    def test_labels_json_created(self):
        run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0)
        json_path = self.dataset_dir / "labels.json"
        self.assertTrue(json_path.exists())
        with open(json_path) as f:
            data = json.load(f)
        self.assertEqual(2, data["total_images"])
        self.assertIn("KSFO", data["odd_dimensions"]["airports"])
        self.assertIn("PANC", data["odd_dimensions"]["airports"])

    def test_images_dir_created(self):
        run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0)
        images_dir = self.dataset_dir / "images"
        self.assertTrue(images_dir.exists())
        pngs = list(images_dir.glob("*.png"))
        self.assertEqual(2, len(pngs))

    def test_readme_created(self):
        run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0)
        readme = self.dataset_dir / "README.md"
        self.assertTrue(readme.exists())
        content = readme.read_text()
        self.assertIn("KSFO", content)
        self.assertIn("Rain", content)

    def test_max_scenarios_limits_output(self):
        labels = run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0, max_scenarios=1)
        self.assertEqual(1, len(labels))

    def test_missing_manifest_returns_empty(self):
        empty_dir = Path(self.tmpdir) / "empty"
        empty_dir.mkdir()
        labels = run_capture_session(
            scenario_dir=empty_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0)
        self.assertEqual([], labels)

    def test_label_fields_populated(self):
        labels = run_capture_session(
            scenario_dir=self.scenario_dir,
            dataset_dir=self.dataset_dir,
            dry_run=True, settle_time=0.0)
        label = labels[0]
        self.assertEqual("ODME_TC_0001.png", label.image_file)
        self.assertEqual("ODME_TC_0001", label.test_case_id)
        self.assertEqual("KSFO", label.airport)
        self.assertEqual("Rain", label.precipitation)
        self.assertAlmostEqual(5.0, label.precip_rate_mm_h)
        self.assertEqual("MandatoryInstruction", label.sign_type)
        self.assertEqual("GoodCondition", label.sign_condition)
        self.assertAlmostEqual(11.0, label.distance_m)


class TestImageLabel(unittest.TestCase):

    def test_dataclass_fields(self):
        label = ImageLabel(
            image_file="test.png", test_case_id="TC_0001",
            airport="KSFO", precipitation="Rain", precip_rate_mm_h=5.0,
            visibility_m=3000, visibility_class="ModerateVisibility",
            sun_elevation_deg=15.0, illuminance_lux=10000,
            time_period="Morning", sign_type="MandatoryInstruction",
            sign_height_cm=40, background_colour="red", text_colour="white",
            distance_m=11.0, elevation_m=1.35, lateral_offset_m=0.5,
            sign_condition="GoodCondition", paint_contrast_pct=90.0,
            sensor_condition="NominalSensor", lens_transmission_pct=98.0,
        )
        self.assertEqual("KSFO", label.airport)
        self.assertEqual("", label.capture_timestamp)
        self.assertEqual("", label.msfs_version)


if __name__ == "__main__":
    unittest.main()
