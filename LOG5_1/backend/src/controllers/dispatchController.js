const { pool } = require('../config/db');

// Lấy danh sách phương tiện từ Database
exports.getVehicles = async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM phuong_tien ORDER BY ma_phuong_tien ASC');
    res.json({ success: true, data: result.rows });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// Lấy danh sách tài xế từ Database
exports.getDrivers = async (req, res) => {
  try {
    const query = `
      SELECT t.ma_nguoi_dung, u.ho_ten, t.so_giay_phep_lai, t.san_sang
      FROM tai_xe t
      JOIN nguoi_dung u ON t.ma_nguoi_dung = u.ma_nguoi_dung;
    `;
    const result = await pool.query(query);
    res.json({ success: true, data: result.rows });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// UC04: Phân công vận chuyển
exports.dispatchOrder = async (req, res) => {
  const { maDon, maDieuPhoi, maTaiXe, maPhuongTien, tuyenDuong, thoiGianDuKien } = req.body;
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const xeCheck = await client.query(`SELECT co_kiem_soat_nhiet_do, san_sang FROM phuong_tien WHERE ma_phuong_tien = $1`, [maPhuongTien]);
    if (xeCheck.rowCount === 0 || !xeCheck.rows[0].co_kiem_soat_nhiet_do) {
      throw new Error('Phương tiện chọn không đáp ứng tiêu chuẩn kiểm soát nhiệt độ thùng xe lạnh!');
    }

    const pcQuery = `
      INSERT INTO phan_cong (ma_don, ma_dieu_phoi, ma_tai_xe, ma_phuong_tien, tuyen_duong, thoi_gian_du_kien, trang_thai)
      VALUES ($1, $2, $3, $4, $5, $6, 'HIEU_LUC')
      RETURNING *;
    `;
    const pcResult = await client.query(pcQuery, [maDon, maDieuPhoi || 2, maTaiXe || 3, maPhuongTien, tuyenDuong, thoiGianDuKien]);
    await client.query(`UPDATE don_van_chuyen SET trang_thai = 'DA_PHAN_CONG' WHERE ma_don = $1;`, [maDon]);
    await client.query('COMMIT');
    res.status(201).json({ success: true, message: 'Phân công vận chuyển thành công vào Database (UC04)', data: pcResult.rows[0] });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(400).json({ success: false, message: error.message });
  } finally {
    client.release();
  }
};
