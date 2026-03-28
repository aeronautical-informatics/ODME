"""Tests for the MSFS RunwaySignClassifier plugin."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "examples"))

from msfs_runway_sign import (
    AIRPORT_DB,
    SIGN_TYPES,
    ScenarioParams,
    extract_params,
    generate_wpr,
    generate_simconnect_script,
    precip_to_clouds,
    temperature_estimate,
    visibility_to_aerosol,
    elev_to_local_hour,
)


class TestVisibilityToAerosol(unittest.TestCase):
    def test_clear_sky(self):
        self.assertAlmostEqual(visibility_to_aerosol(15000), 0.0)
        self.assertAlmostEqual(visibility_to_aerosol(20000), 0.0)

    def test_dense_fog(self):
        self.assertAlmostEqual(visibility_to_aerosol(200), 1.0)
        self.assertAlmostEqual(visibility_to_aerosol(100), 1.0)

    def test_mid_range(self):
        val = visibility_to_aerosol(5000)
        self.assertGreater(val, 0.0)
        self.assertLess(val, 1.0)

    def test_monotonic(self):
        """Lower visibility → higher aerosol."""
        a1 = visibility_to_aerosol(10000)
        a2 = visibility_to_aerosol(3000)
        a3 = visibility_to_aerosol(500)
        self.assertLess(a1, a2)
        self.assertLess(a2, a3)


class TestPrecipToClouds(unittest.TestCase):
    def test_no_precip(self):
        clouds = precip_to_clouds(0, "NoPrecipitation")
        self.assertLess(clouds["density"], 0.3)
        self.assertLess(clouds["coverage"], 0.5)

    def test_rain_increases_density(self):
        light = precip_to_clouds(2, "Rain")
        heavy = precip_to_clouds(18, "Rain")
        self.assertLess(light["density"], heavy["density"])

    def test_snow_lower_base(self):
        snow = precip_to_clouds(5, "Snow")
        rain = precip_to_clouds(5, "Rain")
        self.assertLessEqual(snow["base_m"], rain["base_m"])

    def test_hail_convective(self):
        hail = precip_to_clouds(15, "Hail")
        self.assertGreaterEqual(hail["top_m"], 6000)
        self.assertGreaterEqual(hail["density"], 0.8)


class TestTemperatureEstimate(unittest.TestCase):
    def test_sea_level_noon(self):
        t = temperature_estimate(60.0, 0.0, "Rain")
        self.assertGreater(t, 20.0)

    def test_high_altitude_cold(self):
        t_low = temperature_estimate(20.0, 5431.0, "Rain")
        t_high = temperature_estimate(20.0, 0.0, "Rain")
        self.assertLess(t_low, t_high)

    def test_snow_forces_cold(self):
        t = temperature_estimate(30.0, 0.0, "Snow")
        self.assertLessEqual(t, -2.0)


class TestElevToLocalHour(unittest.TestCase):
    def test_morning(self):
        hour = elev_to_local_hour(15.0, 37.6, morning=True)
        self.assertGreaterEqual(hour, 6)
        self.assertLessEqual(hour, 11)

    def test_afternoon(self):
        hour = elev_to_local_hour(50.0, 37.6, morning=False)
        self.assertGreaterEqual(hour, 12)
        self.assertLessEqual(hour, 16)


class TestExtractParams(unittest.TestCase):
    def test_airport_detection(self):
        values = {
            "KDEN.latitude_deg": 39.8561,
            "KDEN.longitude_deg": -104.6737,
            "KDEN.elevation_ft": 5431.0,
        }
        sp = extract_params(1, values)
        self.assertEqual(sp.airport_icao, "KDEN")
        self.assertAlmostEqual(sp.elevation_ft, 5431.0)

    def test_precipitation(self):
        values = {"Snow.precipitation_mm_h": 7.5}
        sp = extract_params(2, values)
        self.assertEqual(sp.precip_type, "Snow")
        self.assertAlmostEqual(sp.precip_mm_h, 7.5)

    def test_visibility(self):
        values = {"LowVisibility.visibility_m": 800.0}
        sp = extract_params(3, values)
        self.assertEqual(sp.visibility_category, "LowVisibility")
        self.assertAlmostEqual(sp.visibility_m, 800.0)

    def test_time_of_day(self):
        values = {
            "Dusk.sun_elevation_deg": -3.0,
            "Dusk.illuminance_lux": 200.0,
        }
        sp = extract_params(4, values)
        self.assertEqual(sp.time_category, "Dusk")
        self.assertAlmostEqual(sp.sun_elevation_deg, -3.0)

    def test_sign_type(self):
        values = {"DirectionSign.sign_height_cm": 35.0}
        sp = extract_params(5, values)
        self.assertEqual(sp.sign_type, "DirectionSign")
        self.assertAlmostEqual(sp.sign_height_cm, 35.0)

    def test_sensor_params(self):
        values = {
            "DS14.distance_m": 15.0,
            "EL10.elevation_m": 1.1,
            "LO07.lateral_offset_m": 1.0,
        }
        sp = extract_params(6, values)
        self.assertAlmostEqual(sp.distance_m, 15.0)
        self.assertAlmostEqual(sp.elevation_m, 1.1)
        self.assertAlmostEqual(sp.lateral_offset_m, 1.0)

    def test_sign_degradation(self):
        values = {
            "FadedSign.paint_contrast_pct": 45.0,
            "FadedSign.age_years": 12.0,
        }
        sp = extract_params(7, values)
        self.assertEqual(sp.sign_condition, "FadedSign")
        self.assertAlmostEqual(sp.paint_contrast_pct, 45.0)
        self.assertAlmostEqual(sp.age_years, 12.0)

    def test_sensor_degradation(self):
        values = {
            "WaterDroplets.droplet_coverage_pct": 25.0,
            "WaterDroplets.lens_transmission_pct": 75.0,
        }
        sp = extract_params(8, values)
        self.assertEqual(sp.sensor_condition, "WaterDroplets")
        self.assertAlmostEqual(sp.droplet_coverage_pct, 25.0)
        self.assertAlmostEqual(sp.lens_transmission_pct, 75.0)

    def test_full_scenario(self):
        """A complete test case with all ODD dimensions."""
        values = {
            "PANC.latitude_deg": 61.1744,
            "PANC.longitude_deg": -149.9964,
            "PANC.elevation_ft": 152.0,
            "Snow.precipitation_mm_h": 8.0,
            "VeryLowVisibility.visibility_m": 350.0,
            "Dawn.sun_elevation_deg": -2.0,
            "Dawn.illuminance_lux": 100.0,
            "RunwayDistanceRemaining.sign_height_cm": 75.0,
            "DS14.distance_m": 14.5,
            "EL10.elevation_m": 1.15,
            "LO07.lateral_offset_m": 1.1,
            "FadedSign.paint_contrast_pct": 50.0,
            "FadedSign.age_years": 10.0,
            "DirtySensor.dirt_coverage_pct": 15.0,
            "DirtySensor.lens_transmission_pct": 82.0,
        }
        sp = extract_params(99, values)
        self.assertEqual(sp.airport_icao, "PANC")
        self.assertEqual(sp.precip_type, "Snow")
        self.assertAlmostEqual(sp.visibility_m, 350.0)
        self.assertEqual(sp.time_category, "Dawn")
        self.assertEqual(sp.sign_type, "RunwayDistanceRemaining")
        self.assertAlmostEqual(sp.distance_m, 14.5)
        self.assertEqual(sp.sign_condition, "FadedSign")
        self.assertEqual(sp.sensor_condition, "DirtySensor")

    def test_unknown_qname_ignored(self):
        values = {"Unknown.something": 42.0}
        sp = extract_params(10, values)
        # Should use defaults
        self.assertIsNone(sp.airport_icao)
        self.assertEqual(sp.precip_type, "NoPrecipitation")


class TestGenerateWPR(unittest.TestCase):
    def test_creates_wpr_file(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            sp = ScenarioParams(
                tc_id=1,
                airport_icao="KSFO",
                visibility_m=5000.0,
                precip_type="Rain",
                precip_mm_h=8.0,
                sun_elevation_deg=25.0,
                elevation_ft=13.0,
            )
            path = generate_wpr(sp, Path(tmpdir))
            self.assertTrue(path.exists())
            self.assertEqual(path.suffix, ".WPR")

            content = path.read_text()
            self.assertIn("WeatherPreset", content)
            self.assertIn("ODME_TC_0001", content)
            self.assertIn("KSFO", content)
            self.assertIn("AerosolDensity", content)

    def test_snow_generates_snow_cover(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            sp = ScenarioParams(
                tc_id=2,
                precip_type="Snow",
                precip_mm_h=5.0,
                sun_elevation_deg=0.0,
                elevation_ft=5431.0,
            )
            path = generate_wpr(sp, Path(tmpdir))
            content = path.read_text()
            self.assertIn("<SnowCover>1</SnowCover>", content)

    def test_hail_generates_thunderstorm(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            sp = ScenarioParams(
                tc_id=3,
                precip_type="Hail",
                precip_mm_h=20.0,
                sun_elevation_deg=40.0,
                elevation_ft=0.0,
            )
            path = generate_wpr(sp, Path(tmpdir))
            content = path.read_text()
            self.assertIn("<ThunderstormIntensity>1</ThunderstormIntensity>", content)


class TestGenerateSimConnectScript(unittest.TestCase):
    def test_creates_script(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            tmpdir = Path(tmpdir)
            sp = ScenarioParams(
                tc_id=1,
                airport_icao="KBOS",
                latitude=42.3656,
                longitude=-71.0096,
                elevation_ft=20.0,
                time_category="Morning",
                sun_elevation_deg=15.0,
                illuminance_lux=5000.0,
                sign_type="LocationSign",
                distance_m=12.0,
                elevation_m=1.4,
                lateral_offset_m=0.5,
                sign_condition="GoodCondition",
                sensor_condition="NominalSensor",
            )
            wpr_path = tmpdir / "weather_presets" / "ODME_TC_0001.WPR"
            wpr_path.parent.mkdir()
            wpr_path.touch()

            path = generate_simconnect_script(sp, wpr_path, tmpdir)
            self.assertTrue(path.exists())
            self.assertIn("KBOS", path.name)

            content = path.read_text()
            self.assertIn("SimConnect", content)
            self.assertIn("KBOS", content)
            self.assertIn("LocationSign", content)
            self.assertIn('"distance_m": 12.0', content)

    def test_script_contains_scenario_json(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            tmpdir = Path(tmpdir)
            sp = ScenarioParams(tc_id=5, airport_icao="KDEN")
            wpr_path = tmpdir / "test.WPR"
            wpr_path.touch()

            path = generate_simconnect_script(sp, wpr_path, tmpdir)
            content = path.read_text()

            # Extract the JSON from the script and verify it's valid
            start = content.index("SCENARIO = ") + len("SCENARIO = ")
            # Find the closing brace by parsing
            json_str = content[start:content.index("\n\n\ndef main():")]
            data = json.loads(json_str)
            self.assertEqual(data["test_case_id"], 5)
            self.assertEqual(data["position"]["airport"], "KDEN")


class TestAirportDB(unittest.TestCase):
    def test_all_airports_have_coordinates(self):
        for icao, info in AIRPORT_DB.items():
            self.assertIn("lat", info, f"{icao} missing lat")
            self.assertIn("lon", info, f"{icao} missing lon")
            self.assertIn("elev_ft", info, f"{icao} missing elev_ft")
            self.assertIn("sign_waypoints", info, f"{icao} missing sign_waypoints")
            self.assertGreater(len(info["sign_waypoints"]), 0, f"{icao} has no sign waypoints")

    def test_expected_airports(self):
        expected = {"KSFO", "KBOS", "KSAN", "KDEN", "KMIA", "PANC"}
        self.assertEqual(set(AIRPORT_DB.keys()), expected)


class TestSignTypes(unittest.TestCase):
    def test_all_sign_types_have_metadata(self):
        expected = {"MandatoryInstruction", "LocationSign", "DirectionSign", "RunwayDistanceRemaining"}
        self.assertEqual(set(SIGN_TYPES.keys()), expected)
        for name, info in SIGN_TYPES.items():
            self.assertIn("category", info)
            self.assertIn("bg", info)
            self.assertIn("fg", info)


if __name__ == "__main__":
    unittest.main()
