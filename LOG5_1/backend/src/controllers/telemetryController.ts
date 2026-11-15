import { Request, Response } from 'express';
import { pool } from '../config/db';

export const recordTelemetry = async (req: Request, res: Response) => {
  const { maBanGhi, maDon, viTriGPS, nhietDoThungXe, maTaiXe } = req.body;
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    const bgQuery = `
      INSERT INTO ThongSoHanhTrinh (MaBanGhi, MaDon, ViTriGPS, NhietDoThungXe)
      VALUES ($1, $2, $3, $4) RETURNING *;
    `;
    const bgResult = await client.query(bgQuery, [maBanGhi, maDon, viTriGPS, nhietDoThungXe]);

    const catQuery = `
      SELECT l.NhietDoToiThieu, l.NhietDoToiDa 
      FROM DonVanChuyen d
      JOIN LoaiHangLanh l ON d.MaLoai = l.MaLoai
      WHERE d.MaDon = $1;
    `;
    const catResult = await client.query(catQuery, [maDon]);

    let isBreach = false;
    if (catResult.rowCount! > 0) {
      const { nhietdotoithieu, nhietdotoida } = catResult.rows[0];
      if (nhietDoThungXe < nhietdotoithieu || nhietDoThungXe > nhietdotoida) {
        isBreach = true;
        const suCoId = 'SC_' + Date.now();
        await client.query(`
          INSERT INTO SuCo (MaSuCo, MaDon, MaTaiXe, LoaiSuCo, MoTa, TrangThai)
          VALUES ($1, $2, $3, 'Vi phạm ngưỡng nhiệt độ', $4, 'Cho xu ly');
        `, [suCoId, maDon, maTaiXe, `Nhiệt độ ${nhietDoThungXe}°C nằm ngoài dải [${nhietdotoithieu}°C, ${nhietdotoida}°C]`]);

        await client.query(`UPDATE DonVanChuyen SET TrangThai = 'TAM_DUNG' WHERE MaDon = $1`, [maDon]);
      }
    }

    await client.query('COMMIT');
    res.json({
      success: true,
      isBreach,
      message: isBreach ? 'CẢNH BÁO: Vi phạm dải nhiệt! Đã tự động tạo sự cố.' : 'Nhiệt độ bình thường',
      data: bgResult.rows[0]
    });
  } catch (error: any) {
    await client.query('ROLLBACK');
    res.status(400).json({ success: false, message: error.message });
  } finally {
    client.release();
  }
};
