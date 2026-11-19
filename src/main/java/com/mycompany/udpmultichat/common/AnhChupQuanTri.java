package com.mycompany.udpmultichat.common;

import java.util.*;

/** Vai tro: Snapshot bat bien de UI va bao cao dung cung so lieu. Ai goi: QuanTriServer, QuanTriClient, XuatBaoCao. Goi: collection Java. */
public record AnhChupQuanTri(long luc,Map<String,List<List<String>>> bang) {
    // Sao chep sau tung hang, khong de worker bao cao doc du lieu Server dang thay doi.
    public AnhChupQuanTri {
        Map<String,List<List<String>>> sao=new LinkedHashMap<>();
        bang.forEach((k,v) -> sao.put(k,v.stream().map(List::copyOf).toList())); bang=Collections.unmodifiableMap(sao);
    }
    // Bang khong ton tai duoc coi la rong, giup UI hien empty state.
    public List<List<String>> lay(String ten) { return bang.getOrDefault(ten,List.of()); }
}
