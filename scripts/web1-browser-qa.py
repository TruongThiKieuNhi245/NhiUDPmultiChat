"""QA browser that, fixture Java chi co tren test classpath. Khong dung data/ that."""
import json, os, socket, subprocess, time, uuid
from pathlib import Path
from urllib.request import urlopen
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / '.selftest' / ('web1-browser-' + uuid.uuid4().hex[:12])
RUN.mkdir(parents=True)
OUT = ROOT / 'docs/screenshots/web1'
OUT.mkdir(parents=True, exist_ok=True)
with socket.socket() as available:
    available.bind(('127.0.0.1', 0))
    port = available.getsockname()[1]
BASE = f'http://127.0.0.1:{port}'
cp = str(ROOT / 'target/test-classes') + os.pathsep + str(ROOT / 'target/classes') + os.pathsep + (ROOT / 'target/web-classpath.txt').read_text().strip()
log = (RUN / 'server.txt').open('w', encoding='utf-8')
process = subprocess.Popen(['java', '-cp', cp, 'com.mycompany.udpmultichat.web.Web1BrowserFixture',
                            f'--server.port={port}', f'--chat.data-dir={RUN / "data"}',
                            '--logging.level.root=INFO', '--logging.level.org.springframework=INFO'],
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

try:
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError('Fixture stopped: ' + (RUN / 'server.txt').read_text(encoding='utf-8')[-3000:])
        try:
            if urlopen(BASE, timeout=1).status == 200:
                break
        except Exception:
            time.sleep(.5)
    else:
        raise RuntimeError('Fixture startup timeout')
    with sync_playwright() as p:
        browser = p.chromium.launch(channel='msedge', headless=True)
        report['browser'] = browser.version
        a = browser.new_context(viewport={'width': 1440, 'height': 960}, color_scheme='light')
        b = browser.new_context(viewport={'width': 1100, 'height': 850}, color_scheme='light')
        pa, pb = a.new_page(), b.new_page()
        errors = []
        sent_at, received_at = {}, {}
        def record_frame(payload, target, kind):
            frame = json.loads(payload)
            if frame.get('type') == kind:
                key = frame.get('clientMsgId') if kind == 'send' else frame['message']['clientMsgId']
                target[key] = time.perf_counter()
        pa.on('websocket', lambda ws: ws.on('framesent', lambda payload: record_frame(payload, sent_at, 'send')))
        pb.on('websocket', lambda ws: ws.on('framereceived', lambda payload: record_frame(payload, received_at, 'message')))
        for page in [pa,pb]:
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(BASE)
        pa.screenshot(path=str(OUT / '01-dang-nhap-desktop.png'), full_page=True)
        for page, login in [(pa, 'nhi_demo'), (pb, 'an_demo')]:
            page.get_by_role('button', name='Tạo tài khoản', exact=True).click()
            page.get_by_label('Tên đăng nhập', exact=True).fill(login)
            page.get_by_label('Mật khẩu', exact=True).fill('Web1-fixture-password')
            page.locator('#submit-login').click()
            expect(page.locator('#workspace')).to_be_visible(timeout=20000)
            expect(page.locator('#connection')).to_have_text('Đã kết nối', timeout=15000)
        ca = [c for c in a.cookies() if c['name'] == 'JSESSIONID'][0]
        cb = [c for c in b.cookies() if c['name'] == 'JSESSIONID'][0]
        assert ca['value'] != cb['value'] and ca['httpOnly'] and ca['sameSite'] == 'Strict'
        check('Hai tai khoan trong hai BrowserContext, cookie HttpOnly rieng')
        def send(page, text):
            page.get_by_label('Nhắn vào Sảnh chung').fill(text)
            page.get_by_role('button', name='Gửi tin', exact=True).click()
        first = 'Chào An! Mình đã mở phòng chat rồi. Cùng thử tiếng Việt nhé 🌿'
        send(pa, first)
        expect(pb.locator('.message-text').filter(has_text=first)).to_have_count(1)
        expect(pa.locator('.message-state').last).to_have_text('✓ Đã gửi')
        send(pb, 'Chào Nhi, mình nhận được rồi! Nội dung có dấu và xuống dòng đều rõ ràng.')
        expect(pa.locator('.message-text')).to_have_count(2)
        first_id = history(pb)[0]['clientMsgId']
        report['latencyBrowserAToBMs'] = round((received_at[first_id] - sent_at[first_id]) * 1000, 2)
        check('Browser A gui tieng Viet, Browser B nhan realtime va nguoc lai')
        attack = '<img src=x onerror="window.web1xss=1">'
        send(pa, attack)
        expect(pb.locator('.message-text').filter(has_text=attack)).to_have_count(1)
        assert pb.locator('#messages img').count() == 0 and not pb.evaluate('window.web1xss || false')
        check('Noi dung HTML hien bang text, khong thuc thi script')
        control(pa, False)
        before = len(history(pa))
        send(pa, 'Tin này chờ khôi phục UDP, chỉ gửi đúng một lần.')
        expect(pa.get_by_role('button', name='Gửi lại', exact=True)).to_be_visible(timeout=10000)
        retry_id = pa.locator('.message-row').last.get_attribute('data-client-msg-id')
        assert len(history(pa)) == before and len(history(pb)) == before
        expect(pb.locator('.message-text')).to_have_count(before)
        expect(pa.locator('#connection')).to_have_text('Đã kết nối')
        pa.screenshot(path=str(OUT / '03-udp-dung-websocket-con-ket-noi.png'), full_page=True)
        check('Dung receiver UDP, WS van ket noi, tin loi khong luu/phat')
        control(pa, True)
        pa.get_by_role('button', name='Gửi lại', exact=True).click()
        expect(pb.locator(f'[data-client-msg-id="{retry_id}"]')).to_have_count(1)
        expect(pa.locator(f'[data-client-msg-id="{retry_id}"] .message-state')).to_have_text('✓ Đã gửi')
        assert len(history(pa)) == before + 1
        report['retryClientMsgId'] = retry_id
        check('Khoi phuc UDP, gui lai cung clientMsgId chi tao mot tin')
        pa.screenshot(path=str(OUT / '02-chat-desktop-sang.png'), full_page=True)
        pa.get_by_role('button', name='Đổi giao diện sáng tối').last.click()
        pa.screenshot(path=str(OUT / '04-chat-desktop-toi.png'), full_page=True)
        # Dong ket noi browser B bang Offline; A tiep tuc gui, B reconnect va lay bu.
        b.set_offline(True)
        expect(pb.locator('#connection')).to_have_text('Mất kết nối', timeout=15000)
        send(pa, 'Tin gửi khi An mất mạng sẽ được lấy bù sau kết nối lại.')
        expect(pa.locator('.message-state').last).to_have_text('✓ Đã gửi')
        b.set_offline(False)
        expect(pb.locator('#connection')).to_have_text('Đã kết nối', timeout=20000)
        expect(pb.locator('.message-text')).to_have_count(before + 2)
        assert pb.locator(f'[data-client-msg-id="{retry_id}"]').count() == 1
        check('Mat mang browser B, reconnect lay bu lich su va khong nhan trung')
        # Desktop 150% zoom mo phong bang CSS zoom, mobile la viewport mo phong.
        pa.evaluate("document.body.style.zoom='1.5'")
        assert pa.evaluate('document.documentElement.scrollWidth <= innerWidth')
        pa.screenshot(path=str(OUT / '05-chat-zoom-150.png'), full_page=True)
        pa.evaluate("document.body.style.zoom='1'")
        check('Desktop mo phong zoom 150%, khong cuon ngang')
        pb.set_viewport_size({'width':360,'height':800})
        assert pb.evaluate('document.documentElement.scrollWidth <= innerWidth')
        pb.get_by_role('button',name='Mở danh sách phòng').click()
        expect(pb.locator('#sidebar')).to_have_class('sidebar open')
        pb.locator('#room').click()
        expect(pb.locator('#sidebar')).not_to_have_class('sidebar open')
        expect(pb.locator('#sidebar')).not_to_be_in_viewport()
        pb.screenshot(path=str(OUT / '06-chat-mobile-sang.png'), full_page=True)
        pb.get_by_role('button', name='Đổi giao diện sáng tối').last.click()
        pb.screenshot(path=str(OUT / '07-chat-mobile-toi.png'), full_page=True)
        check('Viewport 360px sang/toi, drawer phong va composer hoat dong')
        pb.locator('#message').fill('á' * 501)
        expect(pb.locator('#send')).to_be_disabled()
        pb.locator('#message').fill('')
        check('Composer chan noi dung vuot 1000 byte UTF-8')
        # Logout o sidebar mobile va xac minh session khong doc lich su nua.
        pb.get_by_role('button',name='Mở danh sách phòng').click()
        pb.get_by_role('button',name='Đăng xuất',exact=True).click()
        expect(pb.locator('#auth')).to_be_visible()
        assert b.request.get(BASE + '/api/history').status == 401
        assert not errors, errors
        check('Dang xuat thu hoi phien, khong co loi JavaScript')
        browser.close()
    log.flush()
    lines = (RUN / 'server.txt').read_text(encoding='utf-8', errors='replace').splitlines()
    traces = [line for line in lines if any(stage in line for stage in ['UDP_SEND','UDP_RECEIVE','MESSAGE_COMMITTED','MESSAGE_REPLAY','WS_DELIVER'])]
    retry_traces = [line for line in traces if retry_id in line]
    stages = [s for line in retry_traces for s in ['UDP_SEND','UDP_RECEIVE','MESSAGE_COMMITTED','WS_DELIVER'] if s in line]
    assert stages.index('UDP_SEND') < stages.index('UDP_RECEIVE') < stages.index('MESSAGE_COMMITTED') < stages.index('WS_DELIVER'), stages
    assert sum('MESSAGE_COMMITTED' in line for line in retry_traces) == 1
    report['udpEvidence'] = retry_traces
    (OUT / 'udp-trace.txt').write_text('\n'.join(traces) + '\n', encoding='utf-8')
    check('Doi chieu log clientMsgId: SEND < RECEIVE < COMMITTED < WS_DELIVER')
    report['status'] = 'PASS'
except Exception:
    report['status'] = 'FAIL'
    raise
finally:
    process.terminate()
    try: process.wait(timeout=15)
    except subprocess.TimeoutExpired: process.kill(); process.wait(timeout=5)
    log.close()
    (OUT / 'browser-results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('Evidence:', OUT, flush=True)
