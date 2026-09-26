function isValidStudentNumber(value) {
    return typeof value === 'string' && /^\d{9}$/.test(value);
}

function isValidName(value) {
    if (typeof value !== 'string') return false;
    const trimmed = value.trim();
    return trimmed.length >= 2 && trimmed.length <= 100;
}

function isValidPassword(value) {
    return typeof value === 'string' && value.length >= 8;
}

function isValidClaimCode(value) {
    if (typeof value !== 'string') return false;
    const trimmed = value.trim();
    return trimmed.length >= 4 && trimmed.length <= 32;
}

function isPositiveInt(value) {
    const num = Number(value);
    return Number.isInteger(num) && num > 0;
}

function isValidProgrammeId(value) {
    return isPositiveInt(value);
}

function isValidGroupId(value) {
    return isPositiveInt(value);
}

function isValidBaseVersion(value) {
    return isPositiveInt(value);
}

function isValidSearch(value) {
    return typeof value === 'string' && value.length <= 100;
}

module.exports = {
    isValidStudentNumber,
    isValidName,
    isValidPassword,
    isValidClaimCode,
    isPositiveInt,
    isValidProgrammeId,
    isValidGroupId,
    isValidBaseVersion,
    isValidSearch
};