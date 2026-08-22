# Repository Agent Rules

- Read `docs/v1/README.md` before performing V1 implementation work.
- Read the authoritative specifications required by the assigned workstream.
- Treat the documents referenced by `docs/v1/README.md` as authoritative requirements.
- Do not invent requirements when an authoritative specification is silent or ambiguous.
- Respect frozen cross-agent contracts and escalate conflicts instead of silently changing specifications or contracts.
- Runtime frontend must remain HTML, CSS, and vanilla JavaScript unless an authoritative specification explicitly changes that rule.
- Do not introduce frontend runtime dependencies without explicit approval.
- The two rules above bind every path outside `/android`. `/android` is a native Kotlin and Jetpack Compose client authorized by Amendment 6 in `docs/v1/README.md`; read `android/README.md` and the governing specification item before working there.
- The Android client consumes the frozen `ArticleDataset v1` contract read-only. Android work must not modify `pipeline/**`, `config/**`, or the web runtime, and the dependency-approval rules apply to Android dependencies as written.
- Do not introduce external AI APIs, API keys, backend servers, databases, authentication systems, or telemetry unless explicitly approved.
- Avoid unrelated scope and refactoring.
- Tests relevant to modified code must pass before committing.
- Implementation agents must respect owned and forbidden paths defined in their workstream document.
- Commit bounded work and report the full commit SHA.
- Feature agents never self-merge; integration is owned by the supervisor/integration workflow.
- Preserve user work and existing Git history.
