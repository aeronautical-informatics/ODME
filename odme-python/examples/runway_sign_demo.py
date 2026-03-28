#!/usr/bin/env python3
"""
RunwaySignClassifier End-to-End Demonstrator.

Complete pipeline from ODD model to synthetic training/verification data:

  1. Load ODD from ODME export (or build minimal ODD from scratch)
  2. Generate test cases via LHS across all ODD dimensions
  3. Produce MSFS automation files for each test case
  4. Run MSFS capture session (or dry-run for verification)
  5. Output a labeled image dataset

This demonstrates the ODME workflow against EASA AI objectives:
  W-3.1  ODD definition       -> SES tree with 10 specializations
  W-3.2  Data requirements     -> 72 parameters, traceable to standards
  W-4.4  Verification          -> LHS-sampled test cases with full coverage
  W-3.2  Data traceability     -> Every image labeled with its ODD parameters

Usage:
  # Full pipeline with ODME export:
  python runway_sign_demo.py --odme-project export.json --output-dir ./output

  # Standalone demo (builds ODD internally, no ODME export needed):
  python runway_sign_demo.py --standalone --output-dir ./output --n-samples 50

  # Dry-run capture (no MSFS needed):
  python runway_sign_demo.py --standalone --output-dir ./output \\
      --n-samples 20 --capture --dry-run

  # Full automation (MSFS must be running):
  python runway_sign_demo.py --odme-project export.json --output-dir ./output \\
      --capture --dataset-dir ./dataset
"""
from __future__ import annotations

import argparse
import csv
import json
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# Add parent so the odme package can be found when run from examples dir
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from odme.model import (Parameter, Project, ProjectMeta, SESNode, SESTree,
                         TestCase)
from odme.sampling import (FullODDSampler, LatinHypercubeSampler,
                            extract_specializations)
from odme.export import write_verdicts

# Import the MSFS file generators from the existing plugin
from runway_sign_msfs import (
    AIRPORTS, ScenarioConfig, extract_config,
    generate_wpr, generate_flt, generate_simconnect_script, generate_manifest,
)


# ─── Standalone ODD Builder ─────────────────────────────────────────────────
# When running without an ODME export, build a minimal ODD model in Python
# that mirrors the RunwaySignClassifier example.

