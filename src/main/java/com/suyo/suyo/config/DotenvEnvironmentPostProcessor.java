package com.suyo.suyo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 프로젝트 루트의 .env 파일을 읽어 환경변수로 등록한다.
 * spring-dotenv 라이브러리가 Spring Boot 4의 부트스트랩 방식과 맞지 않아 조용히 무시되는 문제가 있어 직접 구현했다.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DOTENV_FILE_NAME = ".env";
    private static final String PROPERTY_SOURCE_NAME = "dotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenvPath = Path.of(DOTENV_FILE_NAME);
        if (!Files.exists(dotenvPath)) {
            return;
        }

        Map<String, Object> values = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException(".env 파일을 읽지 못했습니다.", e);
        }

        if (!values.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }
}
