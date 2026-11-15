-- PostgreSQL Schema for LOG-05 Cold-Chain Transport Management System

DROP TABLE IF EXISTS KetQuaKiemTra CASCADE;
DROP TABLE IF EXISTS BienBanBanGiao CASCADE;
DROP TABLE IF EXISTS SuCo CASCADE;
DROP TABLE IF EXISTS ThongSoHanhTrinh CASCADE;
DROP TABLE IF EXISTS PhanCong CASCADE;
DROP TABLE IF EXISTS GiayTo CASCADE;
DROP TABLE IF EXISTS DonVanChuyen CASCADE;
DROP TABLE IF EXISTS YeuCauGiayTo CASCADE;
DROP TABLE IF EXISTS PhuongTien CASCADE;
DROP TABLE IF EXISTS LoaiHangLanh CASCADE;
DROP TABLE IF EXISTS NguoiDung CASCADE;

CREATE TABLE NguoiDung (
    MaNguoiDung  VARCHAR(20) PRIMARY KEY,
    HoTen        VARCHAR(120) NOT NULL,
    Email        VARCHAR(100) NOT NULL UNIQUE,
    SoDienThoai  VARCHAR(20),
    MatKhauMaHoa VARCHAR(255) NOT NULL,
    VaiTro       VARCHAR(50) NOT NULL CHECK (VaiTro IN ('NguoiGui', 'DieuPhoi', 'KiemDuyet', 'TaiXe', 'QuanTri'))
);

CREATE TABLE LoaiHangLanh (
    MaLoai          VARCHAR(20) PRIMARY KEY,
    TenLoai         VARCHAR(100) NOT NULL,
    MoTa            VARCHAR(250),
    NhietDoToiThieu DECIMAL(5,2) NOT NULL,
    NhietDoToiDa    DECIMAL(5,2) NOT NULL,
    TrangThaiSuDung BOOLEAN DEFAULT TRUE,
    CONSTRAINT CHK_DaiNhietDo CHECK (NhietDoToiThieu < NhietDoToiDa)
);

CREATE TABLE YeuCauGiayTo (
    MaLoai    VARCHAR(20) NOT NULL REFERENCES LoaiHangLanh(MaLoai) ON DELETE CASCADE,
    MaYeuCau  VARCHAR(20) NOT NULL,
    TenGiayTo VARCHAR(100) NOT NULL,
    BatBuoc   BOOLEAN DEFAULT TRUE,
    MoTa      VARCHAR(250),
    PRIMARY KEY (MaLoai, MaYeuCau)
);

CREATE TABLE DonVanChuyen (
    MaDon            VARCHAR(30) PRIMARY KEY,
    MaNguoiGui       VARCHAR(20) NOT NULL REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    MaLoai           VARCHAR(20) NOT NULL REFERENCES LoaiHangLanh(MaLoai) ON DELETE RESTRICT,
    DiemLay          VARCHAR(250) NOT NULL,
    DiemGiao         VARCHAR(250) NOT NULL,
    KhoiLuong        DECIMAL(10,2) NOT NULL CHECK (KhoiLuong > 0),
    ThoiGianMongMuon TIMESTAMP,
    NgayTao          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    TrangThai        VARCHAR(50) NOT NULL,
    LyDoHuy          VARCHAR(250)
);

CREATE TABLE GiayTo (
    MaGiayTo        VARCHAR(30) PRIMARY KEY,
    MaDon           VARCHAR(30) NOT NULL REFERENCES DonVanChuyen(MaDon) ON DELETE CASCADE,
    MaKiemDuyetVien VARCHAR(20) REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    SoHieu          VARCHAR(50),
    NgayCap         DATE,
    NgayHetHan      DATE,
    TepDinhKem      VARCHAR(255),
    TrangThaiDuyet  VARCHAR(50) DEFAULT 'Cho duyet',
    LyDoTuChoi      VARCHAR(250),
    CONSTRAINT CHK_NgayHetHan CHECK (NgayHetHan IS NULL OR NgayCap IS NULL OR NgayHetHan >= NgayCap)
);

CREATE TABLE PhuongTien (
    MaPhuongTien     VARCHAR(20) PRIMARY KEY,
    BienSo           VARCHAR(20) NOT NULL UNIQUE,
    LoaiPhuongTien   VARCHAR(50),
    CoHeThongLamLanh BOOLEAN DEFAULT TRUE CHECK (CoHeThongLamLanh = TRUE),
    TrangThaiSanSang BOOLEAN DEFAULT TRUE
);

CREATE TABLE PhanCong (
    MaPhanCong     VARCHAR(30) PRIMARY KEY,
    MaDon          VARCHAR(30) NOT NULL REFERENCES DonVanChuyen(MaDon) ON DELETE CASCADE,
    MaDieuPhoiVien VARCHAR(20) NOT NULL REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    MaTaiXe        VARCHAR(20) NOT NULL REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    MaPhuongTien   VARCHAR(20) NOT NULL REFERENCES PhuongTien(MaPhuongTien) ON DELETE RESTRICT,
    TuyenDuong     VARCHAR(250),
    ThoiGianDuKien TIMESTAMP,
    TrangThai      VARCHAR(50)
);

CREATE TABLE ThongSoHanhTrinh (
    MaBanGhi       VARCHAR(30) PRIMARY KEY,
    MaDon          VARCHAR(30) NOT NULL REFERENCES DonVanChuyen(MaDon) ON DELETE CASCADE,
    ThoiDiem       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ViTriGPS       VARCHAR(100),
    NhietDoThungXe DECIMAL(5,2) CHECK (NhietDoThungXe BETWEEN -50 AND 50)
);

CREATE TABLE SuCo (
    MaSuCo         VARCHAR(30) PRIMARY KEY,
    MaDon          VARCHAR(30) NOT NULL REFERENCES DonVanChuyen(MaDon) ON DELETE CASCADE,
    MaTaiXe        VARCHAR(20) REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    MaDieuPhoiVien VARCHAR(20) REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    LoaiSuCo       VARCHAR(100) NOT NULL,
    MoTa           VARCHAR(250),
    ThoiDiem       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    HinhAnh        VARCHAR(255),
    PhuongAnXuLy   VARCHAR(250),
    TrangThai      VARCHAR(50) DEFAULT 'Cho xu ly'
);

CREATE TABLE BienBanBanGiao (
    MaBienBan       VARCHAR(30) PRIMARY KEY,
    MaDon           VARCHAR(30) NOT NULL UNIQUE REFERENCES DonVanChuyen(MaDon) ON DELETE CASCADE,
    MaTaiXe         VARCHAR(20) NOT NULL REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    MaKiemDuyetVien VARCHAR(20) REFERENCES NguoiDung(MaNguoiDung) ON DELETE RESTRICT,
    NguoiNhan       VARCHAR(120) NOT NULL,
    TinhTrangHang   VARCHAR(250),
    NhietDoLucGiao  DECIMAL(5,2),
    ThoiDiemGiao    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KetQua          VARCHAR(50)
);

CREATE TABLE KetQuaKiemTra (
    MaDon               VARCHAR(30) NOT NULL REFERENCES DonVanChuyen(MaDon) ON DELETE CASCADE,
    MaBienBan           VARCHAR(30) NOT NULL REFERENCES BienBanBanGiao(MaBienBan) ON DELETE CASCADE,
    ThoiDiem            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KetQua              VARCHAR(50) NOT NULL,
    DanhSachMucChuaThoa VARCHAR(250),
    PRIMARY KEY (MaDon, MaBienBan)
);
