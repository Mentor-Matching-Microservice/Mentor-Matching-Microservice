ALTER TABLE bookings ADD hold_expires_at DATETIME2(6) NULL;
CREATE INDEX idx_bookings_hold_expires_at ON bookings (hold_expires_at);
