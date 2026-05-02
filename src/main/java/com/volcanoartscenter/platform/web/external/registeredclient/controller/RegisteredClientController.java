package com.volcanoartscenter.platform.web.external.registeredclient.controller;

import com.volcanoartscenter.platform.shared.model.*;
import com.volcanoartscenter.platform.shared.service.CaptchaService;
import com.volcanoartscenter.platform.web.external.registeredclient.service.RegisteredClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class RegisteredClientController {

    private final RegisteredClientService registeredClientService;
    private final CaptchaService captchaService;

    @GetMapping("/art-store")
    public String artStore(@RequestParam(required = false) String category,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) BigDecimal minPrice,
                           @RequestParam(required = false) BigDecimal maxPrice,
                           Model model) {
        User user = currentUser(authentication).orElse(null);
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && authentication.getName() != null && !"anonymousUser".equals(authentication.getName());
        model.addAttribute("currentPage", "art-store");
        model.addAttribute("pageTitle", "Art Store — Volcano Arts Center");
        model.addAttribute("products", registeredClientService.listProducts(category, q, minPrice, maxPrice));
        model.addAttribute("categories", registeredClientService.activeCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("q", q);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        return "external/registered-client/art-store";
    }

    @GetMapping("/art-store/paged")
    public String artStorePaged(@RequestParam(required = false) String category,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) BigDecimal minPrice,
                                @RequestParam(required = false) BigDecimal maxPrice,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                Model model) {
        model.addAttribute("currentPage", "art-store");
        model.addAttribute("pageTitle", "Art Store — Volcano Arts Center");
        model.addAttribute("productsPage", registeredClientService.listProductsPage(category, q, minPrice, maxPrice, page, size));
        model.addAttribute("categories", registeredClientService.activeCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("q", q);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        return "external/registered-client/art-store-paged";
    }

    @GetMapping("/art-store/{slug}")
    public String productDetail(@PathVariable String slug, Authentication authentication, Model model) {
        Product product = registeredClientService.findProduct(slug).orElse(null);
        if (product == null) {
            return "redirect:/art-store";
        }

        model.addAttribute("currentPage", "art-store");
        model.addAttribute("pageTitle", product.getName() + " — Volcano Arts Center");
        model.addAttribute("product", product);
        model.addAttribute("reviews", registeredClientService.productReviews(product.getId()));
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("canSubmitReview", registeredClientService.canSubmitProductReview(product, user));
        model.addAttribute("reviewStatusMessage", registeredClientService.productReviewStatus(product, user));
        model.addAttribute("reviewerDisplayName", user != null ? user.getFullName() : null);
        model.addAttribute("reviewerDisplayEmail", user != null ? user.getEmail() : null);
        return "external/registered-client/product-detail";
    }

    @PostMapping("/art-store/{slug}/reviews")
    public String submitProductReview(@PathVariable String slug,
                                      @RequestParam(required = false) String reviewerName,
                                      @RequestParam(required = false) String reviewerEmail,
                                      @RequestParam(required = false) String reviewerCountry,
                                      @RequestParam Integer rating,
                                      @RequestParam String comment,
                                      @RequestParam(required = false) String captchaToken,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Captcha validation failed.");
            return "redirect:/art-store/" + slug;
        }
        Product product = registeredClientService.findProduct(slug).orElse(null);
        if (product == null) {
            return "redirect:/art-store";
        }

        User user = currentUser(authentication).orElse(null);
        try {
            registeredClientService.submitProductReview(product, user, reviewerName, reviewerEmail, reviewerCountry, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Review submitted. It will appear after moderation.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/art-store/" + slug;
    }



    @GetMapping("/experiences")
    public String experiences(Model model) {
        User user = currentUser(authentication).orElse(null);
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && authentication.getName() != null && !"anonymousUser".equals(authentication.getName());
        model.addAttribute("currentPage", "experiences");
        model.addAttribute("pageTitle", "Experiences — Volcano Arts Center");
        model.addAttribute("experiences", registeredClientService.activeExperiences());
        return "external/registered-client/experiences";
    }

    @GetMapping("/experiences/{slug}")
    public String experienceDetail(@PathVariable String slug, Authentication authentication, Model model) {
        Experience experience = registeredClientService.findExperience(slug).orElse(null);
        if (experience == null) {
            return "redirect:/experiences";
        }

        model.addAttribute("currentPage", "experiences");
        model.addAttribute("pageTitle", experience.getTitle() + " — Volcano Arts Center");
        model.addAttribute("experience", experience);
        model.addAttribute("reviews", registeredClientService.experienceReviews(experience.getId()));
        model.addAttribute("availabilitySlots", registeredClientService.upcomingSlots(experience.getId()));
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("canSubmitReview", registeredClientService.canSubmitExperienceReview(experience, user));
        model.addAttribute("reviewStatusMessage", registeredClientService.experienceReviewStatus(experience, user));
        model.addAttribute("reviewerDisplayName", user != null ? user.getFullName() : null);
        model.addAttribute("reviewerDisplayEmail", user != null ? user.getEmail() : null);
        return "external/registered-client/experience-detail";
    }

    @PostMapping("/experiences/{slug}/book")
    public String bookExperience(@PathVariable String slug,
                                 @RequestParam String guestName,
                                 @RequestParam String guestEmail,
                                 @RequestParam(required = false) String guestPhone,
                                 @RequestParam(required = false) String guestCountry,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDate,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate alternativeDate,
                                 @RequestParam(defaultValue = "1") Integer groupSize,
                                 @RequestParam(required = false) String preferredLanguage,
                                 @RequestParam(required = false) String paymentMethod,
                                 @RequestParam(required = false) String specialRequests,
                                 @RequestParam(required = false) String tourOperatorName,
                                 @RequestParam(required = false) String tourOperatorEmail,
                                 @RequestParam(required = false) String captchaToken,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("successMessage", "Please register or sign in to book experiences.");
            return "redirect:/login";
        }
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("successMessage", "Captcha validation failed.");
            return "redirect:/experiences/" + slug;
        }
        Experience experience = registeredClientService.findExperience(slug).orElse(null);
        if (experience == null) {
            return "redirect:/experiences";
        }
        User user = currentUser(authentication).orElse(null);

        try {
            Booking booking = registeredClientService.createBooking(
                    experience,
                    user,
                    user == null ? guestName : user.getFullName(),
                    user == null ? guestEmail : user.getEmail(),
                    user == null ? guestPhone : user.getPhone(),
                    user == null ? guestCountry : user.getCountry(),
                    preferredDate, alternativeDate, groupSize, preferredLanguage,
                    paymentMethod, specialRequests, tourOperatorName, tourOperatorEmail
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking request received. Reference: " + booking.getBookingReference() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/experiences/" + slug;
    }

    @PostMapping("/experiences/{slug}/reviews")
    public String submitExperienceReview(@PathVariable String slug,
                                         @RequestParam(required = false) String reviewerName,
                                         @RequestParam(required = false) String reviewerEmail,
                                         @RequestParam(required = false) String reviewerCountry,
                                         @RequestParam Integer rating,
                                         @RequestParam String comment,
                                         @RequestParam(required = false) String captchaToken,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Captcha validation failed.");
            return "redirect:/experiences/" + slug;
        }
        Experience experience = registeredClientService.findExperience(slug).orElse(null);
        if (experience == null) {
            return "redirect:/experiences";
        }
        User user = currentUser(authentication).orElse(null);
        try {
            registeredClientService.submitExperienceReview(experience, user, reviewerName, reviewerEmail, reviewerCountry, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Review submitted. It will appear after moderation.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/experiences/" + slug;
    }

    @GetMapping("/conservation")
    public String conservation(Model model) {
        model.addAttribute("currentPage", "conservation");
        model.addAttribute("campaigns", registeredClientService.activeDonationCampaigns());
        model.addAttribute("donationPurposes", Donation.DonationPurpose.values());
        model.addAttribute("recurringFrequencies", Donation.RecurringFrequency.values());
        model.addAttribute("pageTitle", "Conservation — Volcano Arts Center");
        return "external/registered-client/conservation";
    }

    @PostMapping("/conservation/donate")
    public String donate(@RequestParam String donorName,
                         @RequestParam String donorEmail,
                         @RequestParam(required = false) String donorCountry,
                         @RequestParam BigDecimal amount,
                         @RequestParam(defaultValue = "USD") String currency,
                         @RequestParam(defaultValue = "GENERAL") Donation.DonationPurpose purpose,
                         @RequestParam(required = false) String message,
                         @RequestParam(defaultValue = "false") Boolean isRecurring,
                         @RequestParam(required = false) Donation.RecurringFrequency recurringFrequency,
                         @RequestParam(required = false) String paymentMethod,
                         @RequestParam(required = false) Long campaignId,
                         @RequestParam(required = false) String captchaToken,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Captcha validation failed.");
            return "redirect:/conservation";
        }
        User user = currentUser(authentication).orElse(null);
        try {
            Donation donation = registeredClientService.createDonation(
                    user, donorName, donorEmail, donorCountry, amount, currency, purpose, message, isRecurring, recurringFrequency, paymentMethod, campaignId
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thank you for your support. Donation request #" + donation.getId() + " has been recorded.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/conservation";
    }

    @PostMapping("/art-store/{slug}/save")
    public String saveProduct(@PathVariable String slug, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Please sign in as a registered client to save items.");
            return "redirect:/login";
        }
        Product product = registeredClientService.findProduct(slug).orElse(null);
        if (product == null) {
            return "redirect:/art-store";
        }
        registeredClientService.saveProductForUser(user, product);
        redirectAttributes.addFlashAttribute("successMessage", "Item saved to your profile.");
        return "redirect:/art-store/" + slug;
    }

    @PostMapping("/art-store/{slug}/unsave")
    public String unsaveProduct(@PathVariable String slug, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Please sign in as a registered client to manage saved items.");
            return "redirect:/login";
        }
        Product product = registeredClientService.findProduct(slug).orElse(null);
        if (product == null) {
            return "redirect:/art-store";
        }
        registeredClientService.removeSavedProductForUser(user, product);
        redirectAttributes.addFlashAttribute("successMessage", "Item removed from saved list.");
        return "redirect:/art-store/" + slug;
    }

    @GetMapping("/talent")
    public String talent(Authentication authentication, Model model) {
        model.addAttribute("currentPage", "talent");
        model.addAttribute("pageTitle", "Talent Program — Volcano Arts Center");
        model.addAttribute("categories", TalentApplication.ApplicantCategory.values());
        model.addAttribute("areas", TalentApplication.TalentArea.values());
        model.addAttribute("profiles", registeredClientService.publishedTalentProfiles());
        model.addAttribute("isAuthenticated", isAuthenticated(authentication));
        model.addAttribute("isTalentApplicant", hasAuthority(authentication, "ROLE_TALENT_APPLICANT"));
        return "external/talent-applicant/talent";
    }

    @PostMapping("/talent/apply")
    public String applyTalent(@RequestParam String fullName,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String ageRange,
                              @RequestParam(required = false) String gender,
                              @RequestParam(required = false) String location,
                              @RequestParam TalentApplication.ApplicantCategory applicantCategory,
                              @RequestParam TalentApplication.TalentArea talentArea,
                              @RequestParam(required = false) String experienceDescription,
                              @RequestParam(required = false) String motivation,
                              @RequestParam(required = false) String availabilityDetails,
                              @RequestParam(required = false) String accessibilityNeeds,
                              @RequestParam(required = false) String captchaToken,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("successMessage", "Please register a talent applicant account and sign in before applying.");
            return "redirect:/talent/register";
        }
        if (!hasAuthority(authentication, "ROLE_TALENT_APPLICANT")) {
            redirectAttributes.addFlashAttribute("successMessage", "Please use a Talent Applicant account to submit this form.");
            return "redirect:/talent/register";
        }
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("successMessage", "Captcha validation failed.");
            return "redirect:/talent";
        }
        registeredClientService.createTalentApplication(fullName, email, phone, ageRange, gender, location, applicantCategory, talentArea, experienceDescription, motivation, availabilityDetails, accessibilityNeeds);
        redirectAttributes.addFlashAttribute("successMessage",
                "Application submitted successfully. We will contact you after review.");
        return "redirect:/talent";
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        model.addAttribute("currentPage", "blog");
        model.addAttribute("pageTitle", "Blog & News — Volcano Arts Center");
        model.addAttribute("posts", registeredClientService.publishedPosts());
        return "external/guest/blog";
    }

    @GetMapping("/blog/{slug}")
    public String blogDetail(@PathVariable String slug, Model model) {
        BlogPost post = registeredClientService.viewBlogPost(slug).orElse(null);
        if (post == null) {
            return "redirect:/blog";
        }
        model.addAttribute("currentPage", "blog");
        model.addAttribute("pageTitle", post.getTitle() + " — Volcano Arts Center");
        model.addAttribute("post", post);
        return "external/guest/blog-detail";
    }

    @PostMapping("/contact")
    public String submitInquiry(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam String subject,
                                @RequestParam String message,
                                @RequestParam(required = false) String captchaToken,
                                RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("successMessage", "Captcha validation failed.");
            return "redirect:/contact";
        }
        registeredClientService.createContactInquiry(name, email, phone, subject, message);
        redirectAttributes.addFlashAttribute("successMessage", "Message sent successfully. Our team will reply soon.");
        return "redirect:/contact";
    }

    @PostMapping("/tour-operators/request")
    public String submitTourOperatorRequest(@RequestParam String companyName,
                                            @RequestParam String contactName,
                                            @RequestParam String contactEmail,
                                            @RequestParam(required = false) String contactPhone,
                                            @RequestParam(required = false) String country,
                                            @RequestParam(required = false) String requestedExperienceSlug,
                                            @RequestParam(required = false) Integer estimatedGroupSize,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate estimatedDate,
                                            @RequestParam(defaultValue = "true") Boolean invoiceRequired,
                                            @RequestParam(required = false) String requestDetails,
                                            @RequestParam(required = false) String captchaToken,
                                            RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("successMessage", "Captcha validation failed.");
            return "redirect:/contact";
        }
        TourOperatorRequest request = registeredClientService.createTourOperatorRequest(
                companyName, contactName, contactEmail, contactPhone, country,
                requestedExperienceSlug, estimatedGroupSize, estimatedDate, invoiceRequired, requestDetails
        );
        redirectAttributes.addFlashAttribute("successMessage", "Tour-operator request submitted. Reference #" + request.getId() + ".");
        return "redirect:/contact";
    }

    private java.util.Optional<User> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return java.util.Optional.empty();
        }
        return registeredClientService.findUserByEmail(authentication.getName());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getName() != null && !"anonymousUser".equals(authentication.getName());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
