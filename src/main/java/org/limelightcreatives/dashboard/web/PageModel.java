package org.limelightcreatives.dashboard.web;

import jakarta.servlet.http.HttpServletRequest;
import org.limelightcreatives.dashboard.account.CurrentPerson;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Gives every page its {@link Page}, for every controller (error pages included). */
@ControllerAdvice
public class PageModel {

    private final Assets assets;

    public PageModel(Assets assets) {
        this.assets = assets;
    }

    @ModelAttribute("page")
    public Page page(HttpServletRequest request) {
        CsrfToken csrf = request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token ? token : null;
        if (csrf != null) csrf.getToken();
        return new Page(this.assets, csrf, CurrentPerson.get().orElse(null));
    }

    /** The same attribute, for an exception handler's page (Spring doesn't add it there). */
    public void addTo(Model model, HttpServletRequest request) {
        model.addAttribute("page", page(request));
    }
}
