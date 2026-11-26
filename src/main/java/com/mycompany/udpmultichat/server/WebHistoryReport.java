package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.WebMessage;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Bao luu lich su doc offline; khong nhung script, token hay URL tai tep rieng tu. */
public final class WebHistoryReport {
    private WebHistoryReport() { }
    public static String html(String room,String currentUser,List<WebMessage> list) {
        var out=new StringBuilder("<!doctype html><html lang='vi'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Lịch sử trò chuyện</title><style>");
        out.append("body{font:15px/1.65 'Segoe UI',sans-serif;background:#f4f2fa;color:#25223b;margin:0}main{max-width:860px;margin:auto;padding:24px}header{border-bottom:2px solid #6450ba;margin-bottom:28px}h1{font-size:25px}article{display:flex;flex-direction:column;align-items:flex-start;margin:16px 0}.own{align-items:flex-end}.system{align-items:center}.bubble{max-width:75%;background:white;border:1px solid #dad4eb;border-radius:14px;padding:10px 15px;white-space:pre-wrap;overflow-wrap:anywhere}.own .bubble{background:#5940ba;color:white}.system .bubble{background:#eae5f6;color:#493d65}small{color:#5e6178;font-size:12px}.date{text-align:center;margin:24px 0;color:#625b78}.quote{font-size:13px;border-left:3px solid #9d8ae2;padding-left:10px;margin-bottom:8px}.recalled{font-style:italic}@media(max-width:500px){main{padding:16px}.bubble{max-width:86%}}@media print{@page{size:A4;margin:16mm}body{background:white}main{padding:0}.bubble{break-inside:avoid}.own .bubble{color:#25223b;background:#eee9fb}header{break-after:avoid}}</style><main><header><h1>");
        out.append(XuatBaoCao.e(room)).append("</h1><p>Lịch sử trò chuyện · ").append(list.size()).append(" tin</p></header>");
        String previous="";var date=DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        var time=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        for(var item:list) {
            String day=date.format(Instant.ofEpochMilli(item.timestamp()));if(!day.equals(previous)){out.append("<div class='date'>").append(day).append("</div>");previous=day;}
            out.append("<article class='").append(item.kind().equals("Y")?"system":item.userId().equals(currentUser)?"own":"other").append("'><small>").append(XuatBaoCao.e(item.name())).append(" · ").append(time.format(Instant.ofEpochMilli(item.timestamp()))).append("</small><div class='bubble'>");
            if(item.replyTo()>0)out.append("<div class='quote'>").append(XuatBaoCao.e(item.replyName()+": "+item.replyText())).append("</div>");
            String content=item.recalled()?"Tin đã thu hồi":item.file()!=null?"[Tệp] "+item.file().name()+" ("+TepTin.dungLuong(item.file().size())+")":item.kind().equals("S")?"[Sticker] "+item.text():item.text();
            out.append(XuatBaoCao.e(content)).append("</div></article>");
        }
        return out.append("<footer>UDPmultiChat · Bản lưu lịch sử trò chuyện.</footer></main></html>").toString();
    }
}
