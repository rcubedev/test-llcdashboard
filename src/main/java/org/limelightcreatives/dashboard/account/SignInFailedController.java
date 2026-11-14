package org.limelightcreatives.dashboard.account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Where Spring sends people when signing in through auth didn't work (see SecurityConfig). */
@Controller
class SignInFailedController {

    @GetMapping("/sign-in-failed")
    String signInFailed() {
        return "sign-in-failed";
    }
}
