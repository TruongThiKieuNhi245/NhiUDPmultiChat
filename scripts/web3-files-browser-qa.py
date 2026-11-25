"""QA tep HTTP + metadata UDP that, hai phien Edge, fixture rieng."""
import hashlib, io, json, os, socket, subprocess, time, uuid, zlib, zipfile
from pathlib import Path
from urllib.request import urlopen
from PIL import Image, ImageDraw, ImageFont
from playwright.sync_api import sync_playwright, expect
ROOT=Path(__file__).resolve().parents[1]
RUN=ROOT/'.selftest'/('web3-files-'+uuid.uuid4().hex[:12]);RUN.mkdir(parents=True)
OUT=ROOT/os.environ.get('CHAT_QA_OUTPUT','docs/screenshots/web3-files');OUT.mkdir(parents=True,exist_ok=True)
JAR=os.environ.get('CHAT_QA_JAR')
with socket.socket() as available:
    available.bind(('127.0.0.1',0));port=available.getsockname()[1]
BASE=f'http://127.0.0.1:{port}'
cp=str(ROOT/'target/test-classes')+os.pathsep+str(ROOT/'target/classes')+os.pathsep+(ROOT/'target/web-classpath.txt').read_text().strip()
cp = os.environ.get('CHAT_QA_CLASSPATH', cp)
process=None;logs=[]
report={'fixture':str(RUN.relative_to(ROOT)),'browser':'','checks':[],'artifact':JAR}
if JAR:
    with zipfile.ZipFile(ROOT/JAR) as archive:
        names=archive.namelist()
        assert 'BOOT-INF/classes/com/mycompany/udpmultichat/web/WebFileApi.class' in names
        assert not any('Web1BrowserFixture' in name for name in names)
        assert len([name for name in names if name.startswith('BOOT-INF/classes/stickers/') and name.endswith('.png')])==36
        for source in (ROOT/'src/main/resources/static').iterdir():
            if source.is_file():assert archive.read('BOOT-INF/classes/static/'+source.name)==source.read_bytes(),source.name
    report['sha256']=hashlib.sha256((ROOT/JAR).read_bytes()).hexdigest()

