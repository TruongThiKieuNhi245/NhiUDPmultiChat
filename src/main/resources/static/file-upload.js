import { element, fileSize } from './chat-view.js';

// Chi giu mot File trong dialog; callback cu khong duoc cap nhat phien moi.
export class FileUpload {
  revision = 0;
  xhr;
  constructor({ ui, csrf, send, currentRoom, api }) {
    Object.assign(this, { ui, csrf, send, currentRoom, api });
    const input = document.getElementById('file-input');
    document.getElementById('attach').onclick = () => input.click();
    input.onchange = () => { const file=input.files[0];input.value='';if(file)this.open(file); };
  }
  reset() { this.revision++;this.xhr?.abort();this.xhr=null; }
  open(file) {
    const revision=this.revision, room=this.currentRoom();
    if(!room)return;
    const {dialog,content}=this.ui.open('Gửi tệp vào '+room.name);
    const valid=()=>revision===this.revision && dialog.open;
    const card=element('div','upload-card');
    card.append(element('strong','file-name',file.name),element('span','muted',fileSize(file.size)));
    const status=element('p','upload-status','Tối đa 10 MB. Tải lên xong, bạn có thể gửi tệp vào phòng.');
    status.setAttribute('role','status');
    const progress=element('progress');progress.max=100;progress.value=0;progress.hidden=true;progress.setAttribute('aria-label','Tiến độ tải tệp');
    const error=element('p','error');error.setAttribute('role','alert');
    const controls=element('div','upload-actions'), upload=element('button','primary','Tải lên'), cancel=element('button','secondary','Hủy');
    upload.type=cancel.type='button';controls.append(upload,cancel);content.append(card,status,progress,error,controls);
    let uploaded=null, busy=false, handedOff=false, request;
    const reader=new FileReader();
    if(/^image\/(png|jpeg)$/.test(file.type) && file.size<=10*1048576) {
      reader.onload=()=>{if(valid()){const image=element('img','upload-preview');image.width=300;image.height=180;image.alt='Xem trước '+file.name;image.src=reader.result;card.prepend(image);}};
      reader.readAsDataURL(file);
    }
    if(!file.size || file.size>10*1048576) {
      error.textContent=file.size?'Tệp vượt 10 MB. Hãy chọn tệp nhỏ hơn hoặc chia tệp trước khi gửi.':'Tệp rỗng không thể gửi.';upload.disabled=true;
    }
    const abort=()=>{request?.abort();};
    cancel.onclick=()=>busy?abort():dialog.close();
    dialog.addEventListener('close',()=>{
      reader.abort();abort();
      if(uploaded && !handedOff && revision===this.revision)this.api('/files/'+uploaded.id,{method:'DELETE'}).catch(()=>{});
    },{once:true});
    upload.onclick=async()=>{
      if(!valid() || busy)return;
      error.textContent='';
      if(uploaded) {
        upload.disabled=true;
        try {
          handedOff=Boolean(await this.send(uploaded,room.id));
          if(handedOff)dialog.close();else error.textContent='Chưa đưa được tệp vào tin nhắn. Kiểm tra kết nối và quyền phòng rồi thử lại.';
        }catch(failure){if(valid())error.textContent=failure.message;}
        finally{if(valid())upload.disabled=false;}
        return;
      }
      busy=true;upload.disabled=true;cancel.textContent='Hủy tải lên';progress.hidden=false;progress.value=0;status.textContent='Đang tải lên…';
      request=new XMLHttpRequest();this.xhr=request;
      request.open('POST','/api/files/upload?roomId='+encodeURIComponent(room.id));request.timeout=120000;
      const csrf=this.csrf();request.setRequestHeader(csrf.header,csrf.token);
      request.upload.onprogress=event=>{if(valid() && event.lengthComputable){progress.value=Math.floor(event.loaded/event.total*100);status.textContent=progress.value<100?`Đang tải lên · ${progress.value}%`:'Đã truyền xong · Đang kiểm tra tệp…';}};
      const failed=message=>{if(valid()){busy=false;upload.disabled=false;upload.textContent='Thử tải lại';cancel.textContent='Đóng';error.textContent=message;status.textContent='Chưa tải lên thành công. Tệp vẫn được giữ để thử lại.';}if(this.xhr===request)this.xhr=null;};
      request.onerror=()=>failed('Mất kết nối khi tải lên. Hãy kiểm tra mạng rồi thử lại.');
      request.ontimeout=()=>failed('Tải lên quá lâu. Hãy thử lại.');
      request.onabort=()=>failed('Đã hủy tải lên. Bạn có thể thử lại hoặc đóng cửa sổ.');
      request.onload=()=>{
        let result;try{result=JSON.parse(request.responseText);}catch{result={};}
        if(request.status!==200){failed(result.error || (request.status===413?'Tệp vượt 10 MB. Hãy chọn tệp nhỏ hơn.':'Không tải được tệp. Hãy đăng nhập lại nếu phiên đã hết hạn.'));return;}
        if(!valid())return;
        uploaded=result;busy=false;this.xhr=null;progress.value=100;status.textContent='Đã tải lên · Chưa gửi vào hội thoại';upload.disabled=false;upload.textContent='Gửi vào phòng';cancel.textContent='Hủy tệp';
      };
      const body=new FormData();body.append('file',file);request.send(body);
    };
  }
}
