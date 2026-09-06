-- card.number is AES-GCM encrypted with a random IV per row, so it cannot be
-- searched or matched in SQL. last4 stores only the non-sensitive final four
-- digits, which is what the masked API representation already exposes, so
-- searching by it leaks nothing that responses do not already show.
-- Existing rows cannot be backfilled here (the ciphertext is only readable by
-- the application), hence the column is nullable.
ALTER TABLE card ADD COLUMN last4 VARCHAR(4);

CREATE INDEX idx_card_last4 ON card (last4);
