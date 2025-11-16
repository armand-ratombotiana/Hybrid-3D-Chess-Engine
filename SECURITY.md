# Security Guidelines

## Authentication & Authorization

### JWT Token Management

**Token Lifecycle:**
- Access token: 15 minutes
- Refresh token: 7 days
- Algorithm: RS256 (RSA public/private key)

**Best Practices:**
- Store tokens in `httpOnly` cookies (not localStorage for production)
- Implement token refresh before expiration
- Revoke tokens on logout (blacklist in Redis)
- Rotate signing keys periodically

### Password Security

**Requirements:**
- Minimum 8 characters
- Mix of letters, numbers, symbols
- Not in common password list

**Storage:**
- BCrypt hashing (cost factor 12)
- Salt automatically generated per password
- Never log or transmit plaintext passwords

```java
// Example: Password hashing
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
boolean isValid = BCrypt.checkpw(plainPassword, hashedPassword);
```

## API Security

### Rate Limiting

**Limits:**
- Authentication: 5 requests/minute
- API endpoints: 100 requests/minute
- AI prediction: 60 requests/minute
- WebSocket connections: 10/minute

**Implementation:**
- Redis-backed sliding window
- Return `429 Too Many Requests`
- Include `Retry-After` header

### Input Validation

**Backend Validation (Quarkus):**
```java
@POST
@Path("/game/move")
public Response makeMove(@Valid MoveRequest request) {
    // @Valid triggers Bean Validation
    // @NotNull, @Pattern, @Size annotations
}
```

**Frontend Validation (React):**
```typescript
// Validate before sending
if (!isValidFEN(fen)) {
  throw new Error('Invalid FEN string');
}
```

### CORS Configuration

**Development:**
```properties
quarkus.http.cors.origins=http://localhost:3000
```

**Production:**
```properties
quarkus.http.cors.origins=https://chess.example.com
quarkus.http.cors.methods=GET,POST,PUT,DELETE
quarkus.http.cors.headers=authorization,content-type
quarkus.http.cors.credentials=true
```

## WebSocket Security

### Authentication

**Connection:**
```javascript
const ws = new WebSocket(`wss://api.chess.com/ws/game/${gameId}?token=${jwtToken}`);
```

**Server-side validation:**
```java
@OnOpen
public void onOpen(Session session, @PathParam("gameId") String gameId) {
    String token = session.getRequestParameterMap().get("token").get(0);
    validateJWT(token);
    authorizeGameAccess(token, gameId);
}
```

### Message Validation

**Always validate:**
- Message format (JSON schema)
- Sender authorization (can user move this piece?)
- Game state consistency (is move legal?)

## Database Security

### SQL Injection Prevention

**Use parameterized queries:**
```java
// GOOD
entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email")
    .setParameter("email", userInput)
    .getSingleResult();

// BAD - Never do this!
// entityManager.createQuery("SELECT u FROM User u WHERE u.email = '" + userInput + "'")
```

### Connection Security

**Encrypted connections:**
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/chess?ssl=true&sslmode=require
```

**Credentials:**
- Store in environment variables (not in code)
- Use secrets management (Vault, AWS Secrets Manager)
- Rotate credentials regularly

### Data Encryption

**At rest:**
- Enable PostgreSQL encryption (pgcrypto)
- Encrypt sensitive columns (email, payment info)

**In transit:**
- TLS 1.3 for all connections
- Certificate pinning for mobile apps

## AI Engine Security

### API Key Authentication

**Request header:**
```
X-API-Key: your-secret-key-here
```

**Server validation:**
```python
@app.post("/predict")
async def predict(request: PredictRequest, api_key: str = Header(None)):
    if api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")
```

### Training Endpoint Protection

**Admin-only access:**
```python
async def verify_admin_token(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401)

    token = authorization[7:]
    if token != ADMIN_TOKEN:
        raise HTTPException(status_code=403, detail="Admin access required")
```

### Model Artifact Security

**Storage:**
- Restrict file system permissions (owner read/write only)
- Use signed URLs for model downloads
- Checksum verification on load

**Versioning:**
- Immutable model versions
- Audit log for model deployments
- Rollback capability

## Frontend Security

### XSS Prevention

**React auto-escapes:**
```tsx
// Safe - React escapes by default
<div>{userInput}</div>

// Dangerous - avoid dangerouslySetInnerHTML
<div dangerouslySetInnerHTML={{__html: userInput}} />
```

**Content Security Policy:**
```html
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'">
```

### CSRF Protection

**SameSite cookies:**
```
Set-Cookie: session=abc123; SameSite=Strict; Secure; HttpOnly
```

**CSRF tokens for mutations:**
```typescript
axios.post('/api/game/create', data, {
  headers: { 'X-CSRF-Token': getCsrfToken() }
});
```

