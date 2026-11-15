package com.logistics.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "don_van_chuyen")
public class DonHang {

    @Id
    @Column(name = "ma_don")
    private String id;

    @Transient
    private String tenHang;

    @Transient
    private String loaiHang;

    @Transient
    private String diemDi;

    @Transient
    private String diemDen;

    @Transient
    private String trangThai;

    @Transient
    private double minTemp;

    @Transient
    private double maxTemp;

    @Transient
    private double nhietDoThucTe;

    @Transient
    private String nguoiTao;

    @Transient
    private String maTaiXe;

    @Transient
    private String taiXeEmail;

    @Transient
    private String taiXeTen;

    @Transient
    private String bienSoXe;

    @Transient
    private String lyDoBiChan;

    @Transient
    private String lyDoTuChoi;

    @Transient
    private String capNhatCuoi;

    @Transient
    private List<GiayToDinhKem> danhSachGiayTo = new ArrayList<>();

    @Transient
    private List<TramKiemTra> tuyenDuong = new ArrayList<>();

    public DonHang() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMaDon() { return id; }
    public void setMaDon(String maDon) { this.id = maDon; }

    public String getTenHang() { return tenHang != null ? tenHang : "Hàng Lạnh"; }
    public void setTenHang(String tenHang) { this.tenHang = tenHang; }

    public String getLoaiHang() { return loaiHang != null ? loaiHang : "Hàng Lạnh Đặc Thù"; }
    public void setLoaiHang(String loaiHang) { this.loaiHang = loaiHang; }

    public String getDiemDi() { return diemDi != null ? diemDi : "Kho Xuất"; }
    public void setDiemDi(String diemDi) { this.diemDi = diemDi; }

    public String getDiemDen() { return diemDen != null ? diemDen : "Kho Đích"; }
    public void setDiemDen(String diemDen) { this.diemDen = diemDen; }

    public String getTrangThai() { return trangThai != null ? trangThai : "Chờ xử lý"; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public double getMinTemp() { return minTemp; }
    public void setMinTemp(double minTemp) { this.minTemp = minTemp; }

    public double getMaxTemp() { return maxTemp; }
    public void setMaxTemp(double maxTemp) { this.maxTemp = maxTemp; }

    public double getNhietDoThucTe() { return nhietDoThucTe; }
    public void setNhietDoThucTe(double nhietDoThucTe) { this.nhietDoThucTe = nhietDoThucTe; }

    public String getNguoiTao() { return nguoiTao != null ? nguoiTao : "Người dùng"; }
    public void setNguoiTao(String nguoiTao) { this.nguoiTao = nguoiTao; }

    public String getMaTaiXe() { return maTaiXe; }
    public void setMaTaiXe(String maTaiXe) { this.maTaiXe = maTaiXe; }

    public String getTaiXeEmail() { return taiXeEmail; }
    public void setTaiXeEmail(String taiXeEmail) { this.taiXeEmail = taiXeEmail; }

    public String getTaiXeTen() { return taiXeTen != null ? taiXeTen : "Chưa phân công"; }
    public void setTaiXeTen(String taiXeTen) { this.taiXeTen = taiXeTen; }

    public String getBienSoXe() { return bienSoXe != null ? bienSoXe : "N/A"; }
    public void setBienSoXe(String bienSoXe) { this.bienSoXe = bienSoXe; }

    public String getLyDoBiChan() { return lyDoBiChan; }
    public void setLyDoBiChan(String lyDoBiChan) { this.lyDoBiChan = lyDoBiChan; }

    public String getLyDoTuChoi() { return lyDoTuChoi != null ? lyDoTuChoi : lyDoBiChan; }
    public void setLyDoTuChoi(String lyDoTuChoi) { this.lyDoTuChoi = lyDoTuChoi; }

    public String getCapNhatCuoi() { return capNhatCuoi != null ? capNhatCuoi : "Vừa xong"; }
    public void setCapNhatCuoi(String capNhatCuoi) { this.capNhatCuoi = capNhatCuoi; }

    public List<GiayToDinhKem> getDanhSachGiayTo() { return danhSachGiayTo != null ? danhSachGiayTo : new ArrayList<>(); }
    public void setDanhSachGiayTo(List<GiayToDinhKem> danhSachGiayTo) { this.danhSachGiayTo = danhSachGiayTo; }

    public List<TramKiemTra> getTuyenDuong() { return tuyenDuong != null ? tuyenDuong : new ArrayList<>(); }
    public void setTuyenDuong(List<TramKiemTra> tuyenDuong) { this.tuyenDuong = tuyenDuong; }

    public int getSoTramDaQua() {
        if (tuyenDuong == null) return 0;
        int count = 0;
        for (TramKiemTra t : tuyenDuong) {
            if (t.isDaQua()) count++;
        }
        return count;
    }

    public boolean isDuBaTram() {
        return getSoTramDaQua() >= 3;
    }
}