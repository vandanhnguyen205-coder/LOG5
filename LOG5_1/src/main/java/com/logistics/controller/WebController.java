package com.logistics.controller;

import com.logistics.model.NguoiDung;
import com.logistics.service.LogisticsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

@Controller
public class WebController {

    @Autowired
    private LogisticsService service;

    private String sanitizeFilename(String filename, String defaultName) {
        if (filename == null || filename.trim().isEmpty()) return defaultName;
        String clean = Paths.get(filename.trim()).getFileName().toString();
        clean = clean.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
        return clean.isEmpty() ? defaultName : clean;
    }

    @GetMapping("/favicon.ico")
    @ResponseBody
    public void disableFavicon() {}

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", user);

        try {
            model.addAttribute("orders", service.getOrdersForUser(user));
        } catch (Exception e) {
            model.addAttribute("orders", new ArrayList<>());
        }

        try {
            model.addAttribute("users", service.getAllUsers());
        } catch (Exception e) {
            model.addAttribute("users", new ArrayList<>());
        }

        return "dashboard";
    }

    @GetMapping("/orders/complete-delivery")
    public String showCompleteDeliveryPage(@RequestParam(required = false) String orderId, HttpSession session, Model model) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", user);
        try {
            model.addAttribute("orders", service.getOrdersForUser(user));
        } catch (Exception e) {
            model.addAttribute("orders", new ArrayList<>());
        }
        if (orderId != null && !orderId.trim().isEmpty()) {
            model.addAttribute("selectedOrderId", orderId.trim());
        }
        return "complete_delivery";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        NguoiDung user = service.authenticate(email, password);
        if (user != null) {
            session.setAttribute("currentUser", user);
            return "redirect:/";
        }
        model.addAttribute("error", "Email hoặc mật khẩu không chính xác!");
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String hoTen, @RequestParam String email, @RequestParam String password, Model model) {
        String result = service.registerUser(hoTen, email, password);
        if ("SUCCESS".equals(result)) {
            model.addAttribute("success", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            return "login";
        }
        model.addAttribute("error", result);
        return "register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/files/download")
    public ResponseEntity<byte[]> downloadFile(
            @RequestParam(defaultValue = "chung_tu.pdf") String fileName,
            @RequestParam(defaultValue = "Hàng Lạnh LOG-05") String tenHang,
            @RequestParam(defaultValue = "Chứng Từ Kiểm Định") String loaiGiayTo) {

        String cleanName = sanitizeFilename(fileName, "chung_tu.pdf");
        if (!cleanName.endsWith(".pdf")) cleanName += ".pdf";

        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>\nendobj\n" +
                "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n" +
                "5 0 obj\n<< /Length 200 >>\nstream\n" +
                "BT /F1 14 Tf 50 720 TD (HE THONG LOGISTICS CHUOI CUNG UNG LANH LOG-05) Tj ET\n" +
                "BT /F1 12 Tf 50 690 TD (CHUNG TU: " + loaiGiayTo.toUpperCase() + ") Tj ET\n" +
                "BT /F1 12 Tf 50 670 TD (TEN FILE: " + cleanName + ") Tj ET\n" +
                "BT /F1 12 Tf 50 650 TD (MAT HANG: " + tenHang + ") Tj ET\n" +
                "BT /F1 10 Tf 50 600 TD (TRANG THAI: DA KIEM DINH AN TOAN CHUOI CUNG UNG LANH) Tj ET\n" +
                "endstream\nendobj\n" +
                "xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000244 00000 n \n0000000313 00000 n \n" +
                "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n560\n%%EOF";

        byte[] bytes = pdfContent.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(cleanName, StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(contentDisposition);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/orders/create")
    public String createOrder(@RequestParam String tenHang, @RequestParam String loaiHang,
                              @RequestParam double minTemp, @RequestParam double maxTemp,
                              @RequestParam String diemDi, @RequestParam String diemDen,
                              @RequestParam String loaiGiayTo,
                              @RequestParam(value = "fileUpload", required = false) MultipartFile fileUpload,
                              @RequestParam(value = "tenFile", required = false) String tenFile,
                              HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        String fileName = "giay_phep_vanchuyen.pdf";
        if (fileUpload != null && !fileUpload.isEmpty()) {
            fileName = sanitizeFilename(fileUpload.getOriginalFilename(), "giay_phep_vanchuyen.pdf");
        } else if (tenFile != null && !tenFile.trim().isEmpty()) {
            fileName = sanitizeFilename(tenFile, "giay_phep_vanchuyen.pdf");
        }

        service.createOrder(tenHang, loaiHang, minTemp, maxTemp, diemDi, diemDen, loaiGiayTo, fileName, user.getEmail());
        return "redirect:/";
    }

    @PostMapping("/orders/approve-doc")
    public String approveDoc(@RequestParam String orderId, @RequestParam String docId, HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        service.approveDocument(orderId, docId);
        return "redirect:/";
    }

    @PostMapping("/orders/delete-doc")
    public String deleteDoc(@RequestParam String orderId, @RequestParam String docId, HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        service.deleteDocument(orderId, docId);
        return "redirect:/";
    }

    @PostMapping("/orders/reject")
    public String rejectOrder(@RequestParam String orderId, @RequestParam String reason, HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        service.rejectOrder(orderId, reason);
        return "redirect:/";
    }

    @PostMapping("/orders/resubmit")
    public String resubmitOrder(@RequestParam String orderId,
                                @RequestParam(value = "docId", required = false) String docId,
                                @RequestParam(value = "loaiGiayTo", required = false) String loaiGiayTo,
                                @RequestParam(value = "fileUpload", required = false) MultipartFile fileUpload,
                                @RequestParam(value = "newFileName", required = false) String newFileName,
                                HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        String fileName = "giay_phep_da_sua.pdf";
        if (fileUpload != null && !fileUpload.isEmpty()) {
            fileName = sanitizeFilename(fileUpload.getOriginalFilename(), "giay_phep_da_sua.pdf");
        } else if (newFileName != null && !newFileName.trim().isEmpty()) {
            fileName = sanitizeFilename(newFileName, "giay_phep_da_sua.pdf");
        }
        service.resubmitOrder(orderId, docId, loaiGiayTo, fileName);
        return "redirect:/";
    }

    @PostMapping("/orders/assign-driver")
    public String assignDriver(@RequestParam String orderId, @RequestParam String driverEmail, @RequestParam String bienSoXe, HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        service.assignDriver(orderId, driverEmail, bienSoXe);
        return "redirect:/";
    }

    @PostMapping("/orders/start-trip")
    public String startTrip(@RequestParam String orderId, HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        service.startTrip(orderId);
        return "redirect:/";
    }

    @PostMapping("/orders/toggle-checkpoint")
    public String toggleCheckpoint(@RequestParam String orderId, @RequestParam int tramId, HttpSession session, RedirectAttributes redirectAttributes) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        String res = service.passCheckpoint(orderId, tramId);
        if (!res.startsWith("SUCCESS")) {
            redirectAttributes.addFlashAttribute("checkpointWarning", res);
        }
        return "redirect:/";
    }

    @PostMapping("/orders/complete-delivery")
    public String completeDelivery(@RequestParam String orderId,
                                   @RequestParam(value = "podFile", required = false) MultipartFile podFile,
                                   @RequestParam(value = "podFileName", required = false) String podFileName,
                                   HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        String fileName = "bien_ban_giao_hang_pod.pdf";
        if (podFile != null && !podFile.isEmpty()) {
            fileName = sanitizeFilename(podFile.getOriginalFilename(), "bien_ban_giao_hang_pod.pdf");
        } else if (podFileName != null && !podFileName.trim().isEmpty()) {
            fileName = sanitizeFilename(podFileName, "bien_ban_giao_hang_pod.pdf");
        }
        String result = service.completeDelivery(orderId, fileName);
        if (result.startsWith("EX_DELIVERY")) {
            return "redirect:/orders/complete-delivery?blocked=true&orderId=" + orderId;
        }
        return "redirect:/orders/complete-delivery?success=true&orderId=" + orderId;
    }

    @GetMapping("/admin/users")
    public String manageUsers(HttpSession session, Model model) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user == null || !"QUAN_TRI_HE_THONG".equals(user.getVaiTro())) {
            return "redirect:/";
        }
        model.addAttribute("currentUser", user);
        model.addAttribute("users", service.getAllUsers());
        return "admin_users";
    }

    @PostMapping("/admin/users/update-role")
    public String updateRole(@RequestParam String userId, @RequestParam String newRole, HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("currentUser");
        if (user != null && "QUAN_TRI_HE_THONG".equals(user.getVaiTro())) {
            service.updateUserRole(userId, newRole);
        }
        return "redirect:/admin/users";
    }
}