"""Export helpers for ODME data."""

import csv
import json
from io import StringIO
from pathlib import Path

from .model import Parameter, TestCase


def to_csv(
    test_cases: list[TestCase], parameters: list[Parameter]
) -> str:
    """Export test cases as a CSV string.

    The format matches LatinHypercubeSampler.toCsv() in ODME:
    TestCase_ID, ParentNode.paramName, ...
    """
    output = StringIO()
    writer = csv.writer(output)
    header = ["TestCase_ID"] + [p.qualified_name for p in parameters]
    writer.writerow(header)
    for tc in test_cases:
        row = [tc.id] + [tc.values.get(p.qualified_name, "") for p in parameters]
        writer.writerow(row)
    return output.getvalue()


def to_dataframe(test_cases: list[TestCase], parameters: list[Parameter]):
    """Convert test cases to a pandas DataFrame.

    Requires pandas to be installed (pip install odme[pandas]).
    """
    import pandas as pd

    rows = []
    for tc in test_cases:
        row = {"TestCase_ID": tc.id}
        for p in parameters:
            row[p.qualified_name] = tc.values.get(p.qualified_name)
        rows.append(row)
    return pd.DataFrame(rows)


def write_verdicts(verdicts: list[dict], output_dir: Path) -> Path:
    """Write results.json for ODME to import.

    Each verdict dict should have keys:
        testCaseId, scenarioName (optional), verdict, detail (optional)

    Returns the path to the written file.
    """
    result = {"version": "1.0", "verdicts": verdicts}
    path = output_dir / "results.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    return path
