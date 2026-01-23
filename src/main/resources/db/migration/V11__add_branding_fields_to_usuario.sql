-- Migração para adicionar campos de avatar e logo de marca (White Label)
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS avatar VARCHAR(255);
ALTER TABLE usuario ADD COLUMN brand_logo VARCHAR(255);
