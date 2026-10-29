package br.pucpr.authserver;

import br.pucpr.authserver.config.AppProperties;
import br.pucpr.authserver.roles.RoleRepository;
import br.pucpr.authserver.security.ProblemAccessDeniedHandler;
import br.pucpr.authserver.users.UserRepository;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthserverApplicationTests {
    private ConfigurableApplicationContext context;
    private MockMvc mvc;
    private ObjectMapper mapper;
    private UserRepository users;
    private RoleRepository roles;
    private PasswordEncoder encoder;
    private Bootstrapper bootstrapper;
    private AppProperties properties;
    private TransactionTemplate transaction;
    private ProblemAccessDeniedHandler deniedHandler;
    private static final String PASSWORD = "Senha@123";

    @BeforeAll
    void startApplication() {
        // Spring 7's SpringExtension requires JUnit 6. Keep JUnit 5 and manage
        // the real Boot context explicitly, without replacing Spring Test jars.
        context = new SpringApplicationBuilder(AuthserverApplication.class)
                .profiles("test").run("--server.port=0", "--server.address=127.0.0.1");
        mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).apply(springSecurity()).build();
        mapper = context.getBean(ObjectMapper.class);
        users = context.getBean(UserRepository.class);
        roles = context.getBean(RoleRepository.class);
        encoder = context.getBean(PasswordEncoder.class);
        bootstrapper = context.getBean(Bootstrapper.class);
        properties = context.getBean(AppProperties.class);
        transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        deniedHandler = context.getBean(ProblemAccessDeniedHandler.class);
    }

    @AfterAll
    void stopApplication() {
        if (context != null) context.close();
    }

    @BeforeEach
    void cleanUsers() {
        transaction.executeWithoutResult(status -> users.findAll().stream()
                .filter(user -> !user.getEmail().equals(properties.admin().email())).forEach(users::delete));
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health").contextPath("/api"))
                .andExpect(status().isOk()).andExpect(content().json("{\"status\":\"OK\"}"));
    }

    @Test
    void registrationPersistsBcryptNormalizesAndReturnsOnlyPublicFields() throws Exception {
        JsonNode user = json(register("  ANA@EMAIL.COM  ", PASSWORD, " Ana ")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/users/\\d+")))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist()));
        var saved = users.findById(user.get("id").longValue()).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(user.size()).isEqualTo(3);
    }

    @Test
    void duplicateNormalizedEmailReturnsConflict() throws Exception {
        register("ana@email.com", PASSWORD, "Ana").andExpect(status().isCreated());
        register(" ANA@EMAIL.COM ", PASSWORD, "Outra")
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(users.findAll().stream().filter(u -> u.getEmail().equals("ana@email.com")).count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ab@1234", "12345678@", "Abcdefgh@", "Abcdefgh1", "Senha@123-", "Senha 123@", ""})
    void invalidPasswordReturns422(String password) throws Exception {
        register("ana@email.com", password, "Ana").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.password").isArray())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void bcryptLimitCountsUtf8Bytes() throws Exception {
        // 35 letters with two bytes + 1 digit + 1 symbol = 72 bytes; 36 letters = 74 bytes.
        register("utf8-ok@email.com", "é".repeat(35) + "1@", "Ana").andExpect(status().isCreated());
        register("utf8-bad@email.com", "é".repeat(36) + "1@", "Ana")
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors.password").isArray());
        register("ascii-ok@email.com", "a".repeat(70) + "1@", "Ana").andExpect(status().isCreated());
        register("ascii-bad@email.com", "a".repeat(71) + "1@", "Ana").andExpect(status().isUnprocessableContent());
    }

    @Test
    void missingFieldsAreValidationErrors() throws Exception {
        mvc.perform(post("/api/users").contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.password").isArray())
                .andExpect(jsonPath("$.errors.name").isArray());
    }

    @Test
    void invalidEmailAndBlankNameAreValidationErrors() throws Exception {
        register("not-an-email", PASSWORD, "  ").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email").isArray()).andExpect(jsonPath("$.errors.name").isArray());
    }

    @Test
    void malformedJsonIs400AndDoesNotExposeInput() throws Exception {
        mvc.perform(post("/api/users").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"private-value\","))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(content().string(not(containsString("private-value"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void loginReturnsSignedJwtWithRequiredClaimsAnd48HourLifetime() throws Exception {
        JsonNode created = json(register("ana@email.com", PASSWORD, "Ana").andExpect(status().isCreated()));
        JsonNode response = json(login(" ANA@EMAIL.COM ", PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.password").doesNotExist()).andExpect(jsonPath("$.user.passwordHash").doesNotExist()));
        var signed = Jwts.parser().verifyWith(key(properties.security().secret()))
                .requireIssuer("authserver").build().parseSignedClaims(response.get("token").asText());
        var claims = signed.getPayload();
        assertThat(signed.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo(created.get("id").asText());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant())).isEqualTo(Duration.ofHours(48));
        Map<?, ?> user = claims.get("user", Map.class);
        assertThat(((Number) user.get("id")).longValue()).isEqualTo(created.get("id").longValue());
        assertThat(user.get("name")).isEqualTo("Ana");
        assertThat(user.get("roles")).isEqualTo(List.of());
        assertThat(user.size()).isEqualTo(3);
    }

    @Test
    void adminTokenExpiresInOneHourAndContainsRole() throws Exception {
        JsonNode response = json(login(properties.admin().email(), properties.admin().password()).andExpect(status().isOk()));
        var claims = Jwts.parser().verifyWith(key(properties.security().secret())).build()
                .parseSignedClaims(response.get("token").asText()).getPayload();
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant())).isEqualTo(Duration.ofHours(1));
        assertThat(claims.get("user", Map.class).get("roles")).isEqualTo(List.of("ADMIN"));
    }

    @Test
    void nonexistentUserAndWrongPasswordReturnIdentical401() throws Exception {
        register("ana@email.com", PASSWORD, "Ana").andExpect(status().isCreated());
        String incorrect = login("ana@email.com", "Errada@123").andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andReturn().getResponse().getContentAsString();
        String missing = login("missing@email.com", "Errada@123").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(incorrect).isEqualTo(missing);
    }

    @Test
    void oversizedLoginPasswordIs401Not500() throws Exception {
        register("ana@email.com", PASSWORD, "Ana").andExpect(status().isCreated());
        login("ana@email.com", "x".repeat(73)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingLoginFieldsAre422() throws Exception {
        mvc.perform(post("/api/users/login").contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.password").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users", "/users/1", "/roles", "/unknown"})
    void endpointsAreProtectedByDefault(String path) throws Exception {
        mvc.perform(get("/api" + path).contextPath("/api")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.instance").value("/api" + path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalid", "Bearer ", "Basic abc"})
    void malformedAuthorizationIs401(String header) throws Exception {
        mvc.perform(get("/api/users").contextPath("/api").header("Authorization", header))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "issuer", "signature", "algorithm", "no-exp", "no-iat", "no-user"})
    void rejectsInvalidJwt(String defect) throws Exception {
        Instant now = Instant.now();
        var builder = Jwts.builder().issuer(defect.equals("issuer") ? "wrong" : "authserver").subject("1");
        if (!defect.equals("no-iat")) builder.issuedAt(Date.from(now.minusSeconds(100)));
        if (!defect.equals("no-exp")) builder.expiration(Date.from(defect.equals("expired") ? now.minusSeconds(1) : now.plusSeconds(100)));
        if (!defect.equals("no-user")) builder.claim("user", Map.of("id", 1, "name", "Ana", "roles", List.of()));
        var signingKey = key(defect.equals("signature") ? "wrong-signing-key-with-at-least-32-bytes" : properties.security().secret());
        String token = builder.signWith(signingKey, defect.equals("algorithm") ? Jwts.SIG.HS384 : Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/users").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validTokenAllowsListAndFindWithoutExposingPasswordsOrSession() throws Exception {
        JsonNode created = json(register("ana@email.com", PASSWORD, "Ana").andExpect(status().isCreated()));
        String token = json(login("ana@email.com", PASSWORD).andExpect(status().isOk())).get("token").asText();
        var result = mvc.perform(get("/api/users").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].password").isEmpty())
                .andExpect(jsonPath("$[*].passwordHash").isEmpty()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/users/" + created.get("id").longValue()).contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().json(created.toString()));
    }

    @Test
    void nonexistentUserReturns404() throws Exception {
        mvc.perform(get("/api/users/999999").contextPath("/api").header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void nonnumericIdReturns400() throws Exception {
        mvc.perform(get("/api/users/abc").contextPath("/api").header("Authorization", "Bearer " + token()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void nonpositiveIdReturns422(String id) throws Exception {
        mvc.perform(get("/api/users/" + id).contextPath("/api").header("Authorization", "Bearer " + token()))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors.id").isArray());
    }

    @Test
    void sortsNamesIgnoringCaseInBothDirectionsAndFiltersRole() throws Exception {
        var z = json(register("z@email.com", PASSWORD, "Zoe").andExpect(status().isCreated()));
        var a = json(register("a@email.com", PASSWORD, "alice").andExpect(status().isCreated()));
        var b = json(register("b@email.com", PASSWORD, "Bob").andExpect(status().isCreated()));
        transaction.executeWithoutResult(status -> {
            var premium = roles.findByName("PREMIUM").orElseThrow();
            for (JsonNode user : List.of(z, a, b)) users.findById(user.get("id").longValue()).orElseThrow().addRole(premium);
        });
        String token = token();
        mvc.perform(get("/api/users").contextPath("/api").param("role", "premium").param("sortDir", "asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].name").value(contains("alice", "Bob", "Zoe")));
        mvc.perform(get("/api/users").contextPath("/api").param("role", "PREMIUM").param("sortDir", "DESC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].name").value(contains("Zoe", "Bob", "alice")));
        mvc.perform(get("/api/users").contextPath("/api").param("role", "ADMIN").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value(properties.admin().email()));
        mvc.perform(get("/api/users").contextPath("/api").param("role", "UNKNOWN").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void invalidSortDirectionIs400() throws Exception {
        mvc.perform(get("/api/users").contextPath("/api").param("sortDir", "SIDEWAYS")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void bootstrapIsIdempotent() {
        long count = users.count();
        String hash = users.findByEmail(properties.admin().email()).orElseThrow().getPasswordHash();
        bootstrapper.run(new DefaultApplicationArguments());
        bootstrapper.run(new DefaultApplicationArguments());
        assertThat(users.count()).isEqualTo(count);
        assertThat(roles.count()).isEqualTo(2);
        transaction.executeWithoutResult(status -> {
            var admin = users.findByEmail(properties.admin().email()).orElseThrow();
            assertThat(admin.getPasswordHash()).isEqualTo(hash);
            assertThat(encoder.matches(properties.admin().password(), hash)).isTrue();
            assertThat(admin.getRoles()).extracting("name").containsExactly("ADMIN");
        });
    }

    @Test
    void accessDeniedHandlerUsesProblemDetail403() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/users");
        var response = new MockHttpServletResponse();
        deniedHandler.handle(request, response, new AccessDeniedException("internal-details"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(mapper.readTree(response.getContentAsString()).get("status").intValue()).isEqualTo(403);
        assertThat(response.getContentAsString()).doesNotContain("internal-details", "trace");
    }

    @Test
    void swaggerDocumentsExactlyFiveBusinessOperationsAndBearerAuth() throws Exception {
        JsonNode doc = json(mvc.perform(get("/api/v3/api-docs").contextPath("/api")).andExpect(status().isOk()));
        assertThat(doc.get("paths").size()).isEqualTo(4);
        assertThat(doc.at("/paths/~1users").size()).isEqualTo(2);
        assertThat(doc.at("/paths/~1users/get/security/0/bearerAuth").isArray()).isTrue();
        assertThat(doc.at("/paths/~1users/post/security").isMissingNode()).isTrue();
        assertThat(doc.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        mvc.perform(get("/api/swagger-ui/index.html").contextPath("/api")).andExpect(status().isOk());
    }

    @Test
    void secondStageEndpointsAreNotImplemented() throws Exception {
        String token = token();
        mvc.perform(delete("/api/users/1").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/roles").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private ResultActions register(String email, String password, String name) throws Exception {
        return mvc.perform(post("/api/users").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "password", password, "name", name))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/users/login").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String token() throws Exception {
        return json(login(properties.admin().email(), properties.admin().password()).andExpect(status().isOk())).get("token").asText();
    }

    private JsonNode json(ResultActions action) throws Exception {
        return mapper.readTree(action.andReturn().getResponse().getContentAsString());
    }

    private SecretKeySpec key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
