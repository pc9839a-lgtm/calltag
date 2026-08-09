from pathlib import Path

ROOT = Path('app/src/main/java')
patterns = {
    'new AlertDialog.Builder(this)': 'new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)',
    'new AlertDialog.Builder(activity)': 'new AlertDialog.Builder(activity, R.style.Theme_CallTag_Dialog)',
    'new AlertDialog.Builder(getContext())': 'new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)',
    'new AlertDialog.Builder(context)': 'new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)',
}
changed_files = []
counts = {key: 0 for key in patterns}
for path in ROOT.rglob('*.java'):
    value = path.read_text(encoding='utf-8')
    original = value
    for old, new in patterns.items():
        count = value.count(old)
        if count:
            counts[old] += count
            value = value.replace(old, new)
    if value != original:
        path.write_text(value, encoding='utf-8')
        changed_files.append(str(path))
print('themed builders:', counts)
print('changed files:', len(changed_files))
for item in changed_files:
    print(item)
if not changed_files:
    raise SystemExit('No unthemed AlertDialog builders found; expected residual inventory')
