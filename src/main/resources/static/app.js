import { Feedback, moderationText } from './feedback.js';
import { AdminUI } from './admin-ui.js';
import { FileUpload } from './file-upload.js';
import { WorkspaceUI } from './workspace-ui.js';
import { ChatStore, ROOM, CACHE_LIMIT } from './chat-store.js';
import { ChatView, nearBottom } from './chat-view.js';

const $ = id => document.getElementById(id);
const feedback = new Feedback();
const store = new ChatStore(), encoder = new TextEncoder(), timers = new Map();
const view = new ChatView($('messages'), item => sendItem(item), {
  reply: item => { replying = item; showReply(); $('message').focus(); },
  jump: id => jumpTo(id),
  canRecall: item => ui.room()?.manage || (item.userId === me?.id && Date.now()-item.timestamp <= 600000),
  recall: item => ui.form('Thu hồi tin nhắn',[],async () => {
    await api('/recall',{method:'POST',body:JSON.stringify({roomId:item.roomId,msgId:item.msgId})});
  },{submitText:'Thu hồi'})
});
let activeRoom = ROOM, roomEpoch = 0, replying = null, recallCursor = 0, lastTyping = 0, typingTimer;
const drafts = new Map(), roomUnread = new Map(), typingUsers = new Map();
const ui = new WorkspaceUI({ api, action, selectRoom, currentRoom: () => activeRoom,
  jump: jumpTo, sendSticker: code => enqueue(code,'S'), updateProfile });
const admin = new AdminUI({ api, ui,
  open: () => { drawer(false);$('workspace').hidden=true; },
  close: () => { if(me){$('workspace').hidden=false;updateComposer();render();} }
});
const fileUpload = new FileUpload({ ui, csrf: () => csrf, api, currentRoom: () => ui.room(),
  send: (file,room) => room===activeRoom ? enqueue(file.text,'F',{file}) : false });
