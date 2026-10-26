"""Materialize existing Firebase client config from CI secrets, never print values."""
import json
import os
import re
from pathlib import Path

KEYS = ('CALLTAG_FIREBASE_APPLICATION_ID', 'CALLTAG_FIREBASE_API_KEY',
        'CALLTAG_FIREBASE_PROJECT_ID', 'CALLTAG_FIREBASE_SENDER_ID')

def config_from_env(env):
    values = {key: (env.get(key) or '').strip() for key in KEYS}
    missing = [key for key, value in values.items() if not value]
    if missing:
        raise ValueError('Missing Firebase configuration: ' + ', '.join(missing))
    app_id, api_key, project_id, sender_id = (values[key] for key in KEYS)
    if not re.fullmatch(r'1:[0-9]+:android:[a-f0-9]+', app_id):
        raise ValueError('Invalid Firebase Android application ID')
    if not sender_id.isdigit() or app_id.split(':')[1] != sender_id:
        raise ValueError('Firebase sender ID does not match Android application ID')
    if app_id != '1:352819510264:android:4417a574453ae5b289340d':
        raise ValueError('Firebase app differs from the verified 0.44.64 release')
    if not re.fullmatch(r'[a-z][a-z0-9-]{4,28}[a-z0-9]', project_id):
        raise ValueError('Invalid Firebase project ID')
    if not re.fullmatch(r'AIza[A-Za-z0-9_-]{35}', api_key):
        raise ValueError('Invalid Firebase API key format')
    return {
        'project_info': {'project_number': sender_id, 'project_id': project_id},
        'client': [{'client_info': {'mobilesdk_app_id': app_id,
                                   'android_client_info': {'package_name': 'kr.pagero.calltag'}},
                    'api_key': [{'current_key': api_key}],
                    'services': {'appinvite_service': {'other_platform_oauth_client': []}}}],
        'configuration_version': '1',
    }

if __name__ == '__main__':
    target = Path(__file__).resolve().parents[1] / 'app' / 'google-services.json'
    target.write_text(json.dumps(config_from_env(os.environ), indent=2), encoding='utf-8')
    print('Firebase Android client configuration validated and generated (values withheld).')
