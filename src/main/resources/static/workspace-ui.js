import { element, fileSize } from './chat-view.js';
const $ = id => document.getElementById(id);

// Dialog native giu focus va Escape; chi chen markup do ung dung soan san.
export class WorkspaceUI {
  snapshot;
  revision = 0;
  dialogNumber = 0;
  dialog;
  refreshTimer;
  constructor({ api, action, selectRoom, currentRoom, reply, jump, sendSticker, updateProfile }) {
    Object.assign(this, { api, action, selectRoom, currentRoom, reply, jump, sendSticker, updateProfile });
    const guarded = call => async (...args) => {
      const revision=this.revision; const result=await call(...args);
      if(revision!==this.revision)throw new Error('Phiên đã thay đổi.');
      return result;
    };
    this.api=guarded(api);this.action=guarded(action);
    $('create-room').onclick = () => this.roomForm();
    $('join-code').onclick = () => this.form('Tham gia bằng mã', [this.field('key','Mã phòng'),this.field('password','Mật khẩu phòng (nếu có)','password')], async data => {
      const result = await this.action('room-join',data); await this.refresh(); await this.selectRoom(result.roomId);
    });
    $('profile').onclick = () => this.profileForm();
    $('invitations').onclick = () => this.invitationList();
    $('room-info').onclick = () => this.roomInfo();
    $('room-search').oninput = () => this.renderRooms();
    $('find-messages').onclick = () => this.searchDialog();
    $('sticker').onclick = () => this.stickers();
  }
  room() { return this.snapshot?.rooms.find(room => room.id === this.currentRoom()); }
  clear() { this.revision++; clearTimeout(this.refreshTimer); this.dialog?.close(); this.snapshot = null; $('room-list').replaceChildren(); }
  scheduleRefresh() { clearTimeout(this.refreshTimer); this.refreshTimer = setTimeout(() => this.refresh().catch(() => {}),150); }
  async refresh() {
    const revision=this.revision;
    const snapshot = await this.api('/workspace');
    if(revision!==this.revision)throw new Error('Phiên đã thay đổi.');
    this.snapshot = snapshot;
    this.updateProfile(snapshot.profile);
    this.renderRooms();
    $('invitation-count').textContent = snapshot.invitations.length || '';
    if (snapshot.profile.forcePassword) this.passwordForm(true);
    return snapshot;
  }
  renderRooms() {
    if (!this.snapshot) return;
    const focused=document.activeElement?.closest?.('[data-room-id]')?.dataset.roomId;
    document.querySelector('.room-label span').textContent=String(this.snapshot.rooms.length).padStart(2,'0');
    const query = $('room-search').value.toLocaleLowerCase('vi');
    const nodes = this.snapshot.rooms.filter(room => room.name.toLocaleLowerCase('vi').includes(query)).map(room => {
      const button = element('button', 'room' + (room.id === this.currentRoom() ? ' selected' : ''));
      button.type = 'button'; button.dataset.roomId = room.id; button.dataset.joined=String(room.joined);
      if (room.id === 'sanh') button.id = 'room';
      if (room.id === this.currentRoom()) button.setAttribute('aria-current','page');
      const text = element('span','room-title');
      text.append(element('strong','',room.name),element('small','',`${room.memberCount} thành viên · ${room.onlineCount} đang trực tuyến${room.open?'':' · Đã đóng'}`));
      button.append(element('span','hash',room.publicRoom?'#':'⌑'),text);
      if (!room.joined) button.append(element('small','','Tham gia'));
      const count = this.unread?.get(room.id) || 0;
      if(this.pending?.().some(item=>item.roomId===room.id && item.state==='failed')){const failed=element('span','error','!');failed.title='Có tin chưa gửi được';failed.setAttribute('aria-label','Có tin chưa gửi được');button.append(failed);}
      if (count && room.joined) button.append(element('span','room-badge',String(count)));
      button.onclick = () => room.joined ? this.selectRoom(room.id) : this.form('Tham gia '+room.name,[this.field('password','Mật khẩu phòng (nếu có)','password')],async data => {
        await this.action('room-join',{key:room.id,...data}); await this.refresh(); await this.selectRoom(room.id);
      });
      return button;
    });
    if (!nodes.length) nodes.push(element('p','muted','Không có phòng phù hợp.'));
    $('room-list').replaceChildren(...nodes);
    if(focused)$('room-list').querySelector(`[data-room-id="${focused}"]`)?.focus({preventScroll:true});
  }
  open(title) {
    const previous = document.activeElement;
    this.dialog?.close();
    const dialog = element('dialog','app-dialog');
    const header = element('header','dialog-header');
    const heading = element('h2','',title), close = element('button','icon-button','×');
    close.type = 'button'; close.setAttribute('aria-label','Đóng hộp thoại'); close.onclick = () => dialog.close();
    heading.id = 'dialog-title-'+(++this.dialogNumber); dialog.setAttribute('aria-labelledby',heading.id);
    header.append(heading,close); dialog.append(header);
    const content = element('div','dialog-content'); dialog.append(content);
    dialog.onclose = () => { dialog.remove(); if (this.dialog === dialog) this.dialog = null; if(previous?.isConnected && !previous.closest('[inert]') && previous.getClientRects().length)previous.focus();else if(!$('admin-page').hidden)$('admin-refresh').focus();else if(!$('workspace').hidden)$('message').focus();else $('login').focus(); };
    document.body.append(dialog); this.dialog = dialog; dialog.showModal();
    return { dialog, content, close };
  }
  field(name,label,type='text',value='',extra={}) { return { name,label,type,value,...extra }; }
  form(title, fields, submit, options={}) {
    const { dialog,content,close } = this.open(title);
    const form = element('form','dialog-form');
    for (const field of fields) {
      const label = element('label','',field.label), input = element(field.options?'select':field.type === 'textarea' ? 'textarea' : 'input');
      input.id = 'field-'+field.name; input.name = field.name; label.htmlFor = input.id;
      if(field.options)for(const [value,text] of field.options){const option=element('option','',text);option.value=value;input.append(option);}
      else if (field.type !== 'textarea') input.type = field.type;
      if (field.type === 'checkbox') input.checked = Boolean(field.value); else input.value = field.value;
      input.required = Boolean(field.required); input.readOnly = Boolean(field.readOnly);
      if (field.min !== undefined) input.min = field.min;
      if (field.max !== undefined) input.max = field.max;
      if (field.maxLength) input.maxLength = field.maxLength;
      if (field.type === 'password') input.autocomplete = field.name === 'current' ? 'current-password' : 'new-password';
      form.append(label,input);
    }
    const error = element('p','error'); error.id = 'dialog-error'; error.setAttribute('role','alert');
    form.querySelectorAll('input,textarea,select').forEach(input => input.setAttribute('aria-describedby','dialog-error'));
    const send = element('button','primary',options.submitText || 'Lưu'); send.type = 'submit';
    form.append(error,send); content.append(form);
    if (options.forced) { close.hidden = true; dialog.oncancel = event => event.preventDefault(); }
    form.onsubmit = async event => {
      event.preventDefault(); send.disabled = true; error.textContent = '';
      const data = Object.fromEntries(new FormData(form));
      for (const input of form.querySelectorAll('[type=checkbox]')) data[input.name] = String(input.checked);
      try { await submit(data,form); dialog.close(); }
      catch (failure) { error.textContent = failure.message; }
      finally { send.disabled = false; }
    };
    return { dialog,form,content,error };
  }
  roomForm(room) {
    return this.form(room?'Sửa phòng':'Tạo phòng',[
      this.field('name','Tên phòng','text',room?.name || '',{required:true,maxLength:30}),
      this.field('description','Mô tả','text',room?.description || '',{maxLength:80}),
      this.field('publicRoom','Phòng công khai','checkbox',room?.publicRoom ?? true),
      this.field('capacity','Giới hạn thành viên','number',room?.capacity || 50,{min:2,max:500,required:true}),
      ...(room?[this.field('keepPassword','Giữ mật khẩu phòng hiện tại','checkbox',true)]:[]),
      this.field('password','Mật khẩu phòng (để trống nếu không dùng)','password')
    ],async data => {
      const result = await this.action(room?'room-edit':'room-create',{...data,roomId:room?.id || ''});
      await this.refresh(); if (!room) await this.selectRoom(result.roomId);
    });
  }
  async invitationList() {
    await this.refresh();
    const {content} = this.open('Lời mời tham gia phòng');
    if (!this.snapshot.invitations.length) content.append(element('p','muted','Bạn chưa có lời mời đang chờ.'));
    for (const invite of this.snapshot.invitations) {
      const card = element('section','info-card');
      card.append(element('h3','',invite.tenPhong),element('p','',`${invite.tenNguoiMoi} đã mời bạn. Lời mời có hạn 7 ngày.`));
      this.button(card,'Chấp nhận',async () => {
        await this.action('invite-answer',{inviteId:invite.inviteId,accept:'true'}); this.dialog.close(); await this.refresh(); await this.selectRoom(invite.roomId);
      });
      this.button(card,'Từ chối',async () => { await this.action('invite-answer',{inviteId:invite.inviteId,accept:'false'}); await this.invitationList(); });
      content.append(card);
    }
  }
  button(parent,text,callback) {
    const button = element('button','secondary',text); button.type='button';
    button.onclick = async () => {
      button.disabled=true;
      try { await callback(); }
      catch (error) { let node=parent.querySelector('.action-error'); if (!node) { node=element('p','error action-error'); node.setAttribute('role','alert'); parent.append(node); } node.textContent=error.message; }
      finally { if (button.isConnected) button.disabled=false; }
    };
    parent.append(button); return button;
  }
  async roomInfo() {
    const room=this.room(); if (!room?.joined) return;
    const members=await this.api('/members?roomId='+room.id);
    const {content}=this.open('Thông tin · '+room.name);
    content.append(element('p','',room.description),element('p','',`${room.memberCount} thành viên · ${room.onlineCount} đang trực tuyến`),element('p','room-code','Mã phòng: '+room.code));
    const exports=element('section','room-export info-card');exports.append(element('h3','','Xuất lịch sử trò chuyện'),element('p','muted','Bản HTML giữ bố cục hội thoại, dễ đọc và in. Tệp và ảnh được ghi bằng tên; không nhúng dữ liệu nhị phân.'));
    this.button(exports,'Tải bản dễ đọc (.html)',()=>this.exportHistory(room.id,'html'));
    this.button(exports,'Tải văn bản (.txt)',()=>this.exportHistory(room.id,'txt'));content.append(exports);
    this.button(content,room.notificationsMuted?'Bật thông báo phòng':'Tắt thông báo phòng',async () => { await this.action('notifications',{roomId:room.id,muted:String(!room.notificationsMuted)}); await this.refresh(); await this.roomInfo(); });
    if (room.manage && room.id!=='sanh') {
      this.button(content,'Sửa phòng',() => this.roomForm(room));
      this.button(content,room.open?'Đóng phòng':'Mở phòng',async () => { await this.action('room-state',{roomId:room.id,open:String(!room.open)}); await this.refresh(); await this.roomInfo(); });
      this.button(content,'Mời thành viên',() => this.inviteForm(room));
      this.button(content,'Danh sách cấm / gỡ cấm',() => this.restrictions(room));
      this.button(content,'Xóa phòng',() => this.form('Xóa phòng '+room.name,[this.field('confirm','Nhập XÓA để xác nhận')],async data => {
        if (data.confirm!=='XÓA') throw new Error('Hãy nhập XÓA để xác nhận.');
        await this.action('room-delete',{roomId:room.id}); await this.refresh(); await this.selectRoom('sanh');
      },{submitText:'Xóa phòng'}));
    }
    if (room.id!=='sanh' && room.ownerId!==this.snapshot.profile.id) this.button(content,'Rời phòng',() => this.form('Rời '+room.name,[],async () => {
      await this.action('room-leave',{roomId:room.id}); await this.refresh(); await this.selectRoom('sanh');
    },{submitText:'Rời phòng'}));
    content.append(element('h3','','Thành viên'));
    for (const person of members) {
      const card=element('section','info-card');
      card.dataset.memberId=person.id;
      card.append(element('strong','',person.nickname||person.name),element('p','muted',`@${person.login} · ${person.online?'Trực tuyến':'Ngoại tuyến'}${person.id===room.ownerId?' · Chủ phòng':''}`));
      if(person.mutedUntil>Date.now())card.append(element('p','moderation-label',`Cấm chat đến ${new Date(person.mutedUntil).toLocaleString('vi-VN')}`));
      if (person.id===this.snapshot.profile.id || room.manage) this.button(card,'Đổi biệt danh',() => this.form('Biệt danh trong '+room.name,[this.field('nickname','Biệt danh (trống để xóa)','text',person.nickname,{maxLength:30})],async data => { await this.action('member-nickname',{roomId:room.id,userId:person.id,...data}); await this.refresh(); }));
      if (room.manage && person.id!==this.snapshot.profile.id && person.id!==room.ownerId && person.role!=='ADMIN') {
        this.button(card,'Quản lý thành viên',() => this.memberActions(room,person));
      }
      content.append(card);
    }
    if (room.manage) {
      const sent=await this.api('/invitations/sent?roomId='+room.id);
      if (!content.isConnected) return;
      content.append(element('h3','','Lời mời đã gửi'));
      for (const invitation of sent) {
        const card=element('section','info-card'); card.append(element('p','','Đang mời tài khoản: '+invitation.nguoiDuocMoiId));
        this.button(card,'Hủy lời mời',async () => { await this.action('invite-cancel',{inviteId:invitation.id}); await this.roomInfo(); }); content.append(card);
      }
    }
  }
  memberActions(room,person) {
    const {content}=this.open('Quản lý '+person.name+' · @'+person.login);
    for (const [title,kind] of [['Cấm chat có thời hạn','MUTE'],['Cấm vào phòng có thời hạn','BAN']]) this.button(content,title,() => this.form(title,[this.field('seconds','Thời hạn (giây)','number','300',{min:1,max:31536000,required:true}),this.field('reason','Lý do (tối đa 200 ký tự)','text','',{maxLength:200})],async data => { await this.action('member-moderate',{roomId:room.id,userId:person.id,kind,...data}); await this.refresh(); }));
    this.button(content,'Gỡ cấm chat',async () => { await this.action('member-moderate',{roomId:room.id,userId:person.id,kind:'MUTE',seconds:'-1'}); await this.refresh(); this.dialog.close(); });
    this.button(content,'Đuổi khỏi phòng',() => this.form('Đuổi '+person.name,[this.field('reason','Lý do (tối đa 200 ký tự)','text','',{maxLength:200})],async data => { await this.action('member-moderate',{roomId:room.id,userId:person.id,kind:'KICK',reason:data.reason}); await this.refresh(); },{submitText:'Đuổi khỏi phòng'}));
    this.button(content,'Trở lại thông tin phòng',()=>this.roomInfo());
    this.button(content,'Chuyển quyền chủ phòng',() => this.form('Chuyển quyền cho '+person.name,[],async () => { await this.action('room-transfer',{roomId:room.id,userId:person.id}); await this.refresh(); },{submitText:'Chuyển quyền'}));
  }
  async restrictions(room) {
    const rules=await this.api('/moderation?roomId='+encodeURIComponent(room.id));
    const {content}=this.open('Danh sách cấm · '+room.name);
    content.append(element('p','muted','Lệnh còn hiệu lực trong phòng. Gỡ cấm vào không tự thêm lại thành viên; họ có thể tham gia lại theo quyền và sức chứa.'));
    if(!rules.length)content.append(element('p','','Không có lệnh cấm còn hiệu lực.'));
    for(const rule of rules) {
      const card=element('section','info-card restriction-card');card.dataset.userId=rule.userId;card.dataset.kind=rule.kind;
      card.append(element('h3','',rule.name+' · @'+rule.login),element('p','moderation-label',`${rule.kind==='MUTE'?'Cấm chat':'Cấm vào phòng'} đến ${new Date(rule.until).toLocaleString('vi-VN')}`));
      if(rule.reason)card.append(element('p','','Lý do: '+rule.reason));
      this.button(card,rule.kind==='MUTE'?'Gỡ cấm chat':'Gỡ cấm vào phòng',async()=>{await this.action('member-moderate',{roomId:room.id,userId:rule.userId,kind:rule.kind,seconds:'-1'});await this.refresh();await this.restrictions(room);});content.append(card);
    }
    this.button(content,'Trở lại thông tin phòng',()=>this.roomInfo());
  }
  inviteForm(room) {
    this.findPerson('Mời vào '+room.name,async person => { await this.action('invite-send',{roomId:room.id,userId:person.id}); await this.refresh(); },'Gửi lời mời');
  }
  findPerson(title,choose,buttonText='Gỡ cấm') {
    const {content}=this.open(title), form=element('form'), label=element('label','','Tên hoặc tài khoản'), input=element('input');
    input.id='person-query';input.required=true;input.minLength=2;label.htmlFor=input.id;form.append(label,input);
    const send=element('button','primary','Tìm người');send.type='submit';form.append(send);content.append(form);
    const results=element('div');content.append(results);
    form.onsubmit=async event=>{
      event.preventDefault();send.disabled=true;
      try {
        const people=await this.api('/users/search?q='+encodeURIComponent(input.value));results.replaceChildren();
        if (!people.length) results.append(element('p','muted','Không tìm thấy người dùng.'));
        for (const person of people) {
          const card=element('section','info-card');card.append(element('p','',person.name+' · @'+person.login));
          this.button(card,buttonText,async()=>{await choose(person);card.replaceChildren(element('p','','Đã thực hiện.'));});results.append(card);
        }
      } catch(error){results.textContent=error.message;} finally{send.disabled=false;}
    };
  }
  profileForm() {
    const profile=this.snapshot.profile;
    const {form,content,error}=this.form('Hồ sơ của tôi',[
      this.field('login','Tên đăng nhập cố định','text',profile.login,{readOnly:true}),
      this.field('name','Tên hiển thị','text',profile.name,{required:true,maxLength:30}),
      this.field('about','Giới thiệu','text',profile.about,{maxLength:100})
    ],async data=>{
      await this.action('profile-save',{name:data.name,about:data.about,avatarMode:this.avatarMode,avatar:this.avatarData||''});await this.refresh();
    });
    this.avatarMode='KEEP';this.avatarData='';
    const image=element('img','profile-preview');image.alt='Xem trước ảnh đại diện';image.width=96;image.height=96;
    image.hidden=!profile.avatarHash;if(profile.avatarHash)image.src='/api/avatars/'+profile.id+'?v='+profile.avatarHash;
    const file=element('input');file.type='file';file.accept='image/png,image/jpeg';file.id='avatar-file';file.setAttribute('aria-label','Chọn ảnh đại diện PNG hoặc JPG');
    file.onchange=async()=>{
      const item=file.files[0];if(!item)return;
      try {
        if(item.size>2*1024*1024||!['image/png','image/jpeg'].includes(item.type))throw new Error('Chọn PNG/JPG tối đa 2 MB.');
        const bitmap=await createImageBitmap(item);
        if(bitmap.width*bitmap.height>16000000){bitmap.close();throw new Error('Ảnh tối đa 16 triệu điểm ảnh.');}
        const canvas=document.createElement('canvas');canvas.width=canvas.height=96;
        const side=Math.min(bitmap.width,bitmap.height);canvas.getContext('2d').drawImage(bitmap,(bitmap.width-side)/2,(bitmap.height-side)/2,side,side,0,0,96,96);bitmap.close();
        const data=canvas.toDataURL('image/png');this.avatarData=data.split(',')[1];this.avatarMode='SET';image.src=data;image.hidden=false;error.textContent='';
      }catch(failure){error.textContent=failure.message;}
    };
    form.insertBefore(image,form.firstChild);form.insertBefore(file,form.firstChild);
    this.button(content,'Xóa ảnh đại diện',()=>{this.avatarMode='DELETE';image.hidden=true;file.value='';});
    this.button(content,'Đổi mật khẩu',()=>this.passwordForm(false));
  }
  passwordForm(forced) {
    if(forced && this.dialog?.dataset.forced==='true')return;
    const {dialog,content}=this.form(forced?'Đổi mật khẩu trước khi tiếp tục':'Đổi mật khẩu',[
      this.field('current','Mật khẩu hiện tại','password','',{required:true}),this.field('next','Mật khẩu mới','password','',{required:true,maxLength:128})
    ],async data=>{await this.action('password-change',data);await this.refresh();},{forced});
    if(forced){dialog.dataset.forced='true';this.button(content,'Đăng xuất',async()=>{await $('logout').onclick();if(!$('workspace').hidden)throw new Error('Chưa đăng xuất được. Hãy thử lại.');});}
  }
  async stickers() {
    const catalog=await this.api('/stickers');const {content}=this.open('Chọn sticker');
    const search=element('input');search.placeholder='Tìm sticker';search.setAttribute('aria-label','Tìm sticker');content.append(search);
    const group=element('select');group.setAttribute('aria-label','Nhóm sticker');
    const all=element('option','','Tất cả nhóm');all.value='';group.append(all);
    catalog.groups.forEach((name,index)=>{const option=element('option','',name);option.value=index;group.append(option);});content.append(group);
    const grid=element('div','sticker-grid');content.append(grid);
    const draw=()=>{
      grid.replaceChildren();
      for(const item of catalog.items.filter(item=>(!group.value||item.nhom===Number(group.value))&&item.ten.toLocaleLowerCase('vi').includes(search.value.toLocaleLowerCase('vi')))){
        const button=element('button','sticker-choice');button.type='button';button.setAttribute('aria-label',item.ten);
        const img=element('img');img.src='/api/stickers/'+item.ma+'.png';img.alt=item.ten;img.width=64;img.height=64;button.append(img,element('span','',item.ten));
        button.onclick=()=>{this.sendSticker(item.ma);this.dialog.close();};grid.append(button);
      }
      if(!grid.childElementCount)grid.append(element('p','muted','Không có sticker phù hợp.'));
    };
    search.oninput=draw;group.onchange=draw;draw();
  }
  searchDialog() {
    const room=this.room();const {content}=this.open('Tìm tin · '+room.name);
    const form=element('form');const input=element('input');input.required=true;input.maxLength=100;input.setAttribute('aria-label','Từ khóa tin nhắn');
    const search=element('button','primary','Tìm tin');search.type='submit';form.append(input,search);content.append(form);
    const results=element('div');content.append(results);let cursor=0,query='';
    const more=this.button(content,'Kết quả tiếp theo',()=>load());more.hidden=true;
    const load=async()=>{
      const page=await this.api('/search?roomId='+room.id+'&q='+encodeURIComponent(query)+'&after='+cursor);
      results.replaceChildren();
      if(!page.length&&!cursor)results.append(element('p','muted','Không tìm thấy tin phù hợp.'));
      for(const message of page){const card=element('section','info-card');card.dataset.searchMsgId=message.msgId;card.dataset.roomId=room.id;card.append(element('strong','',message.nickname||message.name),element('p','',message.text));this.button(card,'Tới tin nhắn',async()=>{const dialog=this.dialog;await new Promise(resolve=>{dialog.addEventListener('close',resolve,{once:true});dialog.close();});await this.jump(message.msgId);});results.append(card);cursor=message.msgId;}
      more.hidden=page.length<50;
    };
    form.onsubmit=async event=>{event.preventDefault();query=input.value;cursor=0;search.disabled=true;try{await load();}catch(error){results.textContent=error.message;}finally{search.disabled=false;}};
  }
  async exportHistory(roomId,format) {
    try {
      const response=await fetch('/api/export?roomId='+roomId+'&format='+format);if(!response.ok)throw new Error('Không thể xuất lịch sử.');
      const url=URL.createObjectURL(await response.blob());const link=element('a');link.href=url;link.download='lich-su.'+format;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    }catch(error){const {content}=this.open('Xuất lịch sử');content.append(element('p','error',error.message));}
  }
}
