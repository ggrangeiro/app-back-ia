-- Adiciona campos separados de créditos
ALTER TABLE usuario ADD COLUMN subscription_credits INT DEFAULT 0;
ALTER TABLE usuario ADD COLUMN purchased_credits INT DEFAULT 0;

-- Migrar créditos existentes para subscription_credits (opcional)
UPDATE usuario SET subscription_credits = IFNULL(credits, 0) WHERE subscription_credits = 0;
