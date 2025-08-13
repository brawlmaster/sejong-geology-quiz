import os
import json
import threading
import time
import uuid
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional

from fastapi import FastAPI, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field
from pywebpush import webpush, WebPushException

DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
ASSIGNMENTS_FILE = os.path.join(DATA_DIR, 'assignments.json')
SUBSCRIPTIONS_FILE = os.path.join(DATA_DIR, 'subscriptions.json')

APP_NAME = "1반 알림도우미"

# Load .env if present
try:
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

DEFAULT_MANAGER_PASSWORD = os.getenv('MANAGER_PASSWORD', 'sjsh11131118')
VAPID_PUBLIC_KEY = os.getenv('VAPID_PUBLIC_KEY')
VAPID_PRIVATE_KEY = os.getenv('VAPID_PRIVATE_KEY')
VAPID_CLAIMS = {
    "sub": os.getenv('VAPID_SUBJECT', 'mailto:example@example.com')
}

os.makedirs(DATA_DIR, exist_ok=True)
if not os.path.exists(ASSIGNMENTS_FILE):
    with open(ASSIGNMENTS_FILE, 'w', encoding='utf-8') as f:
        json.dump({"assignments": []}, f, ensure_ascii=False, indent=2)
if not os.path.exists(SUBSCRIPTIONS_FILE):
    with open(SUBSCRIPTIONS_FILE, 'w', encoding='utf-8') as f:
        json.dump({"subscriptions": []}, f, ensure_ascii=False, indent=2)

app = FastAPI(title="1반 알림도우미 서버")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# serve client files (../web)
WEB_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'web'))
if os.path.isdir(WEB_DIR):
    app.mount("/web", StaticFiles(directory=WEB_DIR, html=True), name="web")


