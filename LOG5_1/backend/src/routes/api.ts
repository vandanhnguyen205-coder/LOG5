import { Router } from 'express';
import { getOrders, createOrder, cancelOrder } from '../controllers/orderController';
import { recordTelemetry } from '../controllers/telemetryController';
import { uploadDocument, getDocumentsByOrder, reviewDocument } from '../controllers/documentController';

const router = Router();

// Order Routes (UC01, UC11)
router.get('/orders', getOrders);
router.post('/orders', createOrder);
router.post('/orders/:id/cancel', cancelOrder);

// Telemetry Routes (UC05, UC06)
router.post('/telemetry', recordTelemetry);

// Document Routes (UC02, UC03)
router.post('/documents', uploadDocument);
router.get('/orders/:maDon/documents', getDocumentsByOrder);
router.put('/documents/:maGiayTo/review', reviewDocument);

export default router;
