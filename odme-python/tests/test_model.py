"""Tests for the ODME Python SDK model."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import odme
from odme.model import (
    CoverageReport,
    Parameter,
    PESTree,
    Project,
    ProjectMeta,
    SESNode,
    SESTree,
    Scenario,
    TestCase,
    TraceabilityEntry,
)


SAMPLE_EXPORT = {
    "version": "1.0",
    "project": {
        "id": "proj-1",
        "name": "RunwaySignClassifier",
        "directory": "/tmp/test",
    },
    "sesTree": {
        "id": "ses-1",
        "name": "RunwaySignClassifier",
        "schemaVersion": "1.0",
        "root": {
            "id": "root",
            "name": "RunwaySignClassifier",
            "type": "ENTITY",
            "label": "RunwaySignClassifier",
            "path": "/RunwaySignClassifier",
            "variables": {},
            "behaviours": ["detectSign()"],
            "constraints": ["visibility_m > 200"],
            "flags": {"safety_critical": "true"},
            "children": [
                {
                    "id": "env-dec",
                    "name": "Environment",
                    "type": "SPECIALIZATION",
                    "label": "EnvironmentDec",
                    "path": "/RunwaySignClassifier/Environment",
                    "children": [
                        {
                            "id": "fair",
                            "name": "Fair",
                            "type": "ENTITY",
                            "label": "Fair",
                            "path": "/RunwaySignClassifier/Environment/Fair",
                            "variables": {
                                "visibility_m": "[5000, 15000]",
                                "precipitation_mm_h": "0.0",
                            },
                        },
                        {
                            "id": "rainy",
                            "name": "Rainy",
                            "type": "ENTITY",
                            "label": "Rainy",
                            "path": "/RunwaySignClassifier/Environment/Rainy",
                            "variables": {
                                "visibility_m": "[1000, 5000]",
                            },
                        },
                    ],
                }
            ],
        },
    },
    "scenarios": [
        {
            "id": "sc-1",
            "name": "Nominal",
            "status": "APPROVED",
            "risk": "Low",
            "remarks": "Baseline",
            "sourceSESId": "ses-1",
            "pesId": "pes-1",
            "createdAt": "2024-01-15T10:30:00Z",
            "createdBy": "researcher",
            "pesTree": {
                "id": "pes-1",
                "name": "Nominal",
                "sourceSESId": "ses-1",
                "prunedNodeIds": ["rainy"],
                "root": {
                    "id": "root",
                    "name": "RunwaySignClassifier",
                    "type": "ENTITY",
                    "label": "RunwaySignClassifier",
                    "path": "/RunwaySignClassifier",
                    "children": [
                        {
                            "id": "fair",
                            "name": "Fair",
                            "type": "ENTITY",
                            "label": "Fair",
                            "path": "/RunwaySignClassifier/Fair",
                        }
                    ],
                },
            },
        }
    ],
    "parameters": [
        {
            "name": "visibility_m",
            "parentNode": "Fair",
            "qualifiedName": "Fair.visibility_m",
            "dataType": "double",
            "min": 5000.0,
            "max": 15000.0,
        },
        {
            "name": "precipitation_mm_h",
            "parentNode": "Rainy",
            "qualifiedName": "Rainy.precipitation_mm_h",
            "dataType": "double",
            "min": 1.0,
            "max": 20.0,
        },
    ],
    "testCases": [
        {"id": 1, "values": {"Fair.visibility_m": 7500.0, "Rainy.precipitation_mm_h": 10.5}},
        {"id": 2, "values": {"Fair.visibility_m": 12000.0, "Rainy.precipitation_mm_h": 3.2}},
    ],
    "coverage": {
        "totalLeafNodes": 2,
        "coveredLeafNodes": 1,
        "coveragePercent": 50.0,
        "scenarioCount": 1,
        "uncoveredNodes": [{"id": "rainy", "name": "Rainy", "path": "/RunwaySignClassifier/Environment/Rainy"}],
    },
    "traceability": [
        {
            "oddElementId": "fair",
            "oddElementName": "Fair",
            "oddElementPath": "/RunwaySignClassifier/Environment/Fair",
            "scenarioId": "sc-1",
            "scenarioName": "Nominal",
            "testCaseId": "TC_001",
            "verdict": "PASS",
        }
    ],
}


class TestProjectLoading(unittest.TestCase):
    def test_load_from_dict(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(project.version, "1.0")
        self.assertEqual(project.meta.name, "RunwaySignClassifier")

    def test_load_from_file(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            json.dump(SAMPLE_EXPORT, f)
            f.flush()
            project = odme.load_project(f.name)
        self.assertEqual(project.meta.name, "RunwaySignClassifier")

    def test_project_meta(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(project.meta.id, "proj-1")
        self.assertEqual(project.meta.directory, "/tmp/test")


class TestSESTree(unittest.TestCase):
    def test_tree_identity(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(project.ses_tree.id, "ses-1")
        self.assertEqual(project.ses_tree.name, "RunwaySignClassifier")
        self.assertEqual(project.ses_tree.schema_version, "1.0")

    def test_root_node(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        root = project.ses_tree.root
        self.assertIsNotNone(root)
        self.assertEqual(root.name, "RunwaySignClassifier")
        self.assertEqual(root.type, "ENTITY")
        self.assertEqual(root.path, "/RunwaySignClassifier")

    def test_leaf_nodes(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        leaves = project.ses_tree.root.leaf_nodes()
        names = [l.name for l in leaves]
        self.assertIn("Fair", names)
        self.assertIn("Rainy", names)
        self.assertEqual(len(leaves), 2)

    def test_find_by_name(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        fair = project.ses_tree.root.find_by_name("Fair")
        self.assertIsNotNone(fair)
        self.assertEqual(fair.variables["visibility_m"], "[5000, 15000]")

    def test_find_by_name_not_found(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        result = project.ses_tree.root.find_by_name("NonExistent")
        self.assertIsNone(result)

    def test_find_by_type(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        specs = project.ses_tree.root.find_by_type("SPECIALIZATION")
        self.assertEqual(len(specs), 1)
        self.assertEqual(specs[0].name, "Environment")

    def test_subtree(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        all_nodes = project.ses_tree.root.subtree()
        self.assertEqual(len(all_nodes), 4)  # root, env-dec, fair, rainy

    def test_node_behaviours(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertIn("detectSign()", project.ses_tree.root.behaviours)

    def test_node_constraints(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertIn("visibility_m > 200", project.ses_tree.root.constraints)

    def test_node_flags(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(project.ses_tree.root.flags["safety_critical"], "true")


class TestScenarios(unittest.TestCase):
    def test_scenario_count(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(len(project.scenarios), 1)

    def test_scenario_fields(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        sc = project.scenarios[0]
        self.assertEqual(sc.name, "Nominal")
        self.assertEqual(sc.status, "APPROVED")
        self.assertEqual(sc.risk, "Low")
        self.assertEqual(sc.created_by, "researcher")

    def test_scenario_pes_tree(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        pes = project.scenarios[0].pes_tree
        self.assertIsNotNone(pes)
        self.assertEqual(pes.source_ses_id, "ses-1")
        self.assertIn("rainy", pes.pruned_node_ids)
        leaves = pes.root.leaf_nodes()
        self.assertEqual(len(leaves), 1)
        self.assertEqual(leaves[0].name, "Fair")


class TestParameters(unittest.TestCase):
    def test_parameter_count(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(len(project.parameters), 2)

    def test_parameter_fields(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        p = project.parameters[0]
        self.assertEqual(p.name, "visibility_m")
        self.assertEqual(p.parent_node, "Fair")
        self.assertEqual(p.qualified_name, "Fair.visibility_m")
        self.assertEqual(p.data_type, "double")
        self.assertEqual(p.min, 5000.0)
        self.assertEqual(p.max, 15000.0)


class TestTestCases(unittest.TestCase):
    def test_test_case_count(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(len(project.test_cases), 2)

    def test_test_case_values(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        tc = project.test_cases[0]
        self.assertEqual(tc.id, 1)
        self.assertEqual(tc.values["Fair.visibility_m"], 7500.0)


class TestCoverageReport(unittest.TestCase):
    def test_coverage_report(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertIsNotNone(project.coverage)
        self.assertEqual(project.coverage.total_leaf_nodes, 2)
        self.assertEqual(project.coverage.covered_leaf_nodes, 1)
        self.assertEqual(project.coverage.coverage_percent, 50.0)
        self.assertEqual(len(project.coverage.uncovered_nodes), 1)
        self.assertEqual(project.coverage.uncovered_nodes[0]["name"], "Rainy")


class TestTraceability(unittest.TestCase):
    def test_traceability_entries(self):
        project = Project.from_dict(SAMPLE_EXPORT)
        self.assertEqual(len(project.traceability), 1)
        entry = project.traceability[0]
        self.assertEqual(entry.odd_element_name, "Fair")
        self.assertEqual(entry.scenario_name, "Nominal")
        self.assertEqual(entry.test_case_id, "TC_001")
        self.assertEqual(entry.verdict, "PASS")


class TestMissingOptionalFields(unittest.TestCase):
    def test_empty_project(self):
        minimal = {
            "version": "1.0",
            "project": {"id": "p", "name": "P"},
            "sesTree": {"id": "s", "name": "S", "root": None},
            "scenarios": [],
            "parameters": [],
            "testCases": [],
            "traceability": [],
        }
        project = Project.from_dict(minimal)
        self.assertIsNone(project.ses_tree.root)
        self.assertIsNone(project.coverage)
        self.assertEqual(len(project.scenarios), 0)

    def test_node_with_no_optional_fields(self):
        node_dict = {
            "id": "n1",
            "name": "Node",
            "type": "ENTITY",
            "path": "/Node",
        }
        node = SESNode.from_dict(node_dict)
        self.assertEqual(node.variables, {})
        self.assertEqual(node.behaviours, [])
        self.assertEqual(node.constraints, [])
        self.assertEqual(node.children, [])


if __name__ == "__main__":
    unittest.main()
