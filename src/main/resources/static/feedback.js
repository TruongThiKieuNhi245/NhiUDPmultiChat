import { element } from './chat-view.js';
export function moderationText(notice, target=true) {
  const subject=target?'Bạn':notice.targetName;
  const verbs={MUTE:'đã bị cấm chat',UNMUTE:'đã được gỡ cấm chat',BAN:'đã bị cấm vào phòng',UNBAN:'đã được gỡ cấm vào phòng',KICK:'đã được đưa ra khỏi phòng',OWNER:'đã trở thành chủ phòng'};
  const until=notice.until>0?' đến '+new Date(notice.until).toLocaleString('vi-VN'):'';
  return `${subject} ${verbs[notice.kind] || 'được cập nhật quyền'} · ${notice.roomName}${until}.${target?' Người thực hiện: '+notice.actorName+'.':''}${notice.reason?' Lý do: '+notice.reason:''}${notice.kind==='UNBAN'?' Có thể tham gia lại nếu phòng còn mở và còn chỗ.':''}`;
}
// Gioi han ca DOM, timer va tap ID; thong bao khong phai tin chat da luu.
export class Feedback {
  rows=[];seen=new Set();
  constructor() {
    this.host=element('aside','feedback');this.host.setAttribute('aria-label','Thông báo thao tác');document.body.append(this.host);
    document.addEventListener('close',event=>{if(event.target.contains?.(this.host))document.body.append(this.host);},true);
  }
  clear(){for(const row of this.rows)clearTimeout(row.timer);this.rows=[];this.seen.clear();this.host.replaceChildren();document.body.append(this.host);}
  show(text,{id,important=false}={}) {
    if(!text || (id && this.seen.has(id)))return;
    if(id){this.seen.add(id);if(this.seen.size>64)this.seen.delete(this.seen.values().next().value);}
    const dialog=document.querySelector('dialog[open] .dialog-content');(dialog || document.body).append(this.host);
    const node=element('section','feedback-card'+(important?' feedback-important':'')),message=element('p','',text),close=element('button','icon-button','×');
    message.setAttribute('role','status');close.type='button';close.setAttribute('aria-label','Đóng thông báo');node.append(message,close);
    const row={node,timer:null};const dismiss=()=>{clearTimeout(row.timer);node.remove();this.rows=this.rows.filter(item=>item!==row);};close.onclick=dismiss;
    this.rows.push(row);this.host.append(node);while(this.rows.length>3){const old=this.rows.shift();clearTimeout(old.timer);old.node.remove();}
    if(!important)row.timer=setTimeout(dismiss,7000);
  }
  action(name,data,result) {
    if(result?.notice){this.show('Đã thực hiện. '+moderationText(result.notice,false));return;}
    const messages={'room-create':'Đã tạo phòng.','room-edit':'Đã lưu thông tin phòng.','room-state':data.open==='true'?'Đã mở phòng.':'Đã đóng phòng.','room-delete':'Đã xóa phòng.','room-clear':'Đã xóa nội dung phòng.','room-leave':'Đã rời phòng.','room-join':'Đã tham gia phòng.','member-nickname':'Đã cập nhật biệt danh.','invite-send':'Đã gửi lời mời.','invite-answer':data.accept==='true'?'Đã chấp nhận lời mời.':'Đã từ chối lời mời.','invite-cancel':'Đã hủy lời mời.','notifications':'Đã cập nhật thông báo phòng.','user-add':'Đã tạo tài khoản.','user-edit':'Đã lưu tài khoản.','user-lock':data.locked==='true'?'Đã khóa tài khoản và thu hồi phiên.':'Đã mở khóa tài khoản.','user-reset':'Đã cấp mật khẩu tạm và thu hồi phiên cũ.','user-delete':'Đã xóa tài khoản.','password-change':'Đã đổi mật khẩu.','profile-save':'Đã lưu hồ sơ.'};
    this.show(messages[name]);
  }
}
