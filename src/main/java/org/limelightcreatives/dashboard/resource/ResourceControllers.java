package org.limelightcreatives.dashboard.resource;

import org.limelightcreatives.dashboard.event.EventConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** The Resources pages: what participants read, and where staff write it. */
public class ResourceControllers {

    /** A category tab, with how many resources it has. */
    public record Category(String name, int count) {}

    static final String ALL = "All";

    /** An event dashboard's Resources tab: published guides and tutorials, by category, with a search. */
    @Controller
    static class Participant {

        private final Resources resources;

        Participant(Resources resources) {
            this.resources = resources;
        }

        @GetMapping("/participant/{eventId}/resources")
        String resources(EventConfig event, @RequestParam(defaultValue = ALL) String category,
                         @RequestParam(defaultValue = "") String q, Model model) {
            List<Resource> rows = this.resources.forParticipants();
            Map<String, Integer> counts = new TreeMap<>();
            rows.forEach(r -> counts.merge(r.category(), 1, Integer::sum));
            List<Category> categories = new ArrayList<>();
            categories.add(new Category(ALL, rows.size()));
            counts.forEach((name, count) -> categories.add(new Category(name, count)));

            String query = q.strip().toLowerCase(Locale.ROOT);
            List<Resource> visible = rows.stream()
                    .filter(r -> category.equals(ALL) || r.category().equals(category))
                    .filter(r -> query.isEmpty() || r.title().toLowerCase(Locale.ROOT).contains(query)
                            || r.summary().toLowerCase(Locale.ROOT).contains(query)
                            || r.body().toLowerCase(Locale.ROOT).contains(query))
                    .toList();

            model.addAttribute("total", rows.size());
            // only worth tabs when there's more than one category
            model.addAttribute("categories", categories.size() > 2 ? categories : List.of());
            model.addAttribute("category", category);
            model.addAttribute("q", q);
            model.addAttribute("visible", visible);
            return "event/resources";
        }
    }

    /** {@code /staff/resources}: every resource, drafts too, and the editor. */
    @Controller
    @RequestMapping("/staff/resources")
    static class Staff {

        private final Resources resources;

        Staff(Resources resources) {
            this.resources = resources;
        }

        @GetMapping
        String list(@RequestParam(defaultValue = "all") String filter, @RequestParam(defaultValue = "") String q,
                    Model model) {
            List<Resource> rows = this.resources.all();
            String query = q.strip().toLowerCase(Locale.ROOT);
            String f = List.of("all", "published", "drafts").contains(filter) ? filter : "all";
            model.addAttribute("filter", f);
            model.addAttribute("q", q);
            model.addAttribute("total", rows.size());
            model.addAttribute("publishedCount", rows.stream().filter(Resource::published).count());
            model.addAttribute("visible", rows.stream()
                    .filter(r -> switch (f) {
                        case "published" -> r.published();
                        case "drafts" -> !r.published();
                        default -> true;
                    })
                    .filter(r -> query.isEmpty() || r.title().toLowerCase(Locale.ROOT).contains(query)
                            || r.category().toLowerCase(Locale.ROOT).contains(query))
                    .toList());
            return "staff/resources";
        }

        @GetMapping("/new")
        String create(Model model) {
            return editor(ResourceInput.EMPTY, model);
        }

        @GetMapping("/{id}")
        String edit(@PathVariable String id, Model model, RedirectAttributes redirect) {
            var resource = this.resources.find(id);
            if (resource.isEmpty()) {
                redirect.addFlashAttribute("error", "That resource no longer exists.");
                return "redirect:/staff/resources";
            }
            return editor(ResourceInput.of(resource.get()), model);
        }

        /** The editor, unless a failed save left what was typed in the model. */
        private String editor(ResourceInput input, Model model) {
            if (!model.containsAttribute("draft")) model.addAttribute("draft", input);
            model.addAttribute("categories", this.resources.all().stream().map(Resource::category).distinct().sorted().toList());
            model.addAttribute("kinds", Resource.KINDS);
            model.addAttribute("audiences", Resource.AUDIENCES);
            return "staff/resource-edit";
        }

        /** {@code ignored}: a field that didn't convert (e.g. an Order that isn't a number) is left at its default. */
        @PostMapping
        String save(@ModelAttribute ResourceInput draft, BindingResult ignored, RedirectAttributes redirect) {
            try {
                this.resources.save(draft);
                return "redirect:/staff/resources";
            } catch (ResourceException e) {
                redirect.addFlashAttribute("error", e.getMessage());
                redirect.addFlashAttribute("draft", draft);
                return "redirect:/staff/resources/" + (draft.isNew() ? "new" : draft.id());
            }
        }

        @PostMapping("/{id}/published")
        String publish(@PathVariable String id, @RequestParam boolean published) {
            this.resources.setPublished(id, published);
            return "redirect:/staff/resources";
        }

        @PostMapping("/{id}/delete")
        String delete(@PathVariable String id) {
            this.resources.delete(id);
            return "redirect:/staff/resources";
        }
    }
}
