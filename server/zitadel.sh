#!/bin/bash
# setup-zitadel.sh

echo "=== Настройка Zitadel ==="

# Ждем запуска Zitadel
until curl -s http://localhost:8081/debug/healthz > /dev/null; do
  echo "Ожидание запуска Zitadel..."
  sleep 2
done

echo "Zitadel запущен!"

# Получаем токен администратора (нужно будет ввести пароль из логов)
echo "Введите токен администратора из логов Zitadel:"
read -s ZITADEL_TOKEN

# Создаем проект
curl -X POST http://localhost:8081/v2/projects \
  -H "Authorization: Bearer $ZITADEL_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GeoStrat Game",
    "projectRoleAssertion": true,
    "projectRoleCheck": true
  }'

# Создаем OAuth2 клиент
curl -X POST http://localhost:8081/v2/projects/YOUR_PROJECT_ID/apps/oidc \
  -H "Authorization: Bearer $ZITADEL_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "oauth2-proxy",
    "redirectUris": ["http://localhost:4180/oauth2/callback"],
    "postLogoutRedirectUris": ["http://localhost:4180"],
    "responseTypes": ["OIDC_RESPONSE_TYPE_CODE"],
    "grantTypes": ["OIDC_GRANT_TYPE_AUTHORIZATION_CODE"],
    "authMethodType": "OIDC_AUTH_METHOD_TYPE_BASIC"
  }'

echo "Готово! Скопируйте client_id и client_secret в .env файл"