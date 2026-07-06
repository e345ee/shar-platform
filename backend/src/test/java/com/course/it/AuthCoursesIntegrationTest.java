package com.course.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthCoursesIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("course_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", postgres::getHost);
        registry.add("DB_PORT", () -> String.valueOf(postgres.getMappedPort(5432)));
        registry.add("DB_NAME", postgres::getDatabaseName);
        registry.add("DB_SSLMODE", () -> "disable");

        registry.add("SPRING_DATASOURCE_USERNAME", postgres::getUsername);
        registry.add("SPRING_DATASOURCE_PASSWORD", postgres::getPassword);


        registry.add("SERVER_PORT", () -> "0");
    }

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper objectMapper;

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<String> resp = http.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(resp.getBody());
        assertThat(json.path("accessToken").asText()).isNotBlank();
        return json.path("accessToken").asText();
    }

    @Test
    void health_isUp() {
        ResponseEntity<String> resp = http.getForEntity("/actuator/health", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("UP");
    }

    @Test
    void login_works_forSeedUser() throws Exception {
        String token = loginAndGetAccessToken("methodist_anna", "pass");
        assertThat(token).isNotBlank();
    }

    @Test
    void createCourse_requiresAuth_andRole() throws Exception {

        ResponseEntity<String> unauth = http.postForEntity(
                "/api/courses",
                new HttpEntity<>("{\"name\":\"X\"}", jsonHeaders()),
                String.class
        );
        assertThat(unauth.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);


        String teacherToken = loginAndGetAccessToken("teacher_alex", "pass");
        ResponseEntity<String> forbidden = http.exchange(
                "/api/courses",
                HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"X\"}", bearerJsonHeaders(teacherToken)),
                String.class
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);


        String methodistToken = loginAndGetAccessToken("methodist_anna", "pass");
        ResponseEntity<String> created = http.exchange(
                "/api/courses",
                HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"Новый курс\",\"description\":\"Описание\"}", bearerJsonHeaders(methodistToken)),
                String.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode json = objectMapper.readTree(created.getBody());
        assertThat(json.path("id").asInt()).isPositive();
        assertThat(json.path("name").asText()).isEqualTo("Новый курс");
    }

    @Test
    void createCourse_validatesDto() throws Exception {
        String methodistToken = loginAndGetAccessToken("methodist_anna", "pass");

        ResponseEntity<String> bad = http.exchange(
                "/api/courses",
                HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"\"}", bearerJsonHeaders(methodistToken)),
                String.class
        );

        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody()).contains("Ошибка валидации");
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders bearerJsonHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
