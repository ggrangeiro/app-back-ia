-- Adiciona campos de assinatura na tabela usuario
ALTER TABLE usuario ADD COLUMN plan_type VARCHAR(20) DEFAULT 'FREE';
ALTER TABLE usuario ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'INACTIVE';
ALTER TABLE usuario ADD COLUMN subscription_end_date DATETIME NULL;
ALTER TABLE usuario ADD COLUMN credits_reset_date DATETIME NULL;
ALTER TABLE usuario ADD COLUMN generations_used_cycle INT DEFAULT 0;
