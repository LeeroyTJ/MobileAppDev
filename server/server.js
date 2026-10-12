require('dotenv').config();
const express = require('express');
const app = express();
app.use(express.json());

const authRoutes = require('./routes/auth.routes');
const studentRoutes = require('./routes/student.routes');
const groupRoutes = require('./routes/group.routes');
const syncRoutes = require('./routes/sync.routes');

app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/students', studentRoutes);
app.use('/api/v1/groups', groupRoutes);
app.use('/api/v1/sync', syncRoutes);

module.exports = app;

if (require.main === module) {
    const PORT = process.env.PORT || 3000;
    app.listen(PORT, () => {
        console.log(`CohortHub backend running on port ${PORT}`);
    });
}
