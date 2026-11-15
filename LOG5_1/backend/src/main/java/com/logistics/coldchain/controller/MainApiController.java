package com.logistics.coldchain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MainApiController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String sql = "SELECT ma_nguoi_dung, ho_ten, email, vai_tro, so_dien_thoai FROM nguoi_dung WHERE email = ?";
        var users = jdbcTemplate.queryForList(sql, email);
        if (users.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Tài khoản không tồn tại trên hệ thống!");
            return ResponseEntity.status(404).body(err);
        }
        Map<String, Object> user = users.get(0);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đăng nhập thành công!");
        res.put("token", "JWT_SPRING_BOOT_TOKEN_" + user.get("ma_nguoi_dung"));

        Map<String, Object> uMap = new HashMap<>();
        uMap.put("maNguoiDung", user.get("ma_nguoi_dung"));
        uMap.put("hoTen", user.get("ho_ten"));
        uMap.put("email", user.get("email"));
        uMap.put("vaiTro", user.get("vai_tro"));
        uMap.put("soDienThoai", user.get("so_dien_thoai"));
        res.put("user", uMap);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String matKhau = body.get("matKhau");
        String hoTen = body.get("hoTen");

        if (email == null || email.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Email không được để trống!");
            return ResponseEntity.badRequest().body(err);
        }

        // 1. Kiểm tra xem Email đã tồn tại trong CSDL chưa
        String checkSql = "SELECT COUNT(*) FROM nguoi_dung WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, email);
        if (count != null && count > 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Email này đã được sử dụng!");
            return ResponseEntity.badRequest().body(err);
        }

        // 2. Chèn tài khoản mới trực tiếp vào PostgreSQL bằng JdbcTemplate
        String insertSql = "INSERT INTO nguoi_dung (email, mat_khau, ho_ten, vai_tro) VALUES (?, ?, ?, 'NGUOI_GUI_HANG')";
        jdbcTemplate.update(insertSql, email, matKhau, hoTen != null ? hoTen : "Khách Hàng Mới");

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đăng ký tài khoản thành công!");
        res.put("email", email);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        String sql = "SELECT d.ma_don, d.diem_lay_hang, d.diem_giao_hang, d.khoi_luong, d.trang_thai, d.ngay_tao, l.ten_loai, l.nhiet_do_toi_thieu, l.nhiet_do_toi_da " +
                     "FROM don_van_chuyen d JOIN loai_hang_lanh l ON d.ma_loai = l.ma_loai ORDER BY d.ma_don DESC";
        var list = jdbcTemplate.queryForList(sql);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        var list = jdbcTemplate.queryForList("SELECT * FROM loai_hang_lanh ORDER BY ma_loai ASC");
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/vehicles")
    public ResponseEntity<?> getVehicles() {
        var list = jdbcTemplate.queryForList("SELECT * FROM phuong_tien ORDER BY ma_phuong_tien ASC");
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/drivers")
    public ResponseEntity<?> getDrivers() {
        String sql = "SELECT t.ma_nguoi_dung, u.ho_ten, t.so_giay_phep_lai FROM tai_xe t JOIN nguoi_dung u ON t.ma_nguoi_dung = u.ma_nguoi_dung";
        var list = jdbcTemplate.queryForList(sql);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/orders/{maDon}/documents")
    public ResponseEntity<?> getDocs(@PathVariable Integer maDon) {
        String sql = "SELECT g.*, y.ten_giay_to FROM giay_to g JOIN yeu_cau_giay_to y ON g.ma_yeu_cau = y.ma_yeu_cau WHERE g.ma_don = ? ORDER BY g.ma_giay_to DESC";
        var list = jdbcTemplate.queryForList(sql, maDon);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body, @RequestHeader(value = "x-user-role", defaultValue = "NGUOI_GUI_HANG") String role) {
        if (!role.equals("NGUOI_GUI_HANG") && !role.equals("QUAN_TRI_VIEN")) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Truy cập bị từ chối khi tạo đơn!");
            return ResponseEntity.status(403).body(err);
        }
        String sql = "INSERT INTO don_van_chuyen (ma_nguoi_gui, ma_loai, diem_lay_hang, diem_giao_hang, khoi_luong, thoi_gian_mong_muon, trang_thai) VALUES (?, ?, ?, ?, ?, NOW(), 'CHO_KHAI_BAO') RETURNING ma_don";
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class, body.get("maNguoiGui"), body.get("maLoai"), body.get("diemLay"), body.get("diemGiao"), body.get("khoiLuong"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Tạo đơn hàng thành công vào PostgreSQL Database");
        res.put("maDon", id);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/documents")
    public ResponseEntity<?> uploadDoc(@RequestBody Map<String, Object> body) {
        String sql = "INSERT INTO giay_to (ma_don, ma_yeu_cau, so_hieu, ngay_cap, ngay_het_han, tep_dinh_kem, trang_thai_duyet) VALUES (?, ?, ?, CAST(? AS DATE), CAST(? AS DATE), ?, 'CHO_DUYET')";
        jdbcTemplate.update(sql, body.get("maDon"), body.get("maYeuCau"), body.get("soHieu"), body.get("ngayCap"), body.get("ngayHetHan"), body.get("tepDinhKem"));
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'CHO_DUYET' WHERE ma_don = ? AND trang_thai = 'CHO_KHAI_BAO'", body.get("maDon"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Khai báo chứng từ thành công!");
        return ResponseEntity.ok(res);
    }

    @PutMapping("/documents/{id}/review")
    public ResponseEntity<?> reviewDoc(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String status = (String) body.get("trangThaiDuyet");
        Integer maDon = (Integer) body.get("maDon");
        jdbcTemplate.update("UPDATE giay_to SET ma_kiem_duyet = ?, trang_thai_duyet = ?, ly_do_tu_choi = ? WHERE ma_giay_to = ?", body.get("maKiemDuyet"), status, body.get("lyDoTuChoi"), id);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        if ("KHONG_HOP_LE".equals(status)) {
            jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'DA_HUY' WHERE ma_don = ?", maDon);
            res.put("message", "Chứng từ bị từ chối -> Đơn đã bị TỰ ĐỘNG HỦY");
        } else {
            String checkSql = "SELECT COUNT(*) FROM yeu_cau_giay_to y JOIN don_van_chuyen d ON d.ma_loai = y.ma_loai LEFT JOIN giay_to g ON g.ma_don = d.ma_don AND g.ma_yeu_cau = y.ma_yeu_cau AND g.trang_thai_duyet = 'HOP_LE' WHERE d.ma_don = ? AND y.bat_buoc = TRUE AND g.ma_giay_to IS NULL";
            Integer missing = jdbcTemplate.queryForObject(checkSql, Integer.class, maDon);
            if (missing != null && missing == 0) {
                jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'CHO_PHAN_CONG' WHERE ma_don = ?", maDon);
                res.put("newStatus", "CHO_PHAN_CONG");
                res.put("message", "TỰ ĐỘNG: 100% Giấy tờ đã chuyển sang trạng thái [CHO_PHAN_CONG]");
            } else {
                res.put("message", "Đã duyệt chứng từ HỢP LỆ");
            }
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/dispatch")
    public ResponseEntity<?> dispatch(@RequestBody Map<String, Object> body) {
        Integer maPhuongTien = (Integer) body.get("maPhuongTien");
        Boolean validXe = jdbcTemplate.queryForObject("SELECT co_kiem_soat_nhiet_do FROM phuong_tien WHERE ma_phuong_tien = ?", Boolean.class, maPhuongTien);
        if (validXe == null || !validXe) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Phương tiện chọn không đáp ứng tiêu chuẩn làm lạnh!");
            return ResponseEntity.status(400).body(err);
        }
        String sql = "INSERT INTO phan_cong (ma_don, ma_dieu_phoi, ma_tai_xe, ma_phuong_tien, tuyen_duong, trang_thai) VALUES (?, ?, ?, ?, ?, 'HIEU_LUC')";
        jdbcTemplate.update(sql, body.get("maDon"), body.get("maDieuPhoi"), body.get("maTaiXe"), maPhuongTien, body.get("tuyenDuong"));
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'DANG_VAN_CHUYEN' WHERE ma_don = ?", body.get("maDon"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Phân công xe lạnh thành công");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/orders/{maDon}/gate-check")
    public ResponseEntity<?> gateCheck(@PathVariable Integer maDon) {
        String checkDoc = "SELECT COUNT(*) FROM yeu_cau_giay_to y JOIN don_van_chuyen d ON d.ma_loai = y.ma_loai LEFT JOIN giay_to g ON g.ma_don = d.ma_don AND g.ma_yeu_cau = y.ma_yeu_cau AND g.trang_thai_duyet = 'HOP_LE' WHERE d.ma_don = ? AND y.bat_buoc = TRUE AND g.ma_giay_to IS NULL";
        Integer missingDoc = jdbcTemplate.queryForObject(checkDoc, Integer.class, maDon);

        String checkIncident = "SELECT COUNT(*) FROM su_co WHERE ma_don = ? AND trang_thai_giai_quyet = 'CHUA_GIAI_QUYET'";
        Integer activeIncident = jdbcTemplate.queryForObject(checkIncident, Integer.class, maDon);
        List<String> errors = new ArrayList<>();
        if (missingDoc != null && missingDoc > 0) errors.add("Thiếu " + missingDoc + " chứng từ bắt buộc hợp lệ");
        if (activeIncident != null && activeIncident > 0) errors.add("Sự cố chưa giải quyết");
        Map<String, Object> res = new HashMap<>();
        res.put("reasons", errors);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/delivery/pod")
    public ResponseEntity<?> createPod(@RequestBody Map<String, Object> body) {
        String sql = "INSERT INTO bien_ban_giao_nhan (ma_don, ma_tai_xe, nguoi_nhan, tinh_trang_hang, nhiet_do_luc_giao, thoi_gian_giao) VALUES (?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, body.get("maDon"), body.get("maTaiXe"), body.get("nguoiNhan"), body.get("tinhTrangHang"), body.get("nhietDoLucGiao"));
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'DA_BAN_GIAO' WHERE ma_don = ?", body.get("maDon"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Lập biên bản POD thành công!");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/orders/{maDon}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer maDon, @RequestBody Map<String, String> body) {
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'DA_HUY', ly_do_huy = ? WHERE ma_don = ?", body.get("lyDoHuy"), maDon);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã hủy đơn hàng!");
        return ResponseEntity.ok(res);
    }
}