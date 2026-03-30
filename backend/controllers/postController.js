let posts = [];

exports.createPost = (req, res) => {
    const { title, description, category, condition, location } = req.body;

    const now = new Date().toISOString();

    // This matches your Firebase screenshot exactly
    const newPost = {
        id: Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15),
        title: title || "Test Item",
        description: description || "Test Description",
        category: category || "Books",
        condition: condition || "good",
        images: [], // Array for image URLs
        isActive: true,
        latitude: location ? location.lat : null,
        longitude: location ? location.lng : null,
        postedAt: now,
        updatedAt: now,
        requestCount: 0
    };

    posts.push(newPost);

    res.status(201).json({
        success: true,
        message: "Post created successfully",
        data: newPost
    });
};

exports.getAllPosts = (req, res) => {
    res.status(200).json({
        success: true,
        message: "Posts fetched successfully",
        count: posts.length,
        data: posts
    });
};

exports.getPostById = (req, res) => {
    const post = posts.find(p => p.id === req.params.id);
    if (!post) return res.status(404).json({ success: false, message: "Post not found" });
    res.status(200).json({ success: true, data: post });
};

exports.updatePost = (req, res) => {
    const index = posts.findIndex(p => p.id === req.params.id);
    if (index === -1) return res.status(404).json({ success: false, message: "Post not found" });

    posts[index] = { ...posts[index], ...req.body, updatedAt: new Date().toISOString() };
    res.status(200).json({ success: true, message: "Post updated successfully", data: posts[index] });
};

exports.deletePost = (req, res) => {
    const initialLength = posts.length;
    posts = posts.filter(p => p.id !== req.params.id);
    if (posts.length === initialLength) {
        return res.status(404).json({ success: false, message: "Post not found" });
    }
    res.status(200).json({ success: true, message: "Post deleted successfully" });
};
