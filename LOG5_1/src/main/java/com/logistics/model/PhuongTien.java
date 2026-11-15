package com.logistics.model;

public class PhuongTien {
    private String maPhuongTien;
    private String bienSo;
    private String loaiPhuongTien;
    private boolean coHeThongLamLanh;
    private boolean trangThaiSanSang;

    public PhuongTien() {}

    public String getMaPhuongTien() { return maPhuongTien; }
    public void setMaPhuongTien(String maPhuongTien) { this.maPhuongTien = maPhuongTien; }

    public String getBienSo() { return bienSo; }
    public void setBienSo(String bienSo) { this.bienSo = bienSo; }

    public String getLoaiPhuongTien() { return loaiPhuongTien; }
    public void setLoaiPhuongTien(String loaiPhuongTien) { this.loaiPhuongTien = loaiPhuongTien; }

    public boolean isCoHeThongLamLanh() { return coHeThongLamLanh; }
    public void setCoHeThongLamLanh(boolean coHeThongLamLanh) { this.coHeThongLamLanh = coHeThongLamLanh; }

    public boolean isTrangThaiSanSang() { return trangThaiSanSang; }
    public void setTrangThaiSanSang(boolean trangThaiSanSang) { this.trangThaiSanSang = trangThaiSanSang; }
}