-- Migração para adicionar campos de personalização do Expert
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS methodology TEXT;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS communication_style TEXT;
