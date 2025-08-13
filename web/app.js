/* Config */
const APP_NAME = "1반 알림도우미";
const API_BASE_URL = localStorage.getItem("apiBaseUrl") || ""; // same-origin by default
const MANAGER_PASSWORD = "sjsh11131118"; // 서버에서도 검증함

/* Elements */
const monthLabel = document.getElementById("monthLabel");
const calendarGrid = document.getElementById("calendarGrid");
const prevMonthBtn = document.getElementById("prevMonth");
const nextMonthBtn = document.getElementById("nextMonth");
const upcomingList = document.getElementById("upcomingList");
const managerModeBtn = document.getElementById("managerModeBtn");
const notifySubscribeBtn = document.getElementById("notifySubscribe");
const notifyUnsubscribeBtn = document.getElementById("notifyUnsubscribe");

const eventDialog = document.getElementById("eventDialog");
const eventForm = document.getElementById("eventForm");
const dialogTitle = document.getElementById("dialogTitle");
const eventIdInput = document.getElementById("eventId");
const eventTitleInput = document.getElementById("eventTitle");
const eventDescInput = document.getElementById("eventDesc");
const eventDateInput = document.getElementById("eventDate");
const deleteEventBtn = document.getElementById("deleteEvent");

/* State */
let currentDate = new Date();
let assignments = [];
let isManagerMode = false;
let deferredPrompt = null;

/* Utilities */
function formatYYYYMM(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
}
function formatDateTimeK(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  return `${y}.${m}.${d} ${hh}:${mm}`;
}
function toLocalInputValue(date) {
  const tzOffset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - tzOffset * 60000);
  return local.toISOString().slice(0,16);
}
function parseLocalInputValue(val) {
  // datetime-local 값은 로컬 시간으로 해석되므로 추가 보정 없이 Date로 생성
  return new Date(val);
}

/* API */
async function api(path, options = {}) {
  const base = API_BASE_URL || `${location.origin}`;
  const url = `${base}${path}`;
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'omit',
    ...options,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.headers.get('content-type')?.includes('application/json') ? res.json() : res.text();
}

async function loadAssignments() {
  const data = await api('/api/assignments');
  assignments = data.assignments || [];
  render();
}

async function saveAssignment(payload, isUpdate = false) {
  const body = JSON.stringify({ password: MANAGER_PASSWORD, ...payload });
  if (isUpdate) {
    return api(`/api/assignments/${encodeURIComponent(payload.id)}`, { method: 'PUT', body });
  } else {
    return api('/api/assignments', { method: 'POST', body });
  }
}

