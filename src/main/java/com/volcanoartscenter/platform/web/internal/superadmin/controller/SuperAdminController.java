package com.volcanoartscenter.platform.web.internal.superadmin.controller;

import com.volcanoartscenter.platform.web.internal.superadmin.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final PasswordEncoder passwordEncoder;

    // ── Dashboard Overview ──
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("adminPage", "dashboard");
        model.addAttribute("totalProducts", superAdminService.totalProducts());
        model.addAttribute("totalExperiences", superAdminService.totalExperiences());
        model.addAttribute("totalBookings", superAdminService.totalBookings());
        model.addAttribute("totalDonations", superAdminService.totalDonations());
        model.addAttribute("totalTalentApplications", superAdminService.totalTalentApplications());
        model.addAttribute("totalBlogPosts", superAdminService.totalBlogPosts());
        model.addAttribute("totalShippingOrders", superAdminService.totalShippingOrders());
        model.addAttribute("totalInquiries", superAdminService.totalInquiries());
        model.addAttribute("latestBookings", superAdminService.latestBookings());
        model.addAttribute("auditEvents", superAdminService.latestAuditEvents());
        return "internal/super-admin/dashboard";
    }

    // ── Staff & Accounts ──
    @GetMapping("/admin/users")
    public String usersPage(Model model) {
        model.addAttribute("pageTitle", "Staff & Accounts");
        model.addAttribute("adminPage", "users");
        model.addAttribute("staffUsers", superAdminService.listInternalStaff());
        model.addAttribute("allRoles", superAdminService.listAllRoles());
        return "internal/super-admin/users";
    }

    @PostMapping("/admin/users/admins")
    public String createStaff(@RequestParam String email,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String password,
                              @RequestParam(name = "roles", required = false) List<String> roles,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String actor = authentication == null ? "system" : authentication.getName();
            superAdminService.createStaffUser(email, firstName, lastName, passwordEncoder.encode(password), roles, actor);
            redirectAttributes.addFlashAttribute("successMessage", "Staff account created.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/update")
    public String updateStaff(@PathVariable Long id,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String country,
                              @RequestParam(defaultValue = "false") Boolean enabled,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.updateStaffUser(id, firstName, lastName, phone, country, enabled, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Staff account updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/roles")
    public String updateStaffRoles(@PathVariable Long id,
                                   @RequestParam(name = "roles") List<String> roles,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            String actor = authentication == null ? "system" : authentication.getName();
            superAdminService.assignRoles(id, roles, actor);
            redirectAttributes.addFlashAttribute("successMessage", "Roles updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/deactivate")
    public String deactivateStaff(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.deactivateStaff(id, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Staff account deactivated.");
        return "redirect:/admin/users";
    }

    // ── Platform Settings ──
    @GetMapping("/admin/settings")
    public String settingsPage(Model model) {
        model.addAttribute("pageTitle", "Platform Settings");
        model.addAttribute("adminPage", "settings");
        model.addAttribute("paymentSettings", superAdminService.listSettingsByCategory("PAYMENT"));
        model.addAttribute("integrationSettings", superAdminService.listSettingsByCategory("INTEGRATION"));
        model.addAttribute("platformSettings", superAdminService.listSettingsByCategory("PLATFORM"));
        return "internal/super-admin/settings";
    }

    @PostMapping("/admin/settings")
    public String saveSetting(@RequestParam String category,
                              @RequestParam String keyName,
                              @RequestParam(required = false) String valueData,
                              @RequestParam(required = false) String description,
                              @RequestParam(defaultValue = "false") Boolean masked,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.saveSetting(category, keyName, valueData, description, masked, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Setting saved.");
        return "redirect:/admin/settings";
    }

    // ── Audit Log ──
    @GetMapping("/admin/audit-log")
    public String auditLogPage(Model model) {
        model.addAttribute("pageTitle", "Audit Log");
        model.addAttribute("adminPage", "audit-log");
        model.addAttribute("auditEvents", superAdminService.latestAuditEvents());
        return "internal/super-admin/audit-log";
    }

    // ── Overrides & Refunds ──
    @GetMapping("/admin/overrides")
    public String overridesPage(Model model) {
        model.addAttribute("pageTitle", "Overrides & Refunds");
        model.addAttribute("adminPage", "overrides");
        model.addAttribute("bookings", superAdminService.listBookings());
        model.addAttribute("orders", superAdminService.listShippingOrders());
        model.addAttribute("donations", superAdminService.listDonations());
        return "internal/super-admin/overrides";
    }

    @PostMapping("/admin/bookings/{id}/override")
    public String overrideBooking(@PathVariable Long id,
                                  @RequestParam com.volcanoartscenter.platform.shared.model.Booking.BookingStatus status,
                                  @RequestParam(required = false) String adminNotes,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.overrideBooking(id, status, adminNotes, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Booking overridden.");
        return "redirect:/admin/overrides";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam com.volcanoartscenter.platform.shared.model.ShippingOrder.OrderStatus status,
                                    @RequestParam(required = false) String trackingNumber,
                                    @RequestParam(required = false) String adminNotes,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.updateOrderStatus(id, status, trackingNumber, adminNotes, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Order status updated.");
        return "redirect:/admin/overrides";
    }

    @PostMapping("/admin/orders/{id}/refund")
    public String refundOrder(@PathVariable Long id,
                              @RequestParam String disputeReason,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.refundOrder(id, disputeReason, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Order refunded and dispute recorded.");
        return "redirect:/admin/overrides";
    }

    @PostMapping("/admin/donations/{id}/refund")
    public String refundDonation(@PathVariable Long id,
                                 @RequestParam String disputeReason,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        superAdminService.refundDonation(id, disputeReason, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Donation refunded and dispute recorded.");
        return "redirect:/admin/overrides";
    }

    @GetMapping(value = "/admin/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> exportAllData() {
        return ResponseEntity.ok(superAdminService.exportAllDataSnapshot());
    }
}
