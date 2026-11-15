package com.logistics.coldchain.controller;

import com.logistics.coldchain.dto.TelemetryDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TelemetryController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${coldchain.rabbitmq.exchange}")
    private String exchange;

    @Value("${coldchain.rabbitmq.routingkey}")
    private String routingKey;

    @PostMapping("/telemetry")
    public ResponseEntity<?> receiveTelemetry(@RequestBody TelemetryDTO dto, @RequestHeader(value = "x-user-role", defaultValue = "TAI_XE") String role) {
        // Đẩy thông điệp vào RabbitMQ Broker (Producer)
        rabbitTemplate.convertAndSend(exchange, routingKey, dto);

        // Lấy ngưỡng dải nhiệt để phản hồi ngay trên giao diện Web
        String sql = "SELECT l.nhiet_do_toi_thieu, l.nhiet_do_toi_da FROM don_van_chuyen d JOIN loai_hang_lanh l ON d.ma_loai = l.ma_loai WHERE d.ma_don = ?";
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã tiếp nhận dữ liệu cảm biến và đẩy vào RabbitMQ Message Queue");

        try {
            Map<String, Object> range = jdbcTemplate.queryForMap(sql, dto.getMaDon());
            double min = ((Number) range.get("nhiet_do_toi_thieu")).doubleValue();
            double max = ((Number) range.get("nhiet_do_toi_da")).doubleValue();
            boolean isBreach = (dto.getNhietDo() < min || dto.getNhietDo() > max);
            response.put("isBreach", isBreach);
        } catch (Exception e) {
            response.put("isBreach", false);
        }

        return ResponseEntity.ok(response);
    }
}
