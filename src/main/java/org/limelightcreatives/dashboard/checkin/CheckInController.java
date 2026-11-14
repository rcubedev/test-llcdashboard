package org.limelightcreatives.dashboard.checkin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * {@code /staff/checkin}: scan a participant's QR code to check them in, and again to check them out.
 *
 * <p>The camera (static/js/checkin.js) posts each code to {@code /staff/checkin/scan} and shows the answer and
 * the last 10 scans itself. Typing an ID into the box posts the form, which works without JavaScript too.
 * (The old page's server action didn't check who was calling; these are for staff only, see SecurityConfig.)
 */
@Controller
@RequestMapping("/staff/checkin")
class CheckInController {

    /** What the camera sends. */
    record Scan(String code) {}

    private final CheckIns checkIns;

    CheckInController(CheckIns checkIns) {
        this.checkIns = checkIns;
    }

    @GetMapping
    String page() {
        return "staff/checkin";
    }

    @PostMapping
    String typed(@RequestParam(defaultValue = "") String code, RedirectAttributes redirect) {
        redirect.addFlashAttribute("result", this.checkIns.scan(code));
        return "redirect:/staff/checkin";
    }

    @PostMapping("/scan")
    @ResponseBody
    CheckInResult scanned(@RequestBody Scan scan) {
        return this.checkIns.scan(scan.code());
    }
}
