# CallTag server controlled operation (v1)

This is **not** remote code execution or an over-the-air APK patch system.
It is an HTTPS-hosted, versioned, restrict-only policy for capabilities already
implemented in the Android binary. **Android permission, call/SMS transport,
database schema and native UI changes still require a Play update.**

## Endpoint and server-only edit

A Cloudflare Pages deployment of this repository must include `_worker.js`,
`worker/mobile-policy.mjs`, and `remote-config/calltag-v1.json`.
The mobile app GETs:

`https://calltag.pagero.kr/api/mobile-config/v1`

The Pages worker reads the checked-in JSON asset, validates it and responds
`cache-control: no-store`. No auth token or personal data is transmitted by
the mobile fetcher. **Do not assume the endpoint is live until Pages is
deployed and an HTTP 200 JSON payload is verified.**

To tighten an existing capability without updating Android:

1. Set the corresponding JSON flag to `false` in `remote-config/calltag-v1.json`.
2. **Increase `revision` by at least one** (monotonically; never reuse an
   existing revision). Leave `schemaVersion` at 1.
3. Review the change, deploy the Cloudflare Pages production branch, and
   verify the public GET response and headers.
4. An installed app with this code polls on launch and no more than once
   per minute while the app is foregrounded. A freshly dispatched SMS can
   complete before a remote off-state is received; this is not a transactional
   cancellation of already-sent carrier requests.
5. Roll back by making a **new higher revision** with the flag set to true or a server minimum set to 0.
   Rolling back the server to an older revision will not unblock the app.

Flags: `smsSending` (blocks SMS sender), `automaticSms` (blocks auto
call-trigger enqueue), `mmsCompose` (blocks MMS composer/notification),
`leadImport` (blocks PageRo and Universal lead polling/import), and
`cloudSync` (blocks CRM sync). A `true` remote flag does **not** override
the customer's opt-out, device permissions, subscription entitlements,
restored-message quarantine, maintenance gate or privacy checks.

Since revision 2 the same response may also include **automationLimits**:
`minCooldownHours` (0–720), `minLeadDelayMinutes` (0–1440) and
`minFollowupDays` (0–30). The server can only **lengthen** the user's own
dedupe/automatic delay limits; it cannot shorten, opt in, or overwrite
preferences. See `docs/CALLTAG_REMOTE_AUTOMATION_LIMITS.md`.

Client behavior: only schema 1, integer revision >= 1, all five boolean
flags, and valid optional minimums are accepted. Lower revisions, malformed
JSON, bad HTTP status, timeouts and network failure retain the last atomically
stored configuration. Equal revisions are ignored **except** once when a
client upgrades from a previous version that cached that revision before
the new minimums were implemented. On a new installation, flags default to true
until a valid policy is obtained; thus a server emergency switch cannot
guarantee zero sends on an offline, never-fetched installation.

Security: the endpoint is intentionally public and contains no secrets,
personal data, scripts, expressions, or arbitrary runtime rules. Clients
do not execute JSON, and servers cannot bypass an Android permission or
entitlement. Use deployment review and protected branches for revisions.

Release gates: Android physical device smoke test, policy endpoint live GET,
SMS off and re-enable, auto call SMS opt-out, in-flight lead import and
sync, restart/offline cached restrictions, monotonic revision rollback.
Do not claim Play release readiness from CI alone.
