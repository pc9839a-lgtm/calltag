# CallTag v0.44.10 / code88 security hardening

This patch prioritizes attack-surface reduction before new product features.

Planned controls:
- deny cleartext network traffic
- reduce exported component surface
- validate deep links and referral input
- harden Google OAuth callback integrity
- remove obsolete high-risk permissions when no longer required
- audit PendingIntent mutability and explicit destinations
- reject insecure TLS overrides / WebView bridges / world-readable storage
- retain Android Keystore AES-GCM session protection
- keep encrypted backup and FileProvider path restrictions
- add CI security regression contracts
