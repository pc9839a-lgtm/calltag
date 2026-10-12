# CallTag — Google Play SMS / Call Log rejection remediation (2026-10-12)

## Confirmed rejection
2026-10-09: `Device Automation` declaration rejected because Play could not verify declared critical functionality and Play store listing did not match the requested permission use. This is a **policy rejection**, not signing failure.

## Android changes in this branch
- Initial `READ_CALL_LOG` permission is **not automatically requested** 300ms after view launch. The user must read the explanation and tap the dedicated Call Automation permission button.
- Screen explicitly explains Call Log data used (number, completion, direction/time), automatic CRM follow-up and duplicate prevention, non-default-phone operation.
- Global automatic SMS defaults **OFF** for accounts with no prior explicit selection. Per-scenario triggers are also OFF. Existing user's persisted explicit choices are not overwritten.
- SMS setup visibly explains possible carrier SMS charges and asks for explicit permission after a user enables the master switch and at least one condition. Declining permission does not silently save an enabled automatic sending rule.
- We retain `READ_CALL_LOG` and `SEND_SMS` as they implement existing device behavior. **This is not a removal of restricted permissions and does not guarantee policy approval.**

## Play Store listing — submit manually (do not claim until actually published)
Suggested title: `콜태그 - 통화 후 고객관리·자동문자`
Short description: `통화 후 상담기록과 후속업무를 관리하고, 직접 설정한 조건에 따라 안내문자를 자동 발송합니다.`
Suggested opening text:
```
콜태그는 통화 종료를 기준으로 고객관리 업무와 안내문자 발송을 자동화하는 Android 앱입니다.
통화 후 고객 정보를 확인하고 상담 메모·후속 할 일을 남길 수 있습니다.
사용자가 자동문자를 켜고 통화 후·부재중·후속 예약 조건, 템플릿, 발송 회선, 업무시간을 설정한 경우에만 SMS를 자동 발송합니다.
권한: READ_CALL_LOG는 실제 완료된 통화와 대상 번호, 시간 및 중복 처리 여부를 확인하는 데 사용합니다. SEND_SMS는 사용자가 활성화한 규칙에 따른 SMS 발송에 사용합니다.
사용자는 자동문자 규칙을 끌 수 있고 기본 전화 앱을 변경하지 않습니다.
```
Ensure screenshots and feature graphics show **actual on-device settings and follow-up outcome**; never use mock/invented screens.

## Permission declaration — Device automation exception, if and only if reviewer can reproduce
Use case: `Device automation` only if actual user-configured telephony-to-SMS cross-OS automation meets Google criteria. No guarantee Google will agree.
```
CallTag lets a user configure triggers based on completed incoming/outgoing/missed phone calls and optional follow-up periods. The Android app uses READ_CALL_LOG to resolve a completed call, associate a phone number with a local customer/consultation record and prevent duplicate processing. After a user independently enables the automatic SMS master switch plus a specific call scenario and template, SEND_SMS sends the configured SMS using the device's telephony/SMS service. Default SMS and Phone apps are not changed. The user can turn automations off. We request READ_CALL_LOG only after the user taps to enable call-based CRM automation, and SEND_SMS only after explicit activation of an SMS rule. Please follow the provided video/test-account guide to verify end to end.
```
Do **not** claim `Enterprise CRM` unless there is documented corporate login and employee-only access. That exception does not provide SEND_SMS for CRM alone.

## Mandatory reviewer evidence / reproducibility
1. Provide a Play Console **App access** reviewer test account with valid access to CallTag message features (never place password in this repository). Tell reviewer whether login or subscription blocks the feature.
2. Real device screen recording, including actual screenshots and OS permission prompts: login → call automation opt-in → READ_CALL_LOG → SMS automation OFF by default → enable a connected/missed/follow-up trigger, select template/business hours/SIM → SEND_SMS dialog → real test call ending → local CRM and SMS outcome → disable switch → demonstrate no future automatic sending.
3. Link the real, accessible video URL in the **Permissions Declaration Form**, plus concise exact navigation instructions. Re-record after the patch is actually installed, never from mockups.
4. Update **both** full and short Google Play Store listings and upload real product screenshots. Check Play App Content → Permissions Declaration Form and resubmit truthful answers for all permissions.
5. Only after artifacts, actual UI, and Play Console listing match, create a **production-track** release and submit for review. A GitHub built AAB alone is not a submission or approval.

## Go / No-go
- Source contract and Android build PASS only show compile and static correctness.
- Real-device call and SMS behavior must be confirmed before policy resubmission.
- If Play continues to determine this is not qualified Device Automation, do **not** relabel as Enterprise CRM or fabricate evidence. A separate product decision is needed to remove `READ_CALL_LOG`/`SEND_SMS` and switch to non-sensitive caller screening and user-initiated SMS intents (which changes core functionality).
