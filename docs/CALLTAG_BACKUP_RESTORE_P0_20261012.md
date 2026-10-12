# CallTag P0: account identity, backup and restore integrity

This patch prevents a login/refresh response missing `profile.ownerId` from inheriting
the previous account's data owner ID. Such a response is rejected before the session
is persisted. Any necessary backward-compatible server refresh must explicitly
supply a trusted owner ID; do not disable the guard to fix a malformed API response.

Owner-v2 encrypted `.ctbackup` creation now first writes the completed encrypted
ZIP/GCM archive into app-private cache, then copies it to the user-selected SAF
document using write-truncate mode. Crypto/ZIP failures do not prematurely open
the user's document. **A SAF provider can still fail mid-copy**; users should
keep an older known-good backup until a newer backup has been tested.

On restore, if both restoring and its automatic rollback fail, the pre-restore
plaintext snapshot is **kept inside app-private no-backup storage** for assisted
recovery rather than deleted silently. The user must not clear app data or uninstall
the app. The snapshot must be handled as sensitive customer data.

No original legacy database is automatically deleted. Foreign account rows remain
blocked by PR #130. Restore SMS review quarantine remains mandatory.

Before any Play rollout: verify genuine device account A→B→A transitions,
missing-owner auth response, session token refresh, damaged SAF target behavior,
backup create/reopen/decrypt, failing restore & successful rollback, failing
restore + failing rollback rescue snapshot, external database/attachment
consistency and background SMS quarantine. GitHub CI does not replace these tests.
