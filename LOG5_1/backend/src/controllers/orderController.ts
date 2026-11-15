import { Request, Response } from 'express';
import { pool } from '../config/db';

export const getOrders = async (req: Request, res: Response) => {
  try {
    const result = await pool.query(`
      SELECT d.*, l.TenLoai, u.HoTen AS TenNguoiGui 
      FROM DonVanChuyen d
      JOIN LoaiHangLanh l ON d.MaLoai = l.MaLoai
      JOIN NguoiDung u ON d.MaNguoiGui = u.MaNguoiDung
      ORDER BY d.NgayTao DESC
    `);
    res.json({ success: true, data: result.rows });
  } catch (error: any) {
    res.status(500).json({ success: false, message: error.message });
  }
};

export const createOrder = async (req: Request, res: Response) => {
  const { maDon, maNguoiGui, maLoai, diemLay, diemGiao, khoiLuong, thoiGianMongMuon } = req.body;
  try {
    const query = `
      INSERT INTO DonVanChuyen (MaDon, MaNguoiGui, MaLoai, DiemLay, DiemGiao, KhoiLuong, ThoiGianMongMuon, TrangThai)
      VALUES ($1, $2, $3, $4, $5, $6, $7, 'CHO_KHAI_BAO')
      RETURNING *;
    `;
    const values = [maDon, maNguoiGui, maLoai, diemLay, diemGiao, khoiLuong, thoiGianMongMuon];
    const result = await pool.query(query, values);
    res.status(201).json({ success: true, message: 'Tạo đơn thành công (UC01)', data: result.rows[0] });
  } catch (error: any) {
    res.status(400).json({ success: false, message: error.message });
  }
};

export const cancelOrder = async (req: Request, res: Response) => {
  const { id } = req.params;
  const { lyDoHuy } = req.body;
  const client = await pool.connect();

  try {
    await client.query('BEGIN');
    const updateOrder = await client.query(
      `UPDATE DonVanChuyen SET TrangThai = 'DA_HUY', LyDoHuy = $1 WHERE MaDon = $2 RETURNING *`,
      [lyDoHuy, id]
    );
    await client.query(`UPDATE PhanCong SET TrangThai = 'DA_HUY' WHERE MaDon = $1`, [id]);
    await client.query('COMMIT');

    res.json({ success: true, message: 'Đã hủy đơn và giải phóng tài nguyên (UC11)', data: updateOrder.rows[0] });
  } catch (error: any) {
    await client.query('ROLLBACK');
    res.status(400).json({ success: false, message: error.message });
  } finally {
    client.release();
  }
};
