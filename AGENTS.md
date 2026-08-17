# Repository Agent Rules

- Read `docs/v1/README.md` before performing V1 implementation work.
- Treat the documents referenced by `docs/v1/README.md` as authoritative requirements.
- Do not invent requirements when an authoritative specification is silent or ambiguous.
- Escalate ambiguous cross-component contracts to the supervisor.
- Runtime frontend must remain HTML, CSS, and vanilla JavaScript unless an authoritative specification explicitly changes that rule.
- Do not introduce frontend runtime dependencies without explicit approval.
- Do not introduce external AI APIs, API keys, backend servers, databases, authentication systems, or telemetry unless explicitly approved.
- Tests relevant to modified code must pass before committing.
- Implementation agents must respect owned and forbidden paths defined in their workstream document.
- Feature agents do not merge branches; integration is owned by the supervisor/integration workflow.
- Shared contracts must not be changed unilaterally by a feature agent.
- Preserve user work and existing Git history.
