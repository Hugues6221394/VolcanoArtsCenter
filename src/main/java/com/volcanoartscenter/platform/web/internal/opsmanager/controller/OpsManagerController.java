package com.volcanoartscenter.platform.web.internal.opsmanager.controller;

import com.volcanoartscenter.platform.shared.model.Booking;
import com.volcanoartscenter.platform.shared.model.ContactInquiry;
import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import com.volcanoartscenter.platform.shared.model.TalentApplication;
import com.volcanoartscenter.platform.shared.model.TourOperatorRequest;
import com.volcanoartscenter.platform.web.internal.opsmanager.service.OpsManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class OpsManagerController {

    private final OpsManagerService opsManagerService;

    @GetMapping("/admin/ops/bookings")
    public String opsBookings(Model model) {
        model.addAttribute("adminPage", "bookings");
        model.addAttribute("pageTitle", "Bookings");
        model.addAttribute("totalBookings", opsManagerService.totalBookings());
        model.addAttribute("totalOrders", opsManagerService.totalOrders());
        model.addAttribute("totalInquiries", opsManagerService.totalInquiries());
        model.addAttribute("pendingTalentApplications", opsManagerService.pendingTalentApplications());
        model.addAttribute("items", opsManagerService.listBookings());
        return "internal/ops-manager/bookings";
    }

    @GetMapping("/admin/ops/donations")
    public String opsDonations(Model model) {
        model.addAttribute("adminPage", "donations");
        model.addAttribute("pageTitle", "Donations");
        model.addAttribute("items", opsManagerService.listDonations());
        return "internal/ops-manager/donations";
    }

    @GetMapping("/admin/ops/talent-applications")
    public String opsTalentApplications(Model model) {
        model.addAttribute("adminPage", "talent-applications");
        model.addAttribute("pageTitle", "Talent Applications");
        model.addAttribute("items", opsManagerService.listTalentApplications());
        return "internal/ops-manager/talent-applications";
    }
    @GetMapping("/admin/ops/shipping-orders")
    public String opsShippingOrders(Model model) {
        model.addAttribute("adminPage", "shipping-orders");
        model.addAttribute("pageTitle", "Shipping Orders");
        model.addAttribute("items", opsManagerService.listShippingOrders());
        model.addAttribute("clientProfiles", opsManagerService.listClientProfiles());
        return "internal/ops-manager/shipping-orders";
    }

    @GetMapping("/admin/ops/contact-inquiries")
    public String opsContactInquiries(Model model) {
        model.addAttribute("adminPage", "contact-inquiries");
        model.addAttribute("pageTitle", "Contact Inquiries");
        model.addAttribute("items", opsManagerService.listContactInquiries());
        return "internal/ops-manager/contact-inquiries";
    }

    @GetMapping("/admin/ops/operator-requests")
    public String opsOperatorRequests(Model model) {
        model.addAttribute("adminPage", "operator-requests");
        model.addAttribute("pageTitle", "Tour Operator Requests");
        model.addAttribute("items", opsManagerService.listOperatorRequests());
        return "internal/ops-manager/operator-requests";
    }

    @GetMapping("/admin/ops/availability-slots")
    public String opsAvailabilitySlots(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                       @RequestParam(required = false) Long experienceId,
                                       Model model) {
        var items = opsManagerService.listAvailabilitySlots(fromDate, toDate, experienceId);
        model.addAttribute("adminPage", "availability-slots");
        model.addAttribute("pageTitle", "Availability Slots");
        model.addAttribute("items", items);
        model.addAttribute("experiences", opsManagerService.listExperiences());
        model.addAttribute("blackouts", opsManagerService.listBlackoutDates(experienceId));
        model.addAttribute("guides", opsManagerService.listGuideUsers());
        model.addAttribute("slotBookingsById", items.stream().collect(java.util.stream.Collectors.toMap(
                com.volcanoartscenter.platform.shared.model.AvailabilitySlot::getId,
                slot -> opsManagerService.bookingsForSlot(
                        slot.getExperience() == null ? null : slot.getExperience().getId(),
                        slot.getSlotDate()
                )
        )));
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("experienceId", experienceId);
        return "internal/ops-manager/availability-slots";
    }

    @PostMapping("/admin/ops/availability-slots/generate")
    public String generateOpsAvailability(@RequestParam Long experienceId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                          @RequestParam(defaultValue = "15") Integer capacity,
                                          RedirectAttributes redirectAttributes) {
        opsManagerService.generateAvailability(experienceId, fromDate, toDate, capacity);
        redirectAttributes.addFlashAttribute("successMessage", "Availability generated.");
        return "redirect:/admin/ops/availability-slots";
    }

    @PostMapping("/admin/ops/availability-slots/blackout")
    public String addOpsBlackout(@RequestParam Long experienceId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(required = false) String reason,
                                 RedirectAttributes redirectAttributes) {
        opsManagerService.addBlackoutDate(experienceId, date, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Blackout date added.");
        return "redirect:/admin/ops/availability-slots";
    }

    @PostMapping("/admin/ops/availability-slots/{id}/assign-guide")
    public String assignGuide(@PathVariable Long id,
                              @RequestParam(required = false) String guideEmail,
                              @RequestParam(required = false) String guideName,
                              RedirectAttributes redirectAttributes) {
        try {
            opsManagerService.assignGuide(id, guideEmail, guideName);
            redirectAttributes.addFlashAttribute("successMessage", "Guide assignment updated.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/admin/ops/availability-slots";
    }

    @PostMapping("/admin/ops/availability-slots/{id}/update")
    public String updateSlot(@PathVariable Long id,
                             @RequestParam(required = false) com.volcanoartscenter.platform.shared.model.AvailabilitySlot.SlotStatus status,
                             @RequestParam(required = false) Integer maxCapacity,
                             @RequestParam(required = false) Integer bookedCount,
                             RedirectAttributes redirectAttributes) {
        opsManagerService.updateSlot(id, status, maxCapacity, bookedCount);
        redirectAttributes.addFlashAttribute("successMessage", "Slot updated.");
        return "redirect:/admin/ops/availability-slots";
    }

    @PostMapping("/admin/ops/availability-slots/blackout/{id}/delete")
    public String removeBlackout(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        opsManagerService.removeBlackout(id);
        redirectAttributes.addFlashAttribute("successMessage", "Blackout removed.");
        return "redirect:/admin/ops/availability-slots";
    }

    @PostMapping("/admin/ops/bookings/{id}/status")
    public String updateOpsBookingStatus(@PathVariable Long id,
                                         @RequestParam Booking.BookingStatus status,
                                         @RequestParam(required = false) String adminNotes,
                                         @RequestParam(required = false) String notifyChannel,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        opsManagerService.updateBookingStatus(id, status, adminNotes, notifyChannel, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Booking status updated.");
        return "redirect:/admin/ops/bookings";
    }

    @PostMapping("/admin/ops/contact-inquiries/{id}/status")
    public String updateOpsInquiryStatus(@PathVariable Long id,
                                         @RequestParam ContactInquiry.InquiryStatus status,
                                         @RequestParam(required = false) String notifyChannel,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        opsManagerService.updateInquiryStatus(id, status, notifyChannel, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Inquiry status updated.");
        return "redirect:/admin/ops/contact-inquiries";
    }

    @PostMapping("/admin/ops/operator-requests/{id}/status")
    public String updateOpsOperatorRequestStatus(@PathVariable Long id,
                                                  @RequestParam TourOperatorRequest.RequestStatus status,
                                                  @RequestParam(required = false) java.math.BigDecimal partnerPrice,
                                                  @RequestParam(required = false) String partnerPriceCurrency,
                                                  @RequestParam(required = false) String itineraryAssetUrl,
                                                  @RequestParam(required = false) String adminNotes,
                                                  @RequestParam(required = false) String notifyChannel,
                                                  Authentication authentication,
                                                  RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        opsManagerService.updateOperatorRequest(id, status, partnerPrice, partnerPriceCurrency, itineraryAssetUrl, adminNotes, notifyChannel, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Operator request status updated.");
        return "redirect:/admin/ops/operator-requests";
    }

    @PostMapping("/admin/ops/shipping-orders/{id}/status")
    public String updateOpsShippingOrderStatus(@PathVariable Long id,
                                               @RequestParam ShippingOrder.OrderStatus status,
                                               @RequestParam(required = false) String trackingNumber,
                                               @RequestParam(required = false) String adminNotes,
                                               @RequestParam(required = false) String notifyChannel,
                                               Authentication authentication,
                                               RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        opsManagerService.updateShippingOrderStatus(id, status, trackingNumber, adminNotes, notifyChannel, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Shipping order updated.");
        return "redirect:/admin/ops/shipping-orders";
    }

    @PostMapping("/admin/ops/talent-applications/{id}/status")
    public String updateOpsTalentApplicationStatus(@PathVariable Long id,
                                                   @RequestParam TalentApplication.ApplicationStatus status,
                                                   @RequestParam(required = false) String adminNotes,
                                                   @RequestParam(required = false) String notifyChannel,
                                                   Authentication authentication,
                                                   RedirectAttributes redirectAttributes) {
        String actor = authentication == null ? "system" : authentication.getName();
        opsManagerService.updateTalentApplicationStatus(id, status, adminNotes, notifyChannel, actor);
        redirectAttributes.addFlashAttribute("successMessage", "Talent application updated.");
        return "redirect:/admin/ops/talent-applications";
    }
}
