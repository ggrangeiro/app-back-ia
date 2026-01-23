-- Tabela para registrar transações de pagamento do MercadoPago
CREATE TABLE payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id VARCHAR(20),
    credits_amount INT,
    payment_type VARCHAR(20) NOT NULL COMMENT 'SUBSCRIPTION, CREDITS',
    mp_payment_id VARCHAR(100),
    mp_preference_id VARCHAR(100),
    external_reference VARCHAR(200),
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, APPROVED, REJECTED, CANCELLED',
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES usuario(id),
    INDEX idx_mp_payment_id (mp_payment_id),
    INDEX idx_user_status (user_id, status)
);
