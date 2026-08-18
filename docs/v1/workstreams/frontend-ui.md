# Intentional Reading — V1 Frontend UI Workstream

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/workstreams/frontend-ui.md`\
**Workstream type:** Feature implementation\
**Primary branch:** `feat/frontend-ui`\
**Primary ownership:** `index.html`, `css/**`, `js/ui/**`

---

## 1. Mission

Implement the complete V1 presentation layer for Intentional Reading using the approved visual design and frozen semantic contracts.

This workstream owns:

- HTML structure;
- visual design implementation;
- responsive layout;
- Discover card rendering;
- category selector rendering;
- swipe interaction mechanics;
- explicit triage controls;
- Read Later presentation;
- History presentation;
- primary navigation presentation;
- Settings dialog presentation;
- loading/empty/error/degraded states;
- theme presentation;
- accessibility behavior;
- keyboard interaction;
- reduced-motion behavior;
- frontend manual acceptance.

This workstream does **not** own:

- publisher ingestion;
- Article normalization;
- taxonomy;
- base scoring;
- persistence;
- localStorage;
- preference math;
- personalized ranking;
- deck construction;
- GitHub Actions;
- `js/app.js`.

---

## 2. Starting Point

Create this branch/worktree from exactly:

```text
FOUNDATION_SHA
```

Do not branch from:
main
bootstrap SHA
pipeline branch
state-ranking branch
The UI must be independently implementable using mock Article/view-model data.
3. Required Reading
Before implementation, read:
AGENTS.md
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/06-ui-ux.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
docs/v1/workstreams/frontend-ui.md
design-reference/DESIGN.md
Also inspect:
design-reference/intentional-reading-prototype.png
docs/v1/05-personalization-state.md
to understand state semantics and visual intent.
Do not reinterpret state/ranking behavior inside UI code.
4. Owned Paths
This workstream may create or modify:
index.html
css/**
js/ui/**
It may also create non-runtime static assets under an explicitly UI-owned asset directory if needed and approved by the design specification.
Examples:
assets/icons/**
Avoid adding assets unless they provide real value.
5. Forbidden Paths
Do not modify:
script.js
style.css
js/app.js
js/data/**
js/state/**
js/ranking/**
pipeline/**
config/**
tests/pipeline/**
tests/js/**
docs/v1/**
design-reference/**
.github/workflows/**
requirements*.txt
package.json
package-lock.json
Legacy script.js and style.css remain until integration confirms replacements.
Do not delete them from this feature branch.
6. Shared Contract Constraint
The UI must consume the frozen semantic contracts from:
docs/v1/contracts.md
Do not alter:
action names;
navigation destination IDs;
category IDs;
Article field names;
status semantics;
data/state ownership.
If a UI requirement appears incompatible with a frozen contract:
STOP
→ document conflict
→ report supervisor
Do not solve the conflict by embedding state logic in presentation code.
7. Target UI Layout
Implement approximately:
js/ui/
├── discover.js
├── swipe.js
├── read-later.js
├── history.js
├── navigation.js
└── settings.js
Minor additional UI modules are allowed.
Examples:
toast.js
dialog.js
article-card.js
category-selector.js
status.js
theme.js
Do not create a competing application coordinator.
8. index.html Role
index.html is the static application shell.
It should provide:
semantic page structure;
application mount regions;
accessible navigation containers;
Settings dialog container where appropriate;
module entry reference to ./js/app.js;
repository-controlled CSP where approved.
The UI agent may reference js/app.js in markup but must not create or modify the file.
9. Legacy Application Preservation
During this feature branch:
script.js
style.css
remain present.
The new V1 shell may stop referencing them if the new UI shell requires it, but deletion belongs to integration.
If removing their references would leave the branch nonfunctional without js/app.js, that is acceptable because feature branches are subsystem branches, not production branches.
Document this in completion notes.
10. Visual Source of Truth
Use:
design-reference/DESIGN.md
design-reference/intentional-reading-prototype.png
docs/v1/06-ui-ux.md
The prototype provides visual direction.
The specification controls behavior.
Prototype content is illustrative only.
Do not preserve fake publishers, fake article data, or prototype counts.
11. Visual Direction
The UI must feel like:
editorial publication
+
premium reading app
+
restrained tactile triage
It must not feel like:
enterprise dashboard
social-media feed
generic SaaS
Tinder clone
admin console
12. No Article Images
V1 must contain no Article image surface.
Do not create:
thumbnail regions;
fallback images;
image placeholders;
stock-photo cards;
generated imagery.
The card hierarchy is entirely text-driven.
13. Theme Tokens
Implement the exact approved light/dark authored tokens from 06-ui-ux.md / DESIGN.md.
Use CSS custom properties.
Derived values should use:
color-mix(in oklch, ...)
where supported and appropriate.
Do not create a parallel arbitrary palette.
14. System Theme
Support:
light
dark
system
UI/theme rendering should expose a function/interface integration can call with the current appearance state.
system must follow:
prefers-color-scheme
where practical.
The UI module must not persist the appearance setting itself.
15. Typography
Use approved system stacks:
Display
Iowan Old Style
Charter
Palatino Linotype
Georgia
serif
UI
Avenir Next
Avenir
Segoe UI
system-ui
sans-serif
Metadata
SFMono-Regular
Consolas
monospace
Do not load remote fonts.
16. Primary Navigation
Render exactly:
Read Later | Discover | History
Semantic destination IDs remain:
read_later
discover
history
Display labels must not be renamed.
17. Mobile Navigation
At mobile/tablet widths:
Read Later | Discover | History
appears in fixed bottom navigation.
Requirements:
Discover centered;
Discover visually elevated by approximately 7px;
counts visible on Read Later and History;
active destination clear;
minimum usable navigation height 54px;
account for bottom safe-area inset.
18. Desktop Navigation
At approved desktop breakpoint:
>=921px
switch to sticky/top navigation.
Preserve semantic order.
Do not convert the navigation into a left admin sidebar.
19. Settings Entry
Render a:
44px circular gear control
as the secondary Settings entry.
Settings is not a fourth main navigation item.
Each rendered application view must contain exactly one application masthead/header, one `Intentional Reading` masthead identity, and one Settings control. If the prototype PNG appears to repeat the masthead or Settings control, treat it as an illustrative stitching/reference artifact, not a product requirement. The prototype remains authoritative for overall visual intent.
20. Discover Screen
Discover must render one primary decision card.
It may show a restrained offset/background card to indicate a finite queue.
Do not render:
an infinite list;
a masonry feed;
many equal cards;
a scrolling social timeline.
21. Discover View Model
Design rendering so integration can provide conceptually:
{
  article,
  category,
  readLaterCount,
  historyCount,
  generatedAt,
  degraded,
  debug
}
article may be null for:
loading;
empty;
error.
Do not fetch Article data directly inside the UI module.
22. Article Card Fields
Render available:
source
publication age/date
content type
category context
title
excerpt
topic tags
reading time
Do not calculate missing data that the Article contract did not provide.
23. Title
The Article title must:
dominate the card;
use display serif;
wrap naturally;
support long real-world titles;
avoid hard single-line or two-line truncation.
24. Excerpt
Render approximately:
2–4 lines
where present.
If empty:
omit cleanly
Do not display placeholder prose.
25. Tags
Display at most:
5
normal tags.
At very narrow widths, prioritize approximately:
3
Do not infer tags locally.
Use:
tag.label
for display.
26. Content-Type Badge
Render Article:
contentType.label
using approved badge styling.
Do not reclassify content.
27. Reading Time
If:
readingTimeMinutes != null
display concise time.
If null:
omit
Do not compute it in the UI.
28. Publication Age
Render a concise relative age/date when publishedAt exists.
If null:
omit
Do not substitute dataset generation time.
29. Discover Actions
Explicit actions:
Not interested
Read article ↗
Save for later
must emit semantic intents:
dismiss
open
save
UI must not mutate persistent state itself.
30. Action Callback Boundary
Provide callback/event interfaces so integration can subscribe to semantic actions.
Conceptually:
onAction({
  action: "save",
  articleId
})
Exact event implementation may vary.
The semantic action names may not.
31. Read Article
Read Article must:
visibly indicate external navigation;
emit open;
not directly mark Article read;
not directly call localStorage.
Integration owns state processing and external navigation timing.
32. Swipe Module
Implement:
js/ui/swipe.js
using standard browser pointer/touch behavior.
No gesture library.
Return semantic:
dismiss
save
to the UI/integration layer.
33. Swipe Start
Dragging must begin only from non-interactive card surfaces.
Do not hijack pointer input starting on:
button;
link;
category control;
dialog control;
other interactive descendant.
34. Horizontal Intent
Do not aggressively capture vertical scrolling.
Only transition into horizontal card drag after horizontal intent is sufficiently clear.
Mobile vertical scrolling must remain natural.
35. Swipe Threshold
Commit at:
90px horizontal travel
Below threshold:
return card
emit nothing
Do not emit preference/state actions during partial drag.
36. Swipe Left
Visual behavior:
translate left
slight counterclockwise rotation
← Not interested cue
Commit emits:
dismiss
37. Swipe Right
Visual behavior:
translate right
slight clockwise rotation
Save for later → cue
Commit emits:
save
38. Swipe Motion
Default transition:
280ms
cubic-bezier(0.2, 0.8, 0.2, 1)
No bounce.
No reward animation.
39. Persistence Failure Boundary
The UI must support an integration result where a semantic action fails to persist.
In that case:
do not leave the card visually committed;
do not permanently change counts;
surface an accessible error;
restore usable interaction state.
The UI must not assume every emitted action succeeds.
40. Toast
Implement transient status UI with accessible semantics.
Examples:
Saved to Read Later — Undo
Not interested — Undo
Moved to History
Moved back to Read Later
Use:
role=status
aria-live=polite
or equivalent.
41. Undo UI
Visible Undo is available for approximately:
4.5 seconds
after eligible:
save
dismiss
actions.
Undo emits:
undo
The UI must not implement preference reversal itself.
42. Keyboard Shortcuts
Discover supports:
Left Arrow  → dismiss
Right Arrow → save
Z           → undo
Do not fire these shortcuts when focus context makes them unsafe or ambiguous.
Examples:
form input;
dialog controls;
category selector.
43. Visible Controls Always Required
A user must be able to complete the entire application flow without swiping or using keyboard shortcuts.
Gestures and keyboard are accelerators only.
44. Read Later Screen
Render a compact editorial list.
Do not reuse the Discover swipe-card component as the primary Read Later UI.
45. Read Later View Model
Integration should be able to supply:
saved Article records
readLaterCount
historyCount
aggregate reading time
next topic
The UI must not search the current global feed to reconstruct saved items.
46. Read Later Overview
Render approved overview band where applicable:
queue count
known aggregate reading time
next/top topic
If aggregate reading time is unavailable:
omit/show unavailable appropriately
Do not display misleading 0 min.
47. Read Later Rows
Display appropriate:
queue position
saved age
source
category
content type
title
reading time
up to 3 tags
Actions:
Read
Mark read
Remove
Emit:
open
mark_read
remove
48. Read Later Ordering
Render entries in the order supplied by State/Ranking.
Do not re-sort by:
title;
score;
category;
source.
The expected upstream order is savedAt descending.
49. Empty Read Later
Explain briefly that saving worthwhile Discover items builds the queue.
Provide one clear route:
Discover
Do not use guilt-oriented backlog language.
50. History Screen
Render a chronological editorial list.
Expected upstream order:
readAt descending
Do not apply personalization.
51. History Groups
Group presentation by local date:
Today
Yesterday
Earlier
The UI may derive grouping from supplied readAt timestamps.
This is presentation logic, not ranking logic.
52. History Actions
Actions:
Reopen
Mark unread
Emit:
open
mark_unread
Do not directly change Article status.
53. Empty History
Render a restrained neutral empty state.
Do not create:
streak messaging;
achievements;
progress gamification.
54. Navigation Counts
UI accepts counts from integration/state.
Do not maintain independent hidden counters inside DOM components.
Counts must be rerenderable from supplied state.
55. Settings Dialog
Implement accessible compact Settings dialog.
Preferred:
<dialog>
with explicit close control.
Settings includes:
Appearance
Export local data
Import local data
Reset all data
56. Appearance Controls
Render:
Light
Dark
System
and emit:
appearance_change
with exact values:
light
dark
system
Do not persist them directly.
57. Export
Export control emits:
export_data
UI owns presentation/download affordance as coordinated by integration.
Do not invent a separate backup format.
58. Import
UI owns:
file picker;
5 MiB size pre-check;
readable validation-error presentation.
Import emits:
import_data
Emit/hand serialized content to integration/state for structural validation.
Do not parse arbitrary imported JSON into DOM.
59. Reset
Reset control must require explicit confirmation.
The dialog/copy must clearly state that Reset removes:
preferences
Read Later
History
dismissals
local settings
Emit:
reset_data
only after confirmation.
60. Loading State
Render quiet copy conceptually:
Gathering a thoughtful queue…
Use restrained motion.
Do not render animated walls of skeleton cards.
61. Feed Error State
When Discover data cannot load:
clearly state current reading discovery is unavailable;
preserve navigation;
indicate Read Later/History remain available;
provide Retry affordance if integration exposes one.
Do not imply local state loss.
62. Degraded Dataset
When:
degraded = true
Discover still works normally.
May display subtle message:
Some sources were unavailable during the latest refresh.
No alarming full-page failure.
63. No-New-Articles State
When no current Article is eligible:
Use intentional copy conceptually:
Nothing needs your attention right now.
Provide optional Read Later navigation.
Do not:
recycle dismissed items;
recycle History;
show fake filler;
create an endless queue illusion.
64. Debug UI
Support optional debug rendering only when integration supplies debug mode/data.
Use a subordinate disclosure such as:
<details>
May show:
Base
Source preference
Topic preference
Exploration
Final score
Detected tag count
Normal mode must show none of these numeric scores.
65. Debug Must Be Observational
Debug UI must not:
change ordering;
modify state;
fire preferences;
create Article records.
It only renders supplied production ranking data.
66. Accessibility — Semantic HTML
Prefer:
header
nav
main
article
section
button
a
dialog
details
Do not use clickable <div> elements for actions that should be buttons.
67. Accessibility — Focus
Every interactive control must expose clear visible focus.
Do not remove outlines unless replaced by the approved stronger focus style.
Target:
3px accent-derived ring
3px offset
68. Accessibility — Touch Targets
Minimum usable controls:
44px
Round triage controls:
48px
Mobile navigation:
54px minimum height
69. Accessibility — Contrast
Verify:
normal text ≥ 4.5:1
large text/icons ≥ 3:1
in both light and dark themes.
Do not silently change authored base design tokens if they fail.
Escalate a true token conflict.
70. Accessibility — Non-Color Cues
Save/Dismiss state must be understandable through:
text
direction
icon
motion
not color alone.
71. Accessibility — Reduced Motion
Under:
prefers-reduced-motion: reduce
eliminate or effectively minimize:
card rotation;
major transitions;
decorative motion.
State meaning must remain intact.
72. Accessibility — Dialog
Ensure:
semantic modal behavior;
logical initial focus;
close action;
keyboard usability;
sensible focus return after close where practical.
73. Responsive Breakpoints
Implement approved ranges:
320–390
391–920
921–1180
1181+
Do not invent fundamentally different navigation breakpoints without a real browser/layout reason.
74. Required Width Verification
Manually test:
360
390
430
600
768
820
1024
1366
1440
1920
No horizontal page scroll at any required width.
75. Narrow Mobile
At 320–390:
compact padding;
fixed bottom nav;
approximately 3 visible priority tags;
approximately 3 excerpt lines;
triage remains touchable;
title remains readable.
76. Mobile / Tablet
391–920:
single-column layout;
bottom navigation;
up to ~4 excerpt lines;
card fits comfortable gutters;
Settings usable.
77. Desktop
921+:
desktop navigation;
editorial composition;
card remains primary;
contextual side areas may exist;
no dashboard-style metrics rail.
78. Wide Desktop
1181+:
max content width = 1180px
centered.
Do not scale the main card to the entire viewport.
79. Safe Areas
Account for:
env(safe-area-inset-bottom)
where appropriate for fixed mobile navigation.
Page content must not be hidden underneath navigation.
80. Long-Content Stress Cases
Manually test:
very short title;
150–250 character title;
empty excerpt;
null author;
null date;
null reading time;
five tags;
forced-tag-heavy Article;
long source/content-type names.
Optional fields must collapse gracefully.
81. Content Safety
Render Article-derived values using:
textContent
DOM construction
Do not use untrusted Article data with:
innerHTML
outerHTML
insertAdjacentHTML
document.write
82. No Dynamic Code
Do not use:
eval
new Function
string setTimeout
string setInterval
83. External Links
Where direct links are rendered by UI:
HTTP/HTTPS only;
visible external marker;
new tab/window semantics;
noopener noreferrer.
Integration may own actual navigation after applying Open state.
Do not construct publisher URLs.
84. CSP Compatibility
Keep implementation compatible with restrictive policy:
default-src 'self'
script-src 'self'
connect-src 'self'
object-src 'none'
base-uri 'none'
Do not require:
unsafe-eval
inline executable JS
third-party script domains
85. Runtime Dependencies
Use:
HTML
CSS
vanilla JavaScript
Do not add:
React;
Vue;
Svelte;
Angular;
jQuery;
Bootstrap runtime;
Tailwind CDN;
gesture libraries;
icon libraries.
86. Icons
Use restrained:
inline SVG;
Unicode symbols;
CSS shapes;
as appropriate.
Interactive icons require accessible names.
Do not introduce a runtime icon dependency.
87. No Analytics / Telemetry
UI must not send:
swipes
opens
navigation
theme
history
preferences
to any remote service.
No analytics script.
88. No Third-Party Runtime Resources
Do not load:
Google Fonts
CDN JS
CDN CSS
remote icons
tracking pixels
article images
Normal application resources come from the same Pages origin.
89. Mock Data Strategy
Because this branch does not depend on Pipeline or State/Ranking branches, develop against local mock view-model objects.
Mocks should conform to frozen Article contracts.
Do not commit a competing full runtime data source.
Mocks may live inside:
js/ui/
development helper structures only if they are clearly isolated and removable during integration.
Prefer simple test/demo fixture code that does not contaminate production behavior.
90. No app.js Implementation
Do not create:
js/app.js
even if the UI needs orchestration to demo.
If a local UI preview harness is necessary, keep it clearly outside the production composition path and document it.
Prefer implementing render functions callable from a temporary browser console/manual harness.
Integration will wire the modules.
91. UI Module API Design
Expose clean, integration-friendly functions.
Conceptual examples:
renderDiscover(viewModel, handlers)

renderReadLater(viewModel, handlers)

renderHistory(viewModel, handlers)

renderNavigation(viewModel, handlers)

openSettings(viewModel, handlers)
Exact signatures may differ.
Do not require integration to reach into private DOM internals.
92. Render Idempotency
UI render/update functions should support repeated invocation from current application state.
Avoid hidden DOM-only state that integration cannot reconstruct.
Examples of state that should be supplied externally:
active destination
counts
active Article
queue items
History items
selected category
appearance
degraded state
93. Swipe State Exception
Temporary pointer coordinates/drag transforms are legitimately UI-local ephemeral state.
They must not become persistent application state.
Once a swipe action resolves or cancels, the UI should return to a renderable baseline.
94. No Preference Logic
Do not calculate:
+0.45
-0.35
+0.25
anywhere in UI code.
UI knows only semantic actions.
95. No Article Status Mutation
Do not write:
article.status = "saved"
inside UI modules.
State transition result comes back from integration/state and is then rendered.
96. No Ranking Logic
Do not compute:
personalized score;
exploration;
diversity;
deck order.
Render the Article supplied by integration.
97. No Publisher Fetching
Do not:
fetch RSS
fetch Anthropic
fetch Okta
fetch OpenAI
call rss2json
The only eventual frontend data fetch is performed through the Data module/integration.
98. Manual UI Acceptance
Before completion, manually verify:
Discover
Read Later
History
Settings
loading
feed error
degraded feed
no-new-articles
empty Read Later
empty History
debug details
using representative mock data.
99. Interaction Acceptance
Verify:
mouse/touch/pointer swipe
explicit buttons
Left Arrow
Right Arrow
Z Undo
navigation
category selector
Settings dialog
theme controls
where applicable.
100. Theme Acceptance
Manually verify:
Light
Dark
System
including live system mode response where practical.
The UI agent does not need persistent state implementation to verify visual theme switching.
101. Responsive Acceptance
Record results for all ten required viewport widths.
Do not simply state "responsive."
Report explicit widths tested.
102. Reduced Motion Acceptance
Manually test with reduced-motion preference enabled.
Report outcome.
103. Focus / Keyboard Acceptance
Complete a keyboard-only pass covering:
primary navigation;
category selector;
card actions;
Settings;
dialog close;
Read Later actions;
History actions.
Ensure visible focus throughout.
104. Security Review
Before completion inspect UI code for:
innerHTML
insertAdjacentHTML
eval
new Function
javascript:
third-party scripts
remote fonts
Expected result:
NONE
Any exception requires supervisor review.
105. Visual Fidelity Review
Compare implementation against:
DESIGN.md
intentional-reading-prototype.png
Record material differences.
Do not silently decide that a different visual treatment is preferable.
106. No Automated UI Framework Requirement
Do not introduce Playwright/Cypress/Selenium solely for this workstream.
The approved V1 testing strategy uses structured manual browser acceptance unless supervisor later approves browser automation.
107. Contract Conflict Procedure
If required visual behavior genuinely conflicts with frozen contracts/security/accessibility:
stop affected work;
identify exact conflicting sections;
describe required tradeoff;
report supervisor.
Do not edit authoritative specs.
108. Scope Discipline
Do not add:
animations not specified;
social sharing;
article search;
notifications;
recommendation explanations beyond debug;
profile/settings account UI;
reference shelf;
article imagery;
progress streaks;
bookmarks beyond Read Later.
109. Completion Gate
The Frontend UI workstream is complete only when:
index.html V1 shell implemented

css/app.css and required CSS implemented

Discover UI implemented

swipe UI implemented

Read Later UI implemented

History UI implemented

navigation implemented

Settings implemented

loading/empty/error states implemented

Light/Dark/System presentation implemented

keyboard controls implemented

reduced motion implemented

responsive width matrix verified

accessibility checks completed

security sink review completed

no forbidden paths changed

changes committed
110. Completion Report
Report exactly:
Workstream:
Frontend UI

Branch:
feat/frontend-ui

Commit SHA:
<full SHA>

Owned paths changed:
<summary>

Viewport widths tested:
360 / 390 / 430 / 600 / 768 / 820 / 1024 / 1366 / 1440 / 1920

Themes tested:
Light / Dark / System

Keyboard tested:
PASS / FAIL

Swipe/pointer tested:
PASS / FAIL

Reduced motion tested:
PASS / FAIL

Loading/empty/error states:
PASS / FAIL

Settings/dialog:
PASS / FAIL

Unsafe HTML sinks found:
NONE / details

Third-party runtime dependencies:
NONE

Known visual differences from DESIGN.md/prototype:
NONE / details

Shared contract changes:
NONE

Forbidden paths changed:
NONE
If shared contracts or forbidden paths changed, do not declare completion.
111. Commit and Stop
After acceptance passes:
inspect diff;
commit only UI-owned files;
capture full commit SHA;
report results;
stop.
Do not:
merge;
modify js/app.js;
wire State/Ranking;
consume Pipeline branch;
remove legacy root files;
implement GitHub Actions.
Integration owns those steps.
Related Authoritative Documents
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/05-personalization-state.md
docs/v1/06-ui-ux.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
design-reference/DESIGN.md
design-reference/intentional-reading-prototype.png
This workstream implements the approved V1 presentation and interaction system. It must remain independent of persistence, ranking, ingestion, and application composition.
