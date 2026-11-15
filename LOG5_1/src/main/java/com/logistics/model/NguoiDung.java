package com.logistics.model;

import jakarta.persistence.*;

@Entity
@Table(name = "nguoi_dung")
public class NguoiDung {

    @Id
    @Column(name = "ma_nguoi_dung")
    private String maNguoiDung;

    private String hoTen;
    private String email;
    private String soDienThoai;
    private String matKhauMaHoa;
    private String vaiTro;

    public NguoiDung() {}

    public NguoiDung(String maNguoiDung, String hoTen, String email, String soDienThoai, String matKhauMaHoa, String vaiTro) {
        this.maNguoiDung = maNguoiDung;
        this.hoTen = hoTen;
        this.email = email;
        this.soDienThoai = soDienThoai;
        this.matKhauMaHoa = matKhauMaHoa;
        this.vaiTro = vaiTro;
    }

    public String getMaNguoiDung() { return maNguoiDung; }
    public void setMaNguoiDung(String maNguoiDung) { this.maNguoiDung = maNguoiDung; }

    public String getId() { return maNguoiDung; }
    public void setId(String id) { this.maNguoiDung = id; }

    public String getHoTen() { return hoTen != null ? hoTen : "Người dùng"; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getSoDienThoai() { return soDienThoai != null ? soDienThoai : "0900000000"; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getMatKhauMaHoa() { return matKhauMaHoa; }
    public void setMatKhauMaHoa(String matKhauMaHoa) { this.matKhauMaHoa = matKhauMaHoa; }

    public String getVaiTro() { return vaiTro != null ? vaiTro : "NGUOI_GUI_HANG"; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }

    // Mã màu Hex cho style background-color trong dashboard.html
    public String getBg() {
        if (vaiTro == null) return "#64748b";
        switch (vaiTro) {
            case "QUAN_TRI_HE_THONG": return "#dc2626"; // Đỏ
            case "KIEM_DUYET_CHUNG_TU": return "#d97706"; // Vàng hổ phách
            case "DIEU_PHOI_VIEN": return "#0284c7"; // Xanh ngọc
            case "TAI_XE_VAN_CHUYEN": return "#16a34a"; // Xanh lá
            case "NGUOI_GUI_HANG": return "#2563eb"; // Xanh dương
            default: return "#64748b";
        }
    }

    // Nhãn hiển thị vai trò trong dashboard.html (currentUser.label)
    public String getLabel() {
        if (vaiTro == null) return "Người dùng";
        switch (vaiTro) {
            case "QUAN_TRI_HE_THONG": return "Quản trị viên";
            case "KIEM_DUYET_CHUNG_TU": return "Kiểm duyệt viên";
            case "DIEU_PHOI_VIEN": return "Điều phối viên";
            case "TAI_XE_VAN_CHUYEN": return "Tài xế";
            case "NGUOI_GUI_HANG": return "Người gửi hàng";
            default: return vaiTro;
        }
    }

    public String getVaiTroHienThi() {
        return getLabel();
    }
}