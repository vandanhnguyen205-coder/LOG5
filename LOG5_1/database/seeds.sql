-- Sample Data Insertion for LOG-05

INSERT INTO NguoiDung (MaNguoiDung, HoTen, Email, SoDienThoai, MatKhauMaHoa, VaiTro) VALUES
('NG01', 'Nguyễn Văn An', 'an.nguyen@logistics.com', '0901234567', 'hash_pass_1', 'NguoiGui'),
('DP01', 'Trần Thị Bình', 'binh.tran@logistics.com', '0912345678', 'hash_pass_2', 'DieuPhoi'),
('KD01', 'Lê Văn Cường', 'cuong.le@logistics.com', '0923456789', 'hash_pass_3', 'KiemDuyet'),
('TX01', 'Phạm Quốc Dung', 'dung.pham@logistics.com', '0934567890', 'hash_pass_4', 'TaiXe'),
('QT01', 'Hoàng Anh Tuấn', 'tuan.hoang@logistics.com', '0945678901', 'hash_pass_5', 'QuanTri');

INSERT INTO LoaiHangLanh (MaLoai, TenLoai, MoTa, NhietDoToiThieu, NhietDoToiDa, TrangThaiSuDung) VALUES
('LHL01', 'Vắc xin & Sinh phẩm y tế', 'Yêu cầu bảo quản lạnh từ 2 đến 8 độ C', 2.00, 8.00, TRUE),
('LHL02', 'Hải sản đông lạnh', 'Hải sản cấp đông xuất khẩu duy trì âm', -22.00, -18.00, TRUE),
('LHL03', 'Trái cây tươi xuất khẩu', 'Rau củ quả tươi duy trì mát', 4.00, 10.00, TRUE);

INSERT INTO YeuCauGiayTo (MaLoai, MaYeuCau, TenGiayTo, BatBuoc, MoTa) VALUES
('LHL01', 'YC01', 'Giấy kiểm định chất lượng Vắc xin', TRUE, 'Phiếu kiểm nghiệm lô vắc xin của Bộ Y tế'),
('LHL01', 'YC02', 'Giấy phép vận chuyển sinh phẩm', TRUE, 'Giấy phép vận chuyển hàng đặc biệt'),
('LHL02', 'YC01', 'Giấy chứng nhận kiểm dịch hải sản', TRUE, 'Kiểm dịch thủy hải sản xuất khẩu');

INSERT INTO PhuongTien (MaPhuongTien, BienSo, LoaiPhuongTien, CoHeThongLamLanh, TrangThaiSanSang) VALUES
('PT01', '51C-123.45', 'Xe tải lạnh 5 tấn', TRUE, TRUE),
('PT02', '51D-678.90', 'Xe container lạnh 20ft', TRUE, TRUE),
('PT03', '60C-999.88', 'Xe tải lạnh 2.5 tấn', TRUE, FALSE);

INSERT INTO DonVanChuyen (MaDon, MaNguoiGui, MaLoai, DiemLay, DiemGiao, KhoiLuong, ThoiGianMongMuon, NgayTao, TrangThai, LyDoHuy) VALUES
('DON01', 'NG01', 'LHL01', 'Viện Pasteur TP.HCM', 'Bệnh viện Đa khoa Đồng Nai', 150.00, '2026-03-25 08:00:00', '2026-03-24 10:00:00', 'DA_BAN_GIAO', NULL),
('DON02', 'NG01', 'LHL02', 'Cảng Cát Lái TP.HCM', 'Kho lạnh VSIP Bình Dương', 5000.00, '2026-03-26 14:00:00', '2026-03-25 09:00:00', 'DANG_VAN_CHUYEN', NULL);

INSERT INTO GiayTo (MaGiayTo, MaDon, MaKiemDuyetVien, SoHieu, NgayCap, NgayHetHan, TepDinhKem, TrangThaiDuyet, LyDoTuChoi) VALUES
('GT01', 'DON01', 'KD01', 'SH-2026-001', '2026-01-10', '2026-12-31', 'vacxin_cert.pdf', 'Hop le', NULL),
('GT02', 'DON02', 'KD01', 'SH-2026-002', '2026-02-01', '2026-08-30', 'haisan_kd.pdf', 'Hop le', NULL);

INSERT INTO PhanCong (MaPhanCong, MaDon, MaDieuPhoiVien, MaTaiXe, MaPhuongTien, TuyenDuong, ThoiGianDuKien, TrangThai) VALUES
('PC01', 'DON01', 'DP01', 'TX01', 'PT01', 'QL1A TP.HCM -> Biên Hòa', '2026-03-25 10:30:00', 'HOAN_THANH'),
('PC02', 'DON02', 'DP01', 'TX01', 'PT02', 'Xa lộ Hà Nội -> Bình Dương', '2026-03-26 16:00:00', 'DANG_THUC_HIEN');

INSERT INTO ThongSoHanhTrinh (MaBanGhi, MaDon, ThoiDiem, ViTriGPS, NhietDoThungXe) VALUES
('BG01', 'DON01', '2026-03-25 08:30:00', '10.7769,106.7009', 4.50),
('BG02', 'DON01', '2026-03-25 09:30:00', '10.9575,106.8427', 5.00),
('BG03', 'DON02', '2026-03-26 14:30:00', '10.8231,106.6297', -19.50),
('BG04', 'DON02', '2026-03-26 15:00:00', '10.8800,106.7300', -15.00);

INSERT INTO SuCo (MaSuCo, MaDon, MaTaiXe, MaDieuPhoiVien, LoaiSuCo, MoTa, ThoiDiem, HinhAnh, PhuongAnXuLy, TrangThai) VALUES
('SC01', 'DON02', 'TX01', 'DP01', 'Cảnh báo lệch nhiệt độ', 'Nhiệt độ thùng xe tăng từ -19.5 C lên -15 C do hở gioăng cửa', '2026-03-26 15:02:00', 'suco_hozoang.jpg', 'Tăng công suất máy lạnh và kiểm tra lại cửa xe', 'DA_DONG');

INSERT INTO BienBanBanGiao (MaBienBan, MaDon, MaTaiXe, MaKiemDuyetVien, NguoiNhan, TinhTrangHang, NhietDoLucGiao, ThoiDiemGiao, KetQua) VALUES
('BB01', 'DON01', 'TX01', 'KD01', 'Dược sĩ Nguyễn Thị Hoa', 'Hàng nguyên tem nguyên kiện, nhiệt độ đảm bảo', 4.80, '2026-03-25 10:15:00', 'DAT');

INSERT INTO KetQuaKiemTra (MaDon, MaBienBan, ThoiDiem, KetQua, DanhSachMucChuaThoa) VALUES
('DON01', 'BB01', '2026-03-25 10:10:00', 'DAT', 'Tất cả 3/3 tiêu chí (Hồ sơ, Lịch sử nhiệt độ, Bán kính GPS) đều đạt chuẩn.');
