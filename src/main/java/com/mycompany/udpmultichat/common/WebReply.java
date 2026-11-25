package com.mycompany.udpmultichat.common;

/** Chi luu ID tin goc, khong luu ban sao noi dung co the bi thu hoi. */
public final class WebReply {
    private static final java.util.regex.Pattern PREFIX = java.util.regex.Pattern.compile("^«reply:([1-9][0-9]{0,17})::»");
    private WebReply() { }
    public static long id(String text) {
        var match = PREFIX.matcher(text);
        return match.find() ? Long.parseLong(match.group(1)) : 0;
    }
    public static String body(String text) { return PREFIX.matcher(text).replaceFirst(""); }
    public static String encode(String text, long replyTo) {
        if (id(text) != 0) { throw new IllegalArgumentException("Nội dung bắt đầu bằng mã trích dẫn dành riêng. Hãy thêm một khoảng trắng phía trước."); }
        return replyTo == 0 ? text : "«reply:" + replyTo + "::»" + text;
    }
}
