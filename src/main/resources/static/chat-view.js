export function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

export const fileSize = bytes => bytes < 1024 ? `${bytes} B` : bytes < 1048576 ? `${(bytes/1024).toFixed(1)} KB` : `${(bytes/1048576).toFixed(1)} MB`;

const dateKey = item => new Date(item.timestamp).toLocaleDateString('vi-VN');
export const nearBottom = timeline => timeline.scrollHeight - timeline.scrollTop - timeline.clientHeight < 90;

export class ChatView {
  rows = new Map();
  days = new Map();

  constructor(container, retry, actions = {}) {
    this.container = container;
    this.retry = retry;
    this.actions = actions;
  }

  clear() {
    this.rows.clear();
    this.days.clear();
    this.container.replaceChildren();
  }

  createRow(item, own) {
    const row = element('article', 'message-row');
    row.dataset.clientMsgId = item.clientMsgId;
    const avatar = element('span', 'avatar');
    avatar.setAttribute('aria-hidden', 'true');
    const body = element('div', 'message-body');
    const meta = element('div', 'message-meta');
    const identity = element('button', 'sender-name');
    identity.type = 'button';
    const account = element('span', 'sender-id', `Tài khoản: ${item.userId}`);
    account.hidden = true;
    identity.onclick = () => {
      account.hidden = !account.hidden;
      identity.setAttribute('aria-expanded', String(!account.hidden));
    };
    identity.setAttribute('aria-expanded', 'false');
    identity.setAttribute('aria-label', `${item.name}, xem định danh tài khoản`);
    meta.append(identity, account);
    const quote = element('button', 'reply-preview'); quote.type = 'button';
    const text = element('div', 'message-text');
    const footer = element('div', 'message-footer');
    const time = element('time');
    const state = element('span', 'message-state');
    const retry = element('button', 'retry', 'Gửi lại');
    retry.type = 'button';
    footer.append(time, state, retry);
    const menu = element('details', 'message-actions');
    const summary = element('summary', '', '⋯'); summary.setAttribute('aria-label', 'Thao tác tin nhắn');
    const controls = element('div','message-menu');
    const copy = element('button','','Sao chép'), reply = element('button','','Trả lời'), recall = element('button','','Thu hồi');
    for (const button of [copy,reply,recall]) button.type = 'button';
    controls.append(copy,reply,recall); menu.append(summary,controls);
    body.append(meta,quote,text,footer,menu);
    row.append(avatar, body);
    return { row, avatar, meta, identity, text, time, state, retry, own, quote, menu, copy, reply, recall };
  }

