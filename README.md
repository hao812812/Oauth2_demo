# 介紹
 本專案 自建Web雙平台的會員系統，示範 OAuth2 / OpenID Connect 授權流程，包含 Authorization Server（身份提供者）與 Client（應用端）兩個部分，完整呈現 Authorization Code Flow（含 PKCE）在實務中的運作方式。
- 達成 client平台第三方登入、免登入(sso)功能。
- <img width="1913" height="580" alt="image" src="https://github.com/user-attachments/assets/9af774d4-55d4-4ec0-8996-a3e8e974f4c1" />

# 簡介
1. 理解 OIDC 各端點的行為：/authorize、/token、/userinfo、/jwks.json
2. 示範如何產生與驗證 JWT（ID Token）
3. 示範 client 如何處理 state、code_verifier、redirect、token 交換
4. 示範 session 與 access token 的儲存方式
5. 具備可實際執行的 OIDC Login Demo
# 功能特色
身份提供者（Provider / IDP）
- 支援 OAuth2 Authorization Code Flow
- 支援 PKCE（S256）
- 產生 Access Token、ID Token（RS256 JWT）
- JWKS 公鑰端點
- 儲存授權碼、使用者 session、access token
- 驗證 redirect_uri、client_id、scope
- 基本使用者資訊 API（UserInfo）

用戶端（Client）
- 建立 state 與 code_verifier（預防 CSRF）
- 導向 provider 授權
- 接收授權碼並交換 token
- 驗證 ID Token 簽章（從 JWKS 取得 RSA 公鑰）
- 顯示登入後的使用者資訊
# 技術
- 後端 : Java springboot
-  前端框架 : Angular
-  資料庫 : PostgreSQL
-  資安 : JWT 數位簽章、PKCE協定、非對稱加密、Salt密碼加密
-  網頁 : session 資料存取

# 系統架構
<img width="1774" height="645" alt="image" src="https://github.com/user-attachments/assets/a31a8118-5894-46b1-bdf8-e8ac30fe9cec" />

