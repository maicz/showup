package com.showup.api.config;

import com.showup.api.security.CurrentMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Registers {@code @CurrentMember} so controllers can take the acting member id as a parameter. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentMemberArgumentResolver currentMember;

    WebConfig(CurrentMemberArgumentResolver currentMember) {
        this.currentMember = currentMember;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMember);
    }
}
