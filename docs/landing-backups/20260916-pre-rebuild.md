# CallTag 랜딩 개편 전 정본 백업

- 기준 시각: 2026-09-16
- 기준 브랜치: `main`
- 기준 커밋: `6a55db63c5ea1ec628c7c6b73c6a2ae0c2f4d27e`
- `index.html` blob SHA: `6771850f90b6b512bbb843ffb347a7d7fba13ff7`
- 기준 파일 크기: 57,755 bytes

이 문서는 랜딩페이지 개편 전 `index.html` 정본을 불변 Git 객체로 고정하는 롤백 포인트다.

## 즉시 롤백

```bash
git show 6a55db63c5ea1ec628c7c6b73c6a2ae0c2f4d27e:index.html > index.html
```

또는 GitHub에서 위 커밋의 `index.html`을 복원한다.

## 개편 규칙

- Android/Play 릴리스 브랜치와 랜딩 작업을 섞지 않는다.
- 앱·웹·API·로그인 로직은 랜딩 개편 때문에 수정하지 않는다.
- 랜딩 개편은 별도 브랜치에서 섹션 단위로 진행한다.