def build_runway_sign_odd() -> Tuple[SESTree, List[Parameter]]:
    """Build the RunwaySignClassifier ODD programmatically.

    Returns (SESTree, list of continuous Parameters) matching the
    examples/RunwaySignClassifier/RunwaySignClassifier.xml model.
    """
    # Build the SES tree structure
    node_id = 0

    def _node(name: str, ntype: str, path: str,
              children: Optional[List[SESNode]] = None) -> SESNode:
        nonlocal node_id
        node_id += 1
        return SESNode(
            id=str(node_id), name=name, type=ntype,
            label=name, path=path,
            children=children or [],
        )

    # Airports
    airports = [_node(icao, "ENTITY",
                      f"RunwaySignClassifier/Environment/AirportDec/{icao}")
                for icao in ["KSFO", "KBOS", "KSAN", "KDEN", "KMIA", "PANC"]]
    airport_dec = _node("AirportDec", "SPECIALIZATION",
                        "RunwaySignClassifier/Environment/AirportDec",
                        airports)

    # Precipitation
    precips = [_node(n, "ENTITY",
                     f"RunwaySignClassifier/Environment/PrecipitationDec/{n}")
               for n in ["NoPrecipitation", "Rain", "Snow", "Hail"]]
    precip_dec = _node("PrecipitationDec", "SPECIALIZATION",
                       "RunwaySignClassifier/Environment/PrecipitationDec",
                       precips)

    # Visibility
    vis_nodes = [_node(n, "ENTITY",
                       f"RunwaySignClassifier/Environment/VisibilityDec/{n}")
                 for n in ["HighVisibility", "ModerateVisibility",
                           "LowVisibility", "VeryLowVisibility"]]
    vis_dec = _node("VisibilityDec", "SPECIALIZATION",
                    "RunwaySignClassifier/Environment/VisibilityDec",
                    vis_nodes)

    # Time of Day
    time_nodes = [_node(n, "ENTITY",
                        f"RunwaySignClassifier/Environment/TimeOfDayDec/{n}")
                  for n in ["Morning", "Afternoon", "Dusk", "Dawn"]]
    time_dec = _node("TimeOfDayDec", "SPECIALIZATION",
                     "RunwaySignClassifier/Environment/TimeOfDayDec",
                     time_nodes)

    # Sign Type
    sign_nodes = [_node(n, "ENTITY",
                        f"RunwaySignClassifier/Environment/SignTypeDec/{n}")
                  for n in ["MandatoryInstruction", "LocationSign",
                            "DirectionSign", "RunwayDistanceRemaining"]]
    sign_dec = _node("SignTypeDec", "SPECIALIZATION",
                     "RunwaySignClassifier/Environment/SignTypeDec",
                     sign_nodes)

    # Environment
    env = _node("Environment", "ASPECT",
                "RunwaySignClassifier/Environment",
                [airport_dec, precip_dec, vis_dec, time_dec, sign_dec])

    # Sensor — Distance, Elevation, Lateral Offset
    dist_nodes = [_node(n, "ENTITY",
                        f"RunwaySignClassifier/Sensor/DistanceDec/{n}")
                  for n in ["DS10", "DS12", "DS14"]]
    dist_dec = _node("DistanceDec", "SPECIALIZATION",
                     "RunwaySignClassifier/Sensor/DistanceDec", dist_nodes)

    elev_nodes = [_node(n, "ENTITY",
                        f"RunwaySignClassifier/Sensor/ElevationDec/{n}")
                  for n in ["EL10", "EL13", "EL16"]]
    elev_dec = _node("ElevationDec", "SPECIALIZATION",
                     "RunwaySignClassifier/Sensor/ElevationDec", elev_nodes)

    offset_nodes = [_node(n, "ENTITY",
                          f"RunwaySignClassifier/Sensor/LateralOffsetDec/{n}")
                    for n in ["LO00", "LO07", "LO14"]]
    offset_dec = _node("LateralOffsetDec", "SPECIALIZATION",
                       "RunwaySignClassifier/Sensor/LateralOffsetDec",
                       offset_nodes)

    sensor = _node("Sensor", "ASPECT", "RunwaySignClassifier/Sensor",
                   [dist_dec, elev_dec, offset_dec])

    # Degradation
    sign_cond_nodes = [_node(n, "ENTITY",
                             f"RunwaySignClassifier/Degradation/"
                             f"SignConditionDec/{n}")
                       for n in ["GoodCondition", "FadedSign",
                                 "PartiallyOccluded", "WetSign"]]
    sign_cond_dec = _node("SignConditionDec", "SPECIALIZATION",
                          "RunwaySignClassifier/Degradation/SignConditionDec",
                          sign_cond_nodes)

    sensor_cond_nodes = [_node(n, "ENTITY",
                               f"RunwaySignClassifier/Degradation/"
                               f"SensorConditionDec/{n}")
                         for n in ["NominalSensor", "WaterDroplets",
                                   "DirtySensor", "Overexposed"]]
    sensor_cond_dec = _node("SensorConditionDec", "SPECIALIZATION",
                            "RunwaySignClassifier/Degradation/"
                            "SensorConditionDec", sensor_cond_nodes)

    degradation = _node("Degradation", "ASPECT",
                        "RunwaySignClassifier/Degradation",
                        [sign_cond_dec, sensor_cond_dec])

    # pSpec aspect
    pspec = _node("pSpec", "ASPECT", "RunwaySignClassifier/pSpec",
                  [env, sensor, degradation])

    root = _node("RunwaySignClassifier", "ENTITY", "RunwaySignClassifier",
                 [pspec])

    tree = SESTree(id="ses-1", name="RunwaySignClassifier",
                   schema_version="1.0", root=root)

    # Build parameters (continuous ranges only — these are what LHS samples)
    params = _build_parameters()

    return tree, params


