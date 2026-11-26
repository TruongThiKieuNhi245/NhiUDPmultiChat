import { element, fileSize } from './chat-view.js';
const $=id=>document.getElementById(id);
const TITLES={overview:'Tổng quan',users:'Người dùng',rooms:'Phòng',audit:'Nhật ký',reports:'Báo cáo'};
const ROLES=[['THANH_VIEN','Thành viên'],['ADMIN','Quản trị viên']];
export class AdminUI {
  revision=0;active=false;tab='overview';snapshot;timer;loading=false;
  constructor({api,ui,open,close}) {
    Object.assign(this,{api,ui,onOpen:open,onClose:close});
    $('admin-link').onclick=e=>{e.preventDefault();this.open();};
    $('admin-back').onclick=()=>this.close();$('admin-refresh').onclick=()=>this.refresh().catch(e=>this.error(e));
    document.querySelectorAll('[data-admin-tab]').forEach(button=>button.onclick=()=>{this.tab=button.dataset.adminTab;if(this.tab==='reports')clearTimeout(this.timer);this.draw();});
    addEventListener('popstate',()=>location.pathname==='/admin'?this.open(false):this.close(false));
  }
  updateProfile(profile) {
    this.profile=profile;const allowed=profile.role==='ADMIN'&&!profile.forcePassword;
    $('admin-link').hidden=!allowed;$('admin-account').textContent='@'+profile.login;
    if(location.pathname==='/admin' && !profile.forcePassword && !this.active)this.open(false);
    if(this.active && !allowed)this.deny();
  }
  reset() {
    this.revision++;clearTimeout(this.timer);this.loading=false;this.active=false;this.snapshot=null;this.profile=null;this.tab='overview';
    $('admin-page').hidden=true;$('admin-link').hidden=true;$('admin-content').replaceChildren();$('admin-stale').hidden=true;
    for(const id of ['admin-error','admin-time','admin-account','admin-status'])$(id).textContent='';
  }
  open(push=true) {
    if(!this.profile)return;
    this.active=true;this.onOpen();$('admin-page').hidden=false;
    if(push && location.pathname!=='/admin')history.pushState({},'','/admin');
    if(this.profile.role!=='ADMIN' || this.profile.forcePassword){this.deny();return;}
    $('admin-nav').hidden=false;$('admin-refresh').hidden=false;
    if(!this.snapshot)this.refresh().catch(e=>this.error(e));else this.draw();
    $('admin-main').focus();
  }
  close(push=true) {this.active=false;clearTimeout(this.timer);$('admin-page').hidden=true;this.onClose();if(push && location.pathname==='/admin')history.pushState({},'','/');}
  deny() {$('admin-nav').hidden=true;$('admin-refresh').hidden=true;$('admin-title').textContent='Không có quyền quản trị';$('admin-time').textContent='';$('admin-content').replaceChildren(element('p','','Hãy đăng nhập bằng tài khoản quản trị viên.'));}
  error(error) {$('admin-error').textContent=error.message;}
  async request(path,options) {const revision=this.revision;const result=await this.api(path,options);if(revision!==this.revision)throw new Error('Phiên đã thay đổi.');return result;}
  invalidate() {
    if(!this.active || !this.snapshot)return;
    $('admin-stale').hidden=false;
    clearTimeout(this.timer);
    if(this.tab!=='reports')this.timer=setTimeout(()=>this.refresh().catch(e=>this.error(e)),1800);
  }
  async refresh() {
    if(this.loading || !this.active)return;
    const revision=this.revision;clearTimeout(this.timer);const startedTab=this.tab;this.loading=true;$('admin-refresh').disabled=true;$('admin-error').textContent='';
    try {const snapshot=await this.request('/admin/snapshot');if(this.tab==='reports' && startedTab!=='reports'){$('admin-stale').hidden=false;return;}this.snapshot=snapshot;$('admin-stale').hidden=true;this.draw();}
    catch(error){if(revision===this.revision)throw error;}
    finally{if(revision===this.revision){this.loading=false;$('admin-refresh').disabled=false;}}
  }
  rows(key) {return this.snapshot?.data.bang[key] || [];}
  field(...args) {return this.ui.field(...args);}
  async action(name,data) {const result=await this.request('/admin/actions/'+name,{method:'POST',body:JSON.stringify(data)});this.ui.success(name,data,result);await this.refresh();return result;}
  form(title,fields,action,options={}) {return this.ui.form(title,fields,async data=>{await this.action(action,data);},options);}
  button(parent,text,callback,primary=false) {const button=this.ui.button(parent,text,callback);if(primary)button.className='primary';return button;}
  draw() {
    if(!this.active || !this.snapshot)return;
    $('admin-title').textContent=TITLES[this.tab];$('admin-time').textContent='Bản dữ liệu lúc '+new Date(this.snapshot.data.luc).toLocaleString('vi-VN');
    document.querySelectorAll('[data-admin-tab]').forEach(button=>{button.setAttribute('aria-current',button.dataset.adminTab===this.tab?'page':'false');});
    const content=$('admin-content');content.replaceChildren();$('admin-error').textContent='';
    if(this.tab==='overview')this.overview(content);
    if(this.tab==='users')this.users(content);
    if(this.tab==='rooms')this.rooms(content);
    if(this.tab==='audit')this.audit(content);
    if(this.tab==='reports')this.reports(content);
  }
  cards(parent) {
    const cards=element('div','stat-grid');
    for(const row of this.rows('STATS')){const card=element('article','stat-card');card.append(element('span','',row[0]),element('strong','',row[0]==='Dung lượng tệp'?fileSize(Number(row[1])):row[1]));cards.append(card);}parent.append(cards);
  }
  table(parent,columns,rows,actions=null) {
    const wrap=element('div','admin-table-wrap'),table=element('table','admin-table');const head=element('thead'),header=element('tr');
    for(const label of columns)header.append(element('th','',label));if(actions)header.append(element('th','','Thao tác'));head.append(header);table.append(head);
    const body=element('tbody');
    for(const row of rows){const tr=element('tr');for(const cell of row.cells)tr.append(element('td','',String(cell)));if(actions){const td=element('td','table-actions');actions(td,row.source);tr.append(td);}body.append(tr);}
    if(!rows.length){const tr=element('tr'),td=element('td','','Chưa có dữ liệu phù hợp.');td.colSpan=columns.length+(actions?1:0);tr.append(td);body.append(tr);}
    table.append(body);wrap.append(table);parent.append(wrap);
  }
  paged(parent,columns,source,cells,actions,sortColumn=1) {
    const toolbar=element('div','table-toolbar'),input=element('input'),sort=element('select');
    input.placeholder='Tìm trong bảng…';input.setAttribute('aria-label','Tìm trong bảng');sort.setAttribute('aria-label','Sắp xếp bảng');
    for(const [value,label] of [['asc','Tên A–Z'],['desc','Tên Z–A']]){const option=element('option','',label);option.value=value;sort.append(option);}
    toolbar.append(input,sort);parent.append(toolbar);const target=element('div'),pager=element('div','table-pager');parent.append(target,pager);let page=0;
    const draw=()=>{
      const query=input.value.toLocaleLowerCase('vi'),collator=new Intl.Collator('vi');
      const rows=source.filter(row=>row.join(' ').toLocaleLowerCase('vi').includes(query)).sort((a,b)=>collator.compare(a[sortColumn],b[sortColumn])*(sort.value==='asc'?1:-1));
      page=Math.min(page,Math.max(0,Math.ceil(rows.length/25)-1));target.replaceChildren();pager.replaceChildren();
      this.table(target,columns,rows.slice(page*25,page*25+25).map(row=>({cells:cells(row),source:row})),actions);
      const previous=this.button(pager,'Trang trước',()=>{page--;draw();}),next=this.button(pager,'Trang sau',()=>{page++;draw();});previous.disabled=page===0;next.disabled=(page+1)*25>=rows.length;
      pager.append(element('span','muted',`${rows.length} dòng · Trang ${page+1}/${Math.max(1,Math.ceil(rows.length/25))}`));
    };input.oninput=()=>{page=0;draw();};sort.onchange=draw;draw();
  }
  chart(parent,title,rows,nameIndex,countIndex) {
    const card=element('section','admin-chart');card.append(element('h2','',title));
    const max=Math.max(1,...rows.map(r=>Number(r[countIndex])));
    if(!rows.length)card.append(element('p','muted','Chưa có dữ liệu.'));
    for(const row of rows){const label=element('label','chart-line'),meter=element('meter');meter.min=0;meter.max=max;meter.value=Number(row[countIndex]);meter.setAttribute('aria-label',row[nameIndex]+': '+row[countIndex]);label.append(element('span','',row[nameIndex]),meter,element('b','',row[countIndex]));card.append(label);}parent.append(card);
  }
  overview(parent) {
    this.cards(parent);parent.append(element('p','muted report-definition','Tệp/dung lượng: tệp đã gửi còn lưu, không tính upload chờ. Thành viên gồm cả offline; online tính theo phiên đăng nhập.'));
    const tools=element('div','admin-tools');this.button(tools,'Mở báo cáo',()=>{this.tab='reports';this.draw();},true);this.button(tools,'Gửi thông báo toàn hệ thống',()=>this.broadcast());this.button(tools,'Xuất PDF tổng quan',()=>this.download('pdf-overview'));parent.append(tools);
    const charts=element('div','admin-charts');this.chart(charts,'Top người chat',[...this.rows('USERS')].sort((a,b)=>Number(b[6])-Number(a[6])).slice(0,5),2,6);this.chart(charts,'Phòng hoạt động',[...this.rows('ROOMS')].sort((a,b)=>Number(b[7])-Number(a[7])).slice(0,5),2,7);parent.append(charts);
    parent.append(element('h2','','Thành viên theo phòng'));
    this.table(parent,['Phòng','Thành viên','Online','Tin'],this.rows('ROOMS').map(r=>({cells:[r[2],r[6],r[14],r[7]]})));
  }
  users(parent) {
    const tools=element('div','admin-tools');this.button(tools,'Thêm người dùng',()=>this.userForm(),true);this.button(tools,'Kiểm duyệt tài khoản',()=>this.moderate());this.button(tools,'Danh sách cấm toàn hệ thống',()=>this.restrictions('*','Toàn hệ thống'));this.button(tools,'Xuất PDF người dùng',()=>this.download('pdf-users'));parent.append(tools);
    this.paged(parent,['Tài khoản','Tên hiển thị','Vai trò','Trạng thái','Số tin'],this.rows('USERS'),r=>['@'+r[1],r[2],r[3]==='ADMIN'?'Quản trị':'Thành viên',r[4],r[6]],(td,row)=>{this.button(td,'Quản lý',()=>this.userDetails(row));});
  }
  userForm(row) {
    const fields=row?[this.field('id','ID','text',row[0],{readOnly:true}),this.field('name','Tên hiển thị','text',row[2],{required:true,maxLength:30}),this.field('about','Giới thiệu','text',row[11],{maxLength:100})]
      :[this.field('login','Tên đăng nhập','text','',{required:true,maxLength:20}),this.field('name','Tên hiển thị','text','',{required:true,maxLength:30}),this.field('password','Mật khẩu tạm','password','',{required:true,maxLength:128})];
    fields.push(this.field('role','Vai trò','text',row?.[3]||'THANH_VIEN',{options:ROLES}));
    return this.form(row?'Sửa người dùng':'Thêm người dùng',fields,row?'user-edit':'user-add');
  }
  dlgAction(parent,text,callback,extraClass='') {
    const btn=element('button','rp-item'+(extraClass?' '+extraClass:''));btn.type='button';
    btn.append(element('span','',text),element('span','rp-arrow','›'));
    btn.onclick=async()=>{btn.disabled=true;try{await callback();}catch(error){this.error(error);}finally{if(btn.isConnected)btn.disabled=false;}};
    parent.append(btn);return btn;
  }
  userDetails(row) {
    const {content}=this.ui.open('Quản lý @'+row[1]);
    const summary=element('div','dlg-summary');
    summary.append(element('strong','',row[2]),element('p','muted',`@${row[1]} · ${row[3]==='ADMIN'?'Quản trị':'Thành viên'} · ${row[4]}`));
    content.append(summary);
    const manage=element('section','rp-group');manage.append(element('div','rp-group-title','Tài khoản & Bảo mật'));
    this.dlgAction(manage,'Sửa hồ sơ và vai trò',()=>this.userForm(row));
    this.dlgAction(manage,'Cấp lại mật khẩu tạm',()=>{
      const revision=this.revision;this.ui.form('Cấp mật khẩu tạm cho @'+row[1],[],async()=>{
        const result=await this.action('user-reset',{id:row[0]});setTimeout(()=>{
          if(!this.active || revision!==this.revision)return;const {content}=this.ui.open('Mật khẩu tạm mới');content.append(element('p','','Phiên cũ đã bị thu hồi. Mật khẩu chỉ hiển thị tại đây; người dùng phải đổi sau khi đăng nhập.'),element('code','temporary-password',result.temporaryPassword));
        },0);
      },{submitText:'Cấp mật khẩu tạm'});
    });
    this.dlgAction(manage,'Kiểm duyệt tài khoản',()=>this.moderate(row[0]));
    const danger=element('section','rp-group');danger.append(element('div','rp-group-title','Hạn chế & Xóa'));
    this.dlgAction(danger,row[4]==='Bị khóa'?'Mở khóa tài khoản':'Khóa tài khoản',()=>this.form('Xác nhận '+(row[4]==='Bị khóa'?'mở khóa':'khóa'),[this.field('id','ID','text',row[0],{readOnly:true}),this.field('locked','Khóa tài khoản','checkbox',row[4]!=='Bị khóa'),this.field('reason','Lý do','text','',{maxLength:200})],'user-lock'),row[4]==='Bị khóa'?'':'rp-danger');
    this.dlgAction(danger,'Xóa tài khoản',()=>this.form('Xóa @'+row[1],[this.field('id','ID','text',row[0],{readOnly:true}),this.field('confirm','Nhập tên đăng nhập để xác nhận','text','',{required:true})],'user-delete',{submitText:'Xóa tài khoản'}),'rp-danger');
    content.append(manage,danger);
  }
  roomForm(row) {
    const fields=[];if(row)fields.push(this.field('id','ID phòng','text',row[0],{readOnly:true}));
    fields.push(this.field('name','Tên phòng','text',row?.[2]||'',{required:true,maxLength:30}),this.field('description','Mô tả','text',row?.[11]||'',{maxLength:100}),this.field('publicRoom','Phòng công khai','checkbox',!row || row[3]==='Công khai'),this.field('password','Mật khẩu phòng mới (trống để bỏ)','password','',{maxLength:128}),this.field('capacity','Giới hạn thành viên','number',row?.[13]||'30',{min:2,max:500,required:true}));
    if(row)fields.push(this.field('keepPassword','Giữ mật khẩu hiện tại','checkbox',true));
    else fields.push(this.field('ownerId','Chủ phòng','text',this.profile.id,{options:this.rows('USERS').map(r=>[r[0],'@'+r[1]+' · '+r[2]])}));
    return this.form(row?'Sửa phòng':'Tạo phòng',fields,row?'room-edit':'room-create');
  }
  rooms(parent) {
    const tools=element('div','admin-tools');this.button(tools,'Tạo phòng',()=>this.roomForm(),true);this.button(tools,'Kiểm duyệt thành viên',()=>this.moderate());this.button(tools,'Xuất PDF phòng',()=>this.download('pdf-rooms'));parent.append(tools);
    this.paged(parent,['Phòng','Loại','Trạng thái','Thành viên','Online','Tin'],this.rows('ROOMS'),r=>[r[2],r[3],r[4],r[6],r[14],r[7]],(td,row)=>this.button(td,'Quản lý',()=>this.roomDetails(row)),2);
  }
  roomDetails(row) {
    const {content}=this.ui.open('Quản lý '+row[2]);
    const summary=element('div','dlg-summary');
    summary.append(element('strong','',`${row[2]} · Mã: ${row[1]}`),element('p','muted',`${row[6]} thành viên · ${row[14]} trực tuyến · Chủ phòng: ${row[5]}`));
    content.append(summary);
    const manage=element('section','rp-group');manage.append(element('div','rp-group-title','Quản lý phòng'));
    this.dlgAction(manage,'Sửa thông tin phòng',()=>this.roomForm(row));
    this.dlgAction(manage,row[4]==='Mở'?'Đóng phòng':'Mở phòng',async()=>{await this.action('room-state',{id:row[0],open:String(row[4]!=='Mở')});this.ui.dialog.close();});
    this.dlgAction(manage,'Chuyển chủ phòng',()=>this.form('Chuyển chủ phòng',[this.field('id','ID phòng','text',row[0],{readOnly:true}),this.field('userId','Chủ phòng mới','text','',{options:this.rows('USERS').filter(r=>r[4]!=='Bị khóa').map(r=>[r[0],'@'+r[1]])})],'room-transfer'));
    this.dlgAction(manage,'Kiểm duyệt thành viên',()=>this.moderate('',row[0]));
    this.dlgAction(manage,'Danh sách cấm / gỡ cấm',()=>this.restrictions(row[0],row[2]));
    const danger=element('section','rp-group');danger.append(element('div','rp-group-title','Vùng nguy hiểm'));
    this.dlgAction(danger,'Xóa nội dung lịch sử',()=>this.ui.form('Xóa nội dung '+row[2],[this.field('confirm','Nhập XÓA để xác nhận')],async data=>{if(!['XÓA','XOÁ'].includes(data.confirm.trim().toUpperCase()))throw new Error('Hãy nhập XÓA.');await this.action('room-clear',{id:row[0]});},{submitText:'Xóa nội dung'}),'rp-danger');
    this.dlgAction(danger,'Xóa phòng',()=>this.ui.form('Xóa phòng '+row[2],[this.field('confirm','Nhập XÓA để xác nhận')],async data=>{if(!['XÓA','XOÁ'].includes(data.confirm.trim().toUpperCase()))throw new Error('Hãy nhập XÓA.');await this.action('room-delete',{id:row[0]});},{submitText:'Xóa phòng'}),'rp-danger');
    content.append(manage,danger);
    const members=this.rows('MEMBERS').filter(r=>r[0]===row[0]);content.append(element('h3','dlg-subheading',`Danh sách thành viên (${members.length})`));
    const roleLabel=v=>v==='ADMIN'?'Quản trị':v==='CHU_PHONG'?'Chủ phòng':'Thành viên';
    this.table(content,['Tài khoản','Vai trò','Online'],members.map(r=>({cells:['@'+r[3],roleLabel(r[5]),r[6]==='true'?'Có':'Không'],source:r})),(td,r)=>{if(r[2]!==this.profile.id)this.button(td,'Quản lý',()=>this.moderate(r[2],row[0]));});
  }
  async restrictions(roomId,name) {
    const previous=this.ui.dialog;
    const rules=await this.request('/moderation?roomId='+encodeURIComponent(roomId));
    if(previous && (this.ui.dialog!==previous || !previous.open))return;
    const {dialog,content}=this.ui.open('Danh sách cấm · '+name);
    if(!rules.length)content.append(element('p','','Không có lệnh cấm còn hiệu lực.'));
    for(const rule of rules){
      const card=element('section','info-card restriction-card');card.dataset.userId=rule.userId;card.dataset.kind=rule.kind;
      card.append(element('h3','',rule.name+' · @'+rule.login),element('p','moderation-label',`${rule.kind==='MUTE'?'Cấm chat':'Cấm vào phòng'} đến ${new Date(rule.until).toLocaleString('vi-VN')}`),element('p','',rule.reason));
      this.button(card,rule.kind==='MUTE'?'Gỡ cấm chat':'Gỡ cấm vào phòng',async()=>{await this.action('member-moderate',{roomId,userId:rule.userId,kind:rule.kind,seconds:'-1'});if(this.ui.dialog===dialog && dialog.open)await this.restrictions(roomId,name);});content.append(card);
    }
    content.append(element('p','muted','Gỡ cấm vào không tự thêm lại thành viên. Cấm chat toàn hệ thống và từng phòng được áp dụng độc lập.'));
  }
  moderate(userId='',roomId='*') {
    const modal=this.ui.form('Kiểm duyệt tài khoản/phòng',[
      this.field('userId','Tài khoản','text',userId,{options:this.rows('USERS').filter(r=>r[0]!==this.profile.id).map(r=>[r[0],'@'+r[1]])}),
      this.field('roomId','Phạm vi','text',roomId,{options:[['*','Toàn hệ thống (chỉ cấm chat)'],...this.rows('ROOMS').map(r=>[r[0],r[2]])]}),
      this.field('kind','Thao tác','text','MUTE',{options:[['MUTE','Cấm chat'],['UNMUTE','Gỡ cấm chat'],['BAN','Cấm vào phòng'],['UNBAN','Gỡ cấm vào phòng'],['KICK','Đuổi khỏi phòng']]}),
      this.field('seconds','Thời hạn (giây)','number','300',{required:true,min:1,max:31536000}),this.field('reason','Lý do','text','',{maxLength:200})
    ],async data=>{const remove=data.kind.startsWith('UN');await this.action('member-moderate',{...data,kind:remove?data.kind.slice(2):data.kind,seconds:remove?'-1':data.kind==='KICK'?'0':data.seconds});},{submitText:'Áp dụng'});
    const kind=modal.form.elements.kind,seconds=modal.form.elements.seconds;
    kind.onchange=()=>{seconds.disabled=kind.value.startsWith('UN') || kind.value==='KICK';seconds.required=!seconds.disabled;};
  }
  formatAuditRow(r, userMap, roomMap) {
    const time = Number.isNaN(Date.parse(r[0])) ? r[0] : new Date(r[0]).toLocaleString('vi-VN');
    const actor = r[1] ? '@' + r[1] : '—';
    const actMap = {
      WEB_ROOM_MODERATE: 'Kiểm duyệt thành viên', WEB_EXPORT_PDF: 'Xuất PDF tổng hợp',
      'WEB_EXPORT_PDF-OVERVIEW': 'Xuất PDF tổng quan', 'WEB_EXPORT_PDF-USERS': 'Xuất PDF người dùng',
      'WEB_EXPORT_PDF-ROOMS': 'Xuất PDF phòng', 'WEB_EXPORT_PDF-AUDIT': 'Xuất PDF nhật ký',
      WEB_EXPORT_ZIP: 'Xuất ZIP dữ liệu', WEB_EXPORT_HTML: 'Xuất HTML báo cáo', WEB_EXPORT_AUDIT: 'Xuất CSV nhật ký',
      WEB_BROADCAST: 'Thông báo toàn hệ thống', WEB_USER_DELETE: 'Xóa tài khoản',
      WEB_ROOM_DELETE: 'Xóa phòng', WEB_ROOM_CLEAR: 'Xóa lịch sử phòng',
      MUTE: 'Cấm chat', UNMUTE: 'Gỡ cấm chat', BAN: 'Cấm vào phòng', UNBAN: 'Gỡ cấm vào phòng', KICK: 'Đuổi khỏi phòng'
    };
    const action = actMap[r[2]] || r[2];
    const rawTarget = r[3] || '';
    let target = '—', scope = 'Toàn hệ thống';
    if (rawTarget.includes(':')) {
      const [rid, uid] = rawTarget.split(':');
      scope = rid === '*' ? 'Toàn hệ thống' : (roomMap.get(rid) || 'Phòng đã xóa');
      target = userMap.get(uid) || 'Tài khoản đã xóa';
    } else if (r[2].startsWith('WEB_EXPORT')) {
      target = 'Báo cáo hệ thống'; scope = 'Toàn hệ thống';
    } else if (r[2] === 'WEB_BROADCAST') {
      target = 'Tất cả thành viên'; scope = 'Toàn hệ thống';
    } else if (r[2] === 'WEB_ROOM_DELETE' || r[2] === 'WEB_ROOM_CLEAR') {
      target = 'Lịch sử / Phòng'; scope = roomMap.get(rawTarget) || 'Phòng đã xóa';
    } else if (userMap.has(rawTarget)) {
      target = userMap.get(rawTarget);
    } else if (roomMap.has(rawTarget)) {
      target = 'Phòng'; scope = roomMap.get(rawTarget);
    } else if (rawTarget && rawTarget !== '*') {
      target = rawTarget.length > 20 ? 'Hệ thống' : rawTarget;
    }
    let detail = r[4] || '—';
    detail = detail
      .replace(/^MUTE;\s*thời hạn giây=-1$/, 'Gỡ cấm chat')
      .replace(/^BAN;\s*thời hạn giây=-1$/, 'Gỡ cấm vào phòng')
      .replace(/^MUTE;\s*thời hạn giây=(\d+)$/, 'Cấm chat · $1 giây')
      .replace(/^BAN;\s*thời hạn giây=(\d+)$/, 'Cấm vào phòng · $1 giây')
      .replace(/^KICK;\s*thời hạn giây=.*$/, 'Đuổi khỏi phòng');
    return [time, actor, action, target, scope, detail];
  }
  audit(parent) {
    const tools=element('div','admin-tools');this.button(tools,'Xuất PDF nhật ký',()=>this.download('pdf-audit'),true);this.button(tools,'Tải nhật ký CSV',()=>this.download('audit'));parent.append(tools);
    const userMap = new Map(this.rows('USERS').map(u => [u[0], `${u[2]} (@${u[1]})`]));
    const roomMap = new Map(this.rows('ROOMS').map(rm => [rm[0], rm[2]]));
    roomMap.set('sanh', 'Sảnh chung');
    this.paged(parent,['Thời gian','Người thực hiện','Hành động','Đối tượng','Phòng / Phạm vi','Chi tiết'],this.rows('AUDIT'),r=>this.formatAuditRow(r,userMap,roomMap),null,0);
  }
  reports(parent) {
    const intro=element('section','report-intro');intro.append(element('h2','','Báo cáo hệ thống'),element('p','','PDF A4 có dấu tiếng Việt, bảng thành viên theo phòng, biểu đồ tin theo ngày và top hoạt động. Số liệu xuất khớp bản đang xem.'));
    const buttons=element('div','admin-tools');this.button(buttons,'Tải báo cáo PDF',()=>this.download('pdf'),true);this.button(buttons,'Tải 4 CSV + HTML (.zip)',()=>this.download('zip'));this.button(buttons,'Tải bản HTML',()=>this.download('html'));intro.append(buttons);parent.append(intro);
    this.cards(parent);parent.append(element('h2','','Số người dùng theo từng phòng'));
    this.table(parent,['Phòng','Thành viên','Online','Tin','Tệp'],this.rows('ROOMS').map(r=>({cells:[r[2],r[6],r[14],r[7],r[8]]})));
    const days=new Map();for(const r of this.rows('DAYS'))days.set(r[0],(days.get(r[0])||0)+Number(r[2]));this.chart(parent,'Tin nhắn theo ngày',[...days].sort().slice(-14),0,1);
  }
  broadcast() {
    const id=btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(16)))).replaceAll('+','-').replaceAll('/','_').replaceAll('=','');
    const {content}=this.ui.form('Thông báo toàn hệ thống',[this.field('text','Nội dung thông báo','textarea','',{required:true,maxLength:200})],async data=>{
      const result=await this.request('/admin/broadcast',{method:'POST',body:JSON.stringify({id,text:data.text})});await this.refresh();
      if(result.failed)throw new Error(`Đã lưu ${result.committed} phòng; ${result.failed} phòng chưa được xác nhận. Bấm gửi lại giữ cùng mã yêu cầu.`);
    },{submitText:'Gửi thông báo'});
    content.prepend(element('p','muted','Thông báo đến mọi phòng, kể cả phòng riêng/đang đóng. Chỉ ghi nhận đã gửi sau khi backend lưu qua UDP.'));
  }
  async download(format) {
    const revision=this.revision,id=this.snapshot.id;$('admin-status').textContent='Đang tạo tệp…';
    const response=await fetch('/api/admin/reports/'+id+'/'+format);
    if(!response.ok){const body=await response.json().catch(()=>({}));throw new Error(body.error||'Không tải được báo cáo. Hãy làm mới dữ liệu rồi thử lại.');}
    const blob=await response.blob();if(revision!==this.revision)return;
    const names={pdf:'bao-cao.pdf','pdf-overview':'bao-cao-tong-quan.pdf','pdf-users':'bao-cao-nguoi-dung.pdf','pdf-rooms':'bao-cao-phong.pdf','pdf-audit':'bao-cao-nhat-ky.pdf',zip:'bao-cao-nang-cao.zip',audit:'nhatky.csv',html:'tong_hop.html'};
    const url=URL.createObjectURL(blob),link=element('a');link.href=url;link.download=names[format]||'bao-cao.pdf';link.click();setTimeout(()=>URL.revokeObjectURL(url),5000);$('admin-status').textContent='Đã tạo tệp từ bản dữ liệu đang xem.';
  }
}
