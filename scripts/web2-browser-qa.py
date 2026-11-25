"""QA browser that, fixture Java chi co tren test classpath. Khong dung data/ that."""
import json, os, socket, subprocess, time, uuid
from pathlib import Path
from urllib.request import urlopen
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / '.selftest' / ('web2-browser-' + uuid.uuid4().hex[:12])
RUN.mkdir(parents=True)
OUT = ROOT / 'docs/screenshots/web2'
OUT.mkdir(parents=True, exist_ok=True)
with socket.socket() as available:
    available.bind(('127.0.0.1', 0))
    port = available.getsockname()[1]
BASE = f'http://127.0.0.1:{port}'
cp = str(ROOT / 'target/test-classes') + os.pathsep + str(ROOT / 'target/classes') + os.pathsep + (ROOT / 'target/web-classpath.txt').read_text().strip()
log = (RUN / 'server.txt').open('w', encoding='utf-8')
process = subprocess.Popen(['java', '-cp', cp, 'com.mycompany.udpmultichat.web.Web1BrowserFixture',
                            f'--server.port={port}', f'--chat.data-dir={RUN / "data"}',
                            '--fixture.force-password=true','--logging.level.root=INFO', '--logging.level.org.springframework=INFO'],
                           cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                           creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
report = {'baseUrl': BASE, 'fixture': str(RUN.relative_to(ROOT)), 'checks': [], 'browser': ''}

def check(name):
    report['checks'].append(name)
    print('PASS:', name, flush=True)

def history(page):
    return page.evaluate("async () => (await fetch('/api/history')).json()")

def control(page, running):
    result = page.evaluate("""async running => {
      const csrf = await (await fetch('/api/csrf')).json();
      const r = await fetch('/api/fixture/udp', {method:'POST', headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify({running})});
      return {status:r.status,body:await r.json()};
    }""", running)
    assert result['status'] == 200 and result['body']['running'] == running, result


def request(page,path,data=None):
    return page.evaluate("""async ({path,data})=>{
      const c=await(await fetch('/api/csrf')).json();
      const r=await fetch('/api'+path,data===null?{}:{method:'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},body:JSON.stringify(data)});
      return {status:r.status,body:await r.json().catch(()=>null)};
    }""",{'path':path,'data':data})

def act(page,action,data):
    result=request(page,'/actions/'+action,data);assert result['status']==200,result;return result['body']

def state(page):
    result=request(page,'/workspace');assert result['status']==200,result;return result['body']

def close(page):
    if page.locator('dialog[open]').count():page.get_by_role('button',name='Đóng hộp thoại',exact=True).click()

def send(page,text):
    expect(page.locator('dialog[open]')).to_have_count(0)
    page.locator('#message').fill(text)
    try:expect(page.locator('#send')).to_be_enabled(timeout=6000)
    except Exception:
        report['failureDebug']={'connection':page.locator('#connection').text_content(),'text':page.locator('#message').input_value(),'room':page.locator('#room-name').text_content(),'state':state(page),'errors':errors}
        screenshot(page,'failure-ui.png')
        raise
    page.locator('#send').click()
    expect(page.locator('.message-state').last).to_have_text('✓ Đã gửi',timeout=10000)
    return int(page.locator('.message-row').last.get_attribute('data-msg-id'))

def row(page,id):return page.locator(f'.message-row[data-msg-id="{id}"]')

def menu(page,id):
    locator=row(page,id);locator.hover();locator.locator('summary').click();return locator

def select(page,id):
    page.locator(f'#room-list [data-room-id="{id}"][data-joined="true"]').click();page.wait_for_timeout(200)

def screenshot(page,name):
    page.wait_for_timeout(300);page.screenshot(path=str(OUT/name),full_page=True)

try:
    for _ in range(120):
        if process.poll() is not None:raise RuntimeError((RUN/'server.txt').read_text(encoding='utf-8')[-3000:])
        try:
            if urlopen(BASE,timeout=1).status==200:break
        except Exception:time.sleep(.5)
    else:raise RuntimeError('Fixture startup timeout')
    with sync_playwright() as p:
        browser=p.chromium.launch(channel='msedge',headless=True);report['browser']=browser.version
        a=browser.new_context(viewport={'width':1440,'height':960},color_scheme='light',permissions=['clipboard-read','clipboard-write'])
        b=browser.new_context(viewport={'width':1100,'height':850},color_scheme='light')
        c=browser.new_context(viewport={'width':1000,'height':850},color_scheme='light')
        pa,pb,pc=a.new_page(),b.new_page(),c.new_page();errors=[]
        for page,login in [(pa,'web2_alice'),(pb,'web2_bob')]:
            page.on('pageerror',lambda e:errors.append(str(e)));page.goto(BASE)
            page.get_by_role('button',name='Tạo tài khoản',exact=True).click()
            page.get_by_label('Tên đăng nhập',exact=True).fill(login);page.get_by_label('Mật khẩu',exact=True).fill('Web2-fixture-password')
            page.locator('#submit-login').click();expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
        assert request(pa,'/register',{'login':'web2_carol','password':'Web2-fixture-password'})['status']==200
        aid,bid=state(pa)['profile']['id'],state(pb)['profile']['id']
        check('Dang ky/dang nhap hai context doc lap va tao tai khoan offline')
        pa.locator('#create-room').click()
        pa.get_by_label('Tên phòng',exact=True).fill('Nhóm riêng Alpha');pa.get_by_label('Mô tả',exact=True).fill('Trao đổi đồ án mạng')
        pa.get_by_label('Phòng công khai',exact=True).uncheck();pa.get_by_label('Giới hạn thành viên',exact=True).fill('2')
        pa.locator('dialog').get_by_role('button',name='Lưu',exact=True).click();expect(pa.locator('#room-name')).to_have_text('Nhóm riêng Alpha')
        alpha=next(room for room in state(pa)['rooms'] if room['name']=='Nhóm riêng Alpha');rid=alpha['id'];code=alpha['code']
        assert not any(room['id']==rid for room in state(pb)['rooms'])
        pa.locator('#room-info').click();pa.get_by_role('button',name='Mời thành viên',exact=True).click()
        pa.get_by_label('Tên hoặc tài khoản',exact=True).fill('web2_carol');pa.get_by_role('button',name='Tìm người',exact=True).click()
        pa.get_by_role('button',name='Gửi lời mời',exact=True).click();expect(pa.locator('dialog')).to_contain_text('Đã thực hiện.');close(pa)
        assert next(room for room in state(pa)['rooms'] if room['id']==rid)['memberCount']==1
        pb.locator('#join-code').click();pb.get_by_label('Mã phòng',exact=True).fill(code)
        pb.locator('dialog').get_by_role('button',name='Lưu',exact=True).click();expect(pb.locator('#room-name')).to_have_text('Nhóm riêng Alpha')
        expect(pa.locator('#room-description')).to_contain_text('2 thành viên')
        secret=send(pa,'Tin riêng Alpha: chỉ thành viên nhận. 🌿');expect(row(pb,secret)).to_have_count(1)
        # C dang nhap sau khi da duoc moi, nhung phong da day.
        pc.on('pageerror',lambda e:errors.append(str(e)));pc.goto(BASE)
        pc.get_by_label('Tên đăng nhập',exact=True).fill('web2_carol');pc.get_by_label('Mật khẩu',exact=True).fill('Web2-fixture-password');pc.locator('#submit-login').click()
        expect(pc.locator('#invitation-count')).to_have_text('1',timeout=15000);pc.locator('#invitations').click()
        pc.get_by_role('button',name='Chấp nhận',exact=True).click();expect(pc.locator('.action-error')).to_contain_text('đầy')
        assert len(state(pc)['invitations'])==1;close(pc)
        assert request(pc,f'/history?roomId={rid}')['status']==400
        check('Phong rieng khong lo danh sach/ma, loi moi offline khong chiem cho, chap nhan kiem lai suc chua')
        # Ho so, avatar, nickname.
        pb.locator('#profile').click();pb.get_by_label('Tên hiển thị',exact=True).fill('Bạn B tiếng Việt');pb.get_by_label('Giới thiệu',exact=True).fill('Mình đang làm đồ án')
        from PIL import Image
        image=RUN/'avatar.png';Image.new('RGB',(140,120),(90,60,180)).save(image)
        pb.locator('#avatar-file').set_input_files(str(image));expect(pb.locator('.profile-preview')).to_be_visible()
        pb.locator('dialog').get_by_role('button',name='Lưu',exact=True).click();expect(pb.locator('#account-name')).to_have_text('Bạn B tiếng Việt')
        pb.locator('#room-info').click()
        own_card=pb.locator('.info-card').filter(has_text='@web2_bob');own_card.get_by_role('button',name='Đổi biệt danh',exact=True).click()
        pb.get_by_label('Biệt danh (trống để xóa)',exact=True).fill('B của Alpha');pb.locator('dialog').get_by_role('button',name='Lưu',exact=True).click()
        response=send(pb,'Tên và biệt danh được phân biệt bằng accountId.')
        expect(row(pa,response).locator('.sender-name')).to_have_text('B của Alpha',timeout=15000)
        expect(row(pa,response).locator('.avatar img')).to_be_visible()
        assert row(pa,response).get_attribute('data-user-id')==bid
        check('Ho so co avatar xem truoc, ten hien thi va biet danh realtime dung phong/ID')
        # Sticker qua UDP va picker tim kiem/nhom.
        pa.locator('#sticker').click();expect(pa.locator('.sticker-choice')).to_have_count(36)
        pa.get_by_label('Tìm sticker',exact=True).fill('zz-khong-co');expect(pa.locator('.sticker-grid')).to_contain_text('Không có sticker phù hợp.')
        pa.get_by_label('Tìm sticker',exact=True).fill('Vui vẻ');pa.get_by_role('button',name='Vui vẻ',exact=True).click()
        expect(pb.locator('.message-sticker')).to_have_count(1);assert pb.locator('.message-sticker').evaluate('e=>e.complete && e.naturalWidth>0')
        check('36 sticker PNG Noto, picker tim kiem/trang thai rong, tin sticker qua UDP')
        # Reply/copy/thu hoi va tim kiem.
        menu(pb,secret).get_by_role('button',name='Trả lời',exact=True).click();expect(pb.locator('#replying')).to_be_visible()
        reply_id=send(pb,'Mình đang trả lời tin gốc.');expect(row(pa,reply_id).locator('.reply-preview')).to_contain_text('Tin riêng Alpha')
        row(pa,reply_id).locator('.reply-preview').click();expect(row(pa,secret)).to_be_focused()
        menu(pa,response).get_by_role('button',name='Sao chép',exact=True).click();assert pa.evaluate('navigator.clipboard.readText()')=='Tên và biệt danh được phân biệt bằng accountId.'
        pa.locator('body').click(position={'x':800,'y':80})
        pb.reload();expect(pb.locator('#connection')).to_have_text('Đã kết nối',timeout=15000);select(pb,rid)
        expect(row(pb,reply_id).locator('.reply-preview')).to_contain_text('Tin riêng Alpha')
        b.set_offline(True);expect(pb.locator('#connection')).to_have_text('Mất kết nối',timeout=10000)
        menu(pa,secret).get_by_role('button',name='Thu hồi',exact=True).click();pa.locator('dialog').get_by_role('button',name='Thu hồi',exact=True).click()
        b.set_offline(False);expect(pb.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
        expect(row(pb,secret).locator('.message-text')).to_have_text('Tin đã thu hồi',timeout=15000);expect(row(pb,reply_id).locator('.reply-preview')).to_contain_text('Tin đã thu hồi')
        pb.reload();expect(pb.locator('#connection')).to_have_text('Đã kết nối',timeout=15000)
        select(pb,rid);expect(row(pb,secret).locator('.message-text')).to_have_text('Tin đã thu hồi')
        pa.locator('#find-messages').click();pa.get_by_label('Từ khóa tin nhắn',exact=True).fill('biệt danh');pa.get_by_role('button',name='Tìm tin',exact=True).click()
        expect(pa.locator('dialog .info-card')).to_have_count(1);pa.get_by_role('button',name='Tới tin nhắn',exact=True).click();expect(row(pa,response)).to_be_focused()
        with pa.expect_download() as download:
            pa.locator('#export-chat').click()
        exported=Path(download.value.path()).read_text(encoding='utf-8');assert 'Tin riêng Alpha' not in exported and 'Mình đang trả lời' in exported
        check('Copy/reply/toi tin goc/thu hoi khi offline va bu sau reconnect, lich su, tim kiem va xuat TXT khong lo quote da thu hoi')
        # Hai phong, nhap theo phong va badge.
        beta=act(pb,'room-create',{'name':'Phòng Beta','description':'Phòng công khai','publicRoom':'true','password':'','capacity':'10'})['roomId']
        # Snapshot phat WS se cap nhat sidebar.
        expect(pb.locator(f'[data-room-id="{beta}"]')).to_have_count(1);select(pb,beta)
        beta_msg=send(pb,'Tin chỉ thuộc phòng Beta.');assert request(pa,f'/history?roomId={beta}')['status']==400
        assert pa.locator('.message-text').filter(has_text='Tin chỉ thuộc phòng Beta.').count()==0
        pa.locator('#message').fill('Bản nháp của Alpha')
        act(pa,'room-join',{'key':beta,'password':''});expect(pa.locator(f'[data-room-id="{beta}"]')).to_have_count(1);select(pa,beta)
        expect(pa.locator('#message')).to_have_value('');select(pa,rid);expect(pa.locator('#message')).to_have_value('Bản nháp của Alpha');pa.locator('#message').fill('')
        select(pb,rid);pb.locator('#message').fill('Đang nhập thử');expect(pa.locator('#typing')).to_contain_text('đang nhập');pb.locator('#message').fill('')
        expect(pa.locator('#typing')).to_have_text('',timeout=6000)
        check('Hai phong cach ly, ban nhap theo phong, typing tu het han')
        # Mute/kick va loi moi cu sau khi co cho trong.
        act(pa,'member-moderate',{'roomId':rid,'userId':bid,'kind':'MUTE','seconds':'60'})
        pb.locator('#message').fill('Chưa được gửi');expect(pb.locator('#send')).to_be_disabled(timeout=15000)
        assert request(pb,'/history?roomId='+rid)['status']==200
        act(pa,'member-moderate',{'roomId':rid,'userId':bid,'kind':'MUTE','seconds':'-1'});expect(pb.locator('#send')).to_be_enabled(timeout=15000);pb.locator('#message').fill('')
        act(pa,'member-moderate',{'roomId':rid,'userId':bid,'kind':'KICK'})
        expect(pb.locator('#room-name')).to_have_text('Sảnh chung',timeout=15000);assert request(pb,'/history?roomId='+rid)['status']==400
        pc.locator('#invitations').click();pc.get_by_role('button',name='Chấp nhận',exact=True).click();expect(pc.locator('#room-name')).to_have_text('Nhóm riêng Alpha')
        assert len(state(pc)['invitations'])==0
        check('Mute chan gui nhung van doc, go mute khong mat WS, kick thu hoi quyen, loi moi cu nhan duoc khi co cho')
        screenshot(pa,'01-chat-nhieu-phong.png');pa.locator('#room-info').click();screenshot(pa,'02-thanh-vien-loi-moi.png');close(pa)
        pa.get_by_role('button',name='Đổi giao diện sáng tối').last.click();screenshot(pa,'03-chat-toi.png')
        pc.set_viewport_size({'width':360,'height':800});pc.wait_for_timeout(300);assert pc.evaluate('document.documentElement.scrollWidth<=innerWidth')
        screenshot(pc,'04-mobile.png');pc.locator('#menu').click();screenshot(pc,'05-mobile-phong.png');pc.locator('#close-drawer').click();expect(pc.locator('#menu')).to_be_focused()
        pc.locator('#room-info').click();screenshot(pc,'06-mobile-thong-tin.png');assert pc.evaluate('document.documentElement.scrollWidth<=innerWidth');close(pc)
        check('Desktop sang/toi va mobile 360, drawer/dialog focus va khong cuon ngang')
        control(pa,False)
        pa.locator('#message').fill('Tin chờ để thử lại đúng mã.');pa.locator('#send').click()
        expect(pa.locator('.retry').last).to_be_visible(timeout=10000)
        pending_id=pa.locator('.message-row').last.get_attribute('data-client-msg-id')
        pa.locator('#outbox').click();expect(pa.locator('dialog')).to_contain_text('Tin chờ để thử lại đúng mã.')
        pa.get_by_role('button',name='Bỏ hàng trên thiết bị',exact=True).click()
        expect(pa.locator('dialog')).to_contain_text('không thu hồi một tin có thể đã tới server')
        close(pa);control(pa,True)
        pa.locator(f'[data-client-msg-id="{pending_id}"] .retry').click()
        expect(pa.locator(f'[data-client-msg-id="{pending_id}"] .message-state')).to_have_text('✓ Đã gửi')
        expect(pa.locator('#outbox-count')).to_have_text('')
        check('Outbox cac phong, canh bao bo hang cuc bo va retry UDP giu ma khong trung')
        # Doi mat khau qua form, thoat va dang nhap lai bang mat khau moi.
        pb.locator('#profile').click();pb.get_by_role('button',name='Đổi mật khẩu',exact=True).click()
        pb.get_by_label('Mật khẩu hiện tại',exact=True).fill('Web2-fixture-password');pb.get_by_label('Mật khẩu mới',exact=True).fill('Web2-new-private-password')
        pb.locator('dialog').get_by_role('button',name='Lưu',exact=True).click();pb.locator('#logout').click();expect(pb.locator('#auth')).to_be_visible()
        pb.get_by_label('Tên đăng nhập',exact=True).fill('web2_bob');pb.get_by_label('Mật khẩu',exact=True).fill('Web2-new-private-password');pb.locator('#submit-login').click()
        expect(pb.locator('#connection')).to_have_text('Đã kết nối',timeout=15000);assert state(pb)['profile']['name']=='Bạn B tiếng Việt'
        forced_context=browser.new_context(viewport={'width':1000,'height':850});forced=forced_context.new_page();forced.on('pageerror',lambda e:errors.append(str(e)))
        forced.goto(BASE);forced.get_by_label('Tên đăng nhập',exact=True).fill('admin');forced.get_by_label('Mật khẩu',exact=True).fill('admin123');forced.locator('#submit-login').click()
        expect(forced.locator('dialog')).to_contain_text('Đổi mật khẩu trước khi tiếp tục',timeout=15000)
        forced.keyboard.press('Escape');expect(forced.locator('dialog')).to_be_visible()
        assert request(forced,'/history')['status']==400
        forced.locator('dialog').get_by_role('button',name='Đăng xuất',exact=True).click();expect(forced.locator('#auth')).to_be_visible()
        forced.get_by_label('Tên đăng nhập',exact=True).fill('admin');forced.get_by_label('Mật khẩu',exact=True).fill('admin123');forced.locator('#submit-login').click()
        forced.get_by_label('Mật khẩu hiện tại',exact=True).fill('admin123');forced.get_by_label('Mật khẩu mới',exact=True).fill('Admin-fixture-private-password')
        forced.locator('dialog').get_by_role('button',name='Lưu',exact=True).click();expect(forced.locator('dialog[open]')).to_have_count(0)
        assert state(forced)['profile']['forcePassword']==False and request(forced,'/history')['status']==200
        check('Mat khau bat buoc doi: Escape bi chan, co logout, doi xong moi vao nghiep vu')
        assert not errors,errors
        check('Doi mat khau, logout/login moi, ho so duoc giu va khong loi JavaScript')
        browser.close()
    report['status']='PASS'
except Exception:
    report['status']='FAIL'
    raise
finally:
    process.terminate()
    try:process.wait(timeout=15)
    except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
    log.close()
    (OUT/'browser-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print('Evidence:',OUT,flush=True)
