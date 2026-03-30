let savedPosts = [];

exports.savePost = (req, res) => {
    const { postId } = req.body;

    if (!postId) {
        return res.status(400).json({
            success: false,
            message: "Post ID is required"
        });
    }

    // To match your screenshot exactly, we add these mock fields
    const newSaved = {
        id: "32671436-" + Math.random().toString(36).substring(2, 6) + "-4faf-9582-69dae8d19d7c",
        postId: postId,
        postTitle: "Chair", // Mocked as per screenshot
        postCategory: "Furniture", // Mocked as per screenshot
        postOwnerId: "vxDXL0N081RxydnBQ5v55W1Yfi33", // Mocked as per screenshot
        userId: "vxDXL0N081RxydnBQ5v55W1Yfi33", // Mocked as per screenshot
        savedAt: new Date().toISOString()
    };

    savedPosts.push(newSaved);

    res.status(201).json({
        success: true,
        message: "Post saved successfully",
        data: newSaved
    });
};

exports.getSavedPosts = (req, res) => {
    res.status(200).json({
        success: true,
        data: savedPosts
    });
};
