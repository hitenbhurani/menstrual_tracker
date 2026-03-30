let requests = [];

exports.createRequest = (req, res) => {
    const { name, message } = req.body;
    const newRequest = {
        id: Date.now().toString(),
        name,
        message,
        status: "Pending",
        createdAt: new Date().toISOString()
    };
    requests.push(newRequest);
    res.status(201).json({
        success: true,
        message: "Consultation request sent successfully",
        data: newRequest
    });
};

exports.getAllRequests = (req, res) => {
    res.status(200).json({
        success: true,
        data: requests
    });
};
