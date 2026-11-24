export const ROOM = 'sanh';
export const CACHE_LIMIT = 300;

// Cache va cua so DOM dung chung gioi han; cursor dong bo tach khoi tin realtime.
export class ChatStore {
  saved = new Map();
  pending = new Map();
  cursor = 0;
  latest = true;
  initialized = false;

  key(message) {
    return `${message.roomId}:${message.userId}:${message.clientMsgId || message.msgId}`;
  }

  put(message) {
    if (message.roomId !== ROOM) return false;
    const key = this.key(message);
    const fresh = !this.saved.has(key);
    this.pending.delete(key);
    if (this.latest) this.saved.set(key, message);
    return fresh;
  }

  trim(older = false) {
    const list = [...this.saved.values()].sort((a, b) => a.msgId - b.msgId);
    const remove = older ? list.slice(CACHE_LIMIT) : list.slice(0, Math.max(0, list.length - CACHE_LIMIT));
    for (const item of remove) this.saved.delete(this.key(item));
    if (older && remove.length) this.latest = false;
  }

  list() {
    return [...[...this.saved.values()].sort((a, b) => a.msgId - b.msgId), ...this.pending.values()];
  }

  clear() {
    this.saved.clear();
    this.pending.clear();
    this.cursor = 0;
    this.latest = true;
    this.initialized = false;
  }
}