ui.unread = roomUnread;
ui.pending = () => [...store.pending.values()];
async function action(name,data) { const session=generation;const result=await api('/actions/'+name,{method:'POST',body:JSON.stringify(data)});if(session===generation)feedback.action(name,data,result);return result; }
ui.notify=(text,options)=>feedback.show(text,options);
ui.success=(name,data,result)=>feedback.action(name,data,result);
function updateProfile(profile) {
  if (!me) return;
  const forced = me.forcePassword;
  Object.assign(me,profile);admin.updateProfile(profile);
  $('account-name').textContent = me.name;
  $('account-login').textContent = '@'+me.login;
  const avatar = $('account-avatar'); avatar.textContent = me.name.slice(0,1).toUpperCase();
  if (me.avatarHash) { const img=document.createElement('img'); img.alt='';img.width=34;img.height=34;img.src='/api/avatars/'+me.id+'?v='+me.avatarHash;avatar.replaceChildren(img); }
  if (!profile.forcePassword && ui.snapshot?.rooms.length && !ui.room()?.joined) { ui.dialog?.close();selectRoom('sanh',true); }
  updateRoomHeader();
  if(ui.room()?.joined)refreshNames();
  if (forced && !profile.forcePassword) selectRoom('sanh',true);
}
async function refreshNames() {
  const session=generation,epoch=roomEpoch;
  try {
    const members=await api('/members?roomId='+activeRoom);
    if(session!==generation || epoch!==roomEpoch || !me)return;
    const byId=new Map(members.map(member=>[member.id,member]));
    for(const [key,item] of store.saved){const member=byId.get(item.userId);store.saved.set(key,{...item,nickname:member?.nickname || '',avatarHash:member?.avatarHash || item.avatarHash});}
    render({anchor:readingAnchor()});
  }catch { /* Phong vua roi hoac da bi khoa quyen se duoc snapshot xu ly. */ }
}
function updateRoomHeader() {
  const room = ui.room();
  $('room-name').textContent = room?.name || 'Sảnh chung';
  $('room-description').textContent = room ? `${room.memberCount} thành viên · ${room.onlineCount} đang trực tuyến${room.open?'':' · Đã đóng'}` : 'Phòng trò chuyện chung';
  const label = 'Nhắn vào '+(room?.name || 'Sảnh chung');
  document.querySelector('label[for=message]').textContent = label;
  $('message').placeholder = 'Nhắn tin…';
  $('timeline').setAttribute('aria-label','Tin nhắn trong '+(room?.name || 'Sảnh chung'));
  document.querySelector('#room-welcome h2').textContent = 'Xin chào, '+(room?.name || 'Sảnh chung')+'!';
  document.title = 'UDPmultiChat · '+(room?.name || 'Sảnh chung');
  updateComposer();
}
function showReply() {
  $('replying').hidden = !replying;
  $('replying-text').textContent = replying ? `Trả lời ${replying.nickname || replying.name}: ${replying.kind==='S'?'[Sticker]':replying.kind==='F'?'[Tệp] '+(replying.file?.name || ''):replying.text.slice(0,100)}` : '';
  updateComposer();
}
$('cancel-reply').onclick = () => { replying=null; showReply(); $('message').focus(); };
async function selectRoom(id,force=false) {
  if(id!=='sanh' && !ui.snapshot?.rooms.some(room=>room.id===id && room.joined))return;
  if (!me || (activeRoom===id && !force)) { drawer(false); return; }
  drafts.delete(activeRoom); drafts.set(activeRoom,{text:$('message').value,reply:replying?{...replying,text:''}:null});
  if (drafts.size>20) drafts.delete(drafts.keys().next().value);
  clearTimeout(typingTimer);lastTyping=0;roomEpoch++; activeRoom=id; store.roomId=id; store.saved.clear(); store.cursor=0;store.initialized=false;store.latest=true;
  recallCursor=0;unread=0;roomUnread.delete(id);view.clear();typingUsers.clear();$('typing').textContent='';
  const entry=drafts.get(id);$('message').value=entry?.text || '';replying=entry?.reply || null;
  updateRoomHeader();showReply();ui.renderRooms();drawer(false);
  if (socket?.readyState===1) {
    try { await syncHistory(generation,roomEpoch); } catch (error) { alertChat(error.message); }
  }
}
async function jumpTo(id) {
  const existing = $('messages').querySelector(`[data-msg-id="${id}"]`);
  if (existing) { existing.scrollIntoView({block:'center'}); existing.tabIndex=-1; existing.focus({preventScroll:true}); return; }
  const epoch=roomEpoch, session=generation;
  try {
    const page=await api('/history/around?roomId='+activeRoom+'&msgId='+id);
    if(epoch!==roomEpoch || session!==generation)return;
    store.saved.clear();store.latest=false;
    for(const item of page)store.saved.set(store.key(item),store.sanitize(item));
    render();const target=$('messages').querySelector(`[data-msg-id="${id}"]`);target?.scrollIntoView({block:'center'});target?.setAttribute('tabindex','-1');target?.focus({preventScroll:true});
  }catch(error){alertChat(error.message);}
}
function applyRecall(message) {
  store.markRecall(message.roomId,message.msgId);
  for(const item of store.pending.values())if(item.roomId===message.roomId && item.replyTo===message.msgId)item.replyText='Tin đã thu hồi';
  for(const card of document.querySelectorAll('[data-search-msg-id]'))if(card.dataset.roomId===message.roomId && card.dataset.searchMsgId===String(message.msgId))card.textContent='Tin đã thu hồi';
  const update=item=>item.msgId===message.msgId ? {...item,text:'',kind:'D',recalled:true,replyTo:0,replyText:'',file:null}
    : item.replyTo===message.msgId ? {...item,replyText:'Tin đã thu hồi'} : item;
  if(message.roomId!==activeRoom)return;
  for(const [key,item] of store.saved)store.saved.set(key,update(item));
  if(replying?.msgId===message.msgId){replying=null;showReply();}
  render({anchor:readingAnchor()});
}
async function syncRecalls(session,epoch) {
  let page;
  do {
    page=await api('/recalls?roomId='+activeRoom+'&after='+recallCursor);
    if(session!==generation || epoch!==roomEpoch)return;
    for(const id of page.ids)applyRecall({roomId:activeRoom,msgId:id});
    recallCursor=page.cursor;
  } while(page.more);
}
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
// Hop nhat request khoi tao va submit som de tranh hai cookie CSRF canh tranh.
let csrfLoading;
async function loadCsrf() {
  if(!csrfLoading)csrfLoading=api('/csrf').then(value=>{csrf=value;}).finally(()=>{csrfLoading=null;});
  return csrfLoading;
}
function stopSession() {
  generation++;
  clearTimeout(reconnectTimer);clearTimeout(typingTimer);
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
  feedback.clear();fileUpload.reset();admin.reset();ui.clear();drafts.clear();roomUnread.clear();typingUsers.clear();replying=null;roomEpoch++;activeRoom=ROOM;store.roomId=ROOM;
  draft = '';
  $('message').value = '';
  store.clear();
  view.clear();
  unread = 0;
  historyFailed = false;
  $('workspace').hidden = true;
  $('auth').hidden = false;
  setAuthMode(false);$('auth-error').textContent = reason;
  $('login').focus();
}
function setAuthMode(value) {
  registering = value;
  $('form-title').textContent = registering ? 'Bắt đầu câu chuyện mới.' : 'Rất vui được gặp lại.';
  $('form-description').textContent = registering ? 'Tạo tài khoản để gặp mọi người trong Sảnh chung.' : 'Đăng nhập để tiếp tục cuộc trò chuyện.';
  $('submit-login').textContent = registering ? 'Tạo tài khoản →' : 'Đăng nhập →';
  $('switch-description').textContent = registering ? 'Đã có tài khoản?' : 'Chưa có tài khoản?';
  $('switch-mode').textContent = registering ? 'Đăng nhập' : 'Tạo tài khoản';
  $('password').autocomplete = registering ? 'new-password' : 'current-password';
  $('auth-error').textContent = '';
}
$('switch-mode').onclick = () => setAuthMode(!registering);
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

