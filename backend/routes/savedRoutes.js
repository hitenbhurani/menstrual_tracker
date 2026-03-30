const express = require('express');
const router = express.Router();
const savedController = require('../controllers/savedController');

router.post('/', savedController.savePost);
router.get('/', savedController.getSavedPosts);

module.exports = router;
