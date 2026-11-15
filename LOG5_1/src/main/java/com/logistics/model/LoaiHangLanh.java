package com.logistics.model;

public class LoaiHangLanh {
    private String maLoai;
    private String tenLoai;
    private String moTa;
    private double nhietDoToiThieu;
    private double nhietDoToiDa;
    private boolean trangThaiSuDung;

    public LoaiHangLanh() {}

    public String getMaLoai() { return maLoai; }
    public void setMaLoai(String maLoai) { this.maLoai = maLoai; }

    public String getTenLoai() { return tenLoai; }
    public void setTenLoai(String tenLoai) { this.tenLoai = tenLoai; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public double getNhietDoToiThieu() { return nhietDoToiThieu; }
    public void setNhietDoToiThieu(double nhietDoToiThieu) { this.nhietDoToiThieu = nhietDoToiThieu; }

    public double getNhietDoToiDa() { return nhietDoToiDa; }
    public void setNhietDoToiDa(double nhietDoToiDa) { this.nhietDoToiDa = nhietDoToiDa; }

    public boolean isTrangThaiSuDung() { return trangThaiSuDung; }
    public void setTrangThaiSuDung(boolean trangThaiSuDung) { this.trangThaiSuDung = trangThaiSuDung; }
}