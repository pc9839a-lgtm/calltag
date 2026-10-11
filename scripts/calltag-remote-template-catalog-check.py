#!/usr/bin/env python3
"""Contract checks for opt-in remote template catalog (not device integration tests)."""
import json
from pathlib import Path
p=Path("app/src/main/java/kr/pagero/calltag")
catalog=json.loads(Path("remote-config/calltag-templates-v1.json").read_text(encoding="utf-8"))
assert catalog["schemaVersion"]==1
assert catalog["revision"]>=1
assert 1<=len(catalog["templates"])<=12
ids=set()
for item in catalog["templates"]:
    assert item["id"] not in ids
    ids.add(item["id"])
    assert len(item["name"])<=60 and 1<=len(item["body"])<=500
    assert item["purpose"] in ("incoming","outgoing","missed","follow_up","general")
client=(p/"CallTagRemoteTemplates.java").read_text(encoding="utf-8")
ui=(p/"MessageTemplateLibraryActivity.java").read_text(encoding="utf-8")
worker=Path("_worker.js").read_text(encoding="utf-8")
for needle in [
    "https://calltag.pagero.kr/api/mobile-templates/v1",
    "if (connection.getResponseCode() != 200) return;",
    "MAX_BYTES = 8192",
    "if (revision <= prefs.getLong(\"revision\",0L)) return;",
    'MessageTemplateEngine.validateTemplate(body)',
    'if (!validation.isValid() || !validation.normalizedTemplate.equals(body))',
    'if (!prefs.edit().putString("catalog",value.toString())',
    'putLong("revision",revision).commit()',
    "public static List<Item> cached(Context context)",
    "public static void refreshAsync(Context context, Runnable onComplete)"
]:
    assert needle in client,needle
assert "MessageTemplateStore.save(this, MessageTemplateStore.Template.create(" in ui
assert '"서버 추천"' in ui
assert '"내 템플릿에 추가"' in ui
assert "MessageTemplateStore.setDefault(" not in ui[ui.index("private void loadRecommendedTemplates"):ui.index("private void showManageDialog")], "remote recommendation MUST NOT auto-activate SMS"
assert "CallTagRemoteTemplates.refreshAsync(this," in ui
assert "serveMobileTemplates(request, env)" in worker
assert worker.index("serveMobileTemplates(request, env)") < worker.index("handlePageroIntegration(request, env)")
assert "SmsSender" not in client and "sendExisting" not in client
print("CallTag template catalog opt-in, validation, cache and no-auto-SMS contracts: PASS")
