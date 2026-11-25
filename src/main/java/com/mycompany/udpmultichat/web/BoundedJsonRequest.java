package com.mycompany.udpmultichat.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Gioi han ca body chunked; khong chi tin Content-Length do Client khai bao. */
final class BoundedJsonRequest extends HttpServletRequestWrapper {
    static final class TooLarge extends IOException {
        private static final long serialVersionUID=1L;
        TooLarge() { super("HTTP body quá giới hạn."); }
    }
    private final int limit;
    private ServletInputStream bounded;
    BoundedJsonRequest(HttpServletRequest request,int limit) { super(request); this.limit=limit; }
    @Override public ServletInputStream getInputStream() throws IOException {
        if(bounded!=null)return bounded;
        ServletInputStream source=super.getInputStream();
        bounded=new ServletInputStream() {
            private int count;
            private void received(int size) throws IOException {
                if(size>0)count+=size;
                if(count>limit)throw new TooLarge();
            }
            @Override public int read() throws IOException { int value=source.read();received(value<0?0:1);return value; }
            @Override public int read(byte[] bytes,int offset,int length) throws IOException {
                int size=source.read(bytes,offset,Math.min(length,Math.max(1,limit-count+1)));received(size);return size;
            }
            @Override public boolean isFinished() { return source.isFinished(); }
            @Override public boolean isReady() { return source.isReady(); }
            @Override public void setReadListener(ReadListener listener) { source.setReadListener(listener); }
            @Override public void close() throws IOException { source.close(); }
        };
        return bounded;
    }
    @Override public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));
    }
}
