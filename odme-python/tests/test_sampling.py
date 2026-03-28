"""Tests for odme.sampling — LHS and ODD sampling."""
from __future__ import annotations

import unittest
from typing import Dict, List

from odme.model import Parameter, SESNode, SESTree, TestCase
from odme.sampling import (
    FullODDSampler,
    LatinHypercubeSampler,
    Specialization,
    extract_specializations,
)


def _param(name: str, parent: str, lo: float, hi: float) -> Parameter:
    return Parameter(name=name, parent_node=parent,
                     qualified_name=f"{parent}.{name}",
                     data_type="double", min=lo, max=hi)


def _node(name: str, ntype: str, children=None) -> SESNode:
    return SESNode(id=name, name=name, type=ntype, label=name,
                   path=name, children=children or [])


def _build_small_tree() -> SESTree:
    """Small tree: Root -> [WeatherDec(Rain,Snow), TimeDec(Day,Night)]."""
    rain = _node("Rain", "ENTITY")
    snow = _node("Snow", "ENTITY")
    weather_dec = _node("WeatherDec", "SPECIALIZATION", [rain, snow])

    day = _node("Day", "ENTITY")
    night = _node("Night", "ENTITY")
    time_dec = _node("TimeDec", "SPECIALIZATION", [day, night])

    root = _node("Root", "ENTITY", [weather_dec, time_dec])
    return SESTree(id="t1", name="Test", schema_version="1.0", root=root)


class TestLatinHypercubeSampler(unittest.TestCase):

    def test_correct_count(self):
        params = [_param("x", "A", 0, 10), _param("y", "A", -1, 1)]
        sampler = LatinHypercubeSampler(params)
        tcs = sampler.sample(20, seed=1)
        self.assertEqual(20, len(tcs))

    def test_ids_sequential(self):
        params = [_param("x", "A", 0, 1)]
        tcs = LatinHypercubeSampler(params).sample(5, seed=1)
        self.assertEqual([1, 2, 3, 4, 5], [tc.id for tc in tcs])

    def test_values_within_range(self):
        params = [_param("x", "A", 10, 20), _param("y", "B", -5, 5)]
        tcs = LatinHypercubeSampler(params).sample(100, seed=42)
        for tc in tcs:
            self.assertGreaterEqual(tc.values["A.x"], 10.0)
            self.assertLessEqual(tc.values["A.x"], 20.0)
            self.assertGreaterEqual(tc.values["B.y"], -5.0)
            self.assertLessEqual(tc.values["B.y"], 5.0)

    def test_strata_coverage(self):
        """Each stratum should contain exactly one sample per dimension."""
        params = [_param("x", "A", 0, 10)]
        n = 10
        tcs = LatinHypercubeSampler(params).sample(n, seed=7)
        # Check that each stratum [0..1), [1..2), ..., [9..10) has one sample
        strata = [int(tc.values["A.x"]) for tc in tcs]
        self.assertEqual(sorted(strata), list(range(n)))

    def test_reproducible_with_seed(self):
        params = [_param("x", "A", 0, 100)]
        a = LatinHypercubeSampler(params).sample(10, seed=99)
        b = LatinHypercubeSampler(params).sample(10, seed=99)
        for ta, tb in zip(a, b):
            self.assertEqual(ta.values, tb.values)

    def test_different_seeds_differ(self):
        params = [_param("x", "A", 0, 100)]
        a = LatinHypercubeSampler(params).sample(10, seed=1)
        b = LatinHypercubeSampler(params).sample(10, seed=2)
        vals_a = [tc.values["A.x"] for tc in a]
        vals_b = [tc.values["A.x"] for tc in b]
        self.assertNotEqual(vals_a, vals_b)

    def test_no_params_returns_empty(self):
        tcs = LatinHypercubeSampler([]).sample(10, seed=1)
        self.assertEqual([], tcs)

    def test_integer_rounding(self):
        params = [Parameter(name="n", parent_node="A",
                            qualified_name="A.n",
                            data_type="int", min=0, max=10)]
        tcs = LatinHypercubeSampler(params).sample(5, seed=1)
        for tc in tcs:
            val = tc.values["A.n"]
            self.assertEqual(val, round(val))


class TestExtractSpecializations(unittest.TestCase):

    def test_finds_specializations(self):
        tree = _build_small_tree()
        specs = extract_specializations(tree)
        names = {s.node_name for s in specs}
        self.assertEqual({"WeatherDec", "TimeDec"}, names)

    def test_choices(self):
        tree = _build_small_tree()
        specs = extract_specializations(tree)
        weather = next(s for s in specs if s.node_name == "WeatherDec")
        self.assertEqual(["Rain", "Snow"], weather.choices)

    def test_empty_tree(self):
        tree = SESTree(id="t", name="Empty", schema_version="1.0", root=None)
        self.assertEqual([], extract_specializations(tree))


