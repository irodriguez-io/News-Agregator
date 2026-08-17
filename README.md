# Intentional Reading

Intentional Reading V1 is a local-first, finite reading queue built with static HTML, CSS, and JavaScript. A bounded Python pipeline generates the validated ArticleDataset consumed by the browser application.

## Local requirements

- Python 3.13
- Node.js 24.x

Install the pinned Python development dependencies in a virtual environment:

```sh
python3.13 -m venv .venv
.venv/bin/python -m pip install -r requirements-dev.txt
```

## Verify and generate

Run the same release gates used by CI:

```sh
.venv/bin/python -m pytest
.venv/bin/python -m pipeline.main --validate-config
.venv/bin/python -m pip_audit -r requirements.txt
npm test
```

Generate the live dataset and assemble the exact allowlisted Pages artifact:

```sh
.venv/bin/python -m pipeline.main
.venv/bin/python scripts/build_pages.py
```

The deployable output is written to `.build/pages` and contains only `index.html`, `css/**`, `js/**`, and `data/articles.json`.

Serve that exact artifact locally before browser acceptance:

```sh
.venv/bin/python -m http.server 4173 --directory .build/pages
```

Then open `http://127.0.0.1:4173/`. Add `?debug=1` to inspect the actual ranking breakdown for the current Discover card.

## Automation

- `.github/workflows/test.yml` runs the Python, configuration, dependency-audit, and JavaScript gates for pull requests and pushes to `main`.
- `.github/workflows/deploy.yml` runs full release gates for pushes/manual runs, performs a bounded pipeline refresh on the six-hour schedule, assembles the same allowlisted artifact, and deploys it to GitHub Pages without committing generated data.
- `.github/dependabot.yml` checks pip, npm, and GitHub Actions dependencies weekly.

The legacy root `script.js` and `style.css` are retained for repository history but are not referenced by the V1 runtime or Pages artifact.
