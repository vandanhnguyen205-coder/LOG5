package com.logistics.service;

import com.logistics.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class LogisticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initDatabaseTablesAndCleanup() {
        try {
            jdbcTemplate.execute("ALTER TABLE don_van_chuyen ADD COLUMN IF NOT EXISTS ten_hang VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE don_van_chuyen ADD COLUMN IF NOT EXISTS loai_hang VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE don_van_chuyen ADD COLUMN IF NOT EXISTS nhiet_do_min DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE don_van_chuyen ADD COLUMN IF NOT EXISTS nhiet_do_max DOUBLE PRECISION");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS tram_kiem_tra (" +
                "ma_don VARCHAR(50) NOT NULL, " +
                "tram_id INT NOT NULL, " +
                "ten_tram VARCHAR(255) NOT NULL, " +
                "da_qua BOOLEAN DEFAULT FALSE, " +
                "thoi_gian_qua TIMESTAMP NULL, " +
                "nhiet_do_xac_nhan DOUBLE PRECISION NULL, " +
                "PRIMARY KEY (ma_don, tram_id))"
            );
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.update(
                "DELETE FROM giay_to g1 WHERE g1.so_hieu = 'Bổ sung' " +
                "AND EXISTS (SELECT 1 FROM giay_to g2 WHERE g2.ma_don = g1.ma_don AND g2.ma_giay_to != g1.ma_giay_to)"
            );
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.update("UPDATE don_van_chuyen SET ly_do_huy = TRIM(REPLACE(ly_do_huy, '🚨', '')) WHERE ly_do_huy LIKE '%🚨%'");
        } catch (Exception ignored) {}
    }

    public List<DonHang> getAllOrders() {
        String sql = "SELECT d.ma_don, d.diem_lay, d.diem_giao, d.trang_thai, d.ly_do_huy, " +
                     "d.ten_hang, d.loai_hang, d.nhiet_do_min, d.nhiet_do_max, " +
                     "l.ten_loai, l.nhiet_do_toi_thieu, l.nhiet_do_toi_da, " +
                     "u_gui.ho_ten AS nguoi_gui_ten, " +
                     "pc.ma_tai_xe, u_tx.email AS tai_xe_email, u_tx.ho_ten AS tai_xe_ten, pt.bien_so " +
                     "FROM don_van_chuyen d " +
                     "LEFT JOIN loai_hang_lanh l ON d.ma_loai = l.ma_loai " +
                     "LEFT JOIN nguoi_dung u_gui ON d.ma_nguoi_gui = u_gui.ma_nguoi_dung " +
                     "LEFT JOIN phan_cong pc ON d.ma_don = pc.ma_don " +
                     "LEFT JOIN nguoi_dung u_tx ON pc.ma_tai_xe = u_tx.ma_nguoi_dung " +
                     "LEFT JOIN phuong_tien pt ON pc.ma_phuong_tien = pt.ma_phuong_tien " +
                     "ORDER BY d.ngay_tao DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DonHang item = new DonHang();
            item.setId(rs.getString("ma_don"));

            String tenHangDb = rs.getString("ten_hang");
            String tenLoaiDb = rs.getString("ten_loai");
            item.setTenHang((tenHangDb != null && !tenHangDb.trim().isEmpty())
                    ? tenHangDb
                    : (tenLoaiDb != null ? tenLoaiDb : "Hàng Lạnh"));

            String loaiHangDb = rs.getString("loai_hang");
            item.setLoaiHang((loaiHangDb != null && !loaiHangDb.trim().isEmpty())
                    ? loaiHangDb
                    : "Hàng Lạnh Đặc Thù");

            item.setDiemDi(rs.getString("diem_lay"));
            item.setDiemDen(rs.getString("diem_giao"));
            item.setTrangThai(rs.getString("trang_thai"));

            double minT = rs.getDouble("nhiet_do_min");
            if (rs.wasNull()) {
                minT = rs.getDouble("nhiet_do_toi_thieu");
                if (rs.wasNull()) minT = -25.0;
            }
            item.setMinTemp(minT);

            double maxT = rs.getDouble("nhiet_do_max");
            if (rs.wasNull()) {
                maxT = rs.getDouble("nhiet_do_toi_da");
                if (rs.wasNull()) maxT = -15.0;
            }
            item.setMaxTemp(maxT);

            Double lastTemp = getLastTemperature(item.getId());
            double defaultMidTemp = Math.round(((minT + maxT) / 2.0) * 10.0) / 10.0;
            item.setNhietDoThucTe(lastTemp != null ? lastTemp : defaultMidTemp);

            item.setNguoiTao(rs.getString("nguoi_gui_ten"));
            item.setMaTaiXe(rs.getString("ma_tai_xe"));
            item.setTaiXeEmail(rs.getString("tai_xe_email"));
            item.setTaiXeTen(rs.getString("tai_xe_ten") != null ? rs.getString("tai_xe_ten") : "Chưa phân công");
            item.setBienSoXe(rs.getString("bien_so") != null ? rs.getString("bien_so") : "N/A");

            String lyDo = rs.getString("ly_do_huy");
            item.setLyDoBiChan(lyDo);
            item.setLyDoTuChoi(lyDo);
            item.setCapNhatCuoi("Vừa xong");

            item.setDanhSachGiayTo(getGiayToByDon(item.getId()));
            item.setTuyenDuong(getCheckpointsByDon(item.getId()));
            return item;
        });
    }

    public List<DonHang> getOrdersForUser(NguoiDung user) {
        List<DonHang> all = getAllOrders();
        if (user == null) return new ArrayList<>();

        if ("TAI_XE_VAN_CHUYEN".equals(user.getVaiTro())) {
            return all.stream()
                    .filter(o -> o.getTaiXeEmail() != null && o.getTaiXeEmail().equalsIgnoreCase(user.getEmail()))
                    .filter(o -> !"Chờ kiểm duyệt".equals(o.getTrangThai())
                              && !"Đã phê duyệt".equals(o.getTrangThai())
                              && !"Từ chối".equals(o.getTrangThai())
                              && !"DA_HUY".equals(o.getTrangThai()))
                    .collect(Collectors.toList());
        }
        return all;
    }

    private Double getLastTemperature(String maDon) {
        try {
            String sql = "SELECT nhiet_do_thung_xe FROM thong_so_hanh_trinh WHERE ma_don = ? ORDER BY thoi_diem DESC LIMIT 1";
            List<Double> list = jdbcTemplate.query(sql, new Object[]{maDon}, (rs, r) -> rs.getDouble("nhiet_do_thung_xe"));
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private double[] getOrderTempBounds(String maDon) {
        try {
            List rows = jdbcTemplate.queryForList(
                "SELECT d.nhiet_do_min, d.nhiet_do_max, l.nhiet_do_toi_thieu, l.nhiet_do_toi_da " +
                "FROM don_van_chuyen d LEFT JOIN loai_hang_lanh l ON d.ma_loai = l.ma_loai WHERE d.ma_don = ?",
                maDon
            );
            if (!rows.isEmpty()) {
                Map row = (Map) rows.get(0);
                double minT = row.get("nhiet_do_min") != null
                        ? ((Number) row.get("nhiet_do_min")).doubleValue()
                        : (row.get("nhiet_do_toi_thieu") != null ? ((Number) row.get("nhiet_do_toi_thieu")).doubleValue() : -25.0);
                double maxT = row.get("nhiet_do_max") != null
                        ? ((Number) row.get("nhiet_do_max")).doubleValue()
                        : (row.get("nhiet_do_toi_da") != null ? ((Number) row.get("nhiet_do_toi_da")).doubleValue() : -15.0);
                return new double[]{minT, maxT};
            }
        } catch (Exception ignored) {}
        return new double[]{-25.0, -15.0};
    }

    public Optional<DonHang> getOrderById(String id) {
        return getAllOrders().stream().filter(o -> o.getId().equalsIgnoreCase(id)).findFirst();
    }

    public List<NguoiDung> getAllUsers() {
        String sql = "SELECT ma_nguoi_dung, ho_ten, email, so_dien_thoai, vai_tro FROM nguoi_dung";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new NguoiDung(
                rs.getString("ma_nguoi_dung"), rs.getString("ho_ten"), rs.getString("email"),
                rs.getString("so_dien_thoai"), "***", rs.getString("vai_tro")
        ));
    }

    public NguoiDung authenticate(String email, String password) {
        String sql = "SELECT * FROM nguoi_dung WHERE email = ? AND mat_khau_ma_hoa = ?";
        List<NguoiDung> users = jdbcTemplate.query(sql, new Object[]{email, password}, (rs, rowNum) -> new NguoiDung(
                rs.getString("ma_nguoi_dung"), rs.getString("ho_ten"), rs.getString("email"),
                rs.getString("so_dien_thoai"), rs.getString("mat_khau_ma_hoa"), rs.getString("vai_tro")
        ));
        return users.isEmpty() ? null : users.get(0);
    }

    public String registerUser(String hoTen, String email, String password) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM nguoi_dung WHERE email = ?", Integer.class, email);
        if (count != null && count > 0) return "Email đã tồn tại trong hệ thống!";

        String maUser = "ND" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update("INSERT INTO nguoi_dung (ma_nguoi_dung, ho_ten, email, so_dien_thoai, mat_khau_ma_hoa, vai_tro) VALUES (?, ?, ?, '0900000000', ?, 'NGUOI_GUI_HANG')",
                maUser, hoTen, email, password);
        return "SUCCESS";
    }

    public void createOrder(String tenHang, String loaiHang, double minTemp, double maxTemp, String diemDi, String diemDen, String loaiGiayTo, String fileName, String userEmail) {
        String maDon = "ORD-" + (System.currentTimeMillis() % 10000);
        List<String> list = jdbcTemplate.query("SELECT ma_nguoi_dung FROM nguoi_dung WHERE email = ?", new Object[]{userEmail}, (rs, r) -> rs.getString("ma_nguoi_dung"));
        String maNguoiGui = list.isEmpty() ? "ND02" : list.get(0);

        double actualMin = Math.min(minTemp, maxTemp);
        double actualMax = Math.max(minTemp, maxTemp);

        jdbcTemplate.update(
                "INSERT INTO don_van_chuyen (ma_don, ma_nguoi_gui, ma_loai, ten_hang, loai_hang, nhiet_do_min, nhiet_do_max, diem_lay, diem_giao, khoi_luong, thoi_gian_mong_muon, ngay_tao, trang_thai) " +
                "VALUES (?, ?, 'LHL01', ?, ?, ?, ?, ?, ?, 500.0, NOW() + INTERVAL '1 day', NOW(), 'Chờ kiểm duyệt')",
                maDon, maNguoiGui, tenHang, loaiHang, actualMin, actualMax, diemDi, diemDen);

        String maGiayTo = "DOC-" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update("INSERT INTO giay_to (ma_giay_to, ma_don, so_hieu, ngay_cap, ngay_het_han, tep_dinh_kem, trang_thai_duyet) VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE + 30, ?, 'Chờ duyệt')",
                maGiayTo, maDon, loaiGiayTo, fileName);

        ensureCheckpointsInitialized(maDon);
    }

    public String updateTemperature(String orderId, double newTemp) {
        String tsId = "TS-" + System.currentTimeMillis();
        jdbcTemplate.update("INSERT INTO thong_so_hanh_trinh (ma_ban_ghi, ma_don, thoi_diem, vi_tri_gps, nhiet_do_thung_xe) VALUES (?, ?, NOW(), '10.762, 106.660', ?)",
                tsId, orderId, newTemp);

        double[] bounds = getOrderTempBounds(orderId);
        double minT = bounds[0];
        double maxT = bounds[1];

        if (newTemp < minT || newTemp > maxT) {
            jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'BI_CHAN', ly_do_huy = ? WHERE ma_don = ?",
                    "VI PHẠM NHIỆT ĐỘ: Nhiệt độ thực tế " + newTemp + "°C nằm ngoài ngưỡng yêu cầu (" + minT + "°C đến " + maxT + "°C)", orderId);

            String scId = "SC-" + System.currentTimeMillis();
            jdbcTemplate.update("INSERT INTO su_co (ma_su_co, ma_don, loai_su_co, mo_ta, thoi_diem, trang_thai) VALUES (?, ?, 'Sốc Nhiệt', ?, NOW(), 'MỚI')",
                    scId, orderId, "Nhiệt độ cảm biến " + newTemp + "°C vượt ngoài ngưỡng (" + minT + "°C ~ " + maxT + "°C)");
            return "EX_TEMP: Cảnh báo nhiệt độ " + newTemp + "°C ngoài ngưỡng (" + minT + "°C ~ " + maxT + "°C)! Đơn hàng đã bị tạm khóa.";
        } else {
            jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Đang vận chuyển', ly_do_huy = NULL WHERE ma_don = ? AND trang_thai != 'Đã giao hàng'", orderId);
            jdbcTemplate.update("UPDATE su_co SET phuong_an_xu_ly = ?, trang_thai = 'ĐÃ_ĐÓNG' WHERE ma_don = ? AND trang_thai != 'ĐÃ_ĐÓNG'",
                    "Nhiệt độ thùng xe đã ổn định trong ngưỡng (" + newTemp + "°C)", orderId);
            return "SUCCESS: Đã ghi nhận nhiệt độ an toàn (" + newTemp + "°C, ngưỡng: " + minT + "°C ~ " + maxT + "°C). Bạn có thể xác nhận qua trạm tiếp theo.";
        }
    }

    public void resolveIncident(String incidentId, String orderId, String solution) {
        jdbcTemplate.update("UPDATE su_co SET phuong_an_xu_ly = ?, trang_thai = 'ĐÃ_ĐÓNG' WHERE ma_su_co = ?", solution, incidentId);
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Đang vận chuyển', ly_do_huy = NULL WHERE ma_don = ?", orderId);
    }

    public List<LoaiHangLanh> getAllCategories() {
        String sql = "SELECT ma_loai, ten_loai, mo_ta, nhiet_do_toi_thieu, nhiet_do_toi_da, trang_thai_su_dung FROM loai_hang_lanh";
        return jdbcTemplate.query(sql, (rs, r) -> {
            LoaiHangLanh item = new LoaiHangLanh();
            item.setMaLoai(rs.getString("ma_loai"));
            item.setTenLoai(rs.getString("ten_loai"));
            item.setMoTa(rs.getString("mo_ta"));
            item.setNhietDoToiThieu(rs.getDouble("nhiet_do_toi_thieu"));
            item.setNhietDoToiDa(rs.getDouble("nhiet_do_toi_da"));
            item.setTrangThaiSuDung(rs.getBoolean("trang_thai_su_dung"));
            return item;
        });
    }

    public Map<String, Object> getStatisticsReport() {
        Map<String, Object> stats = new HashMap<>();
        Integer totalOrders = jdbcTemplate.queryForObject("SELECT count(*) FROM don_van_chuyen", Integer.class);
        Integer deliveredOrders = jdbcTemplate.queryForObject("SELECT count(*) FROM don_van_chuyen WHERE trang_thai = 'Đã giao hàng' OR trang_thai = 'ĐÃ GIAO HÀNG'", Integer.class);
        Integer blockedDeliveries = jdbcTemplate.queryForObject("SELECT count(*) FROM ket_qua_kiem_tra WHERE ket_qua = 'KHONG_DAT'", Integer.class);
        Integer totalIncidents = jdbcTemplate.queryForObject("SELECT count(*) FROM su_co", Integer.class);

        stats.put("totalOrders", totalOrders != null ? totalOrders : 0);
        stats.put("deliveredOrders", deliveredOrders != null ? deliveredOrders : 0);
        stats.put("blockedDeliveries", blockedDeliveries != null ? blockedDeliveries : 0);
        stats.put("totalIncidents", totalIncidents != null ? totalIncidents : 0);
        stats.put("successRate", (totalOrders != null && totalOrders > 0) ? (deliveredOrders * 100.0 / totalOrders) : 0.0);
        return stats;
    }

    public String completeDelivery(String orderId, String podFileName) {
        ensureCheckpointsInitialized(orderId);
        List<String> violations = new ArrayList<>();

        Integer passedCheckpoints = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tram_kiem_tra WHERE ma_don = ? AND da_qua = TRUE",
                Integer.class, orderId);
        int countPassed = passedCheckpoints != null ? passedCheckpoints : 0;
        if (countPassed < 3) {
            violations.add("Chưa đi qua đủ 3 trạm kiểm tra bắt buộc (Mới qua " + countPassed + "/3 trạm)");
        }

        Integer totalDocs = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM giay_to WHERE ma_don = ?", Integer.class, orderId);
        if (totalDocs == null || totalDocs == 0) {
            violations.add("Đơn hàng chưa có chứng từ pháp lý đính kèm");
        }

        Integer unapprovedDocs = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM giay_to WHERE ma_don = ? AND (trang_thai_duyet IS NULL OR trang_thai_duyet != 'Đã duyệt')",
                Integer.class, orderId);
        if (unapprovedDocs != null && unapprovedDocs > 0) {
            violations.add("Còn chứng từ bắt buộc chưa được duyệt hợp lệ");
        }

        double[] bounds = getOrderTempBounds(orderId);
        double minT = bounds[0];
        double maxT = bounds[1];
        Double lastTemp = getLastTemperature(orderId);

        if (lastTemp != null && (lastTemp < minT || lastTemp > maxT)) {
            violations.add("Nhiệt độ thùng xe hiện tại (" + lastTemp + "°C) nằm ngoài ngưỡng yêu cầu (" + minT + "°C ~ " + maxT + "°C)");
        } else {
            jdbcTemplate.update("UPDATE su_co SET phuong_an_xu_ly = 'Nhiệt độ đã ổn định trước khi bàn giao', trang_thai = 'ĐÃ_ĐÓNG' WHERE ma_don = ? AND trang_thai != 'ĐÃ_ĐÓNG'", orderId);
        }

        Integer openIncidents = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM su_co WHERE ma_don = ? AND trang_thai != 'ĐÃ_ĐÓNG'",
                Integer.class, orderId);
        if (openIncidents != null && openIncidents > 0) {
            violations.add("Đơn hàng đang có sự cố nhiệt độ chưa được gỡ khóa");
        }

        if (!violations.isEmpty()) {
            String violationMsg = String.join("; ", violations);
            String bbErrId = "BB-ERR-" + (System.currentTimeMillis() % 10000);
            try {
                jdbcTemplate.update("INSERT INTO bien_ban_ban_giao (ma_bien_ban, ma_don, nguoi_nhan, tinh_trang_hang, nhiet_do_luc_giao, thoi_diem_giao, ket_qua) VALUES (?, ?, 'Chặn bàn giao', ?, ?, NOW(), 'Không đạt')",
                        bbErrId, orderId, violationMsg, lastTemp != null ? lastTemp : ((minT + maxT) / 2.0));
                jdbcTemplate.update("INSERT INTO ket_qua_kiem_tra (ma_don, ma_bien_ban, thoi_diem, ket_qua, danh_sach_muc_chua_thoa) VALUES (?, ?, NOW(), 'KHONG_DAT', ?)",
                        orderId, bbErrId, violationMsg);
            } catch (Exception ignored) {}
            jdbcTemplate.update("UPDATE don_van_chuyen SET ly_do_huy = ? WHERE ma_don = ?", "CHẶN BÀN GIAO: " + violationMsg, orderId);
            return "EX_DELIVERY: CHẶN BÀN GIAO! " + violationMsg;
        }

        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Đã giao hàng', ly_do_huy = NULL WHERE ma_don = ?", orderId);
        String bbId = "BB-" + (System.currentTimeMillis() % 10000);
        double finalTemp = lastTemp != null ? lastTemp : ((minT + maxT) / 2.0);
        jdbcTemplate.update("INSERT INTO bien_ban_ban_giao (ma_bien_ban, ma_don, nguoi_nhan, tinh_trang_hang, nhiet_do_luc_giao, thoi_diem_giao, ket_qua) VALUES (?, ?, 'Người nhận kho', 'Đạt chuẩn POD lạnh', ?, NOW(), 'Thành công')",
                bbId, orderId, finalTemp);
        try {
            jdbcTemplate.update("INSERT INTO ket_qua_kiem_tra (ma_don, ma_bien_ban, thoi_diem, ket_qua, danh_sach_muc_chua_thoa) VALUES (?, ?, NOW(), 'DAT', 'Đạt toàn bộ điều kiện an toàn & đủ 3/3 trạm')",
                    orderId, bbId);
        } catch (Exception ignored) {}
        return "SUCCESS: Đã hoàn thành bàn giao hàng điện tử (POD)!";
    }

    public void approveDocument(String orderId, String docId) {
        String currentStatus = jdbcTemplate.queryForObject(
                "SELECT trang_thai FROM don_van_chuyen WHERE ma_don = ?", String.class, orderId);
        if ("Từ chối".equalsIgnoreCase(currentStatus) || "DA_HUY".equalsIgnoreCase(currentStatus)) {
            return;
        }
        jdbcTemplate.update("UPDATE giay_to SET trang_thai_duyet = 'Đã duyệt' WHERE ma_giay_to = ? AND (trang_thai_duyet IS NULL OR trang_thai_duyet != 'Từ chối')", docId);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM giay_to WHERE ma_don = ? AND (trang_thai_duyet IS NULL OR trang_thai_duyet != 'Đã duyệt')",
                Integer.class, orderId);
        if (remaining == null || remaining == 0) {
            jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Đã phê duyệt' WHERE ma_don = ?", orderId);
        }
    }

    public void assignDriver(String orderId, String driverEmail, String bienSoXe) {
        List<String> txList = jdbcTemplate.query(
                "SELECT ma_nguoi_dung FROM nguoi_dung WHERE email = ?",
                new Object[]{driverEmail},
                (rs, r) -> rs.getString("ma_nguoi_dung")
        );
        if (txList.isEmpty()) return;
        String maTaiXe = txList.get(0);

        String maPt = "PT01";
        List<String> ptList = jdbcTemplate.query(
                "SELECT ma_phuong_tien FROM phuong_tien WHERE bien_so = ?",
                new Object[]{bienSoXe},
                (rs, r) -> rs.getString("ma_phuong_tien")
        );
        if (!ptList.isEmpty()) {
            maPt = ptList.get(0);
        } else {
            try {
                String newPt = "PT" + (System.currentTimeMillis() % 10000);
                jdbcTemplate.update("INSERT INTO phuong_tien (ma_phuong_tien, bien_so, loai_xe, tai_trong, nhiet_do_min, nhiet_do_max, trang_thai) VALUES (?, ?, 'Xe tải lạnh', 2000, -25, -10, 'Sẵn sàng')",
                        newPt, bienSoXe);
                maPt = newPt;
            } catch (Exception ignored) {
                try {
                    jdbcTemplate.update("UPDATE phuong_tien SET bien_so = ? WHERE ma_phuong_tien = 'PT01'", bienSoXe);
                } catch (Exception ignored2) {}
            }
        }

        jdbcTemplate.update("DELETE FROM phan_cong WHERE ma_don = ?", orderId);
        String pcId = "PC-" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update("INSERT INTO phan_cong (ma_phan_cong, ma_don, ma_dieu_phoi_vien, ma_tai_xe, ma_phuong_tien, tuyen_duong, thoi_gian_du_kien, trang_thai) VALUES (?, ?, 'ND04', ?, ?, 'Tuyến đường quy chuẩn', NOW() + INTERVAL '1 day', 'Đã phân công')",
                pcId, orderId, maTaiXe, maPt);
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Đã phân công' WHERE ma_don = ?", orderId);
        ensureCheckpointsInitialized(orderId);
    }

    public void startTrip(String orderId) {
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Đang vận chuyển' WHERE ma_don = ?", orderId);
        ensureCheckpointsInitialized(orderId);
    }

    public void rejectOrder(String orderId, String reason) {
        jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Từ chối', ly_do_huy = ? WHERE ma_don = ?", reason, orderId);
        int updated = jdbcTemplate.update("UPDATE giay_to SET trang_thai_duyet = 'Từ chối' WHERE ma_don = ? AND (trang_thai_duyet IS NULL OR trang_thai_duyet != 'Đã duyệt')", orderId);
        if (updated == 0) {
            jdbcTemplate.update("UPDATE giay_to SET trang_thai_duyet = 'Từ chối' WHERE ma_don = ?", orderId);
        }
    }

    public void deleteDocument(String orderId, String docId) {
        jdbcTemplate.update("DELETE FROM giay_to WHERE ma_giay_to = ? AND ma_don = ?", docId, orderId);
    }

    public void resubmitOrder(String orderId, String docId, String loaiGiayTo, String fileName) {
        if (docId != null && !docId.trim().isEmpty()) {
            if (loaiGiayTo != null && !loaiGiayTo.trim().isEmpty()) {
                jdbcTemplate.update("UPDATE giay_to SET so_hieu = ?, tep_dinh_kem = ?, trang_thai_duyet = 'Chờ duyệt' WHERE ma_giay_to = ? AND ma_don = ?",
                        loaiGiayTo, fileName, docId, orderId);
            } else {
                jdbcTemplate.update("UPDATE giay_to SET tep_dinh_kem = ?, trang_thai_duyet = 'Chờ duyệt' WHERE ma_giay_to = ? AND ma_don = ?",
                        fileName, docId, orderId);
            }
        } else {
            List<String> rejectedIds = jdbcTemplate.query(
                    "SELECT ma_giay_to FROM giay_to WHERE ma_don = ? AND trang_thai_duyet = 'Từ chối'",
                    new Object[]{orderId},
                    (rs, r) -> rs.getString("ma_giay_to")
            );
            if (!rejectedIds.isEmpty()) {
                String primaryDocId = rejectedIds.get(0);
                jdbcTemplate.update("UPDATE giay_to SET tep_dinh_kem = ?, trang_thai_duyet = 'Chờ duyệt' WHERE ma_giay_to = ?",
                        fileName, primaryDocId);
                if (rejectedIds.size() > 1) {
                    jdbcTemplate.update("DELETE FROM giay_to WHERE ma_don = ? AND trang_thai_duyet = 'Từ chối'", orderId);
                }
            } else {
                String maGiayTo = "DOC-" + (System.currentTimeMillis() % 10000);
                String tenLoai = (loaiGiayTo != null && !loaiGiayTo.trim().isEmpty()) ? loaiGiayTo : "Giấy Phép Vận Chuyển Hàng Lạnh";
                jdbcTemplate.update("INSERT INTO giay_to (ma_giay_to, ma_don, so_hieu, ngay_cap, ngay_het_han, tep_dinh_kem, trang_thai_duyet) VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE + 30, ?, 'Chờ duyệt')",
                        maGiayTo, orderId, tenLoai, fileName);
            }
        }

        Integer stillRejected = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM giay_to WHERE ma_don = ? AND trang_thai_duyet = 'Từ chối'",
                Integer.class, orderId);
        Integer totalDocs = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM giay_to WHERE ma_don = ?",
                Integer.class, orderId);
        if ((stillRejected == null || stillRejected == 0) && (totalDocs != null && totalDocs > 0)) {
            jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'Chờ kiểm duyệt', ly_do_huy = NULL WHERE ma_don = ?", orderId);
        }
    }

    private void ensureCheckpointsInitialized(String maDon) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM tram_kiem_tra WHERE ma_don = ?", Integer.class, maDon);
            if (count == null || count == 0) {
                jdbcTemplate.update("INSERT INTO tram_kiem_tra (ma_don, tram_id, ten_tram, da_qua) VALUES (?, 1, 'Trạm 1: Kho Xuất', FALSE)", maDon);
                jdbcTemplate.update("INSERT INTO tram_kiem_tra (ma_don, tram_id, ten_tram, da_qua) VALUES (?, 2, 'Trạm 2: Trạm Kiểm Định Giữa Tuyến', FALSE)", maDon);
                jdbcTemplate.update("INSERT INTO tram_kiem_tra (ma_don, tram_id, ten_tram, da_qua) VALUES (?, 3, 'Trạm 3: Kho Đích', FALSE)", maDon);
            }
        } catch (Exception ignored) {}
    }

    public String passCheckpoint(String orderId, int tramId) {
        ensureCheckpointsInitialized(orderId);

        String orderStatus = jdbcTemplate.queryForObject(
                "SELECT trang_thai FROM don_van_chuyen WHERE ma_don = ?", String.class, orderId);
        if ("BI_CHAN".equalsIgnoreCase(orderStatus)) {
            return "WARN_SHOCK: ⚠️ Đơn hàng đang bị khóa do sự cố nhiệt độ! Không thể xác nhận qua trạm.";
        }
        if ("Đã giao hàng".equalsIgnoreCase(orderStatus)) {
            return "SUCCESS: Đơn hàng đã hoàn tất giao hàng.";
        }

        if (tramId > 1) {
            Boolean prevPassed = jdbcTemplate.queryForObject(
                    "SELECT da_qua FROM tram_kiem_tra WHERE ma_don = ? AND tram_id = ?",
                    Boolean.class, orderId, tramId - 1);
            if (prevPassed == null || !prevPassed) {
                return "WARN_SEQ: ⚠️ Bạn phải hoàn thành Trạm " + (tramId - 1) + " trước khi xác nhận qua Trạm " + tramId + "!";
            }
        }

        Boolean alreadyPassed = jdbcTemplate.queryForObject(
                "SELECT da_qua FROM tram_kiem_tra WHERE ma_don = ? AND tram_id = ?",
                Boolean.class, orderId, tramId);
        if (Boolean.TRUE.equals(alreadyPassed)) {
            return "SUCCESS: Trạm " + tramId + " đã được xác nhận trước đó.";
        }

        List latestTemps = jdbcTemplate.queryForList(
                "SELECT nhiet_do_thung_xe, thoi_diem FROM thong_so_hanh_trinh WHERE ma_don = ? ORDER BY thoi_diem DESC LIMIT 1",
                orderId);
        if (latestTemps.isEmpty()) {
            return "WARN_TEMP: ⚠️ CẢNH BÁO: Bạn chưa cập nhật nhiệt độ cảm biến tại Trạm " + tramId + "! Vui lòng ấn kiểm tra & gửi nhiệt độ trên Flutter trước khi qua trạm.";
        }

        Map firstRow = (Map) latestTemps.get(0);
        Timestamp latestTempTime = (Timestamp) firstRow.get("thoi_diem");
        Double latestTempVal = ((Number) firstRow.get("nhiet_do_thung_xe")).doubleValue();

        if (tramId > 1) {
            Timestamp prevStationTime = jdbcTemplate.queryForObject(
                    "SELECT thoi_gian_qua FROM tram_kiem_tra WHERE ma_don = ? AND tram_id = ?",
                    Timestamp.class, orderId, tramId - 1);
            if (prevStationTime != null && !latestTempTime.after(prevStationTime)) {
                return "WARN_TEMP: ⚠️ CẢNH BÁO: Bạn chưa cập nhật nhiệt độ mới sau khi rời Trạm " + (tramId - 1) + "! Vui lòng kiểm tra & gửi nhiệt độ trên Flutter trước khi qua Trạm " + tramId + ".";
            }
        }

        jdbcTemplate.update(
                "UPDATE tram_kiem_tra SET da_qua = TRUE, thoi_gian_qua = NOW(), nhiet_do_xac_nhan = ? WHERE ma_don = ? AND tram_id = ?",
                latestTempVal, orderId, tramId);
        jdbcTemplate.update(
                "UPDATE don_van_chuyen SET trang_thai = 'Đang vận chuyển' WHERE ma_don = ? AND trang_thai = 'Đã phân công'",
                orderId);

        return "SUCCESS: ✓ Đã xác nhận qua Trạm " + tramId + " thành công (Nhiệt độ: " + latestTempVal + "°C)!";
    }

    public String toggleCheckpoint(String orderId, int tramId) {
        return passCheckpoint(orderId, tramId);
    }

    public void updateUserRole(String userId, String newRole) {
        jdbcTemplate.update("UPDATE nguoi_dung SET vai_tro = ? WHERE ma_nguoi_dung = ?", newRole, userId);
    }

    public List<GiayToDinhKem> getGiayToByDon(String maDon) {
        String sql = "SELECT ma_giay_to, so_hieu, tep_dinh_kem, trang_thai_duyet FROM giay_to WHERE ma_don = ? ORDER BY ma_giay_to ASC";
        return jdbcTemplate.query(sql, new Object[]{maDon}, (rs, rowNum) -> {
            String status = rs.getString("trang_thai_duyet");
            boolean isApproved = "Đã duyệt".equalsIgnoreCase(status);
            return new GiayToDinhKem(
                    rs.getString("ma_giay_to"),
                    rs.getString("so_hieu"),
                    rs.getString("tep_dinh_kem"),
                    isApproved,
                    status != null ? status : "Chờ duyệt"
            );
        });
    }

    public List<TramKiemTra> getCheckpointsByDon(String maDon) {
        ensureCheckpointsInitialized(maDon);
        String sql = "SELECT tram_id, ten_tram, da_qua, nhiet_do_xac_nhan, thoi_gian_qua FROM tram_kiem_tra WHERE ma_don = ? ORDER BY tram_id ASC";
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        return jdbcTemplate.query(sql, new Object[]{maDon}, (rs, rowNum) -> {
            Double temp = rs.getDouble("nhiet_do_xac_nhan");
            if (rs.wasNull()) temp = null;
            Timestamp ts = rs.getTimestamp("thoi_gian_qua");
            String timeStr = ts != null ? fmt.format(ts) : null;
            return new TramKiemTra(
                    rs.getInt("tram_id"),
                    rs.getString("ten_tram"),
                    rs.getBoolean("da_qua"),
                    temp,
                    timeStr
            );
        });
    }
}