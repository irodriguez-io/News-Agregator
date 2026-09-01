# Bundled font assets

Two typefaces ship inside the APK as `res/font/` resources, per `docs/v1/06-ui-ux.md` §11.2. They are
**unmodified upstream files** and make **no network request at any point** — neither at build time nor at
runtime. Neither is a Gradle dependency.

`docs/v1/08-security-dependencies.md` §7.1 records why bundling is permitted where a remote web-font
service is not.

| Resource | Family | Axes used | Upstream | Size |
|---|---|---|---|---|
| `res/font/playfair_display_variable.ttf` | Playfair Display | `wght` only (range 400–900) | `google/fonts` `ofl/playfairdisplay/PlayfairDisplay[wght].ttf` | 300,724 B |
| `res/font/roboto_flex_variable.ttf` | Roboto Flex | `wght` only, of 13 available | `google/fonts` `ofl/robotoflex/RobotoFlex[…13 axes…].ttf` | 1,787,292 B |

## Integrity

```text
playfair_display_variable.ttf  sha256  c40f2293766a503bc70cce9e512ef844a4ccb7cbcde792fe2ea31d191917d8d6
roboto_flex_variable.ttf       sha256  9b523f7d82593df0107173849ebb8c817471a1df4b4fb2c3cbf40cfd810c8281
assets/licenses/OFL-PlayfairDisplay.txt  sha256  566be814f8e96e93dfa16101331557eb6b5467e9e03f627c0910fe93ca12300e
assets/licenses/OFL-RobotoFlex.txt       sha256  9cbaed04b20c853f99840efe5dc96956f6f6120ed83a0ade35f9281a2b63e5d0
```

Re-verify against these hashes before replacing either file. A font that no longer matches its hash is a
modified font, which changes the licence obligations below.

## Licence

Both are **SIL Open Font License 1.1**, confirmed from each family's upstream `METADATA.pb` rather than
assumed. The full licence text for each ships in the APK at `assets/licenses/`, which is what OFL 1.1
requires of a redistributor — the APK contains the Font Software, so it must contain the licence.

**Playfair Display carries a Reserved Font Name: `"Playfair Display"`.** Bundling the unmodified file is
unrestricted, but a *modified* copy may not be distributed under that name. Roboto Flex declares no
reserved name.

No UI surface is required to display these licences and none is added; inclusion in the distributed
artifact satisfies the obligation, and adding a licences screen would introduce user-facing strings that
`docs/v1/06-ui-ux.md` §75.2 treats as shared copy.

## Why the full variable files, and not subsets

Roboto Flex carries 13 axes and the type scale in `06-ui-ux.md` §76.1 uses exactly one, `wght`. Instancing
it down to that axis would cut roughly 1.5 MB.

It was rejected: subsetting needs a font-tooling build dependency, which `AGENTS.md` requires explicit
approval for, and it would turn both files into *modified* fonts with the provenance and Reserved-Font-Name
consequences that follow.

**Measured, rather than estimated:** the two files total 2,088,016 B on disk but add only **1,021,072 B to
the debug APK** — 12,570,133 B before, 13,591,205 B after — because they compress in the archive. That is
+8.1%, roughly half what the on-disk sizes suggest. For a client installed with `adb install -r` it is not
worth a new build dependency.

Playfair Display's file carries only the `wght` axis upstream, so no equivalent question arises for it.
