package org.limelightcreatives.dashboard.account;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

/** Lets a controller method take a {@link Person} parameter: whoever is signed in. */
@Configuration
public class CurrentPerson implements WebMvcConfigurer, HandlerMethodArgumentResolver {

    /** Whoever is signed in, if anyone is (e.g. not on an error page shown before sign-in). */
    public static Optional<Person> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            return Optional.of(Person.of(user));
        }
        return Optional.empty();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == Person.class;
    }

    @Override
    public Person resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mav,
                                  NativeWebRequest request, @Nullable WebDataBinderFactory binders) {
        // every page but the public ones needs a sign-in (SecurityConfig), so there is always someone here
        return get().orElseThrow(() -> new IllegalStateException("Nobody is signed in"));
    }
}
