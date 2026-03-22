"""Command-line argument parsing for ODME Python plugins."""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Optional, Tuple

from . import load_project
from .model import Project


def parse_args(argv=None) -> argparse.Namespace:
    """Parse the standard ODME plugin arguments.

    ODME invokes plugins with:
        python3 script.py --odme-project <json> --output-dir <dir>
    """
    parser = argparse.ArgumentParser(description="ODME Python Plugin")
    parser.add_argument(
        "--odme-project",
        required=True,
        type=Path,
        help="Path to the project_export.json file",
    )
    parser.add_argument(
        "--output-dir",
        required=True,
        type=Path,
        help="Directory for plugin output files",
    )
    return parser.parse_args(argv)


def setup(argv=None) -> Tuple[Project, Path]:
    """Parse arguments, load the project, and return (project, output_dir).

    This is the standard entry point for ODME plugins::

        from odme.cli import setup

        project, output_dir = setup()
        # ... generate files into output_dir ...
    """
    args = parse_args(argv)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    project = load_project(args.odme_project)
    return project, args.output_dir
