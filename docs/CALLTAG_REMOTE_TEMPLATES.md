# CallTag Remote Template Catalog (v1)

This feature allows marketing/customer service teams to update the **public suggestion catalog** on the Cloudflare Pages server without requiring a new Android APK, once this Android version is released.

### Server operation
Edit `remote-config/calltag-templates-v1.json`. Increase `revision` monotonically. Add or modify up to **12 suggestions**, with unique stable `id` (3-40 lowercase alphanumeric/_/-), `name` <=60 chars, `body` <=500 chars, and `purpose` one of `incoming`, `outgoing`, `missed`, `follow_up`, `general`. Only supported variables are allowed: `{고객명}`, `{전화번호}`, `{통화일자}`, `{통화시간}`, `{내이름}`, `{상호명}`, `{다음일정}`.

The Worker exposes `GET https://calltag.pagero.kr/api/mobile-templates/v1` via `worker/mobile-templates.mjs`, using `no-store`. The endpoint carries no user data or subscription credentials. Deploy Pages, verify a live HTTP 200 body, then confirm client retrieval. It is **not live merely because GitHub contains the code**.

### Client safeguards
The user explicitly taps **서버 추천** in the template library. The client validates the complete downloaded catalog and only caches strictly newer revisions; bad responses/offline retain prior valid suggestions. The user previews a suggestion and taps **내 템플릿에 추가** to create a new, account-local copy. The imported message is never automatically made the default, never sent automatically, never replaces customized text. Subsequent server edits do not modify previously saved private templates.

This is NOT remote executable code and cannot change native screens, permissions, or SMS transport rules. Platform update remains necessary for new app capabilities.

### Release gates
Android real-device mobile dialog and template import test; offline cached catalog test; ensure no automatic SMS or default selection; deployment of Worker + JSON asset verified; account A/B template separation; owner upgrade migrations from previous PRs validated. Do not merge or Play deploy before approval.
