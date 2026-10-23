# Parameter Coverage Map

Open a project with saved scenarios, then choose **Scenario Manager > Scenarios
List > Estimate Coverage > Parameter Coverage Map**.

## All parameters

Keep the default bandwidth of **5%** and adequacy target of **1**, then click
**Calculate all parameters**. Each numeric parameter gets a heat strip, range,
score, in-range sample count and excluded sample count. The dataset is the saved
project scenario set exposed by the existing coverage adapter.

Bandwidth determines the distance over which a sample contributes support. In
this view it is a percentage of each parameter's range. For a range of 0–10, 5%
means 0.5 units. A triangular kernel fades from full support at the sample value
to zero one bandwidth away. Adequacy determines how much combined local support
is needed for full green. All rows share a fixed 0–1 coverage scale.

The overview uses five equal regions, geometric scoring and a cold threshold of
0.25. These defaults provide a quick overview; engineering judgement should set
bandwidth and adequacy for a meaningful evaluation.

## Advanced: single parameter

Select a parameter and configure edges, kernel, bandwidth, adequacy, cold
threshold and scoring. Click **Calculate map** beside the **?** help button.
Advanced bandwidth is expressed in parameter units. Custom edges must span the
full declared range and be strictly increasing. Region widths affect summaries,
not the distance over which samples spread heat.

Click a region for its metrics and contributing scenario IDs. CSV exports region
metrics, SVG exports a figure, and cold-region copying provides text for further
sampling work. Configuration persistence, categorical coverage, invalid-range
exclusion and explicit dataset partition/version selection are not implemented.
This is parameter-wise marginal representation, not joint coverage of parameter
combinations.

## Upstream comparison and validation (2026-10-23)

Compared against aeronautical-informatics/ODME main at `51feee31`. Local committed
history matched that revision. The submitted changes add the coverage analyzer,
overview and advanced UI, behaviour XML import/export and rendering fixes,
background project loading, ODD YAML export fixes, tests and the updated manual.
Generated example scenarios, modified runtime example data, build outputs,
executables and local Maven caches are excluded.

With ODME_PYTHON configured to an available interpreter, the full Maven suite
ran 588 tests: 587 passed and one failed. The remaining failure is
`ConstraintEvaluatorTest.evaluate_plainBooleanExpression_returnsTrue`.
The same test fails on a pristine snapshot of upstream `51feee31`; the constraint
evaluator and its test are unchanged by this submission.
