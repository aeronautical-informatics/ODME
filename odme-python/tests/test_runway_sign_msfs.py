"""Tests for the RunwaySignClassifier MSFS scenario generator."""
from __future__ import annotations

import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

# Ensure odme package is importable
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from examples.runway_sign_msfs import (
    AIRPORTS,
    SIGN_DEFAULTS,
    ScenarioConfig,
    extract_config,
    estimate_temperature,
    generate_flt,
    generate_manifest,
    generate_simconnect_script,
    generate_wpr,
    precip_to_weather,
    resolve_node_choice,
    sun_elevation_to_time,
    visibility_to_aerosol,
)


class TestVisibilityToAerosol(unittest.TestCase):

    def test_clear_sky(self):
        self.assertAlmostEqual(visibility_to_aerosol(15000), 0.0, places=2)
        self.assertAlmostEqual(visibility_to_aerosol(20000), 0.0, places=2)

    def test_dense_fog(self):
        self.assertAlmostEqual(visibility_to_aerosol(200), 1.0, places=2)
        self.assertAlmostEqual(visibility_to_aerosol(100), 1.0, places=2)

    def test_moderate_visibility(self):
        aerosol = visibility_to_aerosol(3000)
        self.assertGreater(aerosol, 0.0)
        self.assertLess(aerosol, 1.0)

    def test_monotonic_decrease(self):
        """Higher visibility = lower aerosol density."""
        v500 = visibility_to_aerosol(500)
        v1000 = visibility_to_aerosol(1000)
        v5000 = visibility_to_aerosol(5000)
        v10000 = visibility_to_aerosol(10000)
        self.assertGreater(v500, v1000)
        self.assertGreater(v1000, v5000)
        self.assertGreater(v5000, v10000)


class TestPrecipToWeather(unittest.TestCase):

    def test_no_precip(self):
        w = precip_to_weather(0.0, False, False)
        self.assertEqual(w["thunderstorm"], 0)
        self.assertLess(w["cloud_coverage"], 0.5)

    def test_heavy_rain(self):
        w = precip_to_weather(12.0, False, False)
        self.assertGreater(w["cloud_coverage"], 0.8)
        self.assertEqual(w["thunderstorm"], 0)

    def test_hail_triggers_thunderstorm(self):
        w = precip_to_weather(20.0, False, True)
        self.assertEqual(w["thunderstorm"], 1)

    def test_severe_precipitation(self):
        w = precip_to_weather(25.0, True, False)
        self.assertGreaterEqual(w["cloud_coverage"], 1.0)
        self.assertGreater(w["wind_speed_kts"], 15)


class TestSunElevationToTime(unittest.TestCase):

    def test_noon_high_sun(self):
        hours, minutes = sun_elevation_to_time(60.0, 25.0)
        # High sun => around midday
        self.assertGreaterEqual(hours, 10)
        self.assertLessEqual(hours, 16)

    def test_dawn_low_sun(self):
        hours, minutes = sun_elevation_to_time(5.0, 37.0)
        # Low sun, morning side => early hours
        self.assertLessEqual(hours, 9)

    def test_negative_elevation_twilight(self):
        hours, minutes = sun_elevation_to_time(-3.0, 42.0)
        # Civil twilight => very early or very late
        self.assertTrue(hours <= 6 or hours >= 18)


class TestEstimateTemperature(unittest.TestCase):

    def test_high_altitude_cooler(self):
        ksfo = AIRPORTS["KSFO"]
        kden = AIRPORTS["KDEN"]
        temp_ksfo = estimate_temperature(30.0, ksfo, False)
        temp_kden = estimate_temperature(30.0, kden, False)
        self.assertGreater(temp_ksfo, temp_kden)

    def test_snow_forces_subzero(self):
        ksfo = AIRPORTS["KSFO"]
        temp = estimate_temperature(50.0, ksfo, True)
        self.assertLessEqual(temp, 0.0)