def _build_parameters() -> List[Parameter]:
    """Build the 30 continuous parameters for LHS sampling."""
    p = []

    def _add(name: str, parent: str, lo: float, hi: float):
        p.append(Parameter(
            name=name, parent_node=parent,
            qualified_name=f"{parent}.{name}",
            data_type="double", min=lo, max=hi))

    # Precipitation rates
    _add("precipitation_mm_h", "Rain", 1.0, 20.0)
    _add("precipitation_mm_h", "Snow", 0.5, 10.0)
    _add("precipitation_mm_h", "Hail", 5.0, 30.0)

    # Visibility
    _add("visibility_m", "HighVisibility", 5000.0, 15000.0)
    _add("visibility_m", "ModerateVisibility", 1500.0, 5000.0)
    _add("visibility_m", "LowVisibility", 500.0, 1500.0)
    _add("visibility_m", "VeryLowVisibility", 200.0, 500.0)

    # Time of day
    _add("sun_elevation_deg", "Morning", 5.0, 30.0)
    _add("illuminance_lux", "Morning", 1000.0, 25000.0)
    _add("sun_elevation_deg", "Afternoon", 30.0, 70.0)
    _add("illuminance_lux", "Afternoon", 25000.0, 100000.0)
    _add("sun_elevation_deg", "Dusk", -6.0, 0.0)
    _add("illuminance_lux", "Dusk", 40.0, 1000.0)
    _add("sun_elevation_deg", "Dawn", -6.0, 10.0)
    _add("illuminance_lux", "Dawn", 40.0, 5000.0)

    # Sign height per type
    _add("sign_height_cm", "MandatoryInstruction", 30.0, 50.0)
    _add("sign_height_cm", "LocationSign", 20.0, 40.0)
    _add("sign_height_cm", "DirectionSign", 20.0, 40.0)
    _add("sign_height_cm", "RunwayDistanceRemaining", 60.0, 90.0)

    # Sensor
    _add("distance_m", "DS10", 10.0, 12.0)
    _add("distance_m", "DS12", 12.0, 14.0)
    _add("distance_m", "DS14", 14.0, 16.0)
    _add("elevation_m", "EL10", 1.0, 1.3)
    _add("elevation_m", "EL13", 1.3, 1.6)
    _add("elevation_m", "EL16", 1.6, 1.9)
    _add("lateral_offset_m", "LO00", 0.0, 0.7)
    _add("lateral_offset_m", "LO07", 0.7, 1.4)
    _add("lateral_offset_m", "LO14", 1.4, 2.0)

    # Degradation - sign condition
    _add("paint_contrast_pct", "GoodCondition", 80.0, 100.0)
    _add("paint_contrast_pct", "FadedSign", 30.0, 80.0)

    return p


# ─── Test Case Generation ───────────────────────────────────────────────────

def generate_lhs_test_cases(tree: SESTree, params: List[Parameter],
                            n_pes: int, n_per_pes: int,
                            seed: int = 42) -> List[TestCase]:
    """Generate test cases using combined categorical+continuous LHS.

    Args:
        tree: The SES tree with specializations.
        params: Continuous parameters for LHS.
        n_pes: Number of PES configurations to sample.
        n_per_pes: LHS samples per PES configuration.
        seed: Random seed.

    Returns:
        List of TestCase objects ready for MSFS scenario generation.
    """
    sampler = FullODDSampler(tree, params)
    specs = extract_specializations(tree)
    total_pes = sampler.total_pes_combinations()

    print(f"\nODD Structure:")
    print(f"  Specializations: {len(specs)}")
    for s in specs:
        print(f"    {s.node_name}: {', '.join(s.choices)}")
    print(f"  Total PES combinations: {total_pes:,}")
    print(f"  Continuous parameters:  {len(params)}")
    print(f"\nSampling strategy:")
    print(f"  PES configurations: {n_pes}")
    print(f"  LHS samples/PES:    {n_per_pes}")
    print(f"  Total test cases:   {n_pes * n_per_pes}")
    print()

    samples = sampler.sample(n_pes=n_pes, n_per_pes=n_per_pes, seed=seed)

    # Convert ODDSamples to TestCases with values keyed by parent.param
    test_cases: List[TestCase] = []
    for s in samples:
        # Start with continuous values
        values = dict(s.continuous)

        # Add categorical choice information as sentinel values so that
        # extract_config() can detect which leaf is active
        for spec_name, choice in s.choices.items():
            # Add a dummy parameter under the chosen leaf so
            # resolve_node_choice() picks it up
            values[f"{choice}._selected"] = 1.0

        test_cases.append(TestCase(id=s.id, values=values))

    return test_cases


