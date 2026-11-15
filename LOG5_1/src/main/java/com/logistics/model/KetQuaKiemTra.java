package com.logistics.model;

public class KetQuaKiemTra {
    private String maDon;
    private String maBienBan;
    private String thoiDiem;
    private String ketQua;
    private String danhSachMucChuaThoa;

    public KetQuaKiemTra() {}

    public String getMaDon() { return maDon; }
    public void setMaDon(String maDon) { this.maDon = maDon; }

    public String getMaBienBan() { return maBienBan; }
    public void setMaBienBan(String maBienBan) { this.maBienBan = maBienBan; }

    public String getThoiDiem() { return thoiDiem; }
    public void setThoiDiem(String thoiDiem) { this.thoiDiem = thoiDiem; }

    public String getKetQua() { return ketQua; }
    public void setKetQua(String ketQua) { this.ketQua = ketQua; }

    public String getDanhSachMucChuaThoa() { return danhSachMucChuaThoa; }
    public void setDanhSachMucChuaThoa(String danhSachMucChuaThoa) { this.danhSachMucChuaThoa = danhSachMucChuaThoa; }
}