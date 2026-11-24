"""QA browser that, fixture Java chi co tren test classpath. Khong dung data/ that."""
import json, os, socket, subprocess, time, uuid
from pathlib import Path
from urllib.request import urlopen
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / '.selftest' / ('web1-ux-' + uuid.uuid4().hex[:12])
RUN.mkdir(parents=True)
OUT = ROOT / 'docs/screenshots/web1-ux'
OUT.mkdir(parents=True, exist_ok=True)
with socket.socket() as available:
    available.bind(('127.0.0.1', 0))
    port = available.getsockname()[1]
BASE = f'http://127.0.0.1:{port}'
cp = str(ROOT / 'target/test-classes') + os.pathsep + str(ROOT / 'target/classes') + os.pathsep + (ROOT / 'target/web-classpath.txt').read_text().strip()
log = (RUN / 'server.txt').open('w', encoding='utf-8')
process = subprocess.Popen(['java', '-cp', cp, 'com.mycompany.udpmultichat.web.Web1BrowserFixture',
                            f'--server.port={port}', f'--chat.data-dir={RUN / "data"}',
                            '--fixture.ux=true', '--logging.level.root=INFO', '--logging.level.org.springframework=INFO'],
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


def row(page, msg_id):
    return page.locator(f'.message-row[data-msg-id="{msg_id}"]')

def direction(page, locator, own):
    bubble = locator.locator('.message-text').bounding_box()
    area = page.locator('#timeline').bounding_box()
    assert bubble and area
    left = bubble['x'] - area['x']
    right = area['x'] + area['width'] - bubble['x'] - bubble['width']
    assert (right <= 60 if own else left <= 95), (own, left, right)
    assert bubble['width'] <= min(area['width'] * .86, 580) + 1
    assert locator.locator('.message-text').evaluate("e => getComputedStyle(e).textAlign") == 'left'
    report.setdefault('geometry', []).append({'own': own, 'leftGap': round(left, 1), 'rightGap': round(right, 1), 'width': round(bubble['width'], 1), 'viewport': page.viewport_size})

def snap(page, name):
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUT/name), full_page=True)

def send(page, text):
    page.locator('#message').fill(text)
    page.locator('#send').click()
    expect(page.locator('.message-state').last).to_have_text('✓ Đã gửi',timeout=10000)
    return int(page.locator('.message-row').last.get_attribute('data-msg-id'))

