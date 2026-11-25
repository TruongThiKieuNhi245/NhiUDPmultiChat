export const ROOM = 'sanh';
export const CACHE_LIMIT = 300;

// Cache va cua so DOM dung chung gioi han; cursor dong bo tach khoi tin realtime.
export class ChatStore {
  roomId = ROOM;
  saved = new Map();
  pending = new Map();
  recalled = new Set();
  cursor = 0;
  latest = true;
  initialized = false;

  key(message) {
    return `${message.roomId}:${message.userId}:${message.clientMsgId || message.msgId}`;
  }

  put(message) {
    if (message.roomId !== this.roomId) return false;
    message = this.sanitize(message);
    const key = this.key(message);
    const fresh = !this.saved.has(key);
    this.pending.delete(key);
    if (this.latest && !this.saved.get(key)?.recalled) this.saved.set(key, message);
    return fresh;
  }

  sanitize(message) {
    if (this.recalled.has(`${message.roomId}:${message.msgId}`)) return {...message,text:'',kind:'D',recalled:true,replyTo:0,replyText:'',file:null};
    if (message.replyTo && this.recalled.has(`${message.roomId}:${message.replyTo}`)) return {...message,replyText:'Tin đã thu hồi'};
    return message;
  }
  markRecall(room,id) {
    this.recalled.add(`${room}:${id}`);
    if(this.recalled.size>1024)this.recalled.delete(this.recalled.values().next().value);
  }
  trim(older = false) {
    const list = [...this.saved.values()].sort((a, b) => a.msgId - b.msgId);
    const remove = older ? list.slice(CACHE_LIMIT) : list.slice(0, Math.max(0, list.length - CACHE_LIMIT));
    for (const item of remove) this.saved.delete(this.key(item));
    if (older && remove.length) this.latest = false;
  }

  list() {
    return [...[...this.saved.values()].sort((a, b) => a.msgId - b.msgId), ...[...this.pending.values()].filter(item => item.roomId === this.roomId)];
  }

  clear() {
    this.saved.clear();
    this.pending.clear();
    this.recalled.clear();
    this.cursor = 0;
    this.latest = true;
    this.initialized = false;
  }
}
