const { pool } = require('../config/db');

// Lấy danh sách chứng từ theo mã đơn từ Database
exports.getDocumentsByOrder = async (req, res) => {
  const { maDon } = req.params;
  try {
    const query = `
      SELECT g.*, y.ten_giay_to, y.bat_buoc
      FROM giay_to g
      JOIN yeu_cau_giay_to y ON g.ma_yeu_cau = y.ma_yeu_cau
      WHERE g.ma_don = $1
      ORDER BY g.ma_giay_to DESC;
    `;
    const result = await pool.query(query, [maDon]);
    res.json({ success: true, data: result.rows });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// UC02: Khai báo chứng từ đính kèm
exports.uploadDocument = async (req, res) => {
  const { maDon, maYeuCau, soHieu, ngayCap, ngayHetHan, tepDinhKem } = req.body;
  try {
    const query = `
      INSERT INTO giay_to (ma_don, ma_yeu_cau, so_hieu, ngay_cap, ngay_het_han, tep_dinh_kem, trang_thai_duyet)
      VALUES ($1, $2, $3, $4, $5, $6, 'CHO_DUYET')
      RETURNING *;
    `;
    const result = await pool.query(query, [maDon, maYeuCau, soHieu, ngayCap, ngayHetHan, tepDinhKem]);
    await pool.query(`UPDATE don_van_chuyen SET trang_thai = 'CHO_DUYET' WHERE ma_don = $1 AND trang_thai = 'CHO_KHAI_BAO';`, [maDon]);
    res.status(201).json({ success: true, message: 'Khai báo chứng từ thành công vào Database (UC02)', data: result.rows[0] });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
};

// UC03: Thẩm định chứng từ & Tự động đối soát
exports.reviewDocument = async (req, res) => {
  const { id } = req.params;
  const { maKiemDuyet, trangThaiDuyet, lyDoTuChoi, maDon } = req.body;
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const updateDoc = await client.query(
      `UPDATE giay_to SET ma_kiem_duyet = $1, trang_thai_duyet = $2, ly_do_tu_choi = $3 WHERE ma_giay_to = $4 RETURNING *;`,
      [maKiemDuyet || 4, trangThaiDuyet, lyDoTuChoi || null, id]
    );

    if (trangThaiDuyet === 'KHONG_HOP_LE') {
      await client.query(`UPDATE don_van_chuyen SET trang_thai = 'DA_HUY', ly_do_huy = $1 WHERE ma_don = $2;`, [`Chứng từ bị từ chối: ${lyDoTuChoi}`, maDon]);
      await client.query('COMMIT');
      return res.json({ success: true, message: 'Chứng từ bị từ chối -> Đơn đã bị TỰ ĐỘNG HỦY trong Database', data: updateDoc.rows[0] });
    }

    const checkQuery = `
      SELECT COUNT(*) AS missing_count
      FROM yeu_cau_giay_to y
      JOIN don_van_chuyen d ON d.ma_loai = y.ma_loai
      LEFT JOIN giay_to g ON g.ma_don = d.ma_don AND g.ma_yeu_cau = y.ma_yeu_cau AND g.trang_thai_duyet = 'HOP_LE'
      WHERE d.ma_don = $1 AND y.bat_buoc = TRUE AND g.ma_giay_to IS NULL;
    `;
    const checkRes = await client.query(checkQuery, [maDon]);
    const missingCount = parseInt(checkRes.rows[0].missing_count);

    let statusUpdated = false;
    if (missingCount === 0) {
      await client.query(`UPDATE don_van_chuyen SET trang_thai = 'CHO_PHAN_CONG' WHERE ma_don = $1;`, [maDon]);
      statusUpdated = true;
    }

    await client.query('COMMIT');
    res.json({
      success: true,
      message: trangThaiDuyet === 'HOP_LE' ? 'Đã duyệt chứng từ HỢP LỆ trong Database' : 'Từ chối chứng từ',
      statusUpdated,
      newStatus: statusUpdated ? 'CHO_PHAN_CONG' : undefined,
      data: updateDoc.rows[0]
    });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(400).json({ success: false, message: error.message });
  } finally {
    client.release();
  }
};
