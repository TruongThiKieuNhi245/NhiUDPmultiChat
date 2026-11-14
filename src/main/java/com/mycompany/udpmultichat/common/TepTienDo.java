package com.mycompany.udpmultichat.common;

import java.nio.file.Path;

/** Vai tro: Snapshot tien do bat bien, khong giu socket/UI. Ai goi: TepClient, Listener, TheTep. Goi: TepTin. */
public record TepTienDo(String maClient,TepTin tep,String trangThai,long daNhan,double tocDo,Path duong,String loi,boolean gui) { }
