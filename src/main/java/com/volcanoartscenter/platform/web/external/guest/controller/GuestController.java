package com.volcanoartscenter.platform.web.external.guest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GuestController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("currentPage", "home");
        model.addAttribute("pageTitle", "Volcano Arts Center — Experience Rwanda Through Arts, Culture, and People");
        model.addAttribute("metaDescription",
            "Volcano Arts Center Inc is a cultural and creative hub near Volcanoes National Park, Rwanda. "
            + "Discover authentic art, immersive community-based tourism experiences, and support local communities.");
        return "external/guest/home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("currentPage", "about");
        model.addAttribute("pageTitle", "About Volcano Arts Center");
        model.addAttribute("metaDescription",
            "Learn about Volcano Arts Center Inc — empowering communities through art and tourism since 2012 in Musanze, Rwanda.");
        return "external/guest/about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("currentPage", "contact");
        model.addAttribute("pageTitle", "Contact Volcano Arts Center");
        model.addAttribute("metaDescription",
            "Get in touch with Volcano Arts Center Inc. Inquire about cultural experiences, art purchases, partnerships, or plan your visit near Volcanoes National Park.");
        return "external/guest/contact";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Admin Login");
        return "internal/super-admin/login";
    }
}
