package com.logistics.model;

public class GiayToDinhKem {
    private String id;
    private String loaiGiayTo;
    private String tenFile;
    private boolean approved;
    private String trangThaiDuyet;

    public GiayToDinhKem() {}

    public GiayToDinhKem(String id, String loaiGiayTo, String tenFile, boolean approved) {
        this.id = id;
        this.loaiGiayTo = loaiGiayTo;
        this.tenFile = tenFile;
        this.approved = approved;
        this.trangThaiDuyet = approved ? "Đã duyệt" : "Chờ duyệt";
    }

    public GiayToDinhKem(String id, String loaiGiayTo, String tenFile, boolean approved, String trangThaiDuyet) {
        this.id = id;
        this.loaiGiayTo = loaiGiayTo;
        this.tenFile = tenFile;
        this.approved = approved;
        this.trangThaiDuyet = trangThaiDuyet != null ? trangThaiDuyet : (approved ? "Đã duyệt" : "Chờ duyệt");
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLoaiGiayTo() { return loaiGiayTo; }
    public void setLoaiGiayTo(String loaiGiayTo) { this.loaiGiayTo = loaiGiayTo; }

    public String getTenFile() { return tenFile; }
    public void setTenFile(String tenFile) { this.tenFile = tenFile; }

    public boolean isApproved() { return approved; }
    public boolean getApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getTrangThaiDuyet() {
        return trangThaiDuyet != null ? trangThaiDuyet : (approved ? "Đã duyệt" : "Chờ duyệt");
    }
    public void setTrangThaiDuyet(String trangThaiDuyet) { this.trangThaiDuyet = trangThaiDuyet; }

    public boolean isRejected() {
        return "Từ chối".equalsIgnoreCase(trangThaiDuyet) || "Không hợp lệ".equalsIgnoreCase(trangThaiDuyet);
    }
    public boolean getRejected() { return isRejected(); }
}