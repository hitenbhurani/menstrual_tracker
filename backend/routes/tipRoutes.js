const express = require('express');
const router = express.Router();
const tipController = require('../controllers/tipController');

router.get('/', tipController.getAllTips);

module.exports = router;
