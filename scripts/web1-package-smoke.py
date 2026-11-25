"""Smoke test artifact java -jar, du lieu rieng va restart that."""
import json, os, socket, subprocess, time, uuid, zipfile
from pathlib import Path
from urllib.request import urlopen
from playwright.sync_api import sync_playwright, expect
ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / '.selftest' / ('web1-jar-' + uuid.uuid4().hex[:12]); RUN.mkdir(parents=True)
JAR = ROOT / os.environ.get('CHAT_QA_JAR', 'target/UDPmultiChat-1.0-SNAPSHOT.jar')
with zipfile.ZipFile(JAR) as jar:
    assert 'BOOT-INF/classes/com/mycompany/udpmultichat/web/WebApplication.class' in jar.namelist()
    assert not any('Web1BrowserFixture' in name for name in jar.namelist())
    assert 'BOOT-INF/classes/static/app.js' in jar.namelist()
with socket.socket() as available:
    available.bind(('127.0.0.1',0)); port=available.getsockname()[1]
BASE=f'http://127.0.0.1:{port}'
process=None
logs=[]
def start(number):
    global process
    log=(RUN/f'server-{number}.txt').open('w',encoding='utf-8'); logs.append(log)
    process=subprocess.Popen(['java','-jar',str(JAR),f'--server.port={port}',f'--chat.data-dir={RUN/"data"}'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
    for _ in range(120):
        if process.poll() is not None: raise RuntimeError('Jar stopped')
        try:
            if urlopen(BASE,timeout=1).status==200:return
        except Exception:time.sleep(.5)
    raise RuntimeError('Jar startup timeout')
def stop():
    if process:
        process.terminate()
        try:process.wait(timeout=15)
        except subprocess.TimeoutExpired:process.kill();process.wait()
try:
    with sync_playwright() as p:
        browser=p.chromium.launch(channel='msedge',headless=True)
        start(1)
        page=browser.new_page();page.goto(BASE)
        page.route('**/api/history/page*',lambda route:route.fulfill(status=503,content_type='application/json',body='{}'),times=1)
        page.get_by_role('button',name='Tạo tài khoản',exact=True).click()
        page.get_by_label('Tên đăng nhập',exact=True).fill('jar_demo')
        page.get_by_label('Mật khẩu',exact=True).fill('Web1-fixture-password')
        page.locator('#submit-login').click()
        expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
        expect(page.locator('#new-messages')).to_be_visible()
        page.locator('#new-messages').click()
        expect(page.locator('#chat-alert')).to_be_hidden()
        expect(page.locator('#room-welcome')).to_be_visible()
        page.locator('#message').fill('Tin giữ lại sau khi khởi động lại JAR.')
        page.locator('#send').click()
        expect(page.locator('.message-state')).to_have_text('✓ Đã gửi')
        result=page.evaluate("""async () => {const c=await(await fetch('/api/csrf')).json();return (await fetch('/api/fixture/udp',{method:'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},body:'{"running":false}'})).status;}""")
        assert result==404,result
        page.close();stop();start(2)
        page=browser.new_page();page.goto(BASE)
        page.get_by_label('Tên đăng nhập',exact=True).fill('jar_demo')
        page.get_by_label('Mật khẩu',exact=True).fill('Web1-fixture-password')
        page.locator('#submit-login').click()
        expect(page.locator('.message-text')).to_have_text('Tin giữ lại sau khi khởi động lại JAR.',timeout=20000)
        browser.close()
    result={'artifact':str(JAR.relative_to(ROOT)), 'status':'PASS','checks':['java -jar serves UI and WebSocket','history error offers retry and clears alert after success','Vietnamese chat committed through UDP','test-only UDP control absent: 404','account and history survive actual JAR restart'],'fixture':str(RUN.relative_to(ROOT))}
    (ROOT/os.environ.get('CHAT_QA_OUTPUT', 'docs/screenshots/web1')/'package-results.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result),flush=True)
finally:
    stop()
    for log in logs:log.close()
