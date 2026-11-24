import { ChatStore, ROOM, CACHE_LIMIT } from './chat-store.js';
import { ChatView, nearBottom } from './chat-view.js';

const $ = id => document.getElementById(id);
const store = new ChatStore(), encoder = new TextEncoder(), timers = new Map();
const view = new ChatView($('messages'), item => sendItem(item));
let csrf, me, socket, registering = false, generation = 0, reconnectTimer, reconnectAttempt = 0;
let unread = 0, loadingOlder = false, draft = '', drawerFocus, historyFailed = false;
const setTheme = theme => {
  document.documentElement.dataset.theme = theme;
  try { localStorage.setItem('chat-theme', theme); } catch { /* Tuy chon luu tru co the bi chan. */ }
};
let theme;
try { theme = localStorage.getItem('chat-theme'); } catch { /* Dung tuy chon thiet bi. */ }
setTheme(theme || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
document.querySelectorAll('.theme').forEach(button => button.onclick = () => setTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'));

async function api(url, options = {}) {
  const response = await fetch('/api' + url, { ...options, headers: { 'Content-Type': 'application/json', ...(csrf ? { [csrf.header]: csrf.token } : {}), ...options.headers } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data.error || (response.status === 401 ? 'Phiên đã hết hạn. Hãy đăng nhập lại.' : 'Yêu cầu chưa thành công. Hãy thử lại.'));
    error.status = response.status;
    throw error;
  }
  return data;
}
async function loadCsrf() { csrf = await api('/csrf'); }
function stopSession() {
  generation++;
  clearTimeout(reconnectTimer);
  for (const timer of timers.values()) clearTimeout(timer);
  timers.clear();
  const previous = socket;
  socket = null;
  previous?.close();
}
function showAuth(reason = '') {
  stopSession();
  drawer(false);
  me = null;
  draft = '';
  $('message').value = '';
  store.clear();
  view.clear();
  unread = 0;
  historyFailed = false;
  $('workspace').hidden = true;
  $('auth').hidden = false;
  $('auth-error').textContent = reason;
  $('login').focus();
}
$('switch-mode').onclick = () => {
  registering = !registering;
  $('form-title').textContent = registering ? 'Bắt đầu câu chuyện mới.' : 'Rất vui được gặp lại.';
  $('form-description').textContent = registering ? 'Tạo tài khoản để gặp mọi người trong Sảnh chung.' : 'Đăng nhập để tiếp tục cuộc trò chuyện.';
  $('submit-login').textContent = registering ? 'Tạo tài khoản →' : 'Đăng nhập →';
  $('switch-description').textContent = registering ? 'Đã có tài khoản?' : 'Chưa có tài khoản?';
  $('switch-mode').textContent = registering ? 'Đăng nhập' : 'Tạo tài khoản';
  $('password').autocomplete = registering ? 'new-password' : 'current-password';
  $('auth-error').textContent = '';
};
$('login-form').onsubmit = async event => {
  event.preventDefault();
  $('submit-login').disabled = true;
  $('auth-error').textContent = '';
  try {
    await loadCsrf();
    const body = JSON.stringify({ login: $('login').value.trim(), password: $('password').value });
    if (registering) await api('/register', { method: 'POST', body });
    me = await api('/login', { method: 'POST', body });
    $('password').value = '';
    await loadCsrf();
    enterChat();
  } catch (error) { $('auth-error').textContent = error.message; }
  finally { $('submit-login').disabled = false; }
};
$('logout').onclick = async () => {
  const session = generation;
  try {
    await api('/logout', { method: 'POST' });
    if (session !== generation) return;
    showAuth();
    await loadCsrf();
  } catch (error) { if (session === generation) $('send-error').textContent = error.message; }
};

function drawer(open) {
  if (open) drawerFocus = document.activeElement;
  $('sidebar').classList.toggle('open', open);
  $('shade').hidden = !open;
  $('menu').setAttribute('aria-expanded', String(open));
  const mobile = matchMedia('(max-width: 760px)').matches;
  $('sidebar').inert = mobile && !open;
  $('chat-main').inert = mobile && open;
  if (open) $('close-drawer').focus();
  else if (drawerFocus) { drawerFocus.focus(); drawerFocus = null; }
}
$('menu').onclick = () => drawer(true);
$('close-drawer').onclick = () => drawer(false);
$('shade').onclick = () => drawer(false);
$('room').onclick = () => drawer(false);
document.addEventListener('keydown', event => {
  if (!$('sidebar').classList.contains('open')) return;
  if (event.key === 'Escape') drawer(false);
  if (event.key === 'Tab') {
    const controls = [...$('sidebar').querySelectorAll('a,button')].filter(node => node.getClientRects().length);
    const first = controls[0], last = controls.at(-1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});
matchMedia('(max-width: 760px)').addEventListener('change', () => drawer(false));

function enterChat() {
  stopSession();
  reconnectAttempt = 0;
  $('auth').hidden = true;
  $('workspace').hidden = false;
  $('account-name').textContent = me.name;
  $('account-login').textContent = '@' + me.login;
  $('account-avatar').textContent = me.name.slice(0, 1).toUpperCase();
  $('message').value = draft;
  drawer(false);
  connect();
}
function status(text, online) {
  $('connection').textContent = text;
  $('connection').classList.toggle('online', online);
  $('reconnect').hidden = online || reconnectAttempt < 8;
  updateComposer();
}
function alertChat(text = '') {
  $('chat-alert-text').textContent = text;
  $('chat-alert').hidden = !text;
}
function connect() {
  if (!me || socket) return;
  clearTimeout(reconnectTimer);
  if (!navigator.onLine) { status('Mất kết nối', false); scheduleReconnect(); return; }
  const session = generation;
  status('Đang kết nối…', false);
  const current = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`);
  socket = current;
  const valid = () => session === generation && socket === current && me;
  current.onmessage = async event => {
    if (!valid()) return;
    let data;
    try { data = JSON.parse(event.data); } catch { return; }
    if (data.type === 'ready') {
      reconnectAttempt = 0;
      status('Đã kết nối', true);
      alertChat();
      try { await syncHistory(session); }
      catch (error) { if (valid()) { historyFailed = true; alertChat('Chưa tải được lịch sử. Bấm Về tin mới để thử lại. ' + error.message); $('new-messages').hidden = false; } }
    }
    if (data.type === 'ping' && current.readyState === 1) current.send(JSON.stringify({ type: 'ping' }));
    if (data.type === 'message' || data.type === 'confirmed') accept(data.message);
    if (data.type === 'error') {
      const item = [...store.pending.values()].find(row => row.clientMsgId === data.clientMsgId);
      if (item) { failItem(item, data.error); alertChat('Một tin chưa được xác nhận lưu. Xem lỗi cạnh tin và dùng Gửi lại.'); }
      else $('send-error').textContent = data.error;
    }
  };
  current.onclose = () => {
    if (!valid()) return;
    socket = null;
    status('Mất kết nối', false);
    alertChat('Mất kết nối. Bản nháp vẫn được giữ; tin đã lưu sẽ được đồng bộ khi nối lại.');
    for (const item of store.pending.values()) failItem(item, 'Chưa nhận xác nhận. Hãy gửi lại.');
    scheduleReconnect();
  };
  current.onerror = () => current.close();
}
function scheduleReconnect() {
  clearTimeout(reconnectTimer);
  if (!me) return;
  if (++reconnectAttempt > 8) {
    status('Chưa kết nối được', false);
    alertChat('Chưa thể nối lại. Kiểm tra mạng rồi bấm Kết nối lại.');
    return;
  }
  const session = generation;
  reconnectTimer = setTimeout(async () => {
    if (session !== generation || !me) return;
    try { await api('/me'); if (session === generation) connect(); }
    catch (error) {
      if (session !== generation) return;
      if (error.status === 401) showAuth(error.message);
      else scheduleReconnect();
    }
  }, Math.min(10000, 1000 * 2 ** (reconnectAttempt - 1)));
}
$('reconnect').onclick = () => { reconnectAttempt = 0; connect(); };

function render({ bottom = false, anchor } = {}) {
  if (!me) return;
  const timeline = $('timeline');
  view.render(store.list(), me, item => store.key(item));
  $('room-welcome').hidden = store.saved.size > 0 || store.pending.size > 0;
  const oldest = Math.min(...[...store.saved.values()].map(item => item.msgId));
  $('load-older').hidden = !store.saved.size || oldest <= 1;
  if (anchor?.node.isConnected) timeline.scrollTop += anchor.node.getBoundingClientRect().top - anchor.top;
  if (bottom) timeline.scrollTop = timeline.scrollHeight;
  $('new-messages').hidden = !historyFailed && (bottom || (unread === 0 && store.latest && nearBottom(timeline)));
  $('new-messages').textContent = unread ? `${unread} tin mới · Về cuối ↓` : 'Về tin mới ↓';
}
function readingAnchor() {
  const top = $('timeline').getBoundingClientRect().top;
  const node = [...$('messages').querySelectorAll('.message-row')].find(item => item.getBoundingClientRect().bottom > top);
  return node ? { node, top: node.getBoundingClientRect().top } : undefined;
}
async function latestPage(session = generation) {
  const page = await api('/history/page');
  if (session !== generation || !me) return;
  store.saved.clear();
  store.latest = true;
  for (const item of page) { store.put(item); clearItemTimer(item); }
  store.cursor = page.at(-1)?.msgId || 0;
  store.initialized = true;
  historyFailed = false;
  if (![...store.pending.values()].some(item => item.state === 'failed')) alertChat();
  unread = 0;
  render({ bottom: true });
}
async function syncHistory(session) {
  if (!store.initialized) await latestPage(session);
  if (session !== generation || !me) return;
  let page;
  do {
    page = await api('/history?after=' + store.cursor);
    if (session !== generation || !me) return;
    const bottom = nearBottom($('timeline')) && store.latest, anchor = readingAnchor();
    for (const message of page) {
      if (!bottom && store.saved.size >= CACHE_LIMIT && !store.saved.has(store.key(message))) store.latest = false;
      store.put(message);
      clearItemTimer(message);
      store.cursor = message.msgId;
    }
    store.trim();
    render({ bottom, anchor });
  } while (page.length === 50);
}
$('load-older').onclick = async () => {
  if (loadingOlder) return;
  loadingOlder = true;
  $('load-older').disabled = true;
  const session = generation;
  try {
    const oldest = Math.min(...[...store.saved.values()].map(item => item.msgId));
    const page = await api('/history/page?before=' + oldest);
    if (session !== generation || !me) return;
    const anchor = readingAnchor();
    for (const item of page) store.saved.set(store.key(item), item);
    store.trim(true);
    render({ anchor });
  } catch (error) { if (session === generation) alertChat(error.message); }
  finally { loadingOlder = false; $('load-older').disabled = false; }
};
function accept(message) {
  if (message.roomId !== ROOM) return;
  const bottom = nearBottom($('timeline')) && store.latest, anchor = readingAnchor();
  clearItemTimer(message);
  if (!bottom && store.saved.size >= CACHE_LIMIT && !store.saved.has(store.key(message))) store.latest = false;
  const fresh = store.put(message);
  if (fresh && !bottom && message.userId !== me.id) unread++;
  store.trim();
  render({ bottom, anchor });
  if (!historyFailed && ![...store.pending.values()].some(item => item.state === 'failed') && socket?.readyState === 1) alertChat();
  if (fresh && message.userId !== me.id) $('announcer').textContent = `Tin mới từ ${message.name}`;
}
$('new-messages').onclick = async () => {
  try { await latestPage(); } catch (error) { alertChat(error.message); }
};
$('timeline').onscroll = () => {
  if (!historyFailed && nearBottom($('timeline')) && store.latest) { unread = 0; $('new-messages').hidden = true; }
};

function updateComposer() {
  const input = $('message'), bytes = encoder.encode(input.value).length;
  $('bytes').textContent = `${bytes} / 1000 byte`;
  $('bytes').hidden = bytes < 800;
  $('bytes').classList.toggle('error', bytes > 1000);
  $('limit-error').textContent = bytes > 1000 ? 'Tin vượt 1000 byte UTF-8. Hãy rút ngắn hoặc tách thành nhiều tin.' : '';
  input.setAttribute('aria-invalid', String(bytes > 1000));
  $('send').disabled = socket?.readyState !== 1 || !input.value.trim() || bytes > 1000;
  input.style.height = 'auto';
  input.style.height = Math.min(130, input.scrollHeight) + 'px';
}
$('message').oninput = () => { draft = $('message').value; updateComposer(); };
$('message').onkeydown = event => {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
    event.preventDefault();
    if (!$('send').disabled) $('compose-form').requestSubmit();
  }
};
function messageId() {
  return btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(16)))).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}
function clearItemTimer(item) {
  const key = store.key(item);
  clearTimeout(timers.get(key));
  timers.delete(key);
}
function failItem(item, error) {
  clearItemTimer(item);
  item.state = 'failed';
  item.error = error;
  render({ anchor: readingAnchor() });
}
function sendItem(item) {
  if (socket?.readyState !== 1) { failItem(item, 'Chờ kết nối được khôi phục rồi gửi lại.'); return; }
  const key = store.key(item), session = generation;
  clearItemTimer(item);
  item.state = 'pending';
  item.error = '';
  store.pending.set(key, item);
  render({ bottom: true });
  try { socket.send(JSON.stringify({ type: 'send', roomId: item.roomId, clientMsgId: item.clientMsgId, text: item.text })); }
  catch { failItem(item, 'Chưa gửi được. Hãy thử lại khi có kết nối.'); return; }
  timers.set(key, setTimeout(() => {
    timers.delete(key);
    if (session === generation && store.pending.get(key)?.state === 'pending') failItem(item, 'Chưa nhận xác nhận. Hãy gửi lại.');
  }, 5000));
}
$('compose-form').onsubmit = async event => {
  event.preventDefault();
  if ($('send').disabled) return;
  if (store.pending.size >= 100) { $('send-error').textContent = 'Hãy xử lý các tin chưa gửi trước.'; return; }
  const item = { roomId: ROOM, text: $('message').value, clientMsgId: messageId(), timestamp: Date.now(), name: me.name, userId: me.id };
  const session = generation;
  if (!store.latest) {
    try { await latestPage(session); } catch (error) { $('send-error').textContent = error.message; return; }
    if (session !== generation) return;
  }
  $('send-error').textContent = '';
  sendItem(item);
  $('message').value = '';
  draft = '';
  updateComposer();
  $('message').focus();
};
addEventListener('offline', () => socket?.close());
addEventListener('online', () => { if (me && !socket) { reconnectAttempt = 0; connect(); } });
setInterval(async () => {
  if (!me) return;
  const session = generation;
  try { await api('/me'); }
  catch (error) { if (session === generation && error.status === 401) showAuth(error.message); }
}, 25000);
// VisualViewport giup composer theo phan man hinh con lai khi ban phim ao mo.
function viewportHeight() {
  const zoom = Number.parseFloat(getComputedStyle(document.body).zoom) || 1;
  document.documentElement.style.setProperty('--viewport-height', `${(window.visualViewport?.height || innerHeight) / zoom}px`);
}
window.visualViewport?.addEventListener('resize', viewportHeight);
addEventListener('resize', viewportHeight);
new MutationObserver(viewportHeight).observe(document.body, { attributes: true, attributeFilter: ['style'] });
new ResizeObserver(() => {
  $('chat-main').style.setProperty('--composer-height', `${document.querySelector('.composer-area').offsetHeight}px`);
}).observe(document.querySelector('.composer-area'));
viewportHeight();
try { await loadCsrf(); me = await api('/me'); enterChat(); }
catch (error) { if (error.status !== 401) $('auth-error').textContent = error.message; }
