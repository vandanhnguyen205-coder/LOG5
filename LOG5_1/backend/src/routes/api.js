const express = require('express');
const router = express.Router();

const authCtrl = require('../controllers/authController');
const orderCtrl = require('../controllers/orderController');
const docCtrl = require('../controllers/documentController');
const dispatchCtrl = require('../controllers/dispatchController');
const telemetryCtrl = require('../controllers/telemetryController');
const deliveryCtrl = require('../controllers/deliveryController');
const catCtrl = require('../controllers/categoryController');
const reportCtrl = require('../controllers/reportController');
const { checkRole } = require('../middleware/authMiddleware');

router.post('/auth/login', authCtrl.login);

// Đơn hàng & Danh mục
router.get('/orders', orderCtrl.getOrders);
router.post('/orders', checkRole(['NGUOI_GUI_HANG', 'QUAN_TRI_VIEN']), orderCtrl.createOrder);
router.post('/orders/:id/cancel', checkRole(['NGUOI_GUI_HANG', 'DIEU_PHOI_VIEN', 'QUAN_TRI_VIEN']), orderCtrl.cancelOrder);
router.get('/categories', catCtrl.getCategories);

// Giấy tờ
router.get('/orders/:maDon/documents', docCtrl.getDocumentsByOrder);
router.post('/documents', checkRole(['NGUOI_GUI_HANG', 'QUAN_TRI_VIEN']), docCtrl.uploadDocument);
router.put('/documents/:id/review', checkRole(['KIEM_DUYET_VIEN', 'QUAN_TRI_VIEN']), docCtrl.reviewDocument);

// Phương tiện, tài xế & Phân công
router.get('/vehicles', dispatchCtrl.getVehicles);
router.get('/drivers', dispatchCtrl.getDrivers);
router.post('/dispatch', checkRole(['DIEU_PHOI_VIEN', 'QUAN_TRI_VIEN']), dispatchCtrl.dispatchOrder);

// Cảm biến telemetry & Gate-Check POD
router.post('/telemetry', checkRole(['TAI_XE', 'DIEU_PHOI_VIEN', 'QUAN_TRI_VIEN']), telemetryCtrl.recordTelemetry);
router.put('/incidents/:id/resolve', checkRole(['DIEU_PHOI_VIEN', 'QUAN_TRI_VIEN']), telemetryCtrl.resolveIncident);
router.get('/orders/:maDon/gate-check', deliveryCtrl.checkGateConditions);
router.post('/delivery/pod', checkRole(['TAI_XE', 'KIEM_DUYET_VIEN', 'QUAN_TRI_VIEN']), deliveryCtrl.createDeliveryPOD);

router.get('/reports/statistics', checkRole(['DIEU_PHOI_VIEN', 'QUAN_TRI_VIEN']), reportCtrl.getStatistics);

module.exports = router;
