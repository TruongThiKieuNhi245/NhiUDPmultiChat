package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.common.model.TinNhan;

public record ChatMessage(long msgId, String roomId, String userId, String name, String text,
                          long timestamp, String clientMsgId) {
    @Override public String toString() { return "ChatMessage[msgId=" + msgId + ", roomId=" + roomId + "]"; }
    public static ChatMessage from(TinNhan t) {
        return new ChatMessage(t.msgId, t.roomId, t.userId, t.ten, t.noiDung, t.thoiGian, t.maClient);
    }
}
