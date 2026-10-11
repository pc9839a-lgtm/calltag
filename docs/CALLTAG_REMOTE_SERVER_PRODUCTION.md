# CallTag server-only remote APIs deployment

This branch is a server-only, production-targeted hotfix based on current `main`.
It **does not include or publish Android binary changes** from PRs #121–#128.

Routes:
- `GET /api/mobile-config/v1`: revision 2, existing flags allowed and automation minimums all zero.
- `GET /api/mobile-templates/v1`: public, user-opt-in template suggestions.

Changes to `_worker.js` only intercept these exact routes and otherwise call the existing admin/settlement gateway. Backward compatibility and rate safety tests run on push/PR.

**Deployment mechanics:** merging this PR updates GitHub `main`; Cloudflare Pages will serve it only if the project is connected to GitHub main or deployed separately. A GitHub merge by itself does not prove Pages production deployment. Verify both HTTP routes live with status 200 and content-type JSON, then compare revision values and no-store cache headers.

**Android:** remote controls require a later first client update; Play upload needs a publisher credential and a unique versionCode. The existing build pipeline only builds signed AAB. Do not call this a Play deployment.
