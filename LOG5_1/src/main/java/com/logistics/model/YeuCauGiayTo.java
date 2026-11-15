package com.logistics.model;

public class YeuCauGiayTo {
    private String maLoai;
    private String maYeuCau;
    private String tenGiayTo;
    private boolean batBuoc;
    private String moTa;

    public YeuCauGiayTo() {}

    public String getMaLoai() { return maLoai; }
    public void setMaLoai(String maLoai) { this.maLoai = maLoai; }

    public String getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(String maYeuCau) { this.maYeuCau = maYeuCau; }

    public String getTenGiayTo() { return tenGiayTo; }
    public void setTenGiayTo(String tenGiayTo) { this.tenGiayTo = tenGiayTo; }

    public boolean isBatBuoc() { return batBuoc; }
    public void setBatBuoc(boolean batBuoc) { this.batBuoc = batBuoc; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}