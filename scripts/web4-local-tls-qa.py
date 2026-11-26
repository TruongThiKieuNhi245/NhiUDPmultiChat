"""HTTPS/WSS + UDP + backup/restore/rollback bang JAR that. TLS cert chi fixture local."""
import asyncio,base64,hashlib,json,os,socket,ssl,subprocess,threading,time,uuid
from pathlib import Path
from urllib.request import urlopen,Request
from urllib.error import HTTPError
from aiohttp import web,ClientSession,DummyCookieJar,WSMsgType
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[1];JAR=ROOT/os.environ['CHAT_QA_JAR']
RUN=ROOT/'.selftest'/('tls-data-'+uuid.uuid4().hex[:10]);RUN.mkdir(parents=True)
OUT=ROOT/os.environ.get('CHAT_QA_OUTPUT','docs/screenshots/web4-local');OUT.mkdir(parents=True,exist_ok=True)
PW='Tls-backup-browser-fixture-password';report={'artifact':str(JAR.relative_to(ROOT)),'sha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'fixture':str(RUN.relative_to(ROOT)),'checks':[]};process=None;logs=[]
def free():
 with socket.socket() as sock:sock.bind(('127.0.0.1',0));return sock.getsockname()[1]
backend_port,tls_port=free(),free();BACK=f'http://127.0.0.1:{backend_port}';BASE=f'https://127.0.0.1:{tls_port}'
def check(text):report['checks'].append(text);print('PASS:',text,flush=True)
def start(data,number):
 global process
 log=(RUN/f'backend-{number}.log').open('w',encoding='utf-8');logs.append(log)
 process=subprocess.Popen(['java','-jar',str(JAR),'--spring.profiles.active=proxy',f'--server.port={backend_port}',f'--chat.data-dir={data}'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,env={**os.environ,'CHAT_INITIAL_ADMIN_PASSWORD':PW})
 for _ in range(120):
  if process.poll() is not None:raise RuntimeError('Backend failed; inspect fixture log')
  try:
   if urlopen(BACK,timeout=1).status==200:return
  except Exception:time.sleep(.5)
 raise RuntimeError('Backend timeout')
def stop():
 if process and process.poll() is None:
  process.terminate()
  try:process.wait(timeout=15)
  except subprocess.TimeoutExpired:process.kill();process.wait()
def tool(command,*paths):
 r=subprocess.run(['java','-Dloader.main=com.mycompany.udpmultichat.server.WebDataTool','-cp',str(JAR),'org.springframework.boot.loader.launch.PropertiesLauncher',command,*map(str,paths)],cwd=ROOT,capture_output=True,timeout=90)
 if r.returncode:raise RuntimeError(r.stderr.decode('utf-8',errors='replace')[-1800:])
 return r.stdout.decode('utf-8',errors='replace').strip()
ready=threading.Event();loop_holder={};proxy_errors=[]
async def serve():
 stop_event=asyncio.Event();loop_holder.update(loop=asyncio.get_running_loop(),stop=stop_event)
 async with ClientSession(cookie_jar=DummyCookieJar(),auto_decompress=False) as client:
  async def proxy(request):
   headers={k:v for k,v in request.headers.items() if k.lower() not in ['connection','upgrade','content-length','transfer-encoding','forwarded'] and not k.lower().startswith(('x-forwarded-','sec-websocket-'))}
   headers.update({'X-Forwarded-Proto':'https','X-Forwarded-Host':request.host,'X-Forwarded-Port':str(tls_port)})
   if request.headers.get('Upgrade','').lower()=='websocket':
    try:upstream=await client.ws_connect(BACK+request.rel_url.path_qs,headers=headers)
    except Exception:return web.Response(status=403)
    downstream=web.WebSocketResponse();await downstream.prepare(request)
    async def relay(source,destination):
     async for message in source:
      if message.type==WSMsgType.TEXT:await destination.send_str(message.data)
      elif message.type==WSMsgType.BINARY:await destination.send_bytes(message.data)
      elif message.type in [WSMsgType.CLOSE,WSMsgType.CLOSED,WSMsgType.ERROR]:break
    tasks=[asyncio.create_task(relay(upstream,downstream)),asyncio.create_task(relay(downstream,upstream))]
    try:await asyncio.wait(tasks,return_when=asyncio.FIRST_COMPLETED)
    finally:
     for task in tasks:task.cancel()
     await asyncio.gather(*tasks,return_exceptions=True);await upstream.close();await downstream.close()
    return downstream
   try:
    async with client.request(request.method,BACK+request.rel_url.path_qs,headers=headers,data=await request.read(),allow_redirects=False) as response:
     out_headers=[(k,v) for k,v in response.headers.items() if k.lower() not in ['transfer-encoding','connection','content-length']]
     return web.Response(status=response.status,body=await response.read(),headers=out_headers)
   except Exception:return web.Response(status=502)
  app=web.Application(client_max_size=12*1024*1024);app.router.add_route('*','/{path:.*}',proxy);runner=web.AppRunner(app,access_log=None);await runner.setup();context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.load_cert_chain(RUN/'cert.pem',RUN/'key.pem');await web.TCPSite(runner,'127.0.0.1',tls_port,ssl_context=context).start();ready.set();await stop_event.wait();await runner.cleanup()
def run_proxy():
 try:asyncio.run(serve())
 except Exception as error:proxy_errors.append(str(error));ready.set()
def req(page,path,data=None):
 return page.evaluate("""async({path,data})=>{const c=await(await fetch('/api/csrf')).json();const r=await fetch('/api'+path,data===null?{}:{method:'POST',headers:{'Content-Type':'application/json',[c.header]:c.token},body:JSON.stringify(data)});return {status:r.status,body:await r.json().catch(()=>null)};}""",{'path':path,'data':data})
def login(page,name,register=False):
 page.goto(BASE)
 if register:page.locator('#switch-mode').click()
 page.locator('#login').fill(name);page.locator('#password').fill(PW);page.locator('#submit-login').click();expect(page.locator('#connection')).to_have_text('Đã kết nối',timeout=20000)
def send(page,text):page.locator('#message').fill(text);expect(page.locator('#send')).to_be_enabled();page.locator('#send').click();expect(page.locator('.message-state').last).to_have_text('✓ Đã gửi',timeout=10000)
try:
 openssl=os.environ.get('CHAT_QA_OPENSSL','C:/Program Files/Git/usr/bin/openssl.exe')
 subprocess.run([openssl,'req','-x509','-newkey','rsa:2048','-nodes','-keyout',str(RUN/'key.pem'),'-out',str(RUN/'cert.pem'),'-days','1','-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost,IP:127.0.0.1'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 start(RUN/'data',1);thread=threading.Thread(target=run_proxy,daemon=True);thread.start();assert ready.wait(10) and not proxy_errors,proxy_errors
 with sync_playwright() as p:
  browser=p.chromium.launch(channel='msedge',headless=True);report['browser']=browser.version
  contexts=[browser.new_context(ignore_https_errors=True,viewport={'width':1440,'height':960}) for _ in range(2)];a,b=[c.new_page() for c in contexts];errors=[];ws_urls=[]
  for page in [a,b]:page.on('pageerror',lambda e:errors.append(str(e)));page.on('websocket',lambda ws:ws_urls.append(ws.url))
  login(a,'tlsalice',True);login(b,'tlsbob',True);send(a,'HTTPS/WSS tới UDP thật, dữ liệu có dấu tiếng Việt.');expect(b.locator('#messages')).to_contain_text('HTTPS/WSS tới UDP thật')
  assert ws_urls and all(url.startswith('wss://') for url in ws_urls)
  cookies=contexts[0].cookies();session=next(c for c in cookies if c['name']=='JSESSIONID');assert session['secure'] and session['httpOnly'] and session['sameSite']=='Strict'
  response=a.request.get(BASE);assert response.status==200 and 'strict-transport-security' in response.headers and "frame-ancestors 'none'" in response.headers['content-security-policy']
  assert a.request.post(BASE+'/api/actions/room-create',data={'name':'forged'}).status==403
  ssl_context=ssl._create_unverified_context();cookie='JSESSIONID='+session['value']
  for origin in ['https://evil.invalid',None]:
   headers={'Connection':'Upgrade','Upgrade':'websocket','Sec-WebSocket-Version':'13','Sec-WebSocket-Key':base64.b64encode(os.urandom(16)).decode(),'Cookie':cookie}
   if origin:headers['Origin']=origin
   try:urlopen(Request(BASE+'/ws',headers=headers),context=ssl_context,timeout=5);raise AssertionError('Bad Origin accepted')
   except HTTPError as error:assert error.code in [400,403]
  check('Real TLS proxy: two Edge sessions use HTTPS/WSS; committed text passes real UDP; Secure/HttpOnly/SameSite cookie, HSTS/CSP, CSRF and Origin remain enforced')
  payload=bytes(range(256))*12;file=RUN/'tls-file.bin';file.write_bytes(payload)
  with a.expect_file_chooser() as chooser:a.locator('#attach').click()
  chooser.value.set_files(str(file));a.get_by_role('button',name='Tải lên',exact=True).click();expect(a.locator('.upload-status')).to_have_text('Đã tải lên · Chưa gửi vào hội thoại');a.get_by_role('button',name='Gửi vào phòng',exact=True).click();expect(a.locator('.message-state').last).to_have_text('✓ Đã gửi')
  messages=req(b,'/history')['body'];assert len(messages)==2;fid=messages[-1]['file']['id'];download=b.request.get(BASE+'/api/files/'+fid);assert download.status==200 and download.body()==payload
  a.wait_for_timeout(350);a.screenshot(path=str(OUT/'01-https-chat.png'),full_page=True);check('Authenticated binary upload/download crosses HTTPS, file metadata commits through UDP, receiver bytes match')
  for context in contexts:context.close()
  stop();summary=tool('backup',RUN/'data',RUN/'snapshot.zip');assert 'accounts=3' in summary and 'messages=2' in summary and 'files=1' in summary
  assert tool('verify',RUN/'snapshot.zip')==summary;assert tool('restore',RUN/'snapshot.zip',RUN/'restored')==summary
  check('Production JAR CLI backup/verify/restore succeeds with identical summary; archive written only after backend stopped')
  start(RUN/'restored',2);context=browser.new_context(ignore_https_errors=True,viewport={'width':1440,'height':960});page=context.new_page();login(page,'tlsbob');expect(page.locator('#messages')).to_contain_text('HTTPS/WSS tới UDP thật');assert page.request.get(BASE+'/api/files/'+fid).body()==payload;send(page,'Tin mới sau khôi phục');assert len(req(page,'/history')['body'])==3;context.close();stop()
  check('Restored directory starts via java -jar; accounts/history/file checksum persist and new UDP writes work')
  assert tool('restore',RUN/'snapshot.zip',RUN/'rollback')==summary;start(RUN/'rollback',3);context=browser.new_context(ignore_https_errors=True);page=context.new_page();login(page,'tlsalice');history=req(page,'/history')['body'];assert len(history)==2 and all(row['text']!='Tin mới sau khôi phục' for row in history);assert page.request.get(BASE+'/api/files/'+fid).body()==payload;context.close()
  assert not errors and not proxy_errors,(errors,proxy_errors);browser.close();check('Rollback restores the chosen checkpoint into another new directory; later directory remains intact; no uncaught JS/proxy errors')
 report.update(status='PASS',summary=summary,certificate='self-signed local fixture; Edge ignore_https_errors only in QA',hostTargetTested=False)
except Exception as error:report.update(status='FAIL',error=str(error));raise
finally:
 stop()
 if loop_holder and loop_holder['loop'].is_running():loop_holder['loop'].call_soon_threadsafe(loop_holder['stop'].set)
 for log in logs:log.close()
 (OUT/'tls-data-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