  updateRow(ref, item, own, grouped) {
    ref.row.className = `message-row ${item.kind==='Y'?'system':own ? 'own' : 'other'}${grouped ? ' grouped' : ''}`;
    ref.row.dataset.userId = item.userId;
    if (item.msgId) ref.row.dataset.msgId = item.msgId;
    ref.avatar.hidden = own || item.kind==='Y';
    const avatarKey = item.userId + ':' + (item.avatarHash || '');
    if (ref.avatar.dataset.key !== avatarKey) {
      ref.avatar.dataset.key = avatarKey;
      ref.avatar.textContent = item.name.slice(0, 1).toUpperCase();
      if (item.avatarHash) {
        const image = element('img'); image.src = '/api/avatars/' + item.userId + '?v=' + item.avatarHash;
        image.alt = ''; image.width = 34; image.height = 34;
        image.onerror = () => { ref.avatar.textContent = item.name.slice(0,1).toUpperCase(); };
        ref.avatar.replaceChildren(image);
      }
    }
    ref.meta.hidden = own || grouped || item.kind==='Y';
    ref.identity.textContent = item.nickname || item.name;
    // Khong gan lai textContent neu khong doi, de giu selection khi co tin moi.
    const content = item.recalled ? 'Tin đã thu hồi' : item.text;
    const contentKey = (item.kind || 'T') + ':' + content + (item.kind==='F'?':'+Boolean(item.msgId):'');
    if (ref.text.dataset.content !== contentKey) {
      ref.text.dataset.content = contentKey;
      if (item.kind === 'S' && !item.recalled) {
        const image = element('img','message-sticker'); image.width = 128; image.height = 128;
        image.src = '/api/stickers/' + encodeURIComponent(item.text) + '.png'; image.alt = 'Sticker ' + item.text;
        image.onerror = () => { ref.text.textContent = '[Sticker không còn được hỗ trợ]'; };
        ref.text.replaceChildren(image);
      } else if(item.kind==='F' && item.file && !item.recalled) {
        const file=item.file,card=element('div','file-card');
        if(file.image && item.msgId) {
          const image=element('img','file-thumbnail');image.width=240;image.height=150;image.loading='lazy';
          image.alt='Xem trước '+file.name;image.src='/api/files/'+encodeURIComponent(file.id)+'?thumbnail=true';
          image.onerror=()=>{image.alt='Không tải được ảnh xem trước. Dùng Tải xuống để thử lại.';};card.append(image);
        }
        const size=fileSize(file.size);
        card.append(element('strong','file-name',file.name),element('span','file-size',size));
        if(item.msgId) {
          const link=element('a','file-download','↓ Tải xuống');link.href='/api/files/'+encodeURIComponent(file.id);card.append(link);
        }else card.append(element('span','file-size','Đã tải lên · Đang chờ gửi tin'));
        ref.text.replaceChildren(card);
      } else ref.text.textContent = content;
    }
    ref.row.classList.toggle('recalled', Boolean(item.recalled));
    ref.quote.hidden = !item.replyTo || item.recalled;
    ref.quote.textContent = `${item.replyName || 'Tin gốc'}: ${item.replyText || 'Xem tin được trả lời'}`;
    ref.quote.onclick = () => this.actions.jump?.(item.replyTo);
    ref.menu.hidden = !item.msgId || item.recalled || item.kind==='Y';
    ref.copy.hidden = item.kind !== 'T';
    ref.copy.onclick = async () => {
      try { await navigator.clipboard.writeText(item.text); ref.copy.textContent = 'Đã sao chép'; }
      catch { ref.copy.textContent = 'Hãy chọn chữ và dùng Ctrl+C'; }
    };
    ref.reply.onclick = () => { ref.menu.open = false; this.actions.reply?.(item); };
    ref.recall.hidden = !this.actions.canRecall?.(item);
    ref.recall.onclick = () => { ref.menu.open = false; this.actions.recall?.(item); };
    const date = new Date(item.timestamp);
    ref.time.textContent = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    ref.time.dateTime = date.toISOString();
    ref.time.title = date.toLocaleString('vi-VN');
    ref.state.hidden = !own || item.kind==='Y';
    const failed = !item.msgId && item.state === 'failed';
    const state = item.msgId ? '✓ Đã gửi' : failed ? item.error : 'Đang gửi…';
    if (ref.state.textContent !== state) ref.state.textContent = state;
    ref.state.classList.toggle('failed', failed);
    ref.retry.hidden = !failed;
    ref.retry.onclick = () => this.retry(item);
  }

  render(items, currentUser, key) {
    const nodes = [], active = new Set(), days = new Set();
    let previous;
    for (const item of items) {
      const day = dateKey(item);
      if (!previous || dateKey(previous) !== day) {
        let separator = this.days.get(day);
        if (!separator) {
          separator = element('div', 'day', new Date(item.timestamp).toLocaleDateString('vi-VN', { day: 'numeric', month: 'long', year: 'numeric' }));
          this.days.set(day, separator);
        }
        days.add(day);
        nodes.push(separator);
      }
      const id = key(item), own = item.userId === currentUser.id;
      let ref = this.rows.get(id);
      if (!ref) { ref = this.createRow(item, own); this.rows.set(id, ref); }
      const grouped = item.kind!=='Y' && previous?.kind!=='Y' && previous && previous.userId === item.userId && previous.roomId === item.roomId
        && !previous.recalled && !item.recalled && dateKey(previous) === day && item.timestamp - previous.timestamp >= 0 && item.timestamp - previous.timestamp < 120000;
      this.updateRow(ref, item, own, grouped);
      active.add(id);
      nodes.push(ref.row);
      previous = item;
    }
    for (const [id, ref] of this.rows) if (!active.has(id)) { ref.row.remove(); this.rows.delete(id); }
    for (const [day, node] of this.days) if (!days.has(day)) { node.remove(); this.days.delete(day); }
    // Chi chen/di chuyen hang that su doi thu tu, khong thay ca danh sach.
    let next = this.container.firstChild;
    for (const node of nodes) {
      if (node === next) next = next.nextSibling;
      else this.container.insertBefore(node, next);
    }
  }
}
