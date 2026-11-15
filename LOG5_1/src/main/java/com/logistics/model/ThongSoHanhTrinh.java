package com.logistics.model;

public class ThongSoHanhTrinh {
    private String maBanGhi;
    private String maDon;
    private String thoiDiem;
    private String viTriGps;
    private double nhietDoThungXe;

    public ThongSoHanhTrinh() {}

    public String getMaBanGhi() { return maBanGhi; }
    public void setMaBanGhi(String maBanGhi) { this.maBanGhi = maBanGhi; }

    public String getMaDon() { return maDon; }
    public void setMaDon(String maDon) { this.maDon = maDon; }

    public String getThoiDiem() { return thoiDiem; }
    public void setThoiDiem(String thoiDiem) { this.thoiDiem = thoiDiem; }

    public String getViTriGps() { return viTriGps; }
    public void setViTriGps(String viTriGps) { this.viTriGps = viTriGps; }

    public double getNhietDoThungXe() { return nhietDoThungXe; }
    public void setNhietDoThungXe(double nhietDoThungXe) { this.nhietDoThungXe = nhietDoThungXe; }
}