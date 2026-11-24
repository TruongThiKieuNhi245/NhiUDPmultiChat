const $ = id => document.getElementById(id);
let csrf, me, socket, registering = false, stopped = false, reconnectDelay = 1000, cursor = 0;
const encoder = new TextEncoder(), saved = new Map(), pending = new Map();
const setTheme = theme => { document.documentElement.dataset.theme = theme; localStorage.setItem('chat-theme', theme); };
setTheme(localStorage.getItem('chat-theme') || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
document.querySelectorAll('.theme').forEach(b => b.onclick = () => setTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'));
async function api(url, options = {}) {
  const response = await fetch('/api' + url, { ...options, headers: { 'Content-Type': 'application/json', ...(csrf ? { [csrf.header]: csrf.token } : {}), ...options.headers } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) { const error = new Error(data.error || (response.status === 401 ? 'Phiên đã hết hạn. Hãy đăng nhập lại.' : 'Yêu cầu chưa thành công. Hãy thử lại.')); error.status = response.status; throw error; }
  return data;
}
async function loadCsrf() { csrf = await api('/csrf'); }
function showAuth(reason = '') {
  stopped = true; socket?.close(); me = null; $('workspace').hidden = true; $('auth').hidden = false; $('auth-error').textContent = reason;
  saved.clear(); pending.clear(); cursor = 0; $('messages').replaceChildren();
}
$('switch-mode').onclick = () => {
  registering = !registering; $('form-title').textContent = registering ? 'Bắt đầu câu chuyện mới.' : 'Rất vui được gặp lại.';
  $('form-description').textContent = registering ? 'Tạo tài khoản để gặp mọi người trong Sảnh chung.' : 'Đăng nhập để tiếp tục cuộc trò chuyện.';
  $('submit-login').textContent = registering ? 'Tạo tài khoản →' : 'Đăng nhập →';
  $('switch-description').textContent = registering ? 'Đã có tài khoản?' : 'Chưa có tài khoản?';
  $('switch-mode').textContent = registering ? 'Đăng nhập' : 'Tạo tài khoản';
  $('password').autocomplete = registering ? 'new-password' : 'current-password'; $('auth-error').textContent = '';
};
$('login-form').onsubmit = async e => {
  e.preventDefault(); $('submit-login').disabled = true; $('auth-error').textContent = '';
  try {
    await loadCsrf();
    const body = JSON.stringify({ login: $('login').value.trim(), password: $('password').value });
    if (registering) await api('/register', { method: 'POST', body });
    me = await api('/login', { method: 'POST', body }); $('password').value = ''; await loadCsrf(); enterChat();
  } catch (error) { $('auth-error').textContent = error.message; }
  finally { $('submit-login').disabled = false; }
};
$('logout').onclick = async () => {
  try { await api('/logout', { method: 'POST' }); showAuth(); await loadCsrf(); }
  catch (error) { $('send-error').textContent = error.message; }
};
function drawer(open) { $('sidebar').classList.toggle('open', open); $('shade').hidden = !open; }
$('menu').onclick = () => drawer(true); $('shade').onclick = () => drawer(false); $('room').onclick = () => drawer(false);
document.addEventListener('keydown', e => { if (e.key === 'Escape') drawer(false); });
function enterChat() {
  stopped = false; $('auth').hidden = true; $('workspace').hidden = false;
  $('account-name').textContent = me.name; $('account-login').textContent = '@' + me.login;
  $('account-avatar').textContent = me.name.slice(0, 1).toUpperCase(); connect();
}
function status(text, online) { $('connection').textContent = text; $('connection').classList.toggle('online', online); $('message').disabled = !online; updateBytes(); }
function connect() {
  if (stopped) return;
  if (!navigator.onLine) { status('Mất kết nối', false); setTimeout(connect, 2000); return; }
  status('Đang kết nối…', false);
  socket = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`);
  socket.onmessage = async event => {
    const data = JSON.parse(event.data);
    if (data.type === 'ready') {
      reconnectDelay = 1000; status('Đã kết nối', true); $('chat-alert').hidden = true;
      try { await syncHistory(); } catch (e) { $('chat-alert').textContent = e.message; $('chat-alert').hidden = false; }
    }
    if (data.type === 'ping') { if (socket.readyState === 1) socket.send(JSON.stringify({ type: 'ping' })); }
    if (data.type === 'message' || data.type === 'confirmed') accept(data.message);
    if (data.type === 'error') {
      const row = pending.get(data.clientMsgId);
      if (row) { row.state = 'failed'; row.error = data.error; render(); }
      else $('send-error').textContent = data.error;
    }
  };
  socket.onclose = () => {
    if (stopped) return;
    status('Mất kết nối', false); $('chat-alert').textContent = 'Đang kết nối lại. Tin đã lưu sẽ được đồng bộ tự động.'; $('chat-alert').hidden = false;
    for (const item of pending.values()) { item.state = 'failed'; item.error = 'Chưa nhận xác nhận. Hãy gửi lại.'; } render();
    setTimeout(async () => {
      if (stopped) return;
      try { await api('/me'); connect(); }
      catch (e) { if (e.status === 401) showAuth(e.message); else connect(); }
    }, reconnectDelay);
    reconnectDelay = Math.min(10000, reconnectDelay * 2);
  };
  socket.onerror = () => socket.close();
}
async function syncHistory() {
  let page;
  do {
    page = await api('/history?after=' + cursor);
    for (const message of page) { saved.set(message.msgId, message); pending.delete(message.clientMsgId); cursor = Math.max(cursor, message.msgId); }
    render();
  } while (page.length === 50);
}
function accept(message) {
  if (message.roomId !== 'sanh') return;
  saved.set(message.msgId, message); pending.delete(message.clientMsgId);
  // Cursor chi tien qua lich su lien tuc de tin dao thu tu khong tao lo hong khi reconnect.
  render();
}
function element(tag, className, text) { const e = document.createElement(tag); if (className) e.className = className; if (text !== undefined) e.textContent = text; return e; }
function render() {
  const timeline = $('timeline'), atBottom = timeline.scrollHeight - timeline.scrollTop - timeline.clientHeight < 100;
  const list = [...saved.values()].sort((a, b) => a.msgId - b.msgId).slice(-500);
  const nodes = []; let day = '';
  for (const item of [...list, ...pending.values()]) {
    const date = new Date(item.timestamp), dateText = date.toLocaleDateString('vi-VN', { day: 'numeric', month: 'long', year: 'numeric' });
    if (dateText !== day) { day = dateText; nodes.push(element('div', 'day', dateText)); }
    const row = element('article', 'message-row'); row.dataset.clientMsgId = item.clientMsgId;
    if (item.msgId) row.dataset.msgId = item.msgId;
    const avatar = element('span', 'avatar', item.name.slice(0, 1).toUpperCase()); avatar.setAttribute('aria-hidden', 'true');
    const body = element('div', 'message-body'), meta = element('div', 'message-meta');
    meta.append(element('strong', '', item.name)); if (item.userId === me?.id) meta.append(element('span', 'you', 'Bạn'));
    const time = element('time', '', date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })); time.dateTime = date.toISOString(); meta.append(time);
    body.append(meta, element('p', 'message-text', item.text));
    if (item.userId === me?.id) {
      const state = element('div', 'message-state' + (item.state === 'failed' ? ' failed' : ''), item.msgId ? '✓ Đã gửi' : item.state === 'failed' ? item.error : 'Đang gửi…');
      if (item.state === 'failed') { const retry = element('button', 'link', 'Gửi lại'); retry.type = 'button'; retry.onclick = () => sendItem(item); state.append(retry); }
      body.append(state);
    }
    row.append(avatar, body); nodes.push(row);
  }
  $('messages').replaceChildren(...nodes);
  if (atBottom) timeline.scrollTop = timeline.scrollHeight; else $('new-messages').hidden = false;
}
$('new-messages').onclick = () => { $('timeline').scrollTop = $('timeline').scrollHeight; $('new-messages').hidden = true; };
$('timeline').onscroll = () => { if ($('timeline').scrollHeight - $('timeline').scrollTop - $('timeline').clientHeight < 100) $('new-messages').hidden = true; };
function updateBytes() { const bytes = encoder.encode($('message').value).length; $('bytes').textContent = `${bytes} / 1000 byte`; $('send').disabled = !socket || socket.readyState !== 1 || !$('message').value.trim() || bytes > 1000; }
$('message').oninput = updateBytes;
$('message').onkeydown = e => { if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); if (!$('send').disabled) $('compose-form').requestSubmit(); } };
function messageId() { return btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(16)))).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', ''); }
function sendItem(item) {
  if (socket?.readyState !== 1) { $('send-error').textContent = 'Chờ kết nối được khôi phục rồi gửi lại.'; return; }
  item.state = 'pending'; item.error = ''; pending.set(item.clientMsgId, item); render();
  socket.send(JSON.stringify({ type: 'send', roomId: 'sanh', clientMsgId: item.clientMsgId, text: item.text }));
  setTimeout(() => { if (pending.get(item.clientMsgId)?.state === 'pending') { item.state = 'failed'; item.error = 'Chưa nhận xác nhận. Hãy gửi lại.'; render(); } }, 5000);
}
$('compose-form').onsubmit = e => {
  e.preventDefault(); if ($('send').disabled) return;
  if (pending.size >= 100) { $('send-error').textContent = 'Hãy xử lý các tin chưa gửi trước.'; return; }
  const item = { text: $('message').value, clientMsgId: messageId(), timestamp: Date.now(), name: me.name, userId: me.id };
  $('send-error').textContent = ''; sendItem(item); $('message').value = ''; updateBytes(); $('message').focus();
};
addEventListener('offline', () => socket?.close());
setInterval(async () => { if (!me) return; try { await api('/me'); } catch (e) { if (e.status === 401) showAuth(e.message); } }, 25000);
try { await loadCsrf(); me = await api('/me'); enterChat(); } catch (e) { if (e.status !== 401) $('auth-error').textContent = e.message; }