# ─── Coverage Report ─────────────────────────────────────────────────────────

def print_coverage_report(test_cases: List[TestCase], tree: SESTree,
                          params: List[Parameter]):
    """Print ODD coverage statistics."""
    specs = extract_specializations(tree)

    # Count how many times each specialization choice appears
    from collections import Counter
    choice_counts: Dict[str, Counter] = {}
    for s in specs:
        choice_counts[s.node_name] = Counter()

    for tc in test_cases:
        for key in tc.values:
            parent = key.split(".")[0] if "." in key else key
            for s in specs:
                if parent in s.choices:
                    choice_counts[s.node_name][parent] += 1

    print("\nODD Coverage Report")
    print("=" * 60)
    for s in specs:
        counts = choice_counts[s.node_name]
        covered = sum(1 for c in s.choices if counts[c] > 0)
        total = len(s.choices)
        pct = 100.0 * covered / total if total > 0 else 0
        print(f"\n  {s.node_name} ({covered}/{total} = {pct:.0f}%):")
        for c in s.choices:
            bar = "#" * min(counts[c], 40)
            print(f"    {c:30s} {counts[c]:4d} {bar}")

    # Parameter range coverage
    print(f"\n  Continuous parameter samples: {len(test_cases)}")
    if test_cases and params:
        for p in params[:5]:  # Show first 5
            vals = [tc.values.get(p.qualified_name)
                    for tc in test_cases
                    if p.qualified_name in tc.values]
            if vals:
                print(f"    {p.qualified_name}: "
                      f"[{min(vals):.2f} .. {max(vals):.2f}] "
                      f"(range: {p.min}..{p.max}, n={len(vals)})")
        if len(params) > 5:
            print(f"    ... and {len(params) - 5} more parameters")


