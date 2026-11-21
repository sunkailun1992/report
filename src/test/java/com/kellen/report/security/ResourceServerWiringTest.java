package com.kellen.report.security;

import com.kellen.security.oauth2.test.FakeIssuer;
import com.kellen.security.SecurityUser;
import com.kellen.security.oauth2.FleetResourceServerAutoConfiguration;
import com.kellen.security.oauth2.SecurityContextResult;
import com.kellen.security.oauth2.SecurityContextRpcService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * report 经 utils 资源服务器接入 Keycloak：业务接口没带令牌 401，Keycloak 签给受众 github-api 的 RS256 用户令牌 200，
 * 受众不对 401。真实 Tomcat、真实资源服务器与 Boot 的 issuer-uri 解码器；签发方与 user 服务的业务上下文是本地替身。
 */
@SpringBootTest(classes = ResourceServerWiringTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.name=report-resource-server-isolated", "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false", "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS256",
                "spring.security.oauth2.resourceserver.jwt.audiences=github-api"})
class ResourceServerWiringTest {

    static final FakeIssuer ISSUER = FakeIssuer.start();

    @LocalServerPort
    int port;

    final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void issuer(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", ISSUER::issuer);
    }

    @AfterAll
    static void stop() {
        ISSUER.close();
    }

    @Test
    void businessEndpointWithoutTokenIs401() throws Exception {
        HttpResponse<String> response = get(null);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(challenge -> assertThat(challenge).startsWith("Bearer"));
    }

    @Test
    void businessEndpointWithValidTokenIs200() throws Exception {
        HttpResponse<String> response = get(ISSUER.token("user-a", "github-api", "github-gateway"));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("verified");
    }

    @Test
    void tokenForAnotherAudienceIs401() throws Exception {
        HttpResponse<String> response = get(ISSUER.token("user-a", "github-mcp", "github-gateway"));
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(challenge -> assertThat(challenge)
                .contains("error=\"invalid_token\""));
    }

    private HttpResponse<String> get(String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/report/probe")).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 测试上下文：真实 Spring Security、utils 资源服务器与 Boot 的 issuer-uri 解码器、本服务的真实控制器；
     * user 服务的业务上下文是替身（user-a 在 tenant-a）。
     */
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({TomcatServletWebServerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class, ConfigurationPropertiesAutoConfiguration.class,
            SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
            FleetResourceServerAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
    @Import(ResourceServerWiringTest.Probe.class)
    static class Config {

        @Bean
        SecurityContextRpcService securityContextRpcService() {
            return (userId, requestedTenantId) -> "user-a".equals(userId)
                    ? SecurityContextResult.ok(SecurityUser.builder().userId(userId).username(userId).tenantId("tenant-a")
                    .deptId("dept-a").dataScope("SELF").dataScopeDeptIds(List.of()).authorities(List.of("report:read"))
                    .enabled(true).tenantIds(List.of("tenant-a")).build())
                    : SecurityContextResult.of(SecurityContextResult.Status.USER_NOT_FOUND);
        }
    }

    /**
     * report 目前是骨架、没有业务接口；探针接口与业务接口走同一条资源服务器过滤链。
     */
    @org.springframework.web.bind.annotation.RestController
    static class Probe {

        @org.springframework.web.bind.annotation.GetMapping("/api/report/probe")
        String probe() {
            return "verified";
        }
    }
}
