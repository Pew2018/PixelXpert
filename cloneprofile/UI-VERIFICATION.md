# Clone Helper classic UI refinement

Scope: `cloneprofile` only; application ID `dev.pew2018.pixelcloneprofilehelper`,
entry `.MainActivity`, Java Views / AppCompat / Material Components 1.12.
No SDK levels, Hook logic, IPC, configuration keys, permissions, logging limits,
or PixelXpert `app` sources changed.

## Changes and shared resources

- `MainActivity.java`: 56dp toolbar; explicit single-owner system-bar/cutout
  padding; 16dp bottom breathing room plus real navigation inset; natural list
  heights; compact subordinate status rows; shared group caption for pending
  configuration; readable report summary; reduced log date spacing; inset log
  dividers; raw diagnostic label/value hierarchy; page-specific copy behavior.
- `UiPresentation.java`: display-only local date/time and compact framework
  versions; original timestamps, precision and framework identifiers remain in
  raw diagnostics and export. Invalid dates never become current dates.
- `res/values/colors.xml`, `values-night/colors.xml`: common colors and existing
  night palette. Light secondary text calibrated to #707070 for small text on
  #FAFAFA. Dialog/popup surface white in light mode.
- `res/values/styles.xml`: typography, classic popup/dialog/button styles.
- `res/values/dimens.xml`: margins, minimum heights, indentation, touch and icon
  sizes; `integers.xml`: 200ms disclosure/page transitions and 320ms refresh
  feedback. Layout distances are shared resources rather than screen pixels.
- `res/drawable/pcph_ic_expand.xml`, `pcph_ic_chevron.xml`: centered vector icons;
  disclosure has a separate non-mirrored down arrow for RTL-safe rotation.
- `res/values*/strings.xml`: consistent localized public app name and pending
  configuration wording; internal module/package/protocol names unchanged.

## Animation and behavior

Previously disclosure immediately changed visibility/rotation and ViewFlipper
changed pages without transitions. Now ChangeBounds/Fade moves the status block
and following rows together, the arrow rotates in sync, subpages use an 8dp
fade/translation, and refresh briefly rotates without delaying real work.
System animator enablement and transition scale are respected. Fast repeated
page/disclosure actions finish or replace transitions rather than queueing work.
The original back dispatcher and page hierarchy remain. No new predictive-back
preview implementation is claimed; the existing internal callback only commits
navigation when back is completed.

Binding switches during refresh cannot re-enter save listeners. The two toggles
are **independent in the existing Hook code**, so both remain configurable:
`open_entry` controls availability, `allow_third_party` controls candidates.
No new dependency is invented and neither value is overwritten by the other.
Configuration captions indicate waiting/unconfirmed application, never proof
that the settings process applied the saved values.

## Reproducible device comparison

Use the previous APK and the new CI debug APK on the same device, font, locale,
window size, theme and data; take screenshots and a screen recording of both.
Do not use mock data in release code or infer live Hook success from a channel
connection. Debug upgrade should keep the existing application ID and data.

1. Pixel 8 Pro / Android 16, default display/font, gesture navigation: capture
   home collapsed and expanded, logs, device summary, raw diagnostics, overflow,
   about dialog. Scroll the expanded home/raw/log pages fully to the bottom.
2. Toggle each switch once: one save feedback, correct persistence on re-entry
   and recreation, independent saved values; open system clone settings or its
   existing fallback. Refresh repeatedly; no duplicate writes from binding.
3. Tap disclosure quickly in both directions and navigate home → device → raw
   → device → home. Check coordinated 200ms motion, no scroll-to-top jump,
   correct arrow, keyboard focus, TalkBack switch labels and state announcement.
4. Rotate while scrolled and expanded; revisit each subpage to check scroll and
   expansion restoration. Complete/cancel the system back gesture; compare
   callback behavior to the old APK. Test animation scales 0, .5, 1 and 2.
5. Check missing reports, channel-only reports, actual Hook events, disconnected
   framework, full Vector build string, empty logs and long log details. Dates
   must use device locale/timezone while full copies retain UTC and milliseconds.
6. Device copy must contain that page's summary. Raw copy/export must contain
   fingerprint, full framework ID, scope/Hook/config status and complete event
   data. Select a long raw value, scroll normally, and inspect full event detail.
7. Repeat at approximately 360dp width/short height, font scales 1.3 and 1.5,
   landscape, three-button navigation and dark mode. No overlapping switch
   labels or cut-off dialog actions; last item clears navigation area.
8. Project minimum is API 29: use Android 10 as the oldest supported environment;
   Android 7.1.1 is the visual reference, not a newly supported runtime.

## Validation boundaries

Reference screenshots and source were inspected. Resource XML parsing and
`git diff --check` are local static checks. The existing GitHub Actions job runs
`:cloneprofile:lintDebug :cloneprofile:testDebugUnitTest
:cloneprofile:assembleDebug :cloneprofile:assembleRelease`; the run's result is
reported with delivery. `UiPresentationTest` covers UTC-to-local day rollover,
offsets, legacy local timestamps, invalid dates, and compact/unknown versions.
No Android SDK/emulator or attached device is available in this workspace.
The device matrix above, visual before/after comparison, TalkBack, real service
connection and on-device animation smoothness remain unverified, not passed.
