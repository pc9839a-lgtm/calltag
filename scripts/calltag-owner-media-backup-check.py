#!/usr/bin/env python3
"""Media owner isolation, current-owner zip allowlist and file restore regression.

These are source-contract and filesystem fixtures, not Android device tests.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import hashlib, shutil

p=Path("app/src/main/java/kr/pagero/calltag")
def code(name): return (p/name).read_text(encoding="utf-8")
scope=code("AccountDataScope.java")
media=code("MessageAttachmentStore.java")
mms=code("MmsComposer.java")
activity=code("MmsComposeActivity.java")
backup=code("CallTagBackupManager.java")
account=code("AccountActivity.java")
backup_ui=code("BackupRestoreActivity.java")
for x in ["currentImageDirectoryName(Context context)",
          "currentImageRelativePath(Context context)",
          '"calltag_mms_compose_v1"']:
    assert x in scope, x
assert 'AccountDataScope.currentImageRelativePath(context) + "/" + fileName' in media
assert 'AccountDataScope.currentImageRelativePath(context) + "/" + target.getName()' in media
assert 'String prefix = AccountDataScope.currentImageRelativePath(context) + "/"' in media
assert 'if (!value.startsWith(prefix)) return null;' in media
assert 'leaf.matches("[A-Za-z0-9_-]+\\\\.jpg")' in media
assert 'getCanonicalPath()' in media
assert 'deleteCurrentOwnerImages(Context context)' in media
assert 'imageRelativePathForOwner(String verifiedOwnerId)' in scope
assert 'MessageAttachmentStore.deleteCurrentOwnerImages(this)' in account
assert "AccountDataScope.preferenceName(context, PREFS)" in mms
assert 'EXTRA_OWNER_SCOPE' in mms and 'EXTRA_WORK_EPOCH' in mms
assert 'EXTRA_OWNER_SCOPE' in activity and 'EXTRA_WORK_EPOCH' in activity
assert 'AccountDataScope.isRestoreReviewPending(context)' in mms
for item in [
    '"imagesIncluded", true',
    'AccountDataScope.imageRelativePathForOwner(owner)',
    'AccountDataScope.imageRelativePathForOwner(owner)',
    'stats.imageCount = countFiles(imageTarget)',
    'manifest.optBoolean("imagesIncluded", false)',
    'if (restoreImages)',
    'deleteRecursively(ownerImages)',
    'copyDirectory(restoredImages, ownerImages)',
    'isAllowedOwnerPath(context, path)',
    'isAllowedBackupPath(path)',
]:
    assert item in backup, "backup contract: "+item
assert '"files/message_images/owner-[0-9a-f]{40}"' in backup
assert "path.substring(prefix.length()).matches" in backup
assert "File ownerImages = new File(context.getFilesDir(), scopedImages);" in backup
assert "공용 이미지는 보존하지만" in backup_ui
assert "source.execSQL" not in code("LegacyCrmRecoveryManager.java")

# Three roots: one unowned image and two account-specific owners.
with TemporaryDirectory() as tmp:
    root=Path(tmp)/"message_images"
    alice=root/("owner-"+"a"*40)
    bob=root/("owner-"+"b"*40)
    alice.mkdir(parents=True)
    bob.mkdir(parents=True)
    (root/"draft-old.jpg").write_bytes(b"legacy never auto-claimed")
    (alice/"template-aaa.jpg").write_bytes(b"A image")
    (bob/"template-bbb.jpg").write_bytes(b"B image")
    snapshot=Path(tmp)/"stage"/"files"/"message_images"/alice.name
    shutil.copytree(alice,snapshot)
    assert {i.name for i in snapshot.iterdir()}=={"template-aaa.jpg"}
    digest=hashlib.sha256((snapshot/"template-aaa.jpg").read_bytes()).hexdigest()
    assert digest==hashlib.sha256(b"A image").hexdigest()
    # A-only restore/rollback never affects B or unowned legacy image.
    (alice/"template-aaa.jpg").write_bytes(b"changed A")
    shutil.rmtree(alice)
    shutil.copytree(snapshot,alice)
    assert (alice/"template-aaa.jpg").read_bytes()==b"A image"
    assert (bob/"template-bbb.jpg").read_bytes()==b"B image"
    assert (root/"draft-old.jpg").read_bytes()==b"legacy never auto-claimed"
print("CallTag owner-private MMS and image-only owner backup/rollback: PASS")
