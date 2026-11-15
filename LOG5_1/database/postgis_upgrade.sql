CREATE EXTENSION IF NOT EXISTS postgis;

-- Thêm cột tọa độ địa lý PostGIS vào bảng thong_so_hanh_trinh
ALTER TABLE thong_so_hanh_trinh 
ADD COLUMN IF NOT EXISTS vi_tri_geom GEOGRAPHY(Point, 4326);

-- Trigger tự động chuyển vi_tri_lat, vi_tri_lng thành PostGIS Point
CREATE OR REPLACE FUNCTION update_postgis_geometry()
RETURNS TRIGGER AS $$
BEGIN
    NEW.vi_tri_geom = ST_SetSRID(ST_MakePoint(NEW.vi_tri_lng, NEW.vi_tri_lat), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_postgis_geom ON thong_so_hanh_trinh;
CREATE TRIGGER trg_update_postgis_geom
BEFORE INSERT OR UPDATE ON thong_so_hanh_trinh
FOR EACH ROW EXECUTE FUNCTION update_postgis_geometry();
