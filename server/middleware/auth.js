const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
    throw new Error('JWT_SECRET is not set. Check your .env file.');
}

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) {
        return res.status(401).json({
            success: false,
            error: { code: 'UNAUTHORIZED', message: 'Token missing' }
        });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({
                success: false,
                error: { code: 'FORBIDDEN', message: 'Invalid token' }
            });
        }
        req.user = user;
        next();
    });
}

function verifyRole(role) {
    return (req, res, next) => {
        if (!req.user || req.user.role !== role) {
            return res.status(403).json({
                success: false,
                error: { code: 'FORBIDDEN', message: `${role} access required` }
            });
        }
        next();
    };
}

module.exports = {
    authenticateToken,
    verifyRole
};
