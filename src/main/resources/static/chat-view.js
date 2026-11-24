export function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

const dateKey = item => new Date(item.timestamp).toLocaleDateString('vi-VN');
export const nearBottom = timeline => timeline.scrollHeight - timeline.scrollTop - timeline.clientHeight < 90;

export class ChatView {
  rows = new Map();
  days = new Map();

  constructor(container, retry) {
    this.container = container;
    this.retry = retry;
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
    const text = element('p', 'message-text');
    const footer = element('div', 'message-footer');
    const time = element('time');
    const state = element('span', 'message-state');
    const retry = element('button', 'retry', 'Gửi lại');
    retry.type = 'button';
    footer.append(time, state, retry);
    body.append(meta, text, footer);
    row.append(avatar, body);
    return { row, avatar, meta, identity, text, time, state, retry, own };
  }

  updateRow(ref, item, own, grouped) {
    ref.row.className = `message-row ${own ? 'own' : 'other'}${grouped ? ' grouped' : ''}`;
    ref.row.dataset.userId = item.userId;
    if (item.msgId) ref.row.dataset.msgId = item.msgId;
    ref.avatar.hidden = own;
    ref.avatar.textContent = item.name.slice(0, 1).toUpperCase();
    ref.meta.hidden = own || grouped;
    ref.identity.textContent = item.name;
    // Khong gan lai textContent neu khong doi, de giu selection khi co tin moi.
    if (ref.text.textContent !== item.text) ref.text.textContent = item.text;
    const date = new Date(item.timestamp);
    ref.time.textContent = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    ref.time.dateTime = date.toISOString();
    ref.time.title = date.toLocaleString('vi-VN');
    ref.state.hidden = !own;
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
      const grouped = previous && previous.userId === item.userId && previous.roomId === item.roomId
        && dateKey(previous) === day && item.timestamp - previous.timestamp >= 0 && item.timestamp - previous.timestamp < 120000;
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
