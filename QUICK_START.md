# Quick Start Guide - Google OAuth2

Follow these steps to get Google OAuth2 working quickly.

## 🚀 Quick Setup (5 minutes)

### 1. Copy the environment template
```bash
cp env.example .env
```

### 2. Get Google OAuth Credentials

1. Visit: https://console.cloud.google.com/apis/credentials
2. Create new OAuth 2.0 Client ID (or use existing)
3. **IMPORTANT:** Add this redirect URI:
   ```
   http://localhost:8080/api/v1/auth/callback/google
   ```
4. Copy your **Client ID** and **Client Secret**

### 3. Update your `.env` file

Open `.env` and update these essential variables:

```bash
# Google OAuth (REQUIRED)
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=your-client-secret-here
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=profile,email
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/auth/callback/google

# NOTE: Do NOT set provider endpoints - Spring Security auto-configures them for Google!

# Database (Update if different)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/playvora_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Secret (Generate a random string)
JWT_SECRET=your_super_secret_jwt_key_at_least_32_characters_long_random_string
```

### 4. Generate JWT Secret (Optional but recommended)
```bash
# On macOS/Linux:
openssl rand -base64 32

# Copy the output and paste as JWT_SECRET in .env
```

### 5. Start your application
```bash
./mvnw spring-boot:run
```

### 6. Test OAuth Flow

Open your browser:
```
http://localhost:8080/api/v1/auth/authorize/google
```

You should:
1. Be redirected to Google login
2. Login with your Google account
3. Get redirected back to `https://expitra.com` with cookies set

---

## ✅ Verification Checklist

- [ ] Created `.env` file from `env.example`
- [ ] Added Google Client ID to `.env`
- [ ] Added Google Client Secret to `.env`
- [ ] Added redirect URI `http://localhost:8080/api/v1/auth/callback/google` to Google Cloud Console
- [ ] Updated database credentials in `.env`
- [ ] Generated and added JWT secret to `.env`
- [ ] Application starts without errors
- [ ] Can access `http://localhost:8080/api/v1/auth/authorize/google`
- [ ] Successfully redirected to Google login
- [ ] Successfully redirected back after login
- [ ] Cookies are set (check browser DevTools)

---

## 🐛 Troubleshooting

### "No .env file found"
- Make sure you created `.env` in the project root
- Check that you're running the app from the correct directory

### "redirect_uri_mismatch"
- The redirect URI in `.env` must **exactly match** what's in Google Cloud Console
- Check for typos, trailing slashes, http vs https

### Application won't start
- Check database is running: `docker ps` or `pg_isready`
- Verify PostgreSQL credentials in `.env`
- Check logs: Look for the actual error message

### OAuth not triggering
- Enable debug logging (already set in `env.example`)
- Check logs for OAuth2 client registration
- Verify Spring Boot picked up your environment variables

### Still stuck?
See the detailed guide: `GOOGLE_OAUTH_SETUP.md`

---

## 🔍 Test Endpoints

### Check if OAuth is configured
```bash
curl http://localhost:8080/api/v1/auth/authorize/google
# Should return a redirect (302) to Google
```

### Check application health
```bash
curl http://localhost:8080/actuator/health
```

### View Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

## 📝 Current Configuration Summary

Your OAuth2 is configured with:

- **Authorization Endpoint:** `/api/v1/auth/authorize/{provider}`
  - Example: `/api/v1/auth/authorize/google`
  
- **Callback Endpoint:** `/api/v1/auth/callback/google` (handled by Spring Security)

- **Success:** Redirects to `https://expitra.com` with JWT cookies

- **Cookies Set:**
  - `access_token` (expires in 1 day)
  - `refresh_token` (expires in 7 days)

---

## 🎯 What's Next?

After successful OAuth setup:

1. **Test Protected Endpoints:**
   ```bash
   # Your cookies will be automatically sent
   curl -X GET http://localhost:8080/api/v1/user/profile \
     --cookie "access_token=YOUR_JWT_TOKEN"
   ```

2. **Integrate with Frontend:**
   - Frontend initiates login by redirecting to `/api/v1/auth/authorize/google`
   - After successful auth, user lands back on frontend with cookies
   - Frontend makes API calls with cookies automatically included

3. **Deploy to Production:**
   - Update `.env` with production URLs
   - Add production redirect URI to Google Cloud Console
   - Use HTTPS (cookies are marked as Secure)

---

## 📚 Additional Resources

- Full setup guide: `GOOGLE_OAUTH_SETUP.md`
- Environment variables reference: `env.example`
- API Documentation: http://localhost:8080/swagger-ui.html

---

Need help? Check the logs with debug enabled:
```bash
./mvnw spring-boot:run
# Logs will show OAuth2 flow details
```

