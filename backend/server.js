const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Routes Imports
const healthRoutes = require('./routes/healthRoutes');
const logRoutes = require('./routes/logRoutes');
const tipRoutes = require('./routes/tipRoutes');
const postRoutes = require('./routes/postRoutes');
const requestRoutes = require('./routes/requestRoutes');
const savedRoutes = require('./routes/savedRoutes');

// API Routes
app.use('/health', healthRoutes);
app.use('/api/logs', logRoutes);
app.use('/api/tips', tipRoutes);
app.use('/api/posts', postRoutes);
app.use('/api/requests', requestRoutes);
app.use('/api/saved', savedRoutes);

// Simple Welcome Route
app.get('/', (req, res) => {
    res.json({ message: "Welcome to FemCare API" });
});

app.listen(PORT, () => {
    console.log(`✅ FemCare Backend running on http://localhost:${PORT}`);
});
