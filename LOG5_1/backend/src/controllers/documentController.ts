import { Request, Response } from 'express';
import { pool } from '../config/db';

// UC02: Khai báo giấy tờ đính kèm đơn hàng
export const uploadDocument = async (req: Request, res: Response) => {
  const { maGiayTo, maDon, soHieu, ngayCap, ngayHetHan, tepDinhKem } = req.body;
  try {
    const query = `
      INSERT INTO GiayTo (MaGiayTo, MaDon, SoHieu, NgayCap, NgayHetHan, TepDinhKem, TrangThaiDuyet)
      VALUES ($1, $2, $3, $4, $5, $6, 'Cho duyet')
      RETURNING *;
    `;
    const result = await pool.query(query, [maGiayTo, maDon, soHieu, ngayCap, ngayHetHan, tepDinhKem]);
    
    // Chức năng phụ 1: Tự động cập nhật đơn hàng sang CHO_DUYET
    await pool.query(`UPDATE DonVanChuyen SET TrangThai = 'CHO_DUYET' WHERE MaDon = $1 AND TrangThai = 'CHO_KHAI_BAO'`, [maDon]);

    res.status(201).json({ success: true, message: 'Khai báo giấy tờ thành công (UC02)', data: result.rows[0] });
  } catch (error: any) {
    res.status(400).json({ success: false, message: error.message });
  }
};

// Lấy danh sách giấy tờ của một đơn hàng
export const getDocumentsByOrder = async (req: Request, res: Response) => {
  const { maDon } = req.params;
  try {
    const result = await pool.query(`
      SELECT g.*, u.HoTen AS TenKiemDuyetVien
      FROM GiayTo g
      LEFT JOIN NguoiDung u ON g.MaKiemDuyetVien = u.MaNguoiDung
      WHERE g.MaDon = $1
      ORDER BY g.MaGiayTo ASC;
    `, [maDon]);
    res.json({ success: true, data: result.rows });
  } catch (error: any) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// UC03: Thẩm định giấy tờ & Tự động đối soát đủ điều kiện phân công
export const reviewDocument = async (req: Request, res: Response) => {
  const { maGiayTo } = req.params;
  const { maKiemDuyetVien, trangThaiDuyet, lyDoTuChoi, maDon } = req.body;
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    // 1. Cập nhật trạng thái phê duyệt giấy tờ
    const updateDoc = await client.query(`
      UPDATE GiayTo
      SET MaKiemDuyetVien = $1, TrangThaiDuyet = $2, LyDoTuChoi = $3
      WHERE MaGiayTo = $4
      RETURNING *;
    `, [maKiemDuyetVien, trangThaiDuyet, lyDoTuChoi || null, maGiayTo]);

    // 2. Chức năng phụ 2: Tự động kiểm tra 100% giấy tờ bắt buộc của loại hàng đã Hop le chưa
    const checkQuery = `
      SELECT COUNT(*) AS missing_count
      FROM YeuCauGiayTo y
      JOIN DonVanChuyen d ON d.MaLoai = y.MaLoai
      LEFT JOIN GiayTo g ON g.MaDon = d.MaDon AND g.TrangThaiDuyet = 'Hop le'
      WHERE d.MaDon = $1 AND y.BatBuoc = TRUE AND g.MaGiayTo IS NULL;
    `;
    const checkRes = await client.query(checkQuery, [maDon]);
    const missingCount = parseInt(checkRes.rows[0].missing_count);

    let orderStatusUpdated = false;
    if (missingCount === 0) {
      // 100% chứng từ bắt buộc đạt chuẩn -> Tự động chuyển đơn hàng sang CHO_PHAN_CONG
      await client.query(`UPDATE DonVanChuyen SET TrangThai = 'CHO_PHAN_CONG' WHERE MaDon = $1`, [maDon]);
      orderStatusUpdated = true;
    }

    await client.query('COMMIT');

    res.json({
      success: true,
      message: trangThaiDuyet === 'Hop le' ? 'Đã duyệt chứng từ hợp lệ' : 'Đã từ chối chứng từ',
      orderStatusUpdated,
      newOrderStatus: orderStatusUpdated ? 'CHO_PHAN_CONG' : undefined,
      data: updateDoc.rows[0]
    });
  } catch (error: any) {
    await client.query('ROLLBACK');
    res.status(400).json({ success: false, message: error.message });
  } finally {
    client.release();
  }
};
