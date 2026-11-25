# Nguồn tài nguyên

Bộ sticker: **Noto Emoji 2D**, tác giả Google và các cộng tác viên Noto Emoji.
Giấy phép ảnh: **Apache License 2.0** (khác với giấy phép font OFL của dự án).
Kho gốc: https://github.com/googlefonts/noto-emoji

Phiên bản cố định: `99701e07d3d1fe1bdbcdb14964547f0fd592966f`.
Ảnh gốc: `2D/png/128/emoji_u<mã>.png`, không chỉnh sửa nội dung; ứng dụng chỉ thu phóng.
Thông báo bản quyền upstream: https://github.com/googlefonts/noto-emoji/blob/99701e07d3d1fe1bdbcdb14964547f0fd592966f/2D/svg/LICENSE

Đóng gói tại `src/main/resources/stickers/`: 36 PNG, `LICENSE` (toàn văn Apache 2.0),
`NOTICE` (thông báo Google gốc), `SHA256SUMS` (đối chiếu ảnh tải về).
Danh mục mã Unicode, tên tiếng Việt và bốn nhóm nằm trong `common/StickerDanhMuc.java`.
Không sử dụng ảnh emoji Apple, Microsoft hay Facebook.


## PDF báo cáo web

- Apache PDFBox 3.0.8: dependency Maven `org.apache.pdfbox:pdfbox`, tạo PDF A4 trong Java, không cần trình duyệt/Python trên server. Nguồn và phiên bản: https://pdfbox.apache.org/download.html. Giấy phép Apache 2.0; giữ LICENSE/NOTICE trong các JAR dependency đóng gói.
- Font Noto Sans Regular/Bold, không chỉnh sửa. Nguồn: https://github.com/notofonts/noto-fonts tại commit `ffebf8c1ee449e544955a7e813c54f9b73848eac`, đường dẫn `hinted/ttf/NotoSans/NotoSans-Regular.ttf` và `NotoSans-Bold.ttf`. Dùng nhúng/subset trong PDF để hỗ trợ tiếng Việt, không thay font UI hệ điều hành.
- Giấy phép SIL Open Font License 1.1 đầy đủ và thông báo nguồn nằm cùng font trong `src/main/resources/fonts/LICENSE.txt`, `NOTICE.txt`; các file này được giữ trong JAR. Font Unicode không có glyph được biểu diễn bằng mã `[U+...]` thay vì làm lỗi cả báo cáo.
- SHA-256 Regular: `b85c38ecea8a7cfb39c24e395a4007474fa5a4fc864f6ee33309eb4948d232d5`; Bold: `c976e4b1b99edc88775377fcc21692ca4bfa46b6d6ca6522bfda505b28ff9d6a`.
