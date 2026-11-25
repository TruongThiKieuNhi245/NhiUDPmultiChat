"""Browser QA admin/PDF, fixture rieng; CHAT_QA_JAR chon artifact that."""
import csv,hashlib,io,json,os,socket,subprocess,time,uuid,zipfile
from pathlib import Path
from urllib.request import urlopen
import pymupdf as fitz
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'.selftest'/('web3-admin-'+uuid.uuid4().hex[:10]);RUN.mkdir(parents=True)
OUT=ROOT/os.environ.get('CHAT_QA_OUTPUT','docs/screenshots/web3-admin');OUT.mkdir(parents=True,exist_ok=True)
JAR=os.environ.get('CHAT_QA_JAR');PW='Admin-browser-fixture-private-password';BPW='Member-browser-fixture-password';NEW='Member-new-private-password'
with socket.socket() as free:free.bind(('127.0.0.1',0));port=free.getsockname()[1]
BASE=f'http://127.0.0.1:{port}'
cp=str(ROOT/'target/test-classes')+os.pathsep+str(ROOT/'target/classes')+os.pathsep+(ROOT/'target/web-classpath.txt').read_text().strip()
cp = os.environ.get('CHAT_QA_CLASSPATH', cp)
process=None;logs=[];report={'fixture':str(RUN.relative_to(ROOT)),'artifact':JAR,'checks':[]}
def check(name):report['checks'].append(name);print('PASS:',name,flush=True)
def start(number):
    global process
    log=(RUN/f'server-{number}.txt').open('w',encoding='utf-8');logs.append(log)
    command=['java','-jar',str(ROOT/JAR)] if JAR else ['java','-cp',cp,'com.mycompany.udpmultichat.web.Web1BrowserFixture']
    process=subprocess.Popen(command+[f'--server.port={port}','--server.address=127.0.0.1',f'--chat.data-dir={RUN/"data"}'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,env={**os.environ,'CHAT_INITIAL_ADMIN_PASSWORD':PW},creationflags=subprocess.CREATE_NO_WINDOW)
    for _ in range(120):
        if process.poll() is not None:raise RuntimeError((RUN/f'server-{number}.txt').read_text(encoding='utf-8')[-2000:])
        try:
            if urlopen(BASE,timeout=1).status==200:return
        except Exception:time.sleep(.5)
    raise RuntimeError('Startup timeout')
def stop():
    if process:
        process.terminate()
        try:process.wait(timeout=15)
        except subprocess.TimeoutExpired:process.kill();process.wait()
def req(page,path,data=None):
    return page.evaluate("""async({path,data})=>{const c=await(await fetch('/api/csrf')).json();const r=await fetch('/api'+path,data===null?{}:{method:'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},body:JSON.stringify(data)});return {status:r.status,body:await r.json().catch(()=>null)};}""",{'path':path,'data':data})
def action(page,name,data):
    result=req(page,'/admin/actions/'+name,data);assert result['status']==200,result;return result['body']
def login(page,name,password,register=False):
    page.goto(BASE)
    if register:page.get_by_role('button',name='Tạo tài khoản',exact=True).click()
    page.locator('#login').fill(name);page.locator('#password').fill(password);page.locator('#submit-login').click();expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
def close(page):
    if page.locator('dialog[open]').count():page.keyboard.press('Escape');expect(page.locator('dialog[open]')).to_have_count(0)
def tab(page,name):page.locator(f'[data-admin-tab="{name}"]').click()
def refresh(page):
    expect(page.locator('#admin-refresh')).to_be_enabled()
    with page.expect_response(lambda r:r.url.endswith('/api/admin/snapshot') and r.request.method=='GET') as result:page.locator('#admin-refresh').click()
    response=result.value;assert response.status==200,response.status;expect(page.locator('#admin-refresh')).to_be_enabled();return response.json()
def snap(page,name):page.wait_for_timeout(350);page.screenshot(path=str(OUT/name),full_page=True)
def send(page,text):
    page.locator('#message').fill(text);expect(page.locator('#send')).to_be_enabled();page.locator('#send').click();expect(page.locator('.message-row').last.locator('.message-state')).to_have_text('✓ Đã gửi',timeout=10000)
def user_manage(page,login):
    tab(page,'users');refresh(page);page.locator('tr').filter(has=page.locator('td',has_text='@'+login)).get_by_role('button',name='Quản lý',exact=True).click()
def save(page):page.locator('dialog[open]').get_by_role('button',name='Lưu',exact=True).click();expect(page.locator('dialog[open]')).to_have_count(0)
def pdf_check(bytes,path):
    path.write_bytes(bytes);document=fitz.open(stream=bytes,filetype='pdf');texts=[]
    for i,page in enumerate(document):
        assert abs(page.rect.width-595.276)<.2 and abs(page.rect.height-841.89)<.2
        text=page.get_text();texts.append(text);assert 'Trang '+str(i+1)+'/' in text
        for block in page.get_text('dict')['blocks']:
            if 'lines' not in block:continue
            for line in block['lines']:
                for span in line['spans']:
                    x0,y0,x1,y1=span['bbox'];assert x0>=34 and x1<=page.rect.width-30 and y0>=16 and y1<=page.rect.height-16,(i,span)
        page.get_pixmap(matrix=fitz.Matrix(1.25,1.25),alpha=False).save(str(OUT/f'pdf-page-{i+1:02}.png'))
    text='\n'.join(texts);assert 'BÁO CÁO QUẢN TRỊ' in text and 'Số người dùng theo từng phòng' in text
    report['pdfPages']=len(document);document.close();return text
try:
    start(1)
    with sync_playwright() as p:
        browser=p.chromium.launch(channel='msedge',headless=True);report['browser']=browser.version
        a=browser.new_context(viewport={'width':1440,'height':960},accept_downloads=True,color_scheme='light')
        b=browser.new_context(viewport={'width':1100,'height':900},accept_downloads=True,color_scheme='light')
        pa,pb=a.new_page(),b.new_page();errors=[];frames=[]
        def frame(payload):
            try:frames.append(json.loads(payload))
            except Exception:pass
        pa.on('websocket',lambda ws:ws.on('framereceived',frame))
        for page in (pa,pb):page.on('pageerror',lambda e:errors.append(str(e)))
        login(pb,'memberbob',BPW,True);login(pa,'admin',PW)
        expect(pb.locator('#admin-link')).to_be_hidden();assert req(pb,'/admin/snapshot')['status']==403
        assert req(pb,'/admin/actions/user-edit',{'id':req(pb,'/me')['body']['id'],'name':'Giả admin','role':'ADMIN','about':''})['status']==403
        assert pa.locator('#outbox').count()==0 and pa.locator('#export-chat').count()==0
        check('Sidebar has no outbox or export button; member cannot access/forge admin APIs')
        send(pb,'Nội dung tiếng Việt: <script>alert(1)</script> 🌿')
        pb.locator('#room-info').click()
        with pb.expect_download() as downloaded:pb.get_by_role('button',name='Tải bản dễ đọc (.html)',exact=True).click()
        history=Path(downloaded.value.path()).read_text(encoding='utf-8');assert '&lt;script&gt;' in history and '<article' in history and '<script>' not in history
        close(pb);check('Room information exports escaped, readable HTML conversation; no separate sidebar export')
        pa.locator('#admin-link').click();expect(pa.locator('#admin-page')).to_be_visible();expect(pa).to_have_url(BASE+'/admin')
        snapshot=refresh(pa);assert len(snapshot['data']['bang']['USERS'])==2
        for name,title in [('overview','Tổng quan'),('users','Người dùng'),('rooms','Phòng'),('audit','Nhật ký'),('reports','Báo cáo')]:tab(pa,name);expect(pa.locator('#admin-title')).to_have_text(title)
        check('Dedicated /admin page has all five working tabs and backend-controlled entry')
        tab(pa,'users');pa.get_by_role('button',name='Thêm người dùng',exact=True).click()
        pa.locator('#field-login').fill('offline_member');pa.locator('#field-name').fill('Người dùng ngoại tuyến');pa.locator('#field-password').fill('Temporary-user-private-password');save(pa)
        expect(pa.locator('.admin-table')).to_contain_text('@offline_member')
        tab(pa,'rooms');pa.get_by_role('button',name='Tạo phòng',exact=True).click();pa.locator('#field-name').fill('Lớp học tiếng Việt');pa.locator('#field-description').fill('Báo cáo và trao đổi tài liệu');pa.locator('#field-ownerId').select_option(label='@memberbob · memberbob');save(pa)
        state=req(pb,'/workspace')['body'];room=next(r['id'] for r in state['rooms'] if r['name']=='Lớp học tiếng Việt')
        expect(pb.locator(f'[data-room-id="{room}"]')).to_have_count(1);pb.locator(f'[data-room-id="{room}"]').click();expect(pb.locator('#room-name')).to_have_text('Lớp học tiếng Việt')
        send(pb,'Tin trong lớp: Hà Nội, Đà Nẵng, Thành phố Hồ Chí Minh.')
        note=RUN/'Ghi chú báo cáo.txt';note.write_text('Tệp fixture cho thống kê báo cáo.',encoding='utf-8')
        with pb.expect_file_chooser() as chooser:pb.locator('#attach').click()
        chooser.value.set_files(str(note));pb.get_by_role('button',name='Tải lên',exact=True).click();expect(pb.locator('.upload-status')).to_have_text('Đã tải lên · Chưa gửi vào hội thoại')
        pb.get_by_role('button',name='Gửi vào phòng',exact=True).click();expect(pb.locator('.message-state').last).to_have_text('✓ Đã gửi')
        private=req(pb,'/actions/room-create',{'name':'Nhóm trao đổi riêng','description':'Fixture private statistics','publicRoom':'false','password':'','capacity':'5'})['body']['roomId']
        expect(pb.locator(f'[data-room-id="{private}"]')).to_have_count(1);pb.locator(f'[data-room-id="{private}"]').click();expect(pb.locator('#room-name')).to_have_text('Nhóm trao đổi riêng')
        check('Admin creates user and room; online owner can immediately read/send; committed attachment appears in statistics')
        tab(pa,'reports');snapshot=refresh(pa);sid=snapshot['id'];tables=snapshot['data']['bang'];stats=dict(tables['STATS']);assert stats['Tổng tài khoản']=='3' and stats['Đang online']=='2' and stats['Ngoại tuyến']=='1' and stats['Tổng tệp']=='1',stats
        with pa.expect_download() as downloaded:pa.get_by_role('button',name='Tải báo cáo PDF',exact=True).click()
        first=Path(downloaded.value.path()).read_bytes();out=ROOT/os.environ.get('CHAT_QA_PDF_DIR','output/pdf');out.mkdir(parents=True,exist_ok=True)
        text=pdf_check(first,out/'bao-cao-mau-web3.pdf');assert 'Lớp học tiếng Việt' in text
        assert b.request.get(BASE+'/api/admin/reports/'+sid+'/pdf').status==403
        send(pb,'Tin mới sau khi đã xem báo cáo.');expect(pa.locator('#admin-stale')).to_be_visible()
        assert not any(f.get('type')=='message' and f.get('message',{}).get('text')=='Tin mới sau khi đã xem báo cáo.' for f in frames)
        assert any(f.get('type')=='admin-updated' for f in frames)
        with pa.expect_download() as downloaded:pa.get_by_role('button',name='Tải báo cáo PDF',exact=True).click()
        second=fitz.open(stream=Path(downloaded.value.path()).read_bytes(),filetype='pdf');assert '\n'.join(p.get_text() for p in second)==text;second.close()
        with pa.expect_download() as downloaded:pa.get_by_role('button',name='Tải 4 CSV + HTML (.zip)',exact=True).click()
        with zipfile.ZipFile(downloaded.value.path()) as archive:
            assert set(archive.namelist())=={'nguoidung.csv','phong.csv','thanhvien_theophong.csv','tin_theo_ngay.csv','tong_hop.html'}
            for filename in archive.namelist():
                if filename.endswith('.csv'):assert archive.read(filename).startswith(b'\xef\xbb\xbf')
            days=list(csv.reader(io.StringIO(archive.read('tin_theo_ngay.csv').decode('utf-8-sig'))));assert sum(int(r[2]) for r in days[1:])==int(stats['Tổng tin nhắn'])
        check('Actual PDF download: A4, Vietnamese text, pagination/bounds; preview/PDF/CSV remain on the same snapshot after new chat')
        snap(pa,'01-admin-reports-light.png');tab(pa,'overview');refresh(pa);snap(pa,'02-admin-overview.png')
        pa.locator('#admin-page .theme').click();snap(pa,'03-admin-dark.png')
        pa.set_viewport_size({'width':360,'height':800});tab(pa,'reports');assert pa.evaluate('document.documentElement.scrollWidth<=innerWidth');snap(pa,'04-admin-mobile.png')
        pa.set_viewport_size({'width':1440,'height':960});pa.locator('#admin-page .theme').click()
        check('Admin light/dark and 360 px screenshots; table scroll stays within its panel')
        user_manage(pa,'memberbob');pa.get_by_role('button',name='Cấp lại mật khẩu tạm',exact=True).click();pa.get_by_role('button',name='Cấp mật khẩu tạm',exact=True).click()
        expect(pa.locator('.temporary-password')).to_be_visible();temporary=pa.locator('.temporary-password').text_content();close(pa)
        expect(pb.locator('#auth')).to_be_visible(timeout=15000)
        pb.locator('#login').fill('memberbob');pb.locator('#password').fill(temporary);pb.locator('#submit-login').click()
        expect(pb.locator('dialog[open]')).to_be_visible();pb.get_by_label('Mật khẩu hiện tại',exact=True).fill(temporary);pb.get_by_label('Mật khẩu mới',exact=True).fill(NEW);pb.locator('dialog[open]').get_by_role('button',name='Lưu',exact=True).click()
        expect(pb.locator('dialog[open]')).to_have_count(0);expect(pb.locator('#connection')).to_have_text('Đã kết nối')
        check('Reset immediately revokes HTTP/WS; temporary login must change password through the real form')
        user_manage(pa,'memberbob');pa.get_by_role('button',name='Khóa tài khoản',exact=True).click();pa.locator('#field-reason').fill('QA khóa tài khoản');save(pa)
        expect(pb.locator('#auth')).to_be_visible(timeout=15000)
        user_manage(pa,'memberbob');pa.get_by_role('button',name='Mở khóa tài khoản',exact=True).click();save(pa)
        login(pb,'memberbob',NEW);expect(pb.locator('#admin-link')).to_be_hidden()
        user_manage(pa,'memberbob');pa.get_by_role('button',name='Sửa hồ sơ và vai trò',exact=True).click();pa.locator('#field-role').select_option('ADMIN');save(pa)
        expect(pb.locator('#auth')).to_be_visible(timeout=15000);login(pb,'memberbob',NEW);expect(pb.locator('#admin-link')).to_be_visible();pb.locator('#admin-link').click()
        tab(pa,'reports');current=refresh(pa);sid=current['id'];assert a.request.get(BASE+'/api/admin/reports/'+sid+'/pdf').status==200;assert b.request.get(BASE+'/api/admin/reports/'+sid+'/pdf').status==400
        check('Lock/unlock and role change revoke old sessions; report snapshots remain private between two admins')
        pb.locator('#admin-back').click();pb.locator(f'[data-room-id="{room}"]').click();expect(pb.locator('#room-name')).to_have_text('Lớp học tiếng Việt')
        tab(pa,'overview');pa.get_by_role('button',name='Gửi thông báo toàn hệ thống',exact=True).click();pa.locator('#field-text').fill('Thông báo lớp học từ quản trị viên.')
        if not JAR:assert req(pa,'/fixture/udp',{'running':False})['status']==200
        pa.get_by_role('button',name='Gửi thông báo',exact=True).click()
        if not JAR:
            expect(pa.locator('dialog[open] .error')).to_contain_text('chưa được xác nhận',timeout=10000);assert pb.locator('.message-row.system').count()==0
            assert req(pa,'/fixture/udp',{'running':True})['status']==200;pa.get_by_role('button',name='Gửi thông báo',exact=True).click()
        expect(pa.locator('dialog[open]')).to_have_count(0);expect(pb.locator('.message-row.system')).to_have_count(1)
        bubble=pb.locator('.system .message-text').bounding_box();area=pb.locator('#timeline').bounding_box();assert abs((bubble['x']+bubble['width']/2)-(area['x']+area['width']/2))<3
        assert len([r for r in req(pb,'/history?roomId='+room)['body'] if r['kind']=='Y'])==1
        check('Admin announcement commits through real UDP, retries one ID, and renders centered as a system event')
        tab(pa,'audit');refresh(pa)
        with pa.expect_download() as downloaded:pa.get_by_role('button',name='Tải nhật ký CSV',exact=True).click()
        audit=Path(downloaded.value.path()).read_text(encoding='utf-8-sig');assert 'WEB_USER-RESET' in audit and PW not in audit and temporary not in audit
        assert not errors,errors;check('Audit CSV is separate and excludes passwords; no uncaught JavaScript errors')
        pa.locator('#admin-back').click();pa.locator('#logout').click();expect(pa.locator('#auth')).to_be_visible()
        assert pa.locator('#admin-content').inner_html()==''
        check('Logout clears admin data from DOM and cancels prior session UI callbacks')
        if JAR:
            a.close();b.close();stop();start(2);a=browser.new_context();pa=a.new_page();login(pa,'admin',PW);pa.locator('#admin-link').click();tab(pa,'reports');restored=refresh(pa)
            assert len(restored['data']['bang']['USERS'])==3 and any(r[2]=='Lớp học tiếng Việt' for r in restored['data']['bang']['ROOMS'])
            assert next(r for r in restored['data']['bang']['USERS'] if r[1]=='memberbob')[3]=='ADMIN'
            assert any(r[2]=='WEB_USER-RESET' for r in restored['data']['bang']['AUDIT'])
            assert req(pa,'/fixture/udp',{'running':False})['status']==404
            assert a.request.get(BASE+'/api/admin/reports/'+restored['id']+'/pdf').status==200
            check('java -jar restart preserves users/roles/rooms/audit and real PDF endpoint; fixture controls absent')
        browser.close()
    if JAR:
        with zipfile.ZipFile(ROOT/JAR) as archive:
            assert 'BOOT-INF/classes/fonts/LICENSE.txt' in archive.namelist()
            for source in (ROOT/'src/main/resources/static').iterdir():
                if source.is_file():assert archive.read('BOOT-INF/classes/static/'+source.name)==source.read_bytes(),source.name
        report['sha256']=hashlib.sha256((ROOT/JAR).read_bytes()).hexdigest()
    report['status']='PASS'
except Exception:
    report['status']='FAIL'
    try:pa.screenshot(path=str(RUN/'failure.png'),full_page=True)
    except Exception:pass
    raise
finally:
    stop()
    for log in logs:log.close()
    (OUT/('package-results.json' if JAR else 'browser-results.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
