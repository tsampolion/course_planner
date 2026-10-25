# In-app updates

Status: shipped in 4.1.0 (versions written `0.X.Y.Z` from 0.4.1.1; the first four-part build is installed by hand).

## Why

Course Pacer is not on the Play Store. The owner sets up colleagues' phones once;
after that, they can find and install updates themselves.

## What the app relies on

Every installed copy checks `releases/latest` in the public repository
`tsampolion/course_planner`. A release has tag `v0.X.Y.Z`, exactly one asset named
`course-pacer-0.X.Y.Z.apk` signed with the permanent release key, and its matching
changelog section as the release body. Renaming the asset, changing the tag format,
or making the repository private silently stops in-app updates for installed copies.

## Behaviour

With automatic checks on (the default), launch checks at most once a day after the
last successful check. Failed requests do not move that timestamp; another launch
can try again. Settings → Updates also offers **Check now**, regardless of the
switch or the daily limit.

A newer release adds a dot to ☰ and an **Update** tag to Settings in the menu.
The first section of Settings shows its version and a short preview of the changes.
**Download** opens the APK in the browser; Android then installs it over the current
release, keeping school years and settings. **See all changes** opens the release
page. There is nothing to dismiss: the notice clears when the installed version
catches up. Debuggable builds never check and hide the Updates section.

## Privacy

The only network use is one GET to `api.github.com` for the latest release, with
the app version in the User-Agent. No information about the teacher or school years
is sent. Turn automatic checks off in Settings to stop launch requests.
The app's only permission is `INTERNET`.

## Setting up a colleague's phone

1. Open [Releases](https://github.com/tsampolion/course_planner/releases/latest) in the phone's browser.
2. Download the latest `course-pacer-0.X.Y.Z.apk`.
3. Allow installs from that browser if Android asks.
4. Install the APK and open Course Pacer.

After that, available updates appear in Settings → Updates. Downloading and
installing a release over an existing release keeps the phone's school years.

## A phone running a locally built copy

If Android says “App not installed” when replacing a locally built copy with a
published release, the local copy is debug-signed. Export the school years first
(Settings → Back up), uninstall the local copy, install the release, then import
the exported file. Uninstalling deletes the local app's data.

## Not built

In-app downloading or installation, notifications, background checks, pre-release
channels, and skipping a version.
