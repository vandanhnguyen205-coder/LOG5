package com.logistics.controller;

import com.logistics.model.DonHang;
import com.logistics.model.NguoiDung;
import com.logistics.service.LogisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@SuppressWarnings({"rawtypes", "unchecked"})
public class ApiController {

    @Autowired
    private LogisticsService service;

    @GetMapping("/orders")
    public List getOrders(@RequestParam(required = false) String driverEmail) {
        List all = service.getAllOrders();
        if (driverEmail != null && !driverEmail.trim().isEmpty()) {
            List filtered = new ArrayList();
            for (Object item : all) {
                DonHang o = (DonHang) item;
                if (o.getTaiXeEmail() != null && o.getTaiXeEmail().equalsIgnoreCase(driverEmail.trim())
                        && !"Chờ kiểm duyệt".equals(o.getTrangThai())
                        && !"Đã phê duyệt".equals(o.getTrangThai())
                        && !"Từ chối".equals(o.getTrangThai())
                        && !"DA_HUY".equals(o.getTrangThai())) {
                    filtered.add(o);
                }
            }
            return filtered;
        }
        return all;
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity getOrderDetail(@PathVariable String id) {
        Optional opt = service.getOrderById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody(required = false) Map body,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String password) {
        String em = (body != null && body.get("email") != null) ? body.get("email").toString() : email;
        String pw = (body != null && body.get("password") != null) ? body.get("password").toString() : password;

        Map resp = new HashMap();
        NguoiDung user = service.authenticate(em, pw);
        if (user != null) {
            resp.put("success", true);
            resp.put("user", user);
            return ResponseEntity.ok(resp);
        }
        resp.put("success", false);
        resp.put("message", "Email hoặc mật khẩu không chính xác!");
        return ResponseEntity.ok(resp);
    }

    @PostMapping({"/orders/update-temp", "/orders/{pathOrderId}/update-temp"})
    public ResponseEntity updateTemp(
            @PathVariable(required = false) String pathOrderId,
            @RequestBody(required = false) Map body,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Double newTemp,
            @RequestParam(required = false) Double temp) {

        String id = pathOrderId != null ? pathOrderId : ((body != null && body.get("orderId") != null) ? body.get("orderId").toString() : orderId);
        Double t = newTemp != null ? newTemp : temp;
        if (t == null && body != null) {
            Object v = body.get("newTemp") != null ? body.get("newTemp") : body.get("temp");
            if (v != null) {
                t = Double.parseDouble(v.toString());
            }
        }

        String result = service.updateTemperature(id, t != null ? t : -20.0);
        boolean isShock = result.startsWith("EX_TEMP");

        Map resp = new HashMap();
        resp.put("success", !isShock);
        resp.put("blocked", isShock);
        resp.put("message", result);
        return ResponseEntity.ok(resp);
    }

    @PostMapping({"/orders/pass-checkpoint", "/orders/toggle-checkpoint", "/orders/{pathOrderId}/checkpoint"})
    public ResponseEntity passCheckpoint(
            @PathVariable(required = false) String pathOrderId,
            @RequestBody(required = false) Map body,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Integer tramId) {

        String id = pathOrderId != null ? pathOrderId : ((body != null && body.get("orderId") != null) ? body.get("orderId").toString() : orderId);
        Integer tid = tramId;
        if (tid == null && body != null && body.get("tramId") != null) {
            tid = Integer.parseInt(body.get("tramId").toString());
        }

        String result = service.passCheckpoint(id, tid != null ? tid : 1);
        boolean ok = result.startsWith("SUCCESS");

        Map resp = new HashMap();
        resp.put("success", ok);
        resp.put("warning", !ok);
        resp.put("message", result);
        return ResponseEntity.ok(resp);
    }

    @PostMapping({"/orders/start-trip", "/orders/{pathOrderId}/start-trip"})
    public ResponseEntity startTrip(
            @PathVariable(required = false) String pathOrderId,
            @RequestBody(required = false) Map body,
            @RequestParam(required = false) String orderId) {
        String id = pathOrderId != null ? pathOrderId : ((body != null && body.get("orderId") != null) ? body.get("orderId").toString() : orderId);
        service.startTrip(id);
        Map resp = new HashMap();
        resp.put("success", true);
        resp.put("message", "Đã bắt đầu chuyến đi!");
        return ResponseEntity.ok(resp);
    }

    @PostMapping({"/orders/complete-delivery", "/orders/{pathOrderId}/complete-delivery"})
    public ResponseEntity completeDelivery(
            @PathVariable(required = false) String pathOrderId,
            @RequestBody(required = false) Map body,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String podFileName) {
        String id = pathOrderId != null ? pathOrderId : ((body != null && body.get("orderId") != null) ? body.get("orderId").toString() : orderId);
        String file = podFileName != null ? podFileName : ((body != null && body.get("podFileName") != null) ? body.get("podFileName").toString() : "pod_flutter.pdf");

        String result = service.completeDelivery(id, file);
        boolean ok = result.startsWith("SUCCESS");
        Map resp = new HashMap();
        resp.put("success", ok);
        resp.put("blocked", !ok);
        resp.put("message", result);
        return ResponseEntity.ok(resp);
    }
}