try:
    for _ in range(120):
        if process.poll() is not None: raise RuntimeError((RUN/'server.txt').read_text(encoding='utf-8')[-3000:])
        try:
            if urlopen(BASE,timeout=1).status==200: break
        except Exception: time.sleep(.5)
    else: raise RuntimeError('Fixture startup timeout')
    with sync_playwright() as p:
        browser=p.chromium.launch(channel='msedge',headless=True)
        report['browser']=browser.version
        a=browser.new_context(viewport={'width':1440,'height':960},color_scheme='light')
        b=browser.new_context(viewport={'width':1100,'height':850},color_scheme='light')
        pa,pb=a.new_page(),b.new_page()
        errors=[]
        requests=[]
        # Thu tuc QA luu handle socket, khong them debug API vao ung dung.
        b.add_init_script("window.qaSockets=[]; const Native=window.WebSocket; window.WebSocket=class extends Native { constructor(...args){ super(...args); window.qaSockets.push(this); } };")
        dropped=[]
        def route_ws(ws):
            server=ws.connect_to_server()
            def receive(message):
                frame=json.loads(message)
                if frame.get('type')=='message' and frame['message']['text']=='Khoảng trống cần bù':
                    dropped.append(frame['message']['msgId'])
                else: ws.send(message)
            server.on_message(receive)
        b.route_web_socket('**/ws',route_ws)
        for page,login in [(pa,'ux_alice'),(pb,'ux_bob')]:
            page.on('pageerror', lambda error: errors.append(str(error)))
            page.on('request',lambda req: requests.append(req.url) if '/api/history' in req.url else None)
            page.goto(BASE)
            page.get_by_label('Tên đăng nhập',exact=True).fill(login)
            page.get_by_label('Mật khẩu',exact=True).fill('Web1-fixture-password')
            page.locator('#submit-login').click()
            expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
            expect(page.locator('.message-row')).to_have_count(50)
            assert page.locator('.message-row').first.get_attribute('data-msg-id')=='631'
            expect(page.locator('#room-welcome')).to_be_hidden()
        assert len([u for u in requests if '/history/page' in u])==2, requests
        assert not any('after=0' in u for u in requests), requests
        check('680 tin: mo phong tai trang cuoi 50 tin, khong lap request tu dau')
        identities=[page.evaluate("async()=> (await(await fetch('/api/me')).json())") for page in [pa,pb]]
        assert identities[0]['name']==identities[1]['name'] and identities[0]['id']!=identities[1]['id']
        for page,own_id in [(pa,identities[0]['id']),(pb,identities[1]['id'])]:
            for mid in [679,680]:
                direction(page,row(page,mid),row(page,mid).get_attribute('data-user-id')==own_id)
        first=send(pa,'A')
        expect(row(pb,first)).to_have_count(1)
        direction(pa,row(pa,first),True);direction(pb,row(pb,first),False)
        assert row(pa,first).locator('.message-text').bounding_box()['width']<80
        expect(row(pa,first).locator('.message-meta')).to_be_hidden()
        second=send(pb,'B trả lời bằng tiếng Việt 🌿\nDòng thứ hai.\n\nGiữ đoạn trống.')
        expect(row(pa,second)).to_have_count(1)
        direction(pb,row(pb,second),True);direction(pa,row(pa,second),False)
        row(pa,second).locator('.sender-name').click()
        expect(row(pa,second).locator('.sender-id')).to_have_text('Tài khoản: '+identities[1]['id'])
        check('Hinh hoc hai chieu, cung ten hien thi van phan biet userId; bong bong ngan vua noi dung')
        # Dung receiver de quan sat ca pending va failed tren cung node.
        control(pa,False)
        pa.locator('#message').fill('Tin lỗi giữ đúng phía và cùng mã khi gửi lại.')
        pa.locator('#send').click()
        pending_row=pa.locator('.message-row').last
        retry_id=pending_row.get_attribute('data-client-msg-id')
        pa.evaluate("id => window.qaPending=document.querySelector(`[data-client-msg-id='${id}']`)",retry_id)
        direction(pa,pending_row,True)
        expect(pending_row.locator('.message-state')).to_have_text('Đang gửi…')
        expect(pending_row.locator('.retry')).to_be_visible(timeout=10000)
        direction(pa,pending_row,True)
        snap(pa,'03-loi-va-gui-lai.png')
        control(pa,True)
        pending_row.locator('.retry').focus()
        pa.keyboard.press('Enter')
        expect(pending_row.locator('.message-state')).to_have_text('✓ Đã gửi')
        assert pa.evaluate("id => window.qaPending===document.querySelector(`[data-client-msg-id='${id}']`)",retry_id)
        expect(pb.locator(f'[data-client-msg-id="{retry_id}"]')).to_have_count(1)
        assert pa.locator(f'[data-client-msg-id="{retry_id}"]').count()==1
        direction(pb,pb.locator(f'[data-client-msg-id="{retry_id}"]'),False)
        expect(pa.locator('#chat-alert')).to_be_hidden()
        check('Pending/loi/retry/confirmed giu huong va cung DOM node; retry bang ban phim')
        # Kiem tra selection khong bi mat khi co tin realtime.
        pa.evaluate("""() => {const n=document.querySelector('.message-text').firstChild; const r=document.createRange();r.selectNodeContents(n);window.getSelection().removeAllRanges();window.getSelection().addRange(r);window.qaSelected=window.getSelection().toString();document.querySelector('#timeline').scrollTop=80;}""")
        top=pa.locator('#timeline').evaluate('e=>e.scrollTop')
        next_id=send(pb,'Tin tới trong lúc bạn đang đọc lịch sử.')
        expect(row(pa,next_id)).to_have_count(1)
        assert abs(pa.locator('#timeline').evaluate('e=>e.scrollTop')-top)<3
        assert pa.evaluate('window.getSelection().toString()===window.qaSelected')
        expect(pa.locator('#new-messages')).to_be_visible()
        check('Tin moi khong keo nguoi doc tin cu xuong day, giu selection')
        pa.locator('#new-messages').click()
        pa.wait_for_timeout(150)
        assert pa.locator('#timeline').evaluate('e=>e.scrollHeight-e.scrollTop-e.clientHeight')<3
        own=send(pa,'Tin do mình chủ động gửi được đưa vào tầm nhìn.')
        expect(row(pa,own)).to_be_in_viewport()
        check('Ve tin moi va chu dong gui dua tin vao tam nhin')
        # Theo doi kich thuoc that cua cache sau moi lan trim.
        pa.evaluate("""async()=>{const {ChatStore}=await import('/chat-store.js');const trim=ChatStore.prototype.trim;ChatStore.prototype.trim=function(...args){trim.apply(this,args);window.qaCache=this.saved.size;window.qaMaxCache=Math.max(window.qaMaxCache||0,this.saved.size);};}""")
        anchor_errors=[]
        for i in range(13):
            pa.locator('#timeline').evaluate('e=>e.scrollTop=0')
            first_row=pa.locator('.message-row').first
            mid=first_row.get_attribute('data-msg-id')
            old_top=first_row.bounding_box()['y']
            pa.locator('#load-older').click()
            pa.wait_for_timeout(100)
            if row(pa,mid).count():
                delta=abs(row(pa,mid).bounding_box()['y']-old_top)
                anchor_errors.append(delta)
                assert delta<3,delta
            assert pa.locator('.message-row').count()<=300
            if pa.locator('#load-older').is_hidden(): break
        assert pa.evaluate('window.qaMaxCache')==300
        assert pa.locator('.message-row').first.get_attribute('data-msg-id')=='1'
        assert pa.locator('#new-messages').is_visible()
        pa.locator('#new-messages').click()
        expect(pa.locator('.message-row')).to_have_count(50)
        assert int(pa.locator('.message-row').first.get_attribute('data-msg-id'))>600
        report['cacheMax']=pa.evaluate('window.qaMaxCache')
        report['anchorMaxErrorPx']=max(anchor_errors)
        check('Phan trang ve tin 1 giu anchor; DOM/cache toi da 300; tai lai cua so moi')
        # Bo mot frame co msgId nho, nhan frame lon truoc, reconnect phai bu khoang trong.
        gap=send(pa,'Khoảng trống cần bù')
        high=send(pa,'Tin có mã lớn tới trước khoảng trống')
        expect(row(pb,high)).to_have_count(1)
        assert dropped==[gap] and row(pb,gap).count()==0
        pb.evaluate("window.qaSockets.at(-1).close()")
        expect(row(pb,gap)).to_have_count(1,timeout=20000)
        ids=[int(x) for x in pb.locator('.message-row').evaluate_all("rows=>rows.map(e=>e.dataset.msgId)")]
        assert ids==sorted(set(ids)) and gap in ids and high in ids
        direction(pb,row(pb,gap),False)
        check('Reconnect bu msgId nho bi bo du frame lon da toi, dung thu tu va khong trung')
        # Tai lai trang: huong lich su van dung.
        pb.reload()
        expect(row(pb,high)).to_have_count(1,timeout=15000)
        direction(pb,row(pb,high),False)
        # Noi dung dai xuong dong o mobile, bo go composition va dem byte.
        long_id=send(pa,'https://example.test/'+('duongdanratdai'*40)+'\nTiếng Việt 🌱')
        expect(row(pb,long_id)).to_have_count(1)
        pb.locator('#message').fill('Bản nháp không gửi khi đang gõ')
        before=pb.locator('.message-row').count()
        pb.locator('#message').dispatch_event('keydown',{'key':'Enter','code':'Enter','isComposing':True})
        assert pb.locator('.message-row').count()==before
        pb.locator('#message').press('Shift+Enter')
        assert '\n' in pb.locator('#message').input_value()
        pb.locator('#message').fill('á'*501)
        expect(pb.locator('#send')).to_be_disabled()
        expect(pb.locator('#limit-error')).to_contain_text('rút ngắn')
        pb.locator('#message').fill('')
        pb.locator('#message').fill('\n'.join(['Dòng soạn']*8))
        assert pb.locator('#message').bounding_box()['height']<=130
        pb.locator('#message').fill('')
        check('Lich su giu huong; URL dai; IME/Shift Enter; 1000 byte UTF-8 va composer tu gian')
        snap(pa,'01-desktop-sang.png')
        pa.get_by_role('button',name='Đổi giao diện sáng tối').last.click()
        snap(pa,'02-desktop-toi.png')
        for width,height,name in [(900,900,'04-tablet.png'),(360,800,'05-mobile-sang.png')]:
            pb.set_viewport_size({'width':width,'height':height})
            pb.wait_for_timeout(250)
            assert pb.evaluate('document.documentElement.scrollWidth<=innerWidth')
            assert pb.locator('#send').bounding_box()['height']>=44
            direction(pb,row(pb,long_id),False)
            snap(pb,name)
        pb.locator('#menu').click()
        expect(pb.locator('#close-drawer')).to_be_focused()
        pb.keyboard.press('Escape')
        expect(pb.locator('#menu')).to_be_focused()
        pb.wait_for_timeout(250)
        assert pb.locator('#sidebar').evaluate('e=>e.inert')
        pb.locator('#menu').click()
        pb.get_by_role('button',name='Đóng danh sách phòng',exact=True).click()
        expect(pb.locator('#menu')).to_be_focused()
        pb.get_by_role('button',name='Đổi giao diện sáng tối').last.click()
        snap(pb,'06-mobile-toi.png')
        check('Tablet/mobile 360 sang toi, khong tran ngang, drawer Escape/close tra focus')
        # Zoom CSS mo phong duoc ghi ro; chua phai zoom UI trinh duyet hay thiet bi that.
        for zoom in [1.25,1.5]:
            pa.evaluate('(z)=>document.body.style.zoom=z',zoom)
            pa.wait_for_timeout(100)
            assert pa.evaluate('document.documentElement.scrollWidth<=innerWidth')
            box=pa.locator('#send').bounding_box()
            assert box['y']+box['height']<=pa.viewport_size['height']+1,box
            snap(pa,f'07-zoom-{zoom}.png')
        pa.evaluate("document.body.style.zoom='1'")
        check('Zoom CSS 125/150% khong tran va nut gui trong viewport')
        pa.route('**/api/history/page',lambda route:route.fulfill(status=503,content_type='application/json',body='{}'),times=1)
        pa.reload()
        expect(pa.locator('#new-messages')).to_be_visible(timeout=15000)
        pa.get_by_role('button',name='Về tin mới ↓',exact=True).click()
        expect(pa.locator('.message-row')).to_have_count(50)
        assert pa.locator('#timeline').get_attribute('role')=='region'
        assert pa.locator('#timeline').get_attribute('aria-live') is None
        check('Loi tai lich su co nut thu lai, tai duoc trang cuoi; aria live tach khoi lich su')
        # Logout khi mat WS: callback cu khong lam xuat hien lai workspace.
        pb.locator('#menu').click()
        pb.locator('#logout').click()
        expect(pb.locator('#auth')).to_be_visible()
        pb.wait_for_timeout(1500)
        assert b.request.get(BASE+'/api/history/page').status==401
        assert not errors,errors
        check('Logout huy session va callback cu; endpoint paging tu choi vo danh; khong loi JS')
        browser.close()
    report['status']='PASS'
except Exception:
    report['status']='FAIL'
    try: snap(pa,'failure.png')
    except Exception: pass
    raise
finally:
    process.terminate()
    try: process.wait(timeout=15)
    except subprocess.TimeoutExpired: process.kill();process.wait(timeout=5)
    log.close()
    (OUT/'ux-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print('Evidence:',OUT,flush=True)