class TestResolveNodeChoice(unittest.TestCase):

    def test_identifies_airport(self):
        values = {"KDEN.latitude_deg": 39.86, "KDEN.longitude_deg": -104.67}
        result = resolve_node_choice(values, {"KSFO", "KDEN", "KMIA"})
        self.assertEqual(result, "KDEN")

    def test_returns_none_when_no_match(self):
        values = {"SomeOther.param": 1.0}
        result = resolve_node_choice(values, {"KSFO", "KDEN"})
        self.assertIsNone(result)

    def test_picks_most_referenced(self):
        values = {
            "Rain.precipitation_mm_h": 5.0,
            "Snow.precipitation_mm_h": 2.0,
            "Snow.temperature_c": -5.0,
        }
        # Snow has 2 params, Rain has 1
        result = resolve_node_choice(values, {"Rain", "Snow", "Hail"})
        self.assertEqual(result, "Snow")


class TestExtractConfig(unittest.TestCase):

    def test_defaults_when_empty(self):
        cfg = extract_config(1, {})
        self.assertEqual(cfg.airport_icao, "KSFO")
        self.assertEqual(cfg.precip_type, "NoPrecipitation")
        self.assertEqual(cfg.sign_type, "MandatoryInstruction")
        self.assertEqual(cfg.sign_condition, "GoodCondition")
        self.assertEqual(cfg.sensor_condition, "NominalSensor")

    def test_resolves_airport_from_values(self):
        values = {
            "PANC.latitude_deg": 61.17,
            "PANC.longitude_deg": -149.99,
            "PANC.elevation_ft": 152.0,
        }
        cfg = extract_config(42, values)
        self.assertEqual(cfg.airport_icao, "PANC")
        self.assertAlmostEqual(cfg.airport.lat, 61.1744, places=2)

    def test_resolves_precipitation(self):
        values = {"Snow.precipitation_mm_h": 3.5}
        cfg = extract_config(10, values)
        self.assertEqual(cfg.precip_type, "Snow")
        self.assertAlmostEqual(cfg.precip_mm_h, 3.5)

    def test_resolves_visibility(self):
        values = {"LowVisibility.visibility_m": 800.0}
        cfg = extract_config(20, values)
        self.assertEqual(cfg.visibility_class, "LowVisibility")
        self.assertAlmostEqual(cfg.visibility_m, 800.0)