### Dependency Security

**Regular audits:**
```bash
npm audit
npm audit fix

# Or use Snyk
snyk test
```

**Automated updates:**
- Dependabot (GitHub)
- Renovate Bot
- Pin versions in package.json

## Desktop Client Security

### Local Storage

**Sensitive data:**
- Encrypt credentials before storing
- Use OS keychain (Windows Credential Manager, macOS Keychain)
- Clear on logout

### Update Mechanism

**Code signing:**
- Sign JAR with certificate
- Verify signature before update
- Use HTTPS for downloads

**Auto-updates:**
```java
if (newVersionAvailable()) {
    boolean userApproved = showUpdateDialog();
    if (userApproved) {
        downloadAndVerifyUpdate();
        installUpdate();
    }
}
```

## Infrastructure Security

### Docker Images

**Best practices:**
- Use official base images
- Scan for vulnerabilities (Trivy, Clair)
- Non-root user in containers
- Minimal images (Alpine, Distroless)

```dockerfile
# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

### Kubernetes Security

**Pod Security:**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  capabilities:
    drop: [ALL]
  readOnlyRootFilesystem: true
```

**Network Policies:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-policy
spec:
  podSelector:
    matchLabels:
      app: backend
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: frontend
```

### Secrets Management

**Kubernetes Secrets:**
```bash
kubectl create secret generic db-credentials \
  --from-literal=username=chess_user \
  --from-literal=password=securepassword
```

**Vault integration:**
```java
@ConfigProperty(name = "vault.db.password")
String dbPassword;
```

## Logging & Monitoring

### Secure Logging

**Never log:**
- Passwords (plaintext or hashed)
- API keys
- JWT tokens
- Credit card numbers
- Session IDs

**Example:**
```java
// BAD
logger.info("User login: " + username + ", password: " + password);

// GOOD
logger.info("User login attempt: username={}", username);
```

### Audit Trail

**Log security events:**
- Failed login attempts
- Permission changes
- Admin actions
- Unusual API patterns

**Format:**
```json
{
  "timestamp": "2025-01-16T10:30:45Z",
  "event": "LOGIN_FAILED",
  "user": "admin",
  "ip": "192.168.1.100",
  "reason": "invalid_password"
}
```

### Alerting

**Set up alerts for:**
- 5+ failed logins in 5 minutes
- Unusual AI prediction volume
- Database connection errors
- Unauthorized admin access attempts

## Penetration Testing

### Automated Scanning

**Tools:**
- OWASP ZAP (web app scanning)
- Burp Suite (API testing)
- Nmap (network scanning)
- Nuclei (vulnerability scanning)

**Schedule:**
- Weekly automated scans
- Manual pentests quarterly
- Bug bounty program (optional)

### Security Headers

**HTTP headers to set:**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: no-referrer
Permissions-Policy: geolocation=(), microphone=()
```

## Incident Response

### Response Plan

1. **Detection**: Monitoring alerts
2. **Containment**: Isolate affected systems
3. **Eradication**: Remove threat
4. **Recovery**: Restore services
5. **Lessons Learned**: Post-mortem

### Breach Notification

**Legal requirements:**
- GDPR: 72 hours
- CCPA: Without unreasonable delay
- Notify affected users
- Report to authorities

### Contact

**Security team:**
- Email: security@chess.example.com
- PGP key: [fingerprint]
- Bug bounty: HackerOne

## Compliance

### GDPR

**User rights:**
- Data access request
- Data deletion (right to be forgotten)
- Data portability
- Consent management

**Implementation:**
```java
@DELETE
@Path("/users/me")
public Response deleteAccount(@Context SecurityContext ctx) {
    User user = getCurrentUser(ctx);
    anonymizeUserData(user);
    deleteUserGames(user);
    return Response.noContent().build();
}
```

### CCPA

**Privacy Policy:**
- Clearly state data collection
- Allow opt-out of data sale
- Provide data deletion mechanism

## Security Checklist

### Pre-Deployment

- [ ] All dependencies up-to-date
- [ ] Secrets in environment variables
- [ ] HTTPS enforced
- [ ] Rate limiting enabled
- [ ] Input validation on all endpoints
- [ ] CORS properly configured
- [ ] Security headers set
- [ ] Database encrypted (at rest + transit)
- [ ] Logs don't contain sensitive data
- [ ] Error messages don't leak info
- [ ] Default credentials changed
- [ ] Unnecessary services disabled
- [ ] Firewall rules configured
- [ ] Backups encrypted
- [ ] Monitoring/alerting enabled

### Post-Deployment

- [ ] Penetration test completed
- [ ] Security audit passed
- [ ] Incident response plan documented
- [ ] Team trained on security practices
- [ ] Regular security updates scheduled

## Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

---

**Report security vulnerabilities to:** security@chess.example.com
