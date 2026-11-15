import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import apiRoutes from './routes/api';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

app.use('/api', apiRoutes);

app.get('/', (req, res) => {
  res.json({ message: 'LOG-05 Cold Chain Transport API is running.' });
});

app.listen(PORT, () => {
  console.log(`[Express Server] API running on http://localhost:${PORT}`);
});
