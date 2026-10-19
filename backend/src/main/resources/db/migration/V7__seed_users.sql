-- V7: Seed default users for development and testing.
-- Passwords are BCrypt-hashed (strength 12) as required by AppProperties.security().bcryptStrength().
--
-- Credentials:
--   admin@test.com    / password   (role: ADMIN)
--   reviewer@test.com / password   (role: REVIEWER)
--
-- Hash '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' is BCrypt(12) of "password"
-- Verified: BCryptPasswordEncoder(12).matches("password", hash) == true
--
-- ⚠️  DO NOT use these accounts in production.

INSERT INTO app_user (email, password_hash, full_name, role, active)
VALUES
    (
        'admin@test.com',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'Admin User',
        'ADMIN',
        TRUE
    ),
    (
        'reviewer@test.com',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'Reviewer User',
        'REVIEWER',
        TRUE
    )
ON CONFLICT (email) DO NOTHING;

