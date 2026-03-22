"""Tests for the ODME export helpers."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from odme.export import to_csv, write_verdicts
from odme.model import Parameter, TestCase


class TestToCsv(unittest.TestCase):
    def test_basic_csv(self):
        params = [
            Parameter("visibility_m", "Fair", "Fair.visibility_m", "double", 5000, 15000),
            Parameter("precip", "Rain", "Rain.precip", "double", 1.0, 20.0),
        ]
        test_cases = [
            TestCase(1, {"Fair.visibility_m": 7500.0, "Rain.precip": 10.5}),
            TestCase(2, {"Fair.visibility_m": 12000.0, "Rain.precip": 3.2}),
        ]
        csv_output = to_csv(test_cases, params)
        lines = [l.strip() for l in csv_output.strip().split("\n")]
        self.assertEqual(lines[0], "TestCase_ID,Fair.visibility_m,Rain.precip")
        self.assertEqual(lines[1], "1,7500.0,10.5")
        self.assertEqual(lines[2], "2,12000.0,3.2")

    def test_empty_test_cases(self):
        params = [Parameter("v", "N", "N.v", "double", 0, 1)]
        csv_output = to_csv([], params)
        lines = csv_output.strip().split("\n")
        self.assertEqual(len(lines), 1)  # header only

    def test_missing_value(self):
        params = [Parameter("v", "N", "N.v", "double", 0, 1)]
        test_cases = [TestCase(1, {})]
        csv_output = to_csv(test_cases, params)
        # Should have header and one data row
        lines = csv_output.strip().split("\n")
        self.assertEqual(len(lines), 2)


class TestWriteVerdicts(unittest.TestCase):
    def test_write_and_read(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            verdicts = [
                {"testCaseId": "TC_001", "verdict": "PASS", "detail": "OK"},
                {"testCaseId": "TC_002", "verdict": "FAIL", "detail": "Threshold exceeded"},
            ]
            path = write_verdicts(verdicts, Path(tmpdir))
            self.assertTrue(path.exists())

            with open(path) as f:
                data = json.load(f)
            self.assertEqual(data["version"], "1.0")
            self.assertEqual(len(data["verdicts"]), 2)
            self.assertEqual(data["verdicts"][0]["testCaseId"], "TC_001")
            self.assertEqual(data["verdicts"][1]["verdict"], "FAIL")

    def test_empty_verdicts(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            path = write_verdicts([], Path(tmpdir))
            with open(path) as f:
                data = json.load(f)
            self.assertEqual(data["verdicts"], [])


if __name__ == "__main__":
    unittest.main()