class Assignment(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    title: str
    description: Optional[str] = None
    dueDateTime: str  # ISO
    createdAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    updatedAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    sentNotifications: Dict[str, bool] = Field(default_factory=lambda: {"24h": False, "1h": False})


class AssignmentCreate(BaseModel):
    password: str
    title: str
    description: Optional[str] = None
    dueDateTime: str


class AssignmentUpdate(BaseModel):
    password: str
    title: Optional[str] = None
    description: Optional[str] = None
    dueDateTime: Optional[str] = None


class PasswordOnly(BaseModel):
    password: str


# Storage helpers

def read_json(path: str) -> Dict[str, Any]:
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def write_json(path: str, data: Dict[str, Any]) -> None:
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


# API endpoints

@app.get('/api/assignments')
def get_assignments():
    data = read_json(ASSIGNMENTS_FILE)
    return data


@app.post('/api/assignments')
def create_assignment(payload: AssignmentCreate):
    if payload.password != DEFAULT_MANAGER_PASSWORD:
        raise HTTPException(status_code=403, detail='잘못된 비밀번호')
    a = Assignment(title=payload.title, description=payload.description, dueDateTime=payload.dueDateTime)
    data = read_json(ASSIGNMENTS_FILE)
    data["assignments"].append(a.dict())
    write_json(ASSIGNMENTS_FILE, data)
    return {"assignment": a.dict()}


@app.put('/api/assignments/{assignment_id}')
def update_assignment(assignment_id: str, payload: AssignmentUpdate):
    if payload.password != DEFAULT_MANAGER_PASSWORD:
        raise HTTPException(status_code=403, detail='잘못된 비밀번호')
    data = read_json(ASSIGNMENTS_FILE)
    found = None
    for idx, item in enumerate(data["assignments"]):
        if item["id"] == assignment_id:
            found = item
            break
    if not found:
        raise HTTPException(status_code=404, detail='일정을 찾을 수 없습니다')
    if payload.title is not None:
        found["title"] = payload.title
    if payload.description is not None:
        found["description"] = payload.description
    if payload.dueDateTime is not None:
        found["dueDateTime"] = payload.dueDateTime
        found["sentNotifications"] = {"24h": False, "1h": False}
    found["updatedAt"] = datetime.now(timezone.utc).isoformat()
    data["assignments"][idx] = found
    write_json(ASSIGNMENTS_FILE, data)
    return {"assignment": found}


@app.delete('/api/assignments/{assignment_id}')
def delete_assignment(assignment_id: str, payload: PasswordOnly):
    if payload.password != DEFAULT_MANAGER_PASSWORD:
        raise HTTPException(status_code=403, detail='잘못된 비밀번호')
    data = read_json(ASSIGNMENTS_FILE)
    orig_len = len(data["assignments"])
    data["assignments"] = [a for a in data["assignments"] if a["id"] != assignment_id]
    write_json(ASSIGNMENTS_FILE, data)
    if len(data["assignments"]) == orig_len:
        raise HTTPException(status_code=404, detail='일정을 찾을 수 없습니다')
    return {"ok": True}


@app.get('/api/vapid-public-key')
def get_vapid_public_key():
    if not VAPID_PUBLIC_KEY:
        raise HTTPException(status_code=500, detail='VAPID_PUBLIC_KEY 미설정')
    return {"publicKey": VAPID_PUBLIC_KEY}


@app.post('/api/subscribe')
def subscribe(subscription: Dict[str, Any] = Body(...)):
    data = read_json(SUBSCRIPTIONS_FILE)
    subs: List[Dict[str, Any]] = data.get("subscriptions", [])
    # prevent duplicates by endpoint
    endpoints = {s.get('endpoint') for s in subs}
    if subscription.get('endpoint') not in endpoints:
        subs.append(subscription)
        data["subscriptions"] = subs
        write_json(SUBSCRIPTIONS_FILE, data)
    return {"ok": True}


@app.post('/api/unsubscribe')
def unsubscribe(subscription: Dict[str, Any] = Body(...)):
    endpoint = subscription.get('endpoint')
    data = read_json(SUBSCRIPTIONS_FILE)
    subs: List[Dict[str, Any]] = data.get("subscriptions", [])
    subs = [s for s in subs if s.get('endpoint') != endpoint]
    data["subscriptions"] = subs
    write_json(SUBSCRIPTIONS_FILE, data)
    return {"ok": True}


@app.post('/api/notify/test')
def send_test(title: str = Body("테스트 알림"), body: str = Body("서버에서 보낸 테스트입니다.")):
    send_push_to_all({"title": title, "body": body, "data": {"url": "./index.html"}})
    return {"ok": True}


# Notification logic

def send_push_to_all(payload: Dict[str, Any]) -> None:
    if not VAPID_PUBLIC_KEY or not VAPID_PRIVATE_KEY:
        print("[WARN] VAPID 키가 설정되지 않아 푸시를 보낼 수 없습니다.")
        return
    data = read_json(SUBSCRIPTIONS_FILE)
    subs: List[Dict[str, Any]] = data.get("subscriptions", [])
    for s in list(subs):
        try:
            webpush(
                subscription_info=s,
                data=json.dumps(payload, ensure_ascii=False),
                vapid_private_key=VAPID_PRIVATE_KEY,
                vapid_claims=VAPID_CLAIMS,
                vapid_public_key=VAPID_PUBLIC_KEY,
            )
        except WebPushException as ex:
            print(f"[WARN] 푸시 실패: {ex}. 만료된 구독 제거")
            # remove invalid subscription
            subs = [x for x in subs if x.get('endpoint') != s.get('endpoint')]
            write_json(SUBSCRIPTIONS_FILE, {"subscriptions": subs})
        except Exception as e:
            print(f"[ERROR] 푸시 중 오류: {e}")


def check_and_send_due_notifications():
    while True:
        try:
            data = read_json(ASSIGNMENTS_FILE)
            changed = False
            now = datetime.now(timezone.utc)
            for item in data.get("assignments", []):
                due = datetime.fromisoformat(item["dueDateTime"])
                if due.tzinfo is None:
                    due = due.replace(tzinfo=timezone.utc)
                remaining = (due - now).total_seconds()

                # 24h before
                if remaining <= 24*3600 and not item.get("sentNotifications", {}).get("24h", False):
                    payload = {
                        "title": "24시간 전 알림",
                        "body": f"내일 마감: {item['title']}",
                        "data": {"url": "./index.html"}
                    }
                    send_push_to_all(payload)
                    item.setdefault("sentNotifications", {})["24h"] = True
                    changed = True

                # 1h before
                if remaining <= 3600 and not item.get("sentNotifications", {}).get("1h", False):
                    payload = {
                        "title": "1시간 전 알림",
                        "body": f"1시간 후 마감: {item['title']}",
                        "data": {"url": "./index.html"}
                    }
                    send_push_to_all(payload)
                    item.setdefault("sentNotifications", {})["1h"] = True
                    changed = True
            if changed:
                write_json(ASSIGNMENTS_FILE, data)
        except Exception as e:
            print(f"[ERROR] 스케줄러 오류: {e}")
        time.sleep(60)


# Start scheduler thread
threading.Thread(target=check_and_send_due_notifications, daemon=True).start()


@app.get('/')
def root():
    return RedirectResponse(url="/web/index.html")