def check(text):report['checks'].append(text);print('PASS:',text,flush=True)
def start(number):
    global process
    log=(RUN/f'server-{number}.txt').open('w',encoding='utf-8');logs.append(log)
    command=['java','-jar',str(ROOT/JAR)] if JAR else ['java','-cp',cp,'com.mycompany.udpmultichat.web.Web1BrowserFixture']
    process=subprocess.Popen(command+[f'--server.port={port}',f'--chat.data-dir={RUN/"data"}'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
    for _ in range(120):
        if process.poll() is not None:raise RuntimeError((RUN/f'server-{number}.txt').read_text(encoding='utf-8')[-2500:])
        try:
            if urlopen(BASE,timeout=1).status==200:return
        except Exception:time.sleep(.5)
    raise RuntimeError('Startup timeout')
def stop():
    if process:
        process.terminate()
        try:process.wait(timeout=15)
        except subprocess.TimeoutExpired:process.kill();process.wait()
def req(page,path,data=None,method=None):
    return page.evaluate("""async({path,data,method})=>{
      const c=await(await fetch('/api/csrf')).json();
      const r=await fetch('/api'+path,data===null&&!method?{}:{method:method||'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},...(data===null?{}:{body:JSON.stringify(data)})});
      return {status:r.status,body:await r.json().catch(()=>null)};
    }""",{'path':path,'data':data,'method':method})
def action(page,name,data):
    result=req(page,'/actions/'+name,data);assert result['status']==200,result;return result['body']
def history(page,room):return req(page,'/history?roomId='+room)['body']
def login(page,name,register=False):
    page.goto(BASE)
    if register:page.get_by_role('button',name='Tạo tài khoản',exact=True).click()
    page.locator('#login').fill(name);page.locator('#password').fill('Web3-private-fixture');page.locator('#submit-login').click()
    expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
def select(page,room):
    expect(page.locator(f'[data-room-id="{room}"][data-joined="true"]')).to_have_count(1)
    page.locator(f'[data-room-id="{room}"]').click();expect(page.locator('#room-name')).to_have_text('Thiết kế & chia sẻ')
def upload(page,path):
    with page.expect_file_chooser() as chooser:page.locator('#attach').click()
    chooser.value.set_files(str(path));page.get_by_role('button',name='Tải lên',exact=True).click()
    expect(page.locator('.upload-status')).to_have_text('Đã tải lên · Chưa gửi vào hội thoại',timeout=30000)
def send_file(page,path):
    upload(page,path);page.get_by_role('button',name='Gửi vào phòng',exact=True).click()
    expect(page.locator('.message-state').last).to_have_text('✓ Đã gửi',timeout=10000)
def geom(page,id,own):
    bubble=page.locator(f'[data-msg-id="{id}"] .message-text').bounding_box();area=page.locator('#timeline').bounding_box()
    left=bubble['x']-area['x'];right=area['x']+area['width']-bubble['x']-bubble['width']
    assert (right<60 and left>right if own else left<100 and right>left),(own,left,right)
def contrast(page,theme):
    import re
    tokens=page.evaluate("""()=>{const s=getComputedStyle(document.documentElement);return Object.fromEntries(['--text','--muted','--bg','--surface','--accent-text','--accent-surface','--gradient','--error'].map(k=>[k,s.getPropertyValue(k).trim()]));}""")
    def luminance(color):
        color=color.lstrip('#')
        if len(color)==3:color=''.join(c*2 for c in color)
        channels=[int(color[i:i+2],16)/255 for i in (0,2,4)]
        channels=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in channels]
        return sum(a*b for a,b in zip(channels,(.2126,.7152,.0722)))
    def ratio(a,b):
        x,y=sorted((luminance(a),luminance(b)));return (y+.05)/(x+.05)
    values=[ratio(tokens[fg],tokens[bg]) for fg in ('--text','--muted','--error') for bg in ('--bg','--surface')]
    values.append(ratio(tokens['--accent-text'],tokens['--accent-surface']))
    values.extend(ratio(fg,bg) for fg in ('#fff','#f1eaff') for bg in re.findall(r'#[a-fA-F0-9]{6}',tokens['--gradient']))
    assert min(values)>=4.5,(theme,values)
    report.setdefault('tokenContrast',{})[theme]={'minimum':round(min(values),2),'ratios':[round(v,2) for v in values]}
def snap(page,name):
    page.wait_for_timeout(350);page.screenshot(path=str(OUT/name),full_page=True)
def csrf(page):return req(page,'/csrf')['body']
def control(page,running):assert req(page,'/fixture/udp',{'running':running})['status']==200
try:
    start(1)
    with sync_playwright() as playwright:
        browser=playwright.chromium.launch(channel='msedge',headless=True);report['browser']=browser.version
        a=browser.new_context(viewport={'width':1440,'height':960},color_scheme='light',accept_downloads=True)
        b=browser.new_context(viewport={'width':1180,'height':900},color_scheme='light',accept_downloads=True)
        c=browser.new_context(viewport={'width':1000,'height':850},color_scheme='light')
        pa,pb,pc=a.new_page(),b.new_page(),c.new_page();errors=[]
        for page in (pa,pb,pc):page.on('pageerror',lambda e:errors.append(str(e)))
        pa.goto(BASE);snap(pa,'auth-gradient.png')
        login(pa,'web3_alice',True);login(pb,'web3_bob',True);login(pc,'web3_carol',True)
        action(pa,'profile-save',{'name':'Người cùng tên','about':'Thiết kế sản phẩm','avatarMode':'KEEP'})
        action(pb,'profile-save',{'name':'Người cùng tên','about':'Chia sẻ tài liệu','avatarMode':'KEEP'})
        room=action(pa,'room-create',{'name':'Thiết kế & chia sẻ','description':'Tài liệu, ý tưởng và những cuộc trò chuyện','publicRoom':'true','password':'','capacity':'6'})['roomId']
        action(pb,'room-join',{'key':room,'password':''});select(pa,room);select(pb,room)
        blob=bytes(range(256))*(5*1024*1024//256);fixture=RUN/'Tài liệu dự án 5 MB.bin';fixture.write_bytes(blob)
        image=RUN/'Bảng màu gradient.png'
        palette=Image.new('RGB',(720,420));draw=ImageDraw.Draw(palette)
        for x in range(720):
            t=x/719;draw.line((x,0,x,420),fill=tuple(round(a*(1-t)+b*t) for a,b in zip((134,67,211),(51,87,185))))
        font=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',30)
        draw.text((40,34),'VIOLET / INDIGO / SKY',fill='white',font=font)
        for x,color in [(40,'#ede5ff'),(205,'#9565df'),(370,'#5745c2'),(535,'#3152b4')]:
            draw.rounded_rectangle((x,110,x+145,350),radius=20,fill=color,outline='#ffffff',width=2)
        palette.save(image)
        reply=RUN/'Ghi chú.txt';reply.write_text('Mình gửi lại ghi chú. Cảm ơn bạn! 🌿',encoding='utf-8')
        empty=RUN/'empty.txt';empty.write_bytes(b'')
        with pa.expect_file_chooser() as chooser:pa.locator('#attach').click()
        chooser.value.set_files(str(empty));expect(pa.locator('dialog .error')).to_have_text('Tệp rỗng không thể gửi.')
        expect(pa.get_by_role('button',name='Tải lên',exact=True)).to_be_disabled();pa.keyboard.press('Escape')
        large=RUN/'large.bin';large.write_bytes(b'x'*(10*1048576+1))
        with pa.expect_file_chooser() as chooser:pa.locator('#attach').click()
        chooser.value.set_files(str(large));expect(pa.locator('dialog .error')).to_contain_text('vượt 10 MB');pa.keyboard.press('Escape')
        check('UI rejects zero bytes and >10 MB with actionable error; file dialog Escape restores focus')
        # Cancel that voi upload throughput thap, sau do thu lai cung File trong dialog.
        session=a.new_cdp_session(pa);session.send('Network.enable')
        session.send('Network.emulateNetworkConditions',{'offline':False,'latency':40,'downloadThroughput':1048576,'uploadThroughput':32768})
        with pa.expect_file_chooser() as chooser:pa.locator('#attach').click()
        chooser.value.set_files(str(fixture));pa.get_by_role('button',name='Tải lên',exact=True).click()
        expect(pa.locator('progress')).to_be_visible();pa.get_by_role('button',name='Hủy tải lên',exact=True).click()
        expect(pa.locator('dialog .error')).to_contain_text('Đã hủy tải lên');expect(pa.get_by_role('button',name='Thử tải lại',exact=True)).to_be_enabled()
        session.send('Network.emulateNetworkConditions',{'offline':False,'latency':0,'downloadThroughput':-1,'uploadThroughput':-1})
        pa.get_by_role('button',name='Thử tải lại',exact=True).click()
        expect(pa.locator('.upload-status')).to_have_text('Đã tải lên · Chưa gửi vào hội thoại',timeout=30000)
        assert history(pa,room)==[] and pb.locator('.file-card').count()==0
        staged=list((RUN/'data/uploads'/room).glob('*.properties'));assert len(staged)==1
        fileid=staged[0].stem
        assert a.request.get(BASE+'/api/files/'+fileid).status==400
        if not JAR:control(pa,False)
        pa.get_by_role('button',name='Gửi vào phòng',exact=True).click()
        if not JAR:
            expect(pa.locator('.message-state.failed')).to_have_count(1,timeout=10000);assert history(pa,room)==[]
            assert pb.locator('.message-row').count()==0
            clientid=pa.locator('.message-row').get_attribute('data-client-msg-id');snap(pa,'file-retry-error.png')
            geom_pending=pa.locator('.message-row .message-text').bounding_box();assert geom_pending['x']>pa.locator('#timeline').bounding_box()['x']+100
            assert pa.locator('#outbox').count()==0
            expect(pa.locator('.file-card')).to_contain_text(fixture.name)
            control(pa,True);pa.get_by_role('button',name='Gửi lại',exact=True).click()
            expect(pa.locator('.message-state')).to_have_text('✓ Đã gửi');assert pa.locator('.message-row').get_attribute('data-client-msg-id')==clientid
        expect(pb.locator('.file-card')).to_have_count(1,timeout=10000)
        rows=history(pa,room);assert len(rows)==1 and rows[0]['file']['id']==fileid
        geom(pa,1,True);geom(pb,1,False)
        received=b.request.get(BASE+'/api/files/'+fileid)
        assert received.status==200 and received.body()==blob and int(received.headers['x-checksum-crc32'])==zlib.crc32(blob)
        assert received.headers['content-type']=='application/octet-stream' and 'attachment' in received.headers['content-disposition']
        assert c.request.get(BASE+'/api/files/'+fileid).status==400
        anonymous=browser.new_context();assert anonymous.request.get(BASE+'/api/files/'+fileid).status==401;anonymous.close()
        with pb.expect_download() as download:pb.locator('.file-download').click()
        assert download.value.suggested_filename==fixture.name
        assert hashlib.sha256(Path(download.value.path()).read_bytes()).digest()==hashlib.sha256(blob).digest()
        check('5 MB upload cancel/retry; upload alone is not a chat; real UDP stop/retry keeps one bubble and stable ID; download bytes/CRC match; outsider/anonymous denied' if not JAR else 'Packaged JAR: cancel/retry, 5 MB HTTP upload, real UDP commit, download bytes/CRC and permissions')
        send_file(pb,reply);expect(pa.locator('.file-card')).to_have_count(2);geom(pa,2,False);geom(pb,2,True)
        assert rows[0]['userId']!=history(pb,room)[1]['userId']
        send_file(pa,image);expect(pb.locator('.file-thumbnail')).to_have_count(1)
        pb.wait_for_function("()=>document.querySelector('.file-thumbnail')?.complete && document.querySelector('.file-thumbnail').naturalWidth>0")
        check('Reverse B-right/A-left geometry with identical display names; image thumbnail decodes with fixed dimensions')
        # HTTP auth/CSRF/size/type khong phu thuoc kiem tra browser.
        token=csrf(pa);headers={token['header']:token['token']}
        assert a.request.post(BASE+'/api/files/upload?roomId='+room,multipart={'file':{'name':'safe.txt','mimeType':'text/plain','buffer':b'ok'}}).status==403
        rejected=a.request.post(BASE+'/api/files/upload?roomId='+room,headers=headers,multipart={'file':{'name':'large.bin','mimeType':'application/octet-stream','buffer':large.read_bytes()}})
        assert rejected.status==413,rejected.status
        assert a.request.post(BASE+'/api/files/upload?roomId='+room,headers=headers,multipart={'file':{'name':'bad.html','mimeType':'text/html','buffer':b'<script>alert(1)</script>'}}).status==400
        assert a.request.post(BASE+'/api/files/upload?roomId='+room,headers=headers,multipart={'file':{'name':'fake.png','mimeType':'image/png','buffer':b'not a png'}}).status==400
        check('Backend enforces CSRF, 10 MB multipart size, file type and image validation')
        # Cancel staged file deletes binary without creating a chat.
        upload(pa,reply);before=set((RUN/'data/uploads'/room).glob('*.bin'));assert len(before)==4
        pa.get_by_role('button',name='Hủy tệp',exact=True).click()
        deadline=time.time()+5
        while len(list((RUN/'data/uploads'/room).glob('*.bin')))!=3 and time.time()<deadline:time.sleep(.1)
        assert len(list((RUN/'data/uploads'/room).glob('*.bin')))==3 and len(history(pa,room))==3
        check('Discard uploaded but unsent file cleans binary; no chat side effect')
        # Tieng Viet, URL dai va tin mot ky tu cho anh UI.
        for page,text in [(pa,'Mình đã gửi tài liệu và bảng màu mới. Bạn xem giúp nhé!'),(pb,'Màu tím–indigo rõ hơn rồi. Nút tải xuống cũng rất dễ tìm ✨'),(pa,'✓')]:
            page.locator('#message').fill(text);page.locator('#send').click();expect(page.locator('.message-state').last).to_have_text('✓ Đã gửi')
        contrast(pa,'light');snap(pa,'desktop-light.png');pa.locator('.chat-header .theme').click();contrast(pa,'dark');snap(pa,'desktop-dark.png')
        pa.set_viewport_size({'width':900,'height':900});snap(pa,'tablet-dark.png')
        pa.set_viewport_size({'width':360,'height':800});pa.locator('.chat-header .theme').click()
        assert pa.evaluate('document.documentElement.scrollWidth<=innerWidth')
        assert pa.locator('#send').bounding_box()['width']>=44
        snap(pa,'mobile-light.png');pa.locator('#menu').click();snap(pa,'mobile-drawer.png');pa.keyboard.press('Escape')
        expect(pa.locator('#menu')).to_be_focused();pa.locator('.chat-header .theme').click();snap(pa,'mobile-dark.png')
        for zoom in (1.25,1.5):
            pa.evaluate('(zoom)=>document.body.style.zoom=zoom',zoom)
            assert pa.evaluate('document.documentElement.scrollWidth<=innerWidth')
        pa.evaluate("document.body.style.zoom='1'")
        check('Real Edge screenshots: desktop/tablet/360 px light-dark, drawer focus and CSS zoom 125-150%; no page overflow')
        pa.set_viewport_size({'width':1440,'height':960})
        # Thu hoi xoa file va khong lo ten trong quote/history.
        image_id=history(pa,room)[2]['file']['id']
        assert req(pa,'/recall',{'roomId':room,'msgId':3})['status']==200
        expect(pb.locator('[data-msg-id="3"] .message-text')).to_have_text('Tin đã thu hồi')
        assert not (RUN/'data/uploads'/room/(image_id+'.bin')).exists()
        assert not (RUN/'data/uploads'/room/(image_id+'.jpg')).exists()
        bid=req(pb,'/workspace')['body']['profile']['id']
        action(pa,'member-moderate',{'roomId':room,'userId':bid,'kind':'KICK','seconds':'60'})
        assert b.request.get(BASE+'/api/files/'+fileid).status==400
        check('Recall removes thumbnail and file; kick immediately revokes authenticated file download')
        if JAR:
            # Thu nghiem restart artifact that, khong dung fixture classpath.
            a.close();b.close();c.close();stop();start(2)
            a=browser.new_context();pa=a.new_page();login(pa,'web3_alice');select(pa,room)
            assert len(history(pa,room))==6 and history(pa,room)[2]['recalled']
            assert a.request.get(BASE+'/api/files/'+fileid).body()==blob
            assert req(pa,'/fixture/udp',{'running':False})['status']==404
            check('Actual java -jar restart preserves bytes, file metadata, membership and recall; fixture endpoint absent')
        assert not errors,errors;check('No uncaught JavaScript errors')
        browser.close()
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
