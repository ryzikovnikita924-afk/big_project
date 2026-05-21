package com.example.config.resolver;

import com.example.dto.UserInfoHeaders;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.List;


@Slf4j
public class UserInfoArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_HEADER = "X-Forwarded-User";
    private static final String EMAIL_HEADER = "X-Forwarded-Email";
    private static final String PREFERRED_USERNAME_HEADER = "X-Forwarded-Preferred-Username";
    private static final String GROUPS_HEADER = "X-Forwarded-Groups";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UserInfoHeaders.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public UserInfoHeaders resolveArgument(@NonNull MethodParameter parameter,
                                           ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest,
                                           WebDataBinderFactory binderFactory) {

        var request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }

        String user = readHeader(request, USER_HEADER);
        String email = readHeader(request, EMAIL_HEADER);
        String preferredUsername = readHeader(request, PREFERRED_USERNAME_HEADER);
        List<String> groups = readGroups(request);

        boolean authenticated = user != null || email != null || preferredUsername != null || !groups.isEmpty();

        if (!authenticated) {
            return UserInfoHeaders.anonymous();
        }

        return new UserInfoHeaders(
                true,
                user,
                email,
                preferredUsername,
                groups
        );
    }

    private String readHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private List<String> readGroups(HttpServletRequest request) {
        String rawGroups = readHeader(request, GROUPS_HEADER);
        if (rawGroups == null) {
            return List.of();
        }

        return Arrays.stream(rawGroups.split(","))
                .map(String::trim)
                .filter(group -> !group.isEmpty())
                .toList();
    }
}
