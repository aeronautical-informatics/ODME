"""Tests for runway_sign_demo.py — end-to-end demonstrator."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

# Add examples dir to path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "examples"))

from runway_sign_demo import (
    build_runway_sign_odd,
    generate_lhs_test_cases,
    _build_parameters,
)
from odme.sampling import extract_specializations, FullODDSampler


class TestBuildRunwaySignODD(unittest.TestCase):

    def test_tree_has_root(self):
        tree, _ = build_runway_sign_odd()
        self.assertIsNotNone(tree.root)
        self.assertEqual("RunwaySignClassifier", tree.root.name)

    def test_ten_specializations(self):
        tree, _ = build_runway_sign_odd()
        specs = extract_specializations(tree)
        self.assertEqual(10, len(specs))

    def test_specialization_names(self):
        tree, _ = build_runway_sign_odd()
        specs = extract_specializations(tree)
        names = {s.node_name for s in specs}
        expected = {
            "AirportDec", "PrecipitationDec", "VisibilityDec",
            "TimeOfDayDec", "SignTypeDec",
            "DistanceDec", "ElevationDec", "LateralOffsetDec",
            "SignConditionDec", "SensorConditionDec",
        }
        self.assertEqual(expected, names)

    def test_airport_choices(self):
        tree, _ = build_runway_sign_odd()
        specs = extract_specializations(tree)
        airports = next(s for s in specs if s.node_name == "AirportDec")
        self.assertEqual(6, len(airports.choices))
        self.assertIn("PANC", airports.choices)

    def test_pes_combinations(self):
        tree, _ = build_runway_sign_odd()
        sampler = FullODDSampler(tree, [])
        # 6×4×4×4×4 × 3×3×3 × 4×4 = 663,552
        self.assertEqual(663_552, sampler.total_pes_combinations())

    def test_leaf_count(self):
        tree, _ = build_runway_sign_odd()
        leaves = tree.root.leaf_nodes()
        self.assertEqual(39, len(leaves))


class TestBuildParameters(unittest.TestCase):

    def test_parameter_count(self):
        params = _build_parameters()
        # 30 continuous parameters as per README
        self.assertEqual(30, len(params))

    def test_all_double(self):
        params = _build_parameters()
        for p in params:
            self.assertEqual("double", p.data_type)

    def test_rain_range(self):
        params = _build_parameters()
        rain = next(p for p in params
                    if p.parent_node == "Rain"
                    and p.name == "precipitation_mm_h")
        self.assertEqual(1.0, rain.min)
        self.assertEqual(20.0, rain.max)

    def test_visibility_ranges(self):
        params = _build_parameters()
        vis = {p.parent_node: (p.min, p.max) for p in params
               if p.name == "visibility_m"}
        self.assertEqual(4, len(vis))
        self.assertEqual((200.0, 500.0), vis["VeryLowVisibility"])
        self.assertEqual((5000.0, 15000.0), vis["HighVisibility"])


class TestGenerateLHSTestCases(unittest.TestCase):

    def test_generates_correct_count(self):
        tree, params = build_runway_sign_odd()
        tcs = generate_lhs_test_cases(tree, params, n_pes=4, n_per_pes=5,
                                       seed=42)
        self.assertEqual(20, len(tcs))

    def test_ids_sequential(self):
        tree, params = build_runway_sign_odd()
        tcs = generate_lhs_test_cases(tree, params, n_pes=2, n_per_pes=3,
                                       seed=1)
        ids = [tc.id for tc in tcs]
        self.assertEqual(list(range(1, 7)), ids)

    def test_values_have_selection_markers(self):
        """Each test case should have ._selected markers for categorical."""
        tree, params = build_runway_sign_odd()
        tcs = generate_lhs_test_cases(tree, params, n_pes=1, n_per_pes=1,
                                       seed=42)
        tc = tcs[0]
        # Should have _selected markers from categorical choices
        selected = [k for k in tc.values if "._selected" in k]
        # 10 specializations -> 10 _selected markers
        self.assertEqual(10, len(selected))

    def test_reproducible(self):
        tree, params = build_runway_sign_odd()
        a = generate_lhs_test_cases(tree, params, n_pes=3, n_per_pes=3,
                                     seed=99)
        b = generate_lhs_test_cases(tree, params, n_pes=3, n_per_pes=3,
                                     seed=99)
        for ta, tb in zip(a, b):
            self.assertEqual(ta.values, tb.values)


if __name__ == "__main__":
    unittest.main()
