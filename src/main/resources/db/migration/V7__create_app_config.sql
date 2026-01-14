-- Tabela para armazenar configurações da aplicação (como API Keys)
CREATE TABLE app_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    is_sensitive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_app_config_key (config_key)
);

-- Inserir a chave do Gemini inicialmente
INSERT INTO app_config (config_key, config_value, description, is_sensitive) VALUES
('GEMINI_API_KEY', 'AIzaSyAGfq7owXya1J7AxkdWk_N_uPE259cJZUM', 'Chave da API do Google Gemini para análises de IA', TRUE);
