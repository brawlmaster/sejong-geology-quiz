# 1반 알림도우미

과제/숙제를 달력으로 확인하고, 마감 24시간/1시간 전에 푸시 알림을 보내주는 웹앱(PWA) + Python 서버입니다.

- 앱 이름: 1반 알림도우미
- 제작: 1113 노태민 1118 장서안
- 클라이언트: 정적 HTML (PWA), `web/`
- 서버: FastAPI (Python), `server/`

## 빠른 시작 (서버)

1. Python 3.10+ 권장. 패키지 설치:

```bash
cd /workspace/server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
```

2. VAPID 키 생성 (웹 푸시용)

- Node.js가 있다면:
```bash
npx web-push generate-vapid-keys
```
출력된 `publicKey`, `privateKey`를 `.env`에 기입하세요.

- 또는 Python으로도 생성 가능(예: `py-vapid` 패키지 등). 편한 방법을 사용하세요.

3. 환경변수 설정

`server/.env.example`를 복사해 `.env`로 저장하고 값 채우기:

```bash
cp .env.example .env
# MANAGER_PASSWORD, VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY 설정
```

4. 서버 실행

```bash
uvicorn app:app --host 0.0.0.0 --port 8000
```

서버가 실행되면 API는 `http://localhost:8000` 에서 사용 가능합니다.

## 클라이언트 배포 (GitHub Pages 등)

- `web/` 디렉토리 전체를 GitHub Pages에 올리면 됩니다. PWA 설치를 위해 HTTPS가 필요합니다.
- `web/manifest.webmanifest`에서 `start_url`/`scope` 경로를 맞추세요.
- 아이콘(`web/icons/icon-192.png`, `icon-512.png`)을 실제 PNG로 추가하면 설치 아이콘이 예쁘게 보입니다.
- 서버 주소가 다른 도메인이라면, 브라우저 콘솔에서 다음을 실행하여 API 베이스를 저장할 수 있습니다.

```js
localStorage.setItem('apiBaseUrl', 'https://YOUR-SERVER-DOMAIN');
location.reload();
```

## 기능

- 달력: 월별 보기, 일정(과제/숙제) 표시
- 다가오는 일정: 마감 임박 순으로 최대 15개 표시
- 관리자 모드: 우측 상단 버튼 → 비밀번호 `sjsh11131118` 입력 후 일정 추가/편집/삭제
- 알림: 서비스워커 + 웹푸시. 구독/해제 버튼 제공
- PWA: 홈 화면에 설치 가능 (Android에서 독립 앱처럼 동작)

## API 요약

- GET `/api/assignments`: 일정 목록
- POST `/api/assignments` (body: `{ password, title, description?, dueDateTime }`)
- PUT `/api/assignments/{id}` (body: `{ password, title?, description?, dueDateTime? }`)
- DELETE `/api/assignments/{id}` (body: `{ password }`)
- GET `/api/vapid-public-key`
- POST `/api/subscribe` (body: PushSubscription)
- POST `/api/unsubscribe` (body: PushSubscription)
- POST `/api/notify/test` (body: `{ title, body }`)

## 색상/테마

- 기본 메인 컬러: 네이비
- 시스템 다크 모드에서는 검정 + 네이비 조합, 라이트 모드에서는 반대로 가독성 높은 색 조합 적용

## 주의사항

- 공개 저장소에 VAPID 비공개키를 커밋하지 마세요.
- 관리자 비밀번호는 공개된 값이므로 실제 서비스에서는 별도 인증 방식을 권장합니다.