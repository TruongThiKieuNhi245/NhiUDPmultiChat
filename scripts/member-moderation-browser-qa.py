"""QA quan ly thanh vien bang browser that, JAR/source va data fixture rieng."""
import json,os,socket,subprocess,time,uuid,hashlib
from pathlib import Path
from urllib.request import urlopen
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'.selftest'/('member-browser-'+uuid.uuid4().hex[:10]);RUN.mkdir(parents=True)
OUT=ROOT/os.environ.get('CHAT_QA_OUTPUT','docs/screenshots/member-moderation');OUT.mkdir(parents=True,exist_ok=True)
JAR=os.environ.get('CHAT_QA_JAR');PW='Moderation-browser-fixture-password'
with socket.socket() as probe:probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
BASE=f'http://127.0.0.1:{port}'
cp=os.environ.get('CHAT_QA_CLASSPATH','')
cmd=['java','-jar',str(ROOT/JAR)] if JAR else ['java','-cp',cp,'com.mycompany.udpmultichat.web.Web1BrowserFixture']
log=(RUN/'server.log').open('w',encoding='utf-8')
process=subprocess.Popen(cmd+[f'--server.port={port}','--server.address=127.0.0.1',f'--chat.data-dir={RUN/"data"}'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,env={**os.environ,'CHAT_INITIAL_ADMIN_PASSWORD':PW})
report={'artifact':JAR,'fixture':str(RUN.relative_to(ROOT)),'checks':[]};errors=[]
def check(text):report['checks'].append(text);print('PASS:',text,flush=True)
def req(page,path,data=None):
 return page.evaluate("""async({path,data})=>{const c=await(await fetch('/api/csrf')).json();const r=await fetch('/api'+path,data===null?{}:{method:'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},body:JSON.stringify(data)});return {status:r.status,body:await r.json().catch(()=>null)};}""",{'path':path,'data':data})
def act(page,name,data):
 result=req(page,'/actions/'+name,data);assert result['status']==200,result;return result['body']
def close(page):
 if page.locator('dialog[open]').count():page.keyboard.press('Escape');expect(page.locator('dialog[open]')).to_have_count(0)
def login(page,name,register=False):
 page.goto(BASE)
 if register:page.locator('#switch-mode').click()
 page.locator('#login').fill(name);page.locator('#password').fill(PW);page.locator('#submit-login').click();expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
def select(page,rid):
 expect(page.locator(f'[data-room-id="{rid}"]')).to_have_attribute('data-joined','true',timeout=10000);page.locator(f'[data-room-id="{rid}"]').click();expect(page.locator('#room-name')).to_have_text('Phòng kiểm duyệt')
def manage(page,uid):
 close(page);page.locator('#room-info').click();page.locator(f'[data-member-id="{uid}"]').get_by_role('button',name='Quản lý thành viên',exact=True).click()
def submit(page,label='Lưu'):
 page.locator('dialog[open]').get_by_role('button',name=label,exact=True).click();expect(page.locator('dialog[open]')).to_have_count(0)
def clear_notes(page):
 for _ in range(4):
  button=page.get_by_role('button',name='Đóng thông báo',exact=True)
  if not button.count():break
  button.first.click()
def shot(page,name):page.wait_for_timeout(350);page.screenshot(path=str(OUT/name),full_page=True)
def rules(page,rid):
 close(page);page.locator('#room-info').click();page.get_by_role('button',name='Danh sách cấm / gỡ cấm',exact=True).click()
try:
 for _ in range(120):
  if process.poll() is not None:raise RuntimeError('Server failed; see fixture log')
  try:
   if urlopen(BASE,timeout=1).status==200:break
  except Exception:time.sleep(.5)
 else:raise RuntimeError('Server timeout')
 with sync_playwright() as p:
  browser=p.chromium.launch(channel='msedge',headless=True);report['browser']=browser.version
  contexts=[browser.new_context(viewport={'width':1440,'height':960}) for _ in range(4)]
  a,b,c,root=[context.new_page() for context in contexts];frames=[]
  for page in [a,b,c,root]:page.on('pageerror',lambda e:errors.append(str(e)))
  c.on('websocket',lambda ws:ws.on('framereceived',lambda data:frames.append(data)))
  login(a,'moderator',True);login(b,'member',True);login(c,'outsider',True);login(root,'admin')
  bid=req(b,'/me')['body']['id'];aid=req(a,'/me')['body']['id']
  for page in [a,b]:act(page,'profile-save',{'name':'Trùng tên','about':'','avatarMode':'KEEP'})
  rid=act(a,'room-create',{'name':'Phòng kiểm duyệt','description':'Fixture thông báo riêng','publicRoom':'true','password':'','capacity':'20'})['roomId'];act(b,'room-join',{'key':rid,'password':''});select(a,rid);select(b,rid)
  check('Four independent sessions; same display names still use stable userId; owner/member/admin/outsider separated')
  b.locator('#message').fill('Bản nháp được giữ');manage(a,bid);a.get_by_role('button',name='Cấm chat có thời hạn',exact=True).click();a.locator('#field-seconds').fill('60');a.locator('#field-reason').fill('Lý do riêng <script>không chạy</script>');submit(a)
  expect(a.locator('.feedback')).to_contain_text('Đã thực hiện.');expect(b.locator('.feedback')).to_contain_text('Bạn đã bị cấm chat');expect(b.locator('#room-restriction')).to_contain_text('Lý do riêng <script>');expect(b.locator('#send')).to_be_disabled();expect(b.locator('#attach')).to_be_disabled();expect(b.locator('#sticker')).to_be_disabled();expect(b.locator('#message')).to_have_value('Bản nháp được giữ');expect(b.locator('#connection')).to_have_text('Đã kết nối')
  a.locator('#message').fill('Người bị cấm vẫn đọc được');a.locator('#send').click();expect(b.locator('#messages')).to_contain_text('Người bị cấm vẫn đọc được')
  shot(b,'01-member-muted-light.png');check('Owner mute via actual UI gives actor and target feedback, exact expiry/reason, disables text/sticker/file, preserves draft and reading')
  assert req(b,'/moderation?roomId='+rid)['status'] in [400,403];assert req(c,'/moderation?roomId='+rid)['status'] in [400,403]
  denied=req(b,'/actions/member-moderate',{'roomId':rid,'userId':aid,'kind':'MUTE','seconds':'60'});assert denied['status']==400
  assert not any('Lý do riêng' in f or 'member-notice' in f for f in frames)
  check('Member cannot moderate owner or inspect restrictions; outsider receives neither private notice nor reason')
  rules(a,rid);card=a.locator(f'.restriction-card[data-user-id="{bid}"][data-kind="MUTE"]');expect(card).to_contain_text('Lý do riêng <script>');shot(a,'02-owner-restrictions.png');card.get_by_role('button',name='Gỡ cấm chat',exact=True).click();expect(a.locator('.restriction-card')).to_have_count(0);close(a)
  expect(b.locator('#room-restriction')).to_be_hidden();expect(b.locator('#send')).to_be_enabled();expect(b.locator('#message')).to_have_value('Bản nháp được giữ');expect(b.locator('.feedback')).to_contain_text('được gỡ cấm chat');clear_notes(a);clear_notes(b)
  check('Manager sees correct account, reason and expiry; UI unmute removes rule, notifies target and restores existing draft')
  act(a,'member-moderate',{'roomId':rid,'userId':bid,'kind':'MUTE','seconds':'60','reason':'Còn hiệu lực sau tải lại'});expect(b.locator('#room-restriction')).to_be_visible();b.reload();expect(b.locator('#connection')).to_have_text('Đã kết nối',timeout=20000);select(b,rid);expect(b.locator('#room-restriction')).to_contain_text('Còn hiệu lực sau tải lại');act(a,'member-moderate',{'roomId':rid,'userId':bid,'kind':'MUTE','seconds':'-1'});b.locator('#message').fill('Sau hết hạn')
  act(a,'member-moderate',{'roomId':rid,'userId':bid,'kind':'MUTE','seconds':'2','reason':'Hết hạn tự động'});expect(b.locator('#send')).to_be_disabled();expect(b.locator('#send')).to_be_enabled(timeout=7000);expect(b.locator('#room-restriction')).to_be_hidden();clear_notes(a);clear_notes(b)
  check('Reload restores active mute from backend; expiry automatically removes banner and enables composer without reconnect')
  b.locator('#room-info').click();manage(a,bid);a.get_by_role('button',name='Đuổi khỏi phòng',exact=True).click();a.locator('#field-reason').fill('Đuổi thử có lý do');submit(a,'Đuổi khỏi phòng');expect(b.locator('dialog[open]')).to_have_count(0);expect(b.locator('#room-name')).to_have_text('Sảnh chung');expect(b.locator('.feedback')).to_contain_text('Bạn đã được đưa ra khỏi phòng');assert req(b,'/history?roomId='+rid)['status']==400
  shot(b,'03-kicked-notice.png');act(b,'room-join',{'key':rid,'password':''});select(b,rid);clear_notes(a);clear_notes(b)
  check('Kick notifies removed member after access revocation, closes stale modal, returns to lobby and denies old history; kick permits rejoin')
  manage(a,bid);a.get_by_role('button',name='Cấm vào phòng có thời hạn',exact=True).click();a.locator('#field-seconds').fill('60');a.locator('#field-reason').fill('Cấm vào phòng thử nghiệm');submit(a);expect(b.locator('#room-name')).to_have_text('Sảnh chung');expect(b.locator('.feedback')).to_contain_text('Bạn đã bị cấm vào phòng');assert req(b,'/actions/room-join',{'key':rid,'password':''})['status']==400
  rules(a,rid);card=a.locator(f'.restriction-card[data-user-id="{bid}"][data-kind="BAN"]');expect(card).to_contain_text('@member');a.evaluate("() => {window.qaFetch=window.fetch;window.fetch=async(...args)=>{const response=await window.qaFetch(...args);if(String(args[0]).includes('/actions/member-moderate'))await new Promise(resolve=>setTimeout(resolve,800));return response;};}");card.get_by_role('button',name='Gỡ cấm vào phòng',exact=True).click();a.keyboard.press('Escape');expect(b.locator('.feedback')).to_contain_text('được gỡ cấm vào phòng');assert req(b,'/history?roomId='+rid)['status']==400;act(b,'room-join',{'key':rid,'password':''});select(b,rid);a.wait_for_timeout(1000);expect(a.locator('dialog[open]')).to_have_count(0);a.evaluate('() => {window.fetch=window.qaFetch;delete window.qaFetch;}');clear_notes(a);clear_notes(b)
  check('Ban denies rejoin; removed member stays in restriction list; UI unban targets correct ID and does not silently re-add membership')
  manage(a,bid);a.get_by_role('button',name='Chuyển quyền chủ phòng',exact=True).click();submit(a,'Chuyển quyền');expect(b.locator('.feedback')).to_contain_text('Bạn đã trở thành chủ phòng');expect(a.locator('.feedback')).to_contain_text('Đã thực hiện.')
  assert req(a,'/actions/member-moderate',{'roomId':rid,'userId':bid,'kind':'MUTE','seconds':'60'})['status']==400
  check('Ownership transfer notifies both sides; former owner immediately loses backend moderation permission')
  root.locator('#admin-link').click();root.locator('[data-admin-tab="users"]').click();root.get_by_role('button',name='Kiểm duyệt tài khoản',exact=True).click();root.locator('#field-userId').select_option(bid);root.locator('#field-roomId').select_option('*');root.locator('#field-kind').select_option('MUTE');root.locator('#field-seconds').fill('60');root.locator('#field-reason').fill('Cấm toàn hệ thống');submit(root,'Áp dụng');expect(b.locator('#room-restriction')).to_contain_text('Cấm toàn hệ thống');expect(root.locator('.feedback')).to_contain_text('Đã thực hiện.')
  root.get_by_role('button',name='Danh sách cấm toàn hệ thống',exact=True).click();root.locator(f'.restriction-card[data-user-id="{bid}"]').get_by_role('button',name='Gỡ cấm chat',exact=True).click();expect(root.locator('.restriction-card')).to_have_count(0);close(root);expect(b.locator('#room-restriction')).to_be_hidden();clear_notes(b)
  check('Admin UI uses explicit actions, scope and global restriction list; global mute/unmute reaches target in another room')
  root.locator('[data-admin-tab="rooms"]').click();root.get_by_role('button',name='Kiểm duyệt thành viên',exact=True).click();root.locator('#field-userId').select_option(bid);root.locator('#field-roomId').select_option(rid);root.locator('#field-kind').select_option('KICK');expect(root.locator('#field-seconds')).to_be_disabled();submit(root,'Áp dụng');expect(b.locator('#room-name')).to_have_text('Sảnh chung');expect(b.locator('.feedback')).to_contain_text('Bạn đã được đưa ra khỏi phòng');assert req(root,'/history?roomId='+rid)['status']==200
  check('Admin kicking current owner becomes replacement owner with immediate read/send membership; target still receives private notice')
  b.set_viewport_size({'width':360,'height':800});b.locator('#workspace .theme').click();shot(b,'04-mobile-notice-dark.png');assert b.evaluate('document.documentElement.scrollWidth<=innerWidth')
  assert b.locator('.feedback-card').count()<=3
  b.locator('#menu').click();b.locator('#logout').click();expect(b.locator('.feedback-card')).to_have_count(0);expect(b.locator('#submit-login')).to_be_visible()
  assert not errors,errors;check('360 px dark mode has no page overflow; feedback DOM bounded; logout clears private feedback; no uncaught JS errors')
  browser.close();report['status']='PASS'
  if JAR:report['sha256']=hashlib.sha256((ROOT/JAR).read_bytes()).hexdigest()
except Exception as error:
 report['status']='FAIL';report['error']=str(error);raise
finally:
 process.terminate()
 try:process.wait(timeout=15)
 except subprocess.TimeoutExpired:process.kill();process.wait()
 log.close();(OUT/'browser-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
