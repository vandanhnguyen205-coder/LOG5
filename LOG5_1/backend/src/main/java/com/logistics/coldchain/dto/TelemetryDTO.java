package com.logistics.coldchain.dto;

import java.io.Serializable;

public class TelemetryDTO implements Serializable {
    private Integer maDon;
    private Double viTriLat;
    private Double viTriLng;
    private Double nhietDo;
    private Integer maTaiXe;

    public Integer getMaDon() { return maDon; }
    public void setMaDon(Integer maDon) { this.maDon = maDon; }
    public Double getViTriLat() { return viTriLat; }
    public void setViTriLat(Double viTriLat) { this.viTriLat = viTriLat; }
    public Double getViTriLng() { return viTriLng; }
    public void setViTriLng(Double viTriLng) { this.viTriLng = viTriLng; }
    public Double getNhietDo() { return nhietDo; }
    public void setNhietDo(Double nhietDo) { this.nhietDo = nhietDo; }
    public Integer getMaTaiXe() { return maTaiXe; }
    public void setMaTaiXe(Integer maTaiXe) { this.maTaiXe = maTaiXe; }
}
