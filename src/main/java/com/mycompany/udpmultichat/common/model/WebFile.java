package com.mycompany.udpmultichat.common.model;

/** Metadata cong khai cua the tep, khong chua duong dan luu tru. */
public record WebFile(String id,String name,long size,long crc,String text,boolean image) { }