class TestFullODDSampler(unittest.TestCase):

    def test_sample_count(self):
        tree = _build_small_tree()
        params = [_param("precipitation_mm_h", "Rain", 1, 20),
                  _param("visibility_m", "Day", 5000, 15000)]
        sampler = FullODDSampler(tree, params)
        samples = sampler.sample(n_pes=4, n_per_pes=5, seed=42)
        self.assertEqual(20, len(samples))

    def test_all_have_choices(self):
        tree = _build_small_tree()
        params = [_param("x", "Rain", 0, 10)]
        sampler = FullODDSampler(tree, params)
        samples = sampler.sample(n_pes=3, n_per_pes=2, seed=1)
        for s in samples:
            self.assertIn("WeatherDec", s.choices)
            self.assertIn("TimeDec", s.choices)

    def test_categorical_coverage(self):
        """With enough PES configs, all choices should appear."""
        tree = _build_small_tree()
        params = [_param("x", "Rain", 0, 10)]
        sampler = FullODDSampler(tree, params)
        samples = sampler.sample(n_pes=20, n_per_pes=1, seed=42)
        weather_choices = {s.choices["WeatherDec"] for s in samples}
        time_choices = {s.choices["TimeDec"] for s in samples}
        self.assertEqual({"Rain", "Snow"}, weather_choices)
        self.assertEqual({"Day", "Night"}, time_choices)

    def test_total_pes_combinations(self):
        tree = _build_small_tree()
        sampler = FullODDSampler(tree, [])
        # 2 weather × 2 time = 4
        self.assertEqual(4, sampler.total_pes_combinations())

    def test_to_test_case(self):
        tree = _build_small_tree()
        params = [_param("x", "Rain", 0, 10)]
        sampler = FullODDSampler(tree, params)
        samples = sampler.sample(n_pes=1, n_per_pes=1, seed=1)
        tc = samples[0].to_test_case()
        self.assertIsInstance(tc, TestCase)
        self.assertEqual(1, tc.id)

    def test_reproducible(self):
        tree = _build_small_tree()
        params = [_param("x", "Rain", 0, 10)]
        sampler = FullODDSampler(tree, params)
        a = sampler.sample(n_pes=3, n_per_pes=3, seed=77)
        b = sampler.sample(n_pes=3, n_per_pes=3, seed=77)
        for sa, sb in zip(a, b):
            self.assertEqual(sa.choices, sb.choices)
            self.assertEqual(sa.continuous, sb.continuous)


class TestFullODDSamplerLargeTree(unittest.TestCase):
    """Test with a tree structure matching the RunwaySignClassifier."""

    def _build_rsc_tree(self) -> SESTree:
        def _spec(name, choices):
            children = [_node(c, "ENTITY") for c in choices]
            return _node(name, "SPECIALIZATION", children)

        env = _node("Environment", "ASPECT", [
            _spec("AirportDec", ["KSFO", "KBOS", "KSAN",
                                 "KDEN", "KMIA", "PANC"]),
            _spec("PrecipitationDec", ["NoPrecipitation", "Rain",
                                       "Snow", "Hail"]),
            _spec("VisibilityDec", ["HighVisibility", "ModerateVisibility",
                                    "LowVisibility", "VeryLowVisibility"]),
            _spec("TimeOfDayDec", ["Morning", "Afternoon", "Dusk", "Dawn"]),
            _spec("SignTypeDec", ["MandatoryInstruction", "LocationSign",
                                  "DirectionSign",
                                  "RunwayDistanceRemaining"]),
        ])
        sensor = _node("Sensor", "ASPECT", [
            _spec("DistanceDec", ["DS10", "DS12", "DS14"]),
            _spec("ElevationDec", ["EL10", "EL13", "EL16"]),
            _spec("LateralOffsetDec", ["LO00", "LO07", "LO14"]),
        ])
        degrad = _node("Degradation", "ASPECT", [
            _spec("SignConditionDec", ["GoodCondition", "FadedSign",
                                       "PartiallyOccluded", "WetSign"]),
            _spec("SensorConditionDec", ["NominalSensor", "WaterDroplets",
                                         "DirtySensor", "Overexposed"]),
        ])
        root = _node("RunwaySignClassifier", "ENTITY",
                      [_node("pSpec", "ASPECT", [env, sensor, degrad])])
        return SESTree(id="rsc", name="RSC", schema_version="1.0", root=root)

    def test_total_combinations(self):
        tree = self._build_rsc_tree()
        sampler = FullODDSampler(tree, [])
        # 6×4×4×4×4 × 3×3×3 × 4×4 = 663,552
        self.assertEqual(663_552, sampler.total_pes_combinations())

    def test_ten_specializations(self):
        tree = self._build_rsc_tree()
        specs = extract_specializations(tree)
        self.assertEqual(10, len(specs))

    def test_sampling_200_cases(self):
        tree = self._build_rsc_tree()
        params = [_param("visibility_m", "HighVisibility", 5000, 15000),
                  _param("distance_m", "DS10", 10, 12)]
        sampler = FullODDSampler(tree, params)
        samples = sampler.sample(n_pes=20, n_per_pes=10, seed=42)
        self.assertEqual(200, len(samples))
        # Each sample should have choices for all 10 specializations
        for s in samples:
            self.assertEqual(10, len(s.choices))


class TestSpecializationEdgeCases(unittest.TestCase):

    def test_no_specializations(self):
        root = _node("Root", "ENTITY", [_node("Child", "ASPECT")])
        tree = SESTree(id="t", name="T", schema_version="1.0", root=root)
        specs = extract_specializations(tree)
        self.assertEqual([], specs)

    def test_nested_specializations(self):
        """Specialization under another specialization."""
        inner = _node("InnerDec", "SPECIALIZATION",
                      [_node("A", "ENTITY"), _node("B", "ENTITY")])
        outer_child = _node("X", "ENTITY", [inner])
        outer = _node("OuterDec", "SPECIALIZATION",
                      [outer_child, _node("Y", "ENTITY")])
        root = _node("Root", "ENTITY", [outer])
        tree = SESTree(id="t", name="T", schema_version="1.0", root=root)
        specs = extract_specializations(tree)
        names = {s.node_name for s in specs}
        self.assertEqual({"OuterDec", "InnerDec"}, names)


if __name__ == "__main__":
    unittest.main()
