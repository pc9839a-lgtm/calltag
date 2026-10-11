# CallTag — server minimum automatic SMS safety policy

`remote-config/calltag-v1.json` contains `automationLimits` since revision **2**.
Change the JSON and deploy Cloudflare Pages to update already-supported client
binaries without another Play Store release, **after this client build and the
worker have first been deployed**. Increase the global policy `revision` with
each change (including rollback: use a strictly higher revision).

| Key | Unit | Allowed server values | Effect |
| --- | --- | --- | --- |
| `minCooldownHours` | hours | 0–720 | Effective duplicate window is `max(user cooldown, remote min)` |
| `minLeadDelayMinutes` | minutes | 0–1440 | PageRo auto SMS earliest send is `max(user delay, remote min)` |
| `minFollowupDays` | days | 0–30 | Call follow-up auto SMS earliest send is `max(user delay, remote min)` |

All defaults are 0 = **do not add a server restriction**.
This server mechanism never changes customer-preferred values or template text
and never enables automated SMS. Subscription/role, explicit local
`enabled` toggles, SMS permission, SIM checks, business hours, phone exclusion,
and existing SMS remote kill switches continue to be required. An in-flight
carrier send cannot be retracted.

Existing scheduled/READY *automated* SMS jobs check server minimums again
before dispatch; early jobs are re-scheduled rather than sent. Manual and
campaign messages do **not** acquire these automatic delays. Cooldown is used
in the central job dedupe path, with explicit user force-resend retaining its
existing semantics. If strict rate control for ALL SMS jobs is required,
design and implement a separate server-approved per-owner quota engine.

**Account privacy fix:** `calltag_pagero_lead_message` is now an owner-scoped
SharedPreferences file. Existing unowned/global PageRo settings are not silently
assigned to a newly signed-in account. Its `enabled` preference defaults OFF;
user must explicitly reconfigure on upgrade. Owner-v2 encrypted backups include
the new preference; older v2 archives without it restore with OFF.

If the server config is invalid, network is down or revision is stale, Android
keeps the last atomically committed policy. A previously never-connected app
uses zero added minimums. Background polling is best effort, and updates cannot
undo sends already passed to the carrier.

## Pre-release requirements
1. Cloudflare Pages 200 endpoint serving exact revision and limits after deploy.
2. On real device: account A/B isolation, PageRo opt-in OFF on upgrade, manual
   SMS unaffected; user delay 30m + remote min 60m => 60m; user delay 90m + remote
   min 60m => 90m; follow-up analogous; cooldown check; scheduled job after a
   newer remote revision must be deferred; force-kill/restart + offline policy.
3. Existing CRM recovery, backups and media isolation checks from parent PRs.
4. No main merge/Play deployment until all release gates have passed.
