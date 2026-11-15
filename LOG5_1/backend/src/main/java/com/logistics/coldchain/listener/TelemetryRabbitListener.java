package com.logistics.coldchain.listener;

import com.logistics.coldchain.dto.TelemetryDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TelemetryRabbitListener {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RabbitListener(queues = "${coldchain.rabbitmq.queue}")
    public void processTelemetryData(TelemetryDTO dto) {
        System.out.println("📥 [RABBITMQ CONSUMER] Đã nhận gói tin cảm biến IoT của Đơn #" + dto.getMaDon() + " - Nhiệt độ: " + dto.getNhietDo() + "°C");

        // 1. Lưu bản ghi dữ liệu hành trình vào Database PostgreSQL/PostGIS
        String sqlInsert = "INSERT INTO thong_so_hanh_trinh (ma_don, ma_tai_xe, vi_tri_lat, vi_tri_lng, nhiet_do_thung_xe, thoi_gian) VALUES (?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sqlInsert, dto.getMaDon(), dto.getMaTaiXe(), dto.getViTriLat(), dto.getViTriLng(), dto.getNhietDo());

        // 2. Kiểm tra ngưỡng dải nhiệt an toàn của đơn hàng
        String sqlCheck = "SELECT l.nhiet_do_toi_thieu, l.nhiet_do_toi_da FROM don_van_chuyen d JOIN loai_hang_lanh l ON d.ma_loai = l.ma_loai WHERE d.ma_don = ?";
        jdbcTemplate.query(sqlCheck, (rs) -> {
            double minTemp = rs.getDouble("nhiet_do_toi_thieu");
            double maxTemp = rs.getDouble("nhiet_do_toi_da");

            // Tự động phát hiện đứt gãy chuỗi lạnh (UC06)
            if (dto.getNhietDo() < minTemp || dto.getNhietDo() > maxTemp) {
                System.err.println("⚠️ [CẢNH BÁO ĐỨT GÃY CHUỖI LẠNH] Đơn #" + dto.getMaDon() + " vi phạm dải nhiệt! Nhiệt độ thùng: " + dto.getNhietDo() + "°C (Chuẩn: " + minTemp + "°C ~ " + maxTemp + "°C)");
                
                // Khóa đơn hàng về trạng thái TAM_DUNG & Tạo biên bản sự cố tự động
                jdbcTemplate.update("UPDATE don_van_chuyen SET trang_thai = 'TAM_DUNG' WHERE ma_don = ?", dto.getMaDon());
                jdbcTemplate.update("INSERT INTO su_co (ma_don, loai_su_co, mo_ta, trang_thai_giai_quyet) VALUES (?, 'VUORT_NHIET_DO', ?, 'CHUA_GIAI_QUYET')", 
                        dto.getMaDon(), "Nhiệt độ cảm biến " + dto.getNhietDo() + "°C vượt dải an toàn [" + minTemp + "°C, " + maxTemp + "°C]");
            }
        }, dto.getMaDon());
    }
}
