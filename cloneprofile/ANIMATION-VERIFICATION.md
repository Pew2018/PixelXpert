# Animation and UI scheduling revision

Base: main `406d2db8cf5a7082f39c7b9ed29eeb660ab2fb18` (the prior classic UI refinement).
Only `cloneprofile` is changed. Its existing layout, colors, text hierarchy,
system-bar handling, package ID, SDK levels, Hook and IPC remain intact.

## Implemented behavior

- `PageNavigator`: small FrameLayout host replaces ViewFlipper. Existing real
  ScrollViews remain measured and retain scroll state. Incoming/outgoing pages
  use complementary alpha and 20dp translations over 220ms, FastOutSlowIn-style
  interpolation, mirrored logical direction in RTL. Explicit stacking and opaque
  page backgrounds prevent the outgoing page from disappearing prematurely.
  Repeated destinations settle immediately. Rapid requests finish the committed
  destination before applying the next request; no queued animation chains.
  Touch/key interaction in content is gated during transitions (back still works),
  and offscreen/transient page descendants are excluded from accessibility.
- Predictive back: explicit AndroidX Activity dependency using this repository's
  1.13.0 version; `handleOnBackStarted/Progressed/Cancelled/Pressed` operate the
  same pair of page Views. Preview follows the swipe edge, exposes the correct
  parent (raw → device; logs/device → home), and settles only remaining progress.
  Cancellation restores the same current page without rebuilding its toolbar or
  menu. Toolbar/legacy back uses the normal reverse transition. Home callback
  remains disabled, leaving system back-to-home behavior to Android.
  Progress callbacks require a supporting system (Android 14+); minSdk remains 29.
- `DisclosureMotion`: only the detail block animates height, alpha and arrow for
  200ms. Reversal reads the current rendered values. Available-width measurement,
  natural WRAP_CONTENT completion and width-change settlement avoid stale fixed
  heights. Collapse hides accessibility descendants immediately and sets GONE
  after completion. Initialization/restoration/lifecycle cleanup set final state.
- `HelperApplication`: one process-owned serial executor handles framework binder
  reads and remote SharedPreferences commits. Process startup, service bind and
  user saves request synchronization; display refresh, resume and incoming log
  reports do not write remote configuration. Writes capture both local values;
  superseded queued snapshots are skipped, in-flight writes finish before newer
  writes. Service epochs and revisions reject stale completions. UI-only refresh
  handles can be cancelled without interrupting config writes or in-flight binder
  calls. Local save, sending, send failure and unconfirmed application are distinct.
- `MainActivity`: report-driven UI binds coalesce to one per frame while visible.
  Manual refresh rotates only while the real read task is outstanding, has no
  minimum wait, and is guarded against duplicates. Weak Activity callbacks,
  request generations, cancellation on stop/destroy and animator cleanup prevent
  invalid UI updates. Application-owned saved configuration work survives rotation.
- `LogProjection` + incremental log binding: full stored strings are identities
  (including exact time, package, event and detail). Reuses existing date/event
  Views, inserts/removes only changed items, restores a retained visible anchor,
  and fades only newly added events intersecting the visible viewport for 160ms.
  No animation for initial/offscreen data or mass-clear removals. Original sort,
  raw copy/export and the service's 100-record retention remain unchanged.
- `ClassicMotion`, `integers.xml`, `dimens.xml`: central policy, durations and
  displacement. Disabled animations still normalize visibility, translation,
  opacity, height, touch and accessibility state. Standard switch/Ripple/dialog/
  Snackbar/native scrolling feedback remains unchanged. Toolbar background and
  system bars never participate in content transitions; chrome changes once on
  a committed navigation and stays unchanged during an uncommitted back gesture.

Progress API reference consulted:
https://developer.android.com/guide/navigation/custom-back/support-animations-views

## Checks and limitations

Static checks: XML parsing, Java resource references, full scope/diff review,
and no remaining `HelperApplication.sync()`, `removeAllViews()` or legacy page
animation calls. Existing CI runs lint, unit tests and debug/release assembly.
`LogProjectionTest` covers simultaneous distinct events, raw precision, retained
identity after prepend/pruning, date header reuse, clear and malformed records.
Existing timestamp/candidate-policy tests remain enabled.

No device, emulator or Android SDK is available in this workspace. Compilation
and unit tests are not a measurement of animation quality; no frame-rate or
zero-jank result is claimed. All cases below remain device checks until run.

## Reproducible runtime matrix

Compare the prior debug APK with this revision using the same data/theme/locale.
Use Pixel 8 Pro / Android 16 first, then API 29 for legacy back (not Android 7).

1. Home → logs → home; home → device → home; device → raw → device.
   Compare toolbar back, system button back and predictive gesture commit/cancel.
   Repeat gestures from both edges; test RTL and rapid consecutive back actions.
   Verify home exits via the system and cancelled previews retain toolbar/menu,
   scroll position and raw text selection behavior.
2. Tap disclosure during expansion and collapse, including rapid reversals. Check
   continuous following-group movement, arrow direction, natural final height,
   no forced bottom scroll and TalkBack expanded/collapsed state.
3. Refresh, navigate, return, rotate and background while a real framework read
   is outstanding. Verify spinner stops on completion/cancellation, no duplicate
   refresh, no fixed delay and no stale Activity updates. Toggle rapidly while
   a remote commit is delayed: final remote pair must match the latest local pair.
4. Disconnect/reconnect the framework, including during a write/read. Check stale
   service callbacks cannot reset a newer service, sending failure keeps local
   values, and no report or refresh triggers configuration writes. A retry occurs
   on reconnect or another user save; refresh is deliberately read-only.
5. Add logs while at the top, while reading an older entry, on another page and
   while backgrounded. Check anchor position, stable same-time events, full raw
   detail and newest-first order. Clear all logs and verify immediate empty state.
6. Default and 1.3/1.5 font scales; 360dp short window; landscape/multi-window;
   gesture and three-button navigation; light/dark themes. Check disclosure size,
   old/new page layering, bottom insets, keyboard navigation and TalkBack focus.
7. Default/.5/2/0 system animator and transition scales. Repeat cancellations and
   rapid taps; no stuck alpha, height, blocked input or duplicate completion.
8. For actual fluidity assessment, capture a screen recording and frame timing
   trace on the target device, especially during remote service delays and logs.