async function deleteAssignment(id) {
  return api(`/api/assignments/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    body: JSON.stringify({ password: MANAGER_PASSWORD })
  });
}

/* Calendar Rendering */
function renderCalendar() {
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  monthLabel.textContent = `${year}년 ${month + 1}월`;
  calendarGrid.innerHTML = '';

  const firstDay = new Date(year, month, 1);
  const startDay = firstDay.getDay(); // 0 Sun ... 6 Sat
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const totalCells = Math.ceil((startDay + daysInMonth) / 7) * 7;
  const cells = [];

  for (let i = 0; i < totalCells; i++) {
    const cell = document.createElement('div');
    cell.className = 'calendar-cell';

    const dayNum = i - startDay + 1;
    const date = new Date(year, month, dayNum);
    const dateLabel = document.createElement('div');
    dateLabel.className = 'date-label';

    if (dayNum >= 1 && dayNum <= daysInMonth) {
      dateLabel.textContent = `${dayNum}일`;

      const eventsContainer = document.createElement('div');
      eventsContainer.className = 'events';

      const dayAssignments = assignments
        .filter(a => {
          const due = new Date(a.dueDateTime);
          return due.getFullYear() === year && due.getMonth() === month && due.getDate() === dayNum;
        })
        .sort((a,b) => new Date(a.dueDateTime) - new Date(b.dueDateTime));

      dayAssignments.forEach(a => {
        const chip = document.createElement('div');
        chip.className = 'event-chip';
        chip.dataset.id = a.id;
        chip.innerHTML = `<span class="event-dot"></span><span>${a.title}</span>`;
        chip.addEventListener('click', () => {
          if (isManagerMode) openEditDialog(a);
        });
        eventsContainer.appendChild(chip);
      });

      cell.appendChild(dateLabel);
      cell.appendChild(eventsContainer);

      if (isManagerMode) {
        cell.style.cursor = 'pointer';
        cell.addEventListener('click', (e) => {
          if (e.target.classList.contains('event-chip')) return; // handled above
          openCreateDialog(date);
        });
      }
    } else {
      dateLabel.textContent = '';
      cell.style.visibility = 'hidden';
    }

    calendarGrid.appendChild(cell);
    cells.push(cell);
  }
}

function renderUpcoming() {
  const now = new Date();
  const upcoming = assignments
    .slice()
    .sort((a,b) => new Date(a.dueDateTime) - new Date(b.dueDateTime))
    .filter(a => new Date(a.dueDateTime) >= new Date(now.getTime() - 60*60*1000))
    .slice(0, 15);

  upcomingList.innerHTML = '';
  upcoming.forEach(a => {
    const li = document.createElement('li');
    li.className = 'upcoming-item';
    const due = new Date(a.dueDateTime);
    const leftMs = due - now;
    const hoursLeft = Math.floor(leftMs / (1000*60*60));
    const timeText = `${formatDateTimeK(due)} (약 ${hoursLeft}시간 남음)`;

    const left = document.createElement('div');
    left.innerHTML = `<div class="item-title">${a.title}</div><div class="item-due">${timeText}</div>`;

    const right = document.createElement('div');
    if (isManagerMode) {
      const editBtn = document.createElement('button');
      editBtn.className = 'btn secondary';
      editBtn.textContent = '편집';
      editBtn.addEventListener('click', () => openEditDialog(a));
      right.appendChild(editBtn);
    }

    li.appendChild(left);
    li.appendChild(right);
    upcomingList.appendChild(li);
  });
}

function render() {
  renderCalendar();
  renderUpcoming();
}

// 서버 연결 실패 시에도 로컬에서 추가한 일정이 바로 보이도록(옵션)
// 필요 시, 임시 캐시를 두고 서버 저장 실패 시 사용자에게 알림으로 안내하는 전략도 고려 가능

/* Dialogs */
function openCreateDialog(date) {
  dialogTitle.textContent = '일정 추가';
  eventIdInput.value = '';
  eventTitleInput.value = '';
  eventDescInput.value = '';
  const defaultDate = new Date(date);
  defaultDate.setHours(18, 0, 0, 0);
  eventDateInput.value = toLocalInputValue(defaultDate);
  deleteEventBtn.hidden = true;
  eventDialog.showModal();
}

function openEditDialog(assignment) {
  dialogTitle.textContent = '일정 편집';
  eventIdInput.value = assignment.id;
  eventTitleInput.value = assignment.title;
  eventDescInput.value = assignment.description || '';
  eventDateInput.value = toLocalInputValue(new Date(assignment.dueDateTime));
  deleteEventBtn.hidden = false;
  eventDialog.showModal();
}

// 닫기(X) 버튼 동작
document.querySelector('.dialog-close')?.addEventListener('click', (e) => {
  e.preventDefault();
  e.stopPropagation();
  eventDialog.close('close-button');
});

eventForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = eventIdInput.value || undefined;
  const payload = {
    id,
    title: eventTitleInput.value.trim(),
    description: eventDescInput.value.trim(),
    dueDateTime: parseLocalInputValue(eventDateInput.value).toISOString(),
  };
  if (!payload.title) return;
  try {
    await saveAssignment(payload, Boolean(id));
    // 최신 데이터 반영 전, 현재 월을 그대로 유지하며 즉시 렌더
    await loadAssignments();
    eventDialog.close();
    alert('일정이 저장되었습니다.');
  } catch (err) {
    alert('저장 실패: ' + err.message);
  }
});

deleteEventBtn.addEventListener('click', async () => {
  const id = eventIdInput.value;
  if (!id) return;
  if (!confirm('정말 삭제하시겠습니까?')) return;
  try {
    await deleteAssignment(id);
    await loadAssignments();
    eventDialog.close();
  } catch (err) {
    alert('삭제 실패: ' + err.message);
  }
});

// 취소 버튼으로 항상 닫히도록 보장
const cancelEventBtn = document.getElementById('cancelEvent');
cancelEventBtn.addEventListener('click', (e) => {
  e.preventDefault();
  e.stopPropagation();
  eventDialog.close('cancel');
});

// 바깥(백드롭) 클릭 시 닫기
eventDialog.addEventListener('click', (e) => {
  if (e.target === eventDialog) {
    e.preventDefault();
    eventDialog.close('backdrop');
  }
});

// Esc 키로 닫기 보강
eventDialog.addEventListener('cancel', (e) => {
  e.preventDefault();
  eventDialog.close('esc');
});

/* Navigation */
prevMonthBtn.addEventListener('click', () => {
  currentDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1);
  render();
});
nextMonthBtn.addEventListener('click', () => {
  currentDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1);
  render();
});

/* Manager Mode */
managerModeBtn.addEventListener('click', () => {
  if (!isManagerMode) {
    const pwd = prompt('관리자 비밀번호를 입력하세요');
    if (pwd !== MANAGER_PASSWORD) {
      alert('비밀번호가 올바르지 않습니다.');
      return;
    }
    isManagerMode = true;
    managerModeBtn.textContent = '관리자 모드 해제';
  } else {
    // 관리자 모드 해제 시: 서비스워커 해제 + 모든 캐시 삭제 + 강제 새로고침(캐시 무력화)
    const hardReload = () => {
      const u = new URL(location.href);
      u.searchParams.set('r', Date.now().toString());
      location.replace(u.toString());
    };
    try {
      const clearCaches = () => (self.caches ? caches.keys().then(names => Promise.all(names.map(n => caches.delete(n)))) : Promise.resolve());
      if (navigator.serviceWorker) {
        navigator.serviceWorker.getRegistrations()
          .then(regs => Promise.all(regs.map(r => r.unregister())))
          .catch(() => {})
          .finally(() => {
            clearCaches().finally(hardReload);
          });
        return;
      }
      clearCaches().finally(hardReload);
      return;
    } catch {
      hardReload();
      return;
    }
  }
  render();
});

/* PWA install */
const installBtn = document.getElementById('installBtn');
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
  installBtn.hidden = false;
});
installBtn?.addEventListener('click', async () => {
  if (!deferredPrompt) return;
  deferredPrompt.prompt();
  await deferredPrompt.userChoice;
  deferredPrompt = null;
  installBtn.hidden = true;
});

/* Service Worker */
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js');
  });
}

/* Push Notifications */
async function getVapidPublicKey() {
  try {
    const res = await api('/api/vapid-public-key');
    return res.publicKey;
  } catch (e) {
    console.warn('VAPID 키를 불러오지 못했습니다. 같은 도메인 서버가 필요합니다.', e);
    return null;
  }
}

async function subscribePush() {
  if (!('serviceWorker' in navigator)) return alert('서비스 워커를 지원하지 않는 브라우저입니다.');
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return alert('알림 권한이 거부되었습니다.');

  const registration = await navigator.serviceWorker.ready;
  const vapidKey = await getVapidPublicKey();
  if (!vapidKey) return;

  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(vapidKey)
  });
  await api('/api/subscribe', { method: 'POST', body: JSON.stringify(subscription) });
  alert('알림 신청 완료!');
  updateNotifyButtons();
}

async function unsubscribePush() {
  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.getSubscription();
  if (!subscription) {
    updateNotifyButtons();
    return;
  }
  await subscription.unsubscribe();
  await api('/api/unsubscribe', { method: 'POST', body: JSON.stringify(subscription) });
  alert('알림 취소 완료!');
  updateNotifyButtons();
}

notifySubscribeBtn.addEventListener('click', async () => {
  await subscribePush();
});
notifyUnsubscribeBtn.addEventListener('click', async () => {
  await unsubscribePush();
});

async function updateNotifyButtons() {
  if (!('serviceWorker' in navigator)) return;
  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.getSubscription();
  const isOn = Boolean(subscription);
  notifySubscribeBtn.hidden = isOn;
  notifyUnsubscribeBtn.hidden = !isOn;
}

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

/* Init */
(async function init() {
  try {
    await loadAssignments();
  } catch (e) {
    console.warn('일정을 불러오지 못했습니다. 서버가 필요합니다.', e);
  }
  await updateNotifyButtons();
})();