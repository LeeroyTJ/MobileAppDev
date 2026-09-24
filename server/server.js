const express = require('express');
const app = express();

app.use(express.json());

// Register routes
const authRoutes = require('./routes/auth.routes');
const studentRoutes = require('./routes/student.routes');

app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/students', studentRoutes);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`CohortHub backend running on port ${PORT}`);
});