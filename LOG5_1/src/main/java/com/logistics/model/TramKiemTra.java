package com.logistics.model;

public class TramKiemTra {
    private int id;
    private String tenTram;
    private boolean daQua;
    private Double nhietDoXacNhan;
    private String thoiGianQua;

    public TramKiemTra() {}

    public TramKiemTra(int id, String tenTram, boolean daQua) {
        this.id = id;
        this.tenTram = tenTram;
        this.daQua = daQua;
    }

    public TramKiemTra(int id, String tenTram, boolean daQua, Double nhietDoXacNhan, String thoiGianQua) {
        this.id = id;
        this.tenTram = tenTram;
        this.daQua = daQua;
        this.nhietDoXacNhan = nhietDoXacNhan;
        this.thoiGianQua = thoiGianQua;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTenTram() { return tenTram; }
    public void setTenTram(String tenTram) { this.tenTram = tenTram; }

    public boolean isDaQua() { return daQua; }
    public boolean getDaQua() { return daQua; }
    public void setDaQua(boolean daQua) { this.daQua = daQua; }

    public Double getNhietDoXacNhan() { return nhietDoXacNhan; }
    public void setNhietDoXacNhan(Double nhietDoXacNhan) { this.nhietDoXacNhan = nhietDoXacNhan; }

    public String getThoiGianQua() { return thoiGianQua; }
    public void setThoiGianQua(String thoiGianQua) { this.thoiGianQua = thoiGianQua; }
}