let logs = []; // In-memory database array

exports.createLog = (req, res) => {
    const { title, description, severity } = req.body;
    const newLog = {
        id: Date.now().toString(),
        title,
        description,
        severity,
        createdAt: new Date().toISOString()
    };
    logs.push(newLog);
    res.status(201).json({
        success: true,
        message: "Symptom log created successfully",
        data: newLog
    });
};

exports.getAllLogs = (req, res) => {
    res.status(200).json({
        success: true,
        message: "Logs fetched successfully",
        count: logs.length,
        data: logs
    });
};

exports.getLogById = (req, res) => {
    const log = logs.find(l => l.id === req.params.id);
    if (!log) {
        return res.status(404).json({
            success: false,
            message: "Log not found"
        });
    }
    res.status(200).json({
        success: true,
        data: log
    });
};

exports.updateLog = (req, res) => {
    const index = logs.findIndex(l => l.id === req.params.id);
    if (index === -1) {
        return res.status(404).json({
            success: false,
            message: "Log not found"
        });
    }

    logs[index] = { ...logs[index], ...req.body, updatedAt: new Date().toISOString() };

    res.status(200).json({
        success: true,
        message: "Log updated successfully",
        data: logs[index]
    });
};

exports.deleteLog = (req, res) => {
    const initialLength = logs.length;
    logs = logs.filter(l => l.id !== req.params.id);

    if (logs.length === initialLength) {
        return res.status(404).json({
            success: false,
            message: "Log not found"
        });
    }

    res.status(200).json({
        success: true,
        message: "Log deleted successfully"
    });
};