class TestFileGeneration(unittest.TestCase):

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.output_dir = Path(self.tmpdir)
        self.cfg = ScenarioConfig(
            tc_id=1,
            airport=AIRPORTS["KSFO"],
            airport_icao="KSFO",
            precip_type="Rain",
            precip_mm_h=5.0,
            visibility_m=3000.0,
            visibility_class="ModerateVisibility",
            sun_elevation_deg=20.0,
            illuminance_lux=10000.0,
            time_period="Morning",
            sign_type="MandatoryInstruction",
            sign_height_cm=40.0,
            background_colour="red",
            text_colour="white",
            distance_m=11.0,
            elevation_m=1.45,
            lateral_offset_m=0.35,
            sign_condition="GoodCondition",
            paint_contrast_pct=90.0,
            occlusion_pct=0.0,
            specular_reflection_pct=0.0,
            sensor_condition="NominalSensor",
            lens_transmission_pct=98.0,
        )

    def test_generate_wpr(self):
        path = generate_wpr(self.cfg, self.output_dir)
        self.assertTrue(path.exists())
        self.assertTrue(path.name.endswith(".WPR"))
        content = path.read_text()
        self.assertIn("WeatherPreset", content)
        self.assertIn("ODME_TC_0001", content)
        self.assertIn("AerosolDensity", content)
        self.assertIn("Precipitations", content)

    def test_wpr_aerosol_in_range(self):
        path = generate_wpr(self.cfg, self.output_dir)
        content = path.read_text()
        # Extract aerosol value
        import re
        match = re.search(r"<AerosolDensity>([\d.]+)</AerosolDensity>", content)
        self.assertIsNotNone(match)
        aerosol = float(match.group(1))
        self.assertGreaterEqual(aerosol, 0.0)
        self.assertLessEqual(aerosol, 1.0)

    def test_generate_flt(self):
        path = generate_flt(self.cfg, self.output_dir)
        self.assertTrue(path.exists())
        self.assertTrue(path.name.endswith(".FLT"))
        content = path.read_text()
        self.assertIn("Latitude=", content)
        self.assertIn("Longitude=", content)
        self.assertIn("OnGround=true", content)
        self.assertIn("WeatherPresetFile=", content)

    def test_flt_references_matching_wpr(self):
        path = generate_flt(self.cfg, self.output_dir)
        content = path.read_text()
        self.assertIn("ODME_TC_0001.WPR", content)

    def test_generate_simconnect_script(self):
        path = generate_simconnect_script(self.cfg, self.output_dir)
        self.assertTrue(path.exists())
        self.assertTrue(path.name.endswith(".py"))
        content = path.read_text()
        self.assertIn("DISTANCE_M = 11.00", content)
        self.assertIn("ELEVATION_M = 1.45", content)
        self.assertIn("SIGN_TYPE = \"MandatoryInstruction\"", content)
        self.assertIn("SIGN_CONDITION = \"GoodCondition\"", content)
        self.assertIn("SENSOR_CONDITION = \"NominalSensor\"", content)

    def test_generate_manifest(self):
        configs = [self.cfg]
        path = generate_manifest(configs, self.output_dir)
        self.assertTrue(path.exists())
        content = path.read_text()
        lines = [l.strip() for l in content.strip().split("\n")]
        self.assertEqual(len(lines), 2)  # header + 1 row
        self.assertIn("TestCase_ID", lines[0])
        self.assertIn("ODME_TC_0001", lines[1])
        self.assertIn("KSFO", lines[1])
        self.assertIn("MandatoryInstruction", lines[1])

    def test_snow_scenario(self):
        self.cfg.precip_type = "Snow"
        self.cfg.precip_mm_h = 8.0
        self.cfg.airport = AIRPORTS["PANC"]
        self.cfg.airport_icao = "PANC"
        self.cfg.sun_elevation_deg = -3.0
        self.cfg.time_period = "Dusk"

        wpr = generate_wpr(self.cfg, self.output_dir)
        content = wpr.read_text()
        self.assertIn("<SnowCover>1</SnowCover>", content)
        # Temperature should be sub-zero for snow
        import re
        match = re.search(r"<MSLTemperature>([-\d.]+)</MSLTemperature>", content)
        self.assertIsNotNone(match)
        temp = float(match.group(1))
        self.assertLess(temp, 2.0)

    def test_full_pipeline_multiple_cases(self):
        """Generate 3 test cases and verify all output files."""
        cfgs = []
        for i in range(3):
            c = ScenarioConfig(
                tc_id=i + 1,
                airport=AIRPORTS["KDEN"],
                airport_icao="KDEN",
                precip_type="NoPrecipitation",
                precip_mm_h=0.0,
                visibility_m=10000.0,
                visibility_class="HighVisibility",
                sun_elevation_deg=45.0,
                illuminance_lux=50000.0,
                time_period="Afternoon",
                sign_type="LocationSign",
                sign_height_cm=30.0,
                background_colour="black",
                text_colour="yellow",
                distance_m=13.0,
                elevation_m=1.45,
                lateral_offset_m=1.05,
                sign_condition="FadedSign",
                paint_contrast_pct=55.0,
                occlusion_pct=0.0,
                specular_reflection_pct=0.0,
                sensor_condition="DirtySensor",
                lens_transmission_pct=82.0,
            )
            cfgs.append(c)
            generate_wpr(c, self.output_dir)
            generate_flt(c, self.output_dir)
            generate_simconnect_script(c, self.output_dir)

        manifest = generate_manifest(cfgs, self.output_dir)

        # Verify file counts
        wpr_files = list((self.output_dir / "weather_presets").glob("*.WPR"))
        flt_files = list((self.output_dir / "flights").glob("*.FLT"))
        sc_files = list((self.output_dir / "simconnect").glob("*.py"))
        self.assertEqual(len(wpr_files), 3)
        self.assertEqual(len(flt_files), 3)
        self.assertEqual(len(sc_files), 3)
        self.assertTrue(manifest.exists())


if __name__ == "__main__":
    unittest.main()
