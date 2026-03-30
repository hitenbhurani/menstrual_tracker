const tips = [
    { id: 1, title: "Stay Hydrated", description: "Drink at least 8 glasses of water today for better skin." },
    { id: 2, title: "Iron Rich Foods", description: "Spinach and beetroot help maintain hemoglobin levels." },
    { id: 3, title: "Track Symptoms", description: "Logging daily mood helps predict hormonal shifts." }
];

exports.getAllTips = (req, res) => {
    res.status(200).json({
        success: true,
        message: "Tips fetched successfully",
        data: tips
    });
};