async function enterChat() {
  stopSession();
  const session=generation;
  reconnectAttempt = 0;
  $('auth').hidden = true;
  $('workspace').hidden = false;
  $('account-name').textContent = me.name;
  $('account-login').textContent = '@' + me.login;
  $('account-avatar').textContent = me.name.slice(0, 1).toUpperCase();
  $('message').value = draft;
  drawer(false);
  try { await ui.refresh(); if(me && session===generation) connect(); } catch(error) { if(session===generation)alertChat(error.message); }
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
      // Cursor ID tin khong the dai dien cho lan thu hoi moi cua tin cu.
      recallCursor=0;
      reconnectAttempt = 0;
      status('Đã kết nối', true);
      alertChat();
      try { if (!me.forcePassword) {await ui.refresh();if(valid())await syncHistory(session,roomEpoch);} }
      catch (error) { if (valid()) { historyFailed = true; alertChat('Chưa tải được lịch sử. Bấm Về tin mới để thử lại. ' + error.message); $('new-messages').hidden = false; } }
    }
    if (data.type === 'ping' && current.readyState === 1) current.send(JSON.stringify({ type: 'ping' }));
    if (data.type === 'admin-updated') admin.invalidate();
    if(data.type==='member-notice') {
      const notice=data.data;
      if(notice.userId===me.id){
        if(['KICK','BAN'].includes(notice.kind) && data.roomId===activeRoom)ui.dialog?.close();
        if(!valid())return;
        feedback.show(moderationText(notice),{id:notice.id,important:true});ui.scheduleRefresh();
      }
    }
    if(data.type==='room-activity' && data.data.actorId!==me.id && data.data.userId!==me.id)feedback.show(data.data.text,{id:data.data.id});
    if (data.type === 'workspace') {ui.scheduleRefresh();admin.invalidate();}
    if (data.type === 'history-cleared' && data.roomId===activeRoom){recallCursor=0;syncRecalls(generation,roomEpoch).catch(error=>alertChat(error.message));}
    if (data.type === 'recall') applyRecall(data.data);
    if (data.type === 'typing' && data.roomId===activeRoom && data.data.userId!==me.id) typingUsers.set(data.data.userId,data.data);
    if (data.type === 'message' || data.type === 'confirmed') {accept(data.message);admin.invalidate();}
    if (data.type === 'error') {
      ui.scheduleRefresh();
      const item = [...store.pending.values()].find(row => row.clientMsgId === data.clientMsgId);
      if (item) { failItem(item, data.error); alertChat('Chưa gửi được một tin. Xem thông báo và nút Gửi lại cạnh tin đó.'); }
      else $('send-error').textContent = data.error;
    }
  };
  current.onclose = () => {
    if (!valid()) return;
    socket = null;
    roomEpoch++;
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
  $('room-welcome').hidden = store.saved.size > 0 || [...store.pending.values()].some(item=>item.roomId===activeRoom);
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
async function latestPage(session = generation,epoch = roomEpoch) {
  const page = await api('/history/page?roomId='+activeRoom);
  if (session !== generation || epoch !== roomEpoch || !me) return;
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
async function syncHistory(session,epoch = roomEpoch) {
  if (!store.initialized) await latestPage(session,epoch);
  if (session !== generation || epoch !== roomEpoch || !me) return;
  let page;
  do {
    page = await api('/history?roomId='+activeRoom+'&after=' + store.cursor);
    if (session !== generation || epoch !== roomEpoch || !me) return;
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
  await syncRecalls(session,epoch);
}
$('load-older').onclick = async () => {
  if (loadingOlder) return;
  loadingOlder = true;
  $('load-older').disabled = true;
  const session = generation, epoch = roomEpoch;
  try {
    const oldest = Math.min(...[...store.saved.values()].map(item => item.msgId));
    const page = await api('/history/page?roomId='+activeRoom+'&before=' + oldest);
    if (session !== generation || epoch !== roomEpoch || !me) return;
    const anchor = readingAnchor();
    for (const item of page) store.saved.set(store.key(item),store.sanitize(item));
    store.trim(true);
    render({ anchor });
  } catch (error) { if (session === generation) alertChat(error.message); }
  finally { loadingOlder = false; $('load-older').disabled = false; }
};
function accept(message) {
  if (message.roomId !== activeRoom) {
    clearItemTimer(message);store.pending.delete(store.key(message));ui.renderRooms();
    if(message.userId!==me.id){roomUnread.set(message.roomId,(roomUnread.get(message.roomId)||0)+1);ui.renderRooms();}
    return;
  }
  const bottom = nearBottom($('timeline')) && store.latest, anchor = readingAnchor();
  clearItemTimer(message);
  if (!bottom && store.saved.size >= CACHE_LIMIT && !store.saved.has(store.key(message))) store.latest = false;
  const fresh = store.put(message);
  ui.renderRooms();
  if (fresh && !bottom && message.userId !== me.id) unread++;
  store.trim();
  render({ bottom, anchor });
  if (!historyFailed && ![...store.pending.values()].some(item => item.state === 'failed') && socket?.readyState === 1) alertChat();
  if (fresh && message.userId !== me.id && !ui.room()?.notificationsMuted) $('announcer').textContent = `Tin mới từ ${message.name}`;
}
$('new-messages').onclick = async () => {
  try { await latestPage(); } catch (error) { alertChat(error.message); }
};
$('timeline').onscroll = () => {
  if (!historyFailed && nearBottom($('timeline')) && store.latest) { unread = 0; $('new-messages').hidden = true; }
};

function updateComposer() {
  const input = $('message'), bytes = encoder.encode((replying?'«reply:'+replying.msgId+'::»':'')+input.value).length;
  $('bytes').textContent = `${bytes} / 1000 byte`;
  $('bytes').hidden = bytes < 800;
  $('bytes').classList.toggle('error', bytes > 1000);
  $('limit-error').textContent = bytes > 1000 ? 'Tin vượt 1000 byte UTF-8. Hãy rút ngắn hoặc tách thành nhiều tin.' : '';
  input.setAttribute('aria-invalid', String(bytes > 1000));
  const restriction=$('room-restriction'),currentRoom=ui.room(),muted=currentRoom?.mutedUntil>Date.now();
  const restrictionText=muted?`Bạn đang bị cấm chat đến ${new Date(currentRoom.mutedUntil).toLocaleString('vi-VN')}.${currentRoom.muteReason?' Lý do: '+currentRoom.muteReason:''} Bạn vẫn có thể đọc tin và giữ bản nháp.`:currentRoom && !currentRoom.open?'Phòng đã đóng. Bạn vẫn có thể đọc lịch sử.':'';
  if(restriction.textContent!==restrictionText)restriction.textContent=restrictionText;
  restriction.hidden=!restrictionText;
  const room=ui.room(), writable=!me?.forcePassword && (!room || (room.joined && room.open && room.mutedUntil<Date.now()));
  $('send').disabled = socket?.readyState !== 1 || !writable || !input.value.trim() || bytes > 1000;
  $('sticker').disabled=socket?.readyState!==1 || !writable;
  $('attach').disabled=socket?.readyState!==1 || !writable;
  input.style.height = 'auto';
  input.style.height = Math.min(130, input.scrollHeight) + 'px';
}
$('message').oninput = () => {
  draft = $('message').value; updateComposer();
  clearTimeout(typingTimer);
  const session=generation,epoch=roomEpoch;
  const emit=()=>{if(session!==generation || epoch!==roomEpoch || socket?.readyState!==1 || !$('message').value.trim() || $('sticker').disabled)return;lastTyping=Date.now();socket.send(JSON.stringify({type:'typing',roomId:activeRoom}));};
  typingTimer=setTimeout(emit,Math.max(0,1500-(Date.now()-lastTyping)));
};
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
  ui.renderRooms();
  render({ anchor: readingAnchor() });
}
function sendItem(item) {
  if (socket?.readyState !== 1) { failItem(item, 'Chờ kết nối được khôi phục rồi gửi lại.'); return; }
  const key = store.key(item), session = generation;
  clearItemTimer(item);
  item.state = 'pending';
  item.error = '';
  store.pending.set(key, item);
  ui.renderRooms();
  render({ bottom: true });
  try { socket.send(JSON.stringify({ type: 'send', roomId: item.roomId, clientMsgId: item.clientMsgId, text: item.text,kind:item.kind || 'T',replyTo:item.replyTo || 0 })); }
  catch { failItem(item, 'Chưa gửi được. Hãy thử lại khi có kết nối.'); return; }
  timers.set(key, setTimeout(() => {
    timers.delete(key);
    if (session === generation && store.pending.get(key)?.state === 'pending') failItem(item, 'Chưa nhận xác nhận. Hãy gửi lại.');
  }, 5000));
}
async function enqueue(text,kind='T',extra={}) {
  if (socket?.readyState!==1 || me?.forcePassword) return;
  if (store.pending.size >= 100) { $('send-error').textContent = 'Hãy xử lý các tin chưa gửi trước.'; return; }
  const room=ui.room();
  if (room && (!room.open || room.mutedUntil>Date.now())) { $('send-error').textContent='Phòng đang đóng hoặc bạn đang bị cấm chat.'; return; }
  const quote=kind==='T'?replying:null;
  const item = { roomId: activeRoom,text,kind,replyTo:quote?.msgId || 0,replyName:quote?.name || '',replyText:'',clientMsgId:messageId(),timestamp:Date.now(),name:me.name,userId:me.id,...extra };
  const session=generation,epoch=roomEpoch;
  if (!store.latest) {
    try { await latestPage(session,epoch); } catch(error) { $('send-error').textContent=error.message; return; }
    if(session!==generation || epoch!==roomEpoch)return;
  }
  $('send-error').textContent='';sendItem(item);
  if(kind==='T'){$('message').value='';draft='';replying=null;showReply();drafts.delete(activeRoom);}
  updateComposer();$('message').focus();return true;
}
$('compose-form').onsubmit = event => { event.preventDefault(); if(!$('send').disabled)enqueue($('message').value); };
addEventListener('offline', () => socket?.close());
addEventListener('online', () => { if (me && !socket) { reconnectAttempt = 0; connect(); } });
setInterval(async () => {
  if (!me) return;
  const session = generation;
  try { await api('/me'); }
  catch (error) { if (session === generation && error.status === 401) showAuth(error.message); }
  if(me)ui.scheduleRefresh();
}, 25000);
setInterval(()=>{
  const now=Date.now();for(const [id,item] of typingUsers)if(item.expires<=now)typingUsers.delete(id);
  $('typing').textContent=[...typingUsers.values()].slice(0,3).map(item=>item.name).join(', ')+(typingUsers.size?' đang nhập…':'');
  if(me)updateComposer();
},1000);
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