# ─── Main Pipeline ──────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="RunwaySignClassifier End-to-End Demonstrator")
    parser.add_argument("--odme-project", type=Path,
                        help="Path to ODME plugin_export.json")
    parser.add_argument("--output-dir", type=Path, required=True,
                        help="Output directory for MSFS scenario files")
    parser.add_argument("--standalone", action="store_true",
                        help="Build ODD internally (no ODME export needed)")
    parser.add_argument("--n-samples", type=int, default=50,
                        help="Total number of test cases (default: 50)")
    parser.add_argument("--n-pes", type=int, default=None,
                        help="Number of PES configurations "
                             "(default: auto from n-samples)")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed (default: 42)")
    parser.add_argument("--capture", action="store_true",
                        help="Run MSFS capture after generating scenarios")
    parser.add_argument("--dataset-dir", type=Path, default=None,
                        help="Dataset output dir (default: output-dir/dataset)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Capture dry-run (no MSFS connection)")
    args = parser.parse_args()

    print("=" * 70)
    print("  ODME RunwaySignClassifier — End-to-End Demonstrator")
    print("=" * 70)

    # Step 1: Load or build ODD
    if args.standalone:
        print("\n[Step 1] Building ODD model (standalone mode)")
        tree, params = build_runway_sign_odd()
        test_cases_from_odme = []
    elif args.odme_project:
        print(f"\n[Step 1] Loading ODD from {args.odme_project}")
        import odme
        project = odme.load_project(args.odme_project)
        tree = project.ses_tree
        params = project.parameters
        test_cases_from_odme = project.test_cases
        print(f"  Project: {project.meta.name}")
        print(f"  Existing test cases: {len(test_cases_from_odme)}")
    else:
        print("ERROR: Specify --odme-project or --standalone")
        sys.exit(1)

    # Step 2: Generate test cases via LHS
    if test_cases_from_odme:
        print(f"\n[Step 2] Using {len(test_cases_from_odme)} test cases "
              f"from ODME")
        test_cases = test_cases_from_odme
    else:
        n_total = args.n_samples
        n_pes = args.n_pes or max(1, n_total // 5)
        n_per_pes = max(1, n_total // n_pes)
        print(f"\n[Step 2] Generating test cases via LHS "
              f"({n_pes} PES × {n_per_pes} samples)")
        test_cases = generate_lhs_test_cases(
            tree, params, n_pes=n_pes, n_per_pes=n_per_pes, seed=args.seed)

    # Step 3: Generate MSFS scenario files
    output_dir = args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n[Step 3] Generating MSFS scenario files -> {output_dir}")
    configs: List[ScenarioConfig] = []
    verdicts = []

    for tc in test_cases:
        cfg = extract_config(tc.id, tc.values)
        configs.append(cfg)

        generate_wpr(cfg, output_dir)
        generate_flt(cfg, output_dir)
        generate_simconnect_script(cfg, output_dir)

        detail = (f"{cfg.airport_icao} | {cfg.precip_type} "
                  f"{cfg.precip_mm_h:.1f}mm/h | vis {cfg.visibility_m:.0f}m | "
                  f"sun {cfg.sun_elevation_deg:.1f}\u00b0 | {cfg.sign_type} | "
                  f"{cfg.sign_condition} | {cfg.sensor_condition}")
        verdicts.append({
            "testCaseId": f"TC_{tc.id:04d}",
            "scenarioName": f"ODME_TC_{tc.id:04d}",
            "verdict": "GENERATED",
            "detail": detail,
        })
        print(f"  TC_{tc.id:04d}: {detail}")

    generate_manifest(configs, output_dir)
    if verdicts:
        write_verdicts(verdicts, output_dir)

    print(f"\n  Generated {len(configs)} scenarios:")
    print(f"    weather_presets/{output_dir.name}/*.WPR")
    print(f"    flights/{output_dir.name}/*.FLT")
    print(f"    simconnect/{output_dir.name}/*.py")
    print(f"    scenario_manifest.csv")

    # Coverage report
    print_coverage_report(test_cases, tree, params)

    # Step 4: Capture (optional)
    if args.capture:
        dataset_dir = args.dataset_dir or output_dir / "dataset"
        print(f"\n[Step 4] Running MSFS capture session -> {dataset_dir}")

        from capture_runner import run_capture_session
        labels = run_capture_session(
            scenario_dir=output_dir,
            dataset_dir=dataset_dir,
            dry_run=args.dry_run,
            settle_time=3.0 if args.dry_run else 5.0,
        )
        print(f"\n  Dataset: {len(labels)} labeled images in {dataset_dir}/")
    else:
        print(f"\n[Step 4] Skipped (use --capture to run MSFS capture)")
        print(f"  To capture manually:")
        print(f"    python capture_runner.py "
              f"--scenario-dir {output_dir} "
              f"--dataset-dir {output_dir}/dataset "
              f"--dry-run")

    # Summary
    print(f"\n{'=' * 70}")
    print("  Pipeline Complete")
    print(f"{'=' * 70}")
    print(f"  ODD: {len(extract_specializations(tree))} specializations, "
          f"{len(params)} continuous parameters")
    print(f"  Test cases: {len(test_cases)}")
    print(f"  MSFS scenarios: {len(configs)}")
    print(f"  Output: {output_dir}")
    print()
    print("  EASA Traceability:")
    print("    W-3.1: ODD defined via SES with "
          f"{len(extract_specializations(tree))} specialization axes")
    print(f"    W-3.2: {len(params)} parameters traceable to "
          f"ICAO/WMO/CIE/FAA")
    print(f"    W-4.4: {len(test_cases)} test cases via Latin Hypercube "
          f"Sampling")
    if args.capture:
        print(f"    W-3.2: Dataset labels provide full data provenance")
    print()


if __name__ == "__main__":
    main()
