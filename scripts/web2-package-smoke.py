"""Smoke test artifact java -jar, du lieu rieng va restart that."""
import json, os, socket, subprocess, time, uuid, zipfile
from pathlib import Path
from urllib.request import urlopen
from playwright.sync_api import sync_playwright, expect
ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / '.selftest' / ('web2-jar-' + uuid.uuid4().hex[:12]); RUN.mkdir(parents=True)
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

def request(page,path,data=None):
    return page.evaluate("""async({path,data})=>{
      const c=await(await fetch('/api/csrf')).json();
      const response=await fetch('/api'+path,data===null?{}:{method:'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},body:JSON.stringify(data)});
      return {status:response.status,body:await response.json().catch(()=>null)};
    }""",{'path':path,'data':data})

def action(page,name,data):
    result=request(page,'/actions/'+name,data);assert result['status']==200,result;return result['body']

def login(page,username,password='Jar-web2-fixture-password',register=False):
    page.goto(BASE)
    if register:page.get_by_role('button',name='Tạo tài khoản',exact=True).click()
    page.get_by_label('Tên đăng nhập',exact=True).fill(username)
    page.get_by_label('Mật khẩu',exact=True).fill(password);page.locator('#submit-login').click()
    expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)

try:
    with sync_playwright() as p:
        browser=p.chromium.launch(channel='msedge',headless=True)
        first=browser.new_context(viewport={'width':1280,'height':900});pa=first.new_page();start(1)
        login(pa,'jar_owner',register=True)
        assert request(pa,'/register',{'login':'jar_invited','password':'Jar-web2-fixture-password'})['status']==200
        room=action(pa,'room-create',{'name':'Phòng giữ qua restart','description':'Fixture Web 2','publicRoom':'false','password':'','capacity':'3'})['roomId']
        person=request(pa,'/users/search?q=jar_invited')['body'][0]
        invitation=action(pa,'invite-send',{'roomId':room,'userId':person['id']})['id']
        import io,base64,hashlib
        from PIL import Image
        png=io.BytesIO();Image.new('RGB',(96,96),(80,65,170)).save(png,format='PNG');image=png.getvalue()
        action(pa,'profile-save',{'name':'Chủ phòng sau restart','about':'Hồ sơ bền vững','avatarMode':'SET','avatar':base64.b64encode(image).decode()})
        expect(pa.locator(f'[data-room-id="{room}"][data-joined="true"]')).to_have_count(1)
        pa.locator(f'[data-room-id="{room}"]').click();expect(pa.locator('#room-name')).to_have_text('Phòng giữ qua restart')
        pa.locator('#message').fill('Tin gốc sẽ được thu hồi.');pa.locator('#send').click()
        expect(pa.locator('.message-state').last).to_have_text('✓ Đã gửi')
        pa.locator('#sticker').click();pa.get_by_role('button',name='Vui vẻ',exact=True).click()
        expect(pa.locator('.message-sticker')).to_have_count(1)
        first_row=pa.locator('.message-row[data-msg-id="1"]');first_row.hover();first_row.locator('summary').click();first_row.get_by_role('button',name='Trả lời',exact=True).click()
        pa.locator('#message').fill('Trả lời không sao chép nội dung gốc.');pa.locator('#send').click()
        expect(pa.locator('.message-state').last).to_have_text('✓ Đã gửi')
        assert request(pa,'/recall',{'roomId':room,'msgId':1})['status']==200
        assert request(pa,'/fixture/udp',{'running':False})['status']==404
        saved=request(pa,'/history?roomId='+room)['body'];assert len(saved)==3 and saved[0]['recalled'] and saved[2]['replyText']=='Tin đã thu hồi'
        owner_id=request(pa,'/workspace')['body']['profile']['id']
        pa.close();first.close();stop();start(2)
        a=browser.new_context(viewport={'width':1280,'height':900});b=browser.new_context(viewport={'width':1000,'height':850})
        pa,pb=a.new_page(),b.new_page();login(pa,'jar_owner')
        snapshot=request(pa,'/workspace')['body'];assert snapshot['profile']['name']=='Chủ phòng sau restart' and snapshot['profile']['about']=='Hồ sơ bền vững'
        found=next(r for r in snapshot['rooms'] if r['id']==room);assert found['joined'] and found['manage'] and found['memberCount']==1
        assert request(pa,'/invitations/sent?roomId='+room)['body'][0]['id']==invitation
        login(pb,'jar_invited');expect(pb.locator('#invitation-count')).to_have_text('1')
        pb.locator('#invitations').click();pb.get_by_role('button',name='Chấp nhận',exact=True).click()
        expect(pb.locator('#room-name')).to_have_text('Phòng giữ qua restart');expect(pb.locator('.message-row')).to_have_count(3)
        expect(pb.locator('[data-msg-id="1"] .message-text')).to_have_text('Tin đã thu hồi')
        expect(pb.locator('[data-msg-id="3"] .reply-preview')).to_contain_text('Tin đã thu hồi')
        assert pb.locator('.message-sticker').evaluate('e=>e.complete && e.naturalWidth>0')
        assert b.request.get(BASE+'/api/avatars/'+owner_id).body()==image
        assert request(pb,'/actions/room-delete',{'roomId':room})['status']==400
        exported=b.request.get(BASE+'/api/export?roomId='+room)
        assert exported.status==200 and 'Tin gốc sẽ được thu hồi.' not in exported.text()
        assert len(request(pb,'/workspace')['body']['invitations'])==0
        assert len(request(pb,'/history?roomId='+room)['body'])==3
        browser.close()
    with zipfile.ZipFile(JAR) as archive:
        resources=[name for name in archive.namelist() if name.startswith('BOOT-INF/classes/stickers/') and name.endswith('.png')]
        assert len(resources)==36
        assert 'BOOT-INF/classes/static/workspace-ui.js' in archive.namelist()
        for source in (ROOT/'src/main/resources/static').iterdir():
            if source.is_file():assert archive.read('BOOT-INF/classes/static/'+source.name)==source.read_bytes(),source.name
    result={'status':'PASS','artifact':str(JAR.relative_to(ROOT)),'sha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'fixture':str(RUN.relative_to(ROOT)),
            'checks':['Executable Spring Boot JAR serves multi-room UI','Text, sticker and reply committed through real UDP','test-only UDP endpoint absent: 404','actual JAR restart preserves room, profile, avatar and offline invitation','recall and quote remain hidden after restart; TXT excludes recalled text','invitation accepted after restart, membership authorized, no duplicate messages','36 PNG stickers and all static resources match workspace source']}
    output=ROOT/'docs/screenshots/web2';output.mkdir(parents=True,exist_ok=True)
    (output/'package-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False),flush=True)
finally:
    stop()
    for log in logs:log.close()
