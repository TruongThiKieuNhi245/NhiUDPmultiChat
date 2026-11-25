package com.mycompany.udpmultichat.common.model;

/** DTO web khong chua token; trich dan duoc giai tu tin goc hien tai. */
public record WebMessage(long msgId, String roomId, String userId, String name, String text,
                         long timestamp, String clientMsgId, String kind, boolean recalled,
                         long replyTo, String replyName, String replyText, String nickname, String avatarHash, WebFile file) { }
