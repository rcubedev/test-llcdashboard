package org.limelightcreatives.dashboard.event;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.limelightcreatives.dashboard.web.PageModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

/**
 * The pages under {@code /participant/{eventId}} (the old app/participant/[eventId]/layout.tsx): a controller
 * method takes an {@link EventConfig} parameter and gets the event from the address. An event that isn't
 * there (or isn't active) shows "This event doesn't exist", a locked one "This event isn't open yet", and the
 * method isn't called.
 *
 * <p>The event is also put in the model as {@code event}, for the dashboard's shell (event/shell.html).
 */
@Configuration
public class EventPages implements WebMvcConfigurer, HandlerMethodArgumentResolver {

    private final Events events;

    public EventPages(Events events) {
        this.events = events;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == EventConfig.class;
    }

    @Override
    public EventConfig resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mav,
                                       NativeWebRequest request, @Nullable WebDataBinderFactory binders) {
        String id = eventId(request);
        EventConfig event = this.events.active(id).orElseThrow(() -> new NotFound(id));
        if (event.locked()) throw new Locked(event);
        if (mav != null) mav.addAttribute("event", event);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static String eventId(NativeWebRequest request) {
        var variables = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        String id = variables == null ? null : variables.get("eventId");
        if (id == null) throw new IllegalStateException("An EventConfig parameter needs an {eventId} in the address");
        return id;
    }

    /** No active event with this id. */
    static final class NotFound extends RuntimeException {
        final String id;

        NotFound(String id) {
            super(null, null, false, false);
            this.id = id;
        }
    }

    /** The event is there but not open yet. */
    static final class Locked extends RuntimeException {
        final EventConfig event;

        Locked(EventConfig event) {
            super(null, null, false, false);
            this.event = event;
        }
    }

    /**
     * Shows the two cards instead of the page. (Exception handlers don't get PageModel's attributes, so these
     * add them.)
     */
    @ControllerAdvice
    static class Cards {

        private final PageModel page;

        Cards(PageModel page) {
            this.page = page;
        }

        @ExceptionHandler
        @ResponseStatus(HttpStatus.NOT_FOUND)
        String notFound(NotFound missing, Model model, HttpServletRequest request) {
            this.page.addTo(model, request);
            model.addAttribute("eventId", missing.id);
            return "event/not-found";
        }

        @ExceptionHandler
        String locked(Locked locked, Model model, HttpServletRequest request) {
            this.page.addTo(model, request);
            model.addAttribute("event", locked.event);
            return "event/locked";
        }
    }
}
