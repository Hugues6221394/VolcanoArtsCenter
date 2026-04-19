package com.volcanoartscenter.platform.web.external.registeredclient.service;

import com.volcanoartscenter.platform.shared.model.*;
import com.volcanoartscenter.platform.shared.repository.*;
import com.volcanoartscenter.platform.shared.service.AvailabilityService;
import com.volcanoartscenter.platform.shared.service.ComplianceService;
import com.volcanoartscenter.platform.shared.service.NotificationService;
import com.volcanoartscenter.platform.shared.service.integration.IntegrationFacadeService;
import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisteredClientService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ReviewRepository reviewRepository;
    private final ExperienceRepository experienceRepository;
    private final BookingRepository bookingRepository;
    private final DonationRepository donationRepository;
    private final TalentApplicationRepository talentApplicationRepository;
    private final BlogPostRepository blogPostRepository;
    private final ShippingOrderRepository shippingOrderRepository;
    private final ContactInquiryRepository contactInquiryRepository;
    private final TourOperatorRequestRepository tourOperatorRequestRepository;
    private final SavedItemRepository savedItemRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final ComplianceService complianceService;
    private final IntegrationFacadeService integrationFacadeService;
    private final NotificationService notificationService;

    public List<Product> listProducts(String category, String q, BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.searchCatalog(category, q, minPrice, maxPrice,
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent();
    }

    public org.springframework.data.domain.Page<Product> listProductsPage(String category, String q, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, size));
        return productRepository.searchCatalog(category, q, minPrice, maxPrice, pageable);
    }

    public List<ProductCategory> activeCategories() {
        return productCategoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public Optional<Product> findProduct(String slug) {
        return productRepository.findBySlugAndAvailableTrue(slug);
    }

    public List<Review> productReviews(Long productId) {
        return reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId);
    }

    public void submitProductReview(Product product, User user, String reviewerName, String reviewerEmail, String reviewerCountry,
                                    Integer rating, String comment) {
        int normalized = Math.max(1, Math.min(5, rating));

        // Guard: must be authenticated
        if (user == null) {
            throw new IllegalStateException("You must be logged in to leave a review.");
        }

        // Guard: must have purchased and received this product (DELIVERED status)
        if (!shippingOrderRepository.hasDeliveredProduct(user.getId(), product.getId())) {
            throw new IllegalStateException("You can only review products you have purchased and received.");
        }

        // Guard: one review per user per product
        if (reviewRepository.findByUserAndProductId(user, product.getId()).isPresent()) {
            throw new IllegalStateException("You already reviewed this product.");
        }

        reviewRepository.save(Review.builder()
                .reviewerName(user.getFullName())
                .reviewerEmail(user.getEmail())
                .reviewerCountry(user.getCountry())
                .user(user)
                .rating(normalized)
                .comment(comment)
                .approved(false)
                .featured(false)
                .product(product)
                .build());
    }

    public void submitExperienceReview(Experience experience, User user, String reviewerName, String reviewerEmail, String reviewerCountry,
                                       Integer rating, String comment) {
        int normalized = Math.max(1, Math.min(5, rating));

        // Guard: must be authenticated
        if (user == null) {
            throw new IllegalStateException("You must be logged in to leave a review.");
        }

        // Guard: must have completed this experience (COMPLETED booking)
        if (!bookingRepository.hasCompletedBooking(user.getId(), experience.getId())) {
            throw new IllegalStateException("You can only review experiences you have completed.");
        }

        // Guard: one review per user per experience
        if (reviewRepository.findByUserAndExperienceId(user, experience.getId()).isPresent()) {
            throw new IllegalStateException("You already reviewed this experience.");
        }

        reviewRepository.save(Review.builder()
                .reviewerName(user.getFullName())
                .reviewerEmail(user.getEmail())
                .reviewerCountry(user.getCountry())
                .user(user)
                .rating(normalized)
                .comment(comment)
                .approved(false)
                .featured(false)
                .experience(experience)
                .build());
    }

    public ShippingOrder createShippingOrderFromCart(Cart cart, User user, String recipientName, String recipientEmail, String recipientPhone,
                                             String addressLine1, String addressLine2, String city, String state,
                                             String postalCode, String country, String paymentMethod) {

        BigDecimal productTotal = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        java.util.List<OrderItem> items = new java.util.ArrayList<>();

        ShippingOrder order = ShippingOrder.builder()
                .orderReference("SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .user(user)
                .recipientName(recipientName)
                .recipientEmail(recipientEmail)
                .recipientPhone(recipientPhone)
                .addressLine1(addressLine1)
                .addressLine2(addressLine2)
                .city(city)
                .state(state)
                .postalCode(postalCode)
                .country(country)
                .carrier("Rwanda".equalsIgnoreCase(country) ? ShippingOrder.ShippingCarrier.LOCAL : ShippingOrder.ShippingCarrier.FEDEX)
                .currency("USD")
                .paymentMethod(paymentMethod)
                .paymentStatus(ShippingOrder.PaymentStatus.UNPAID)
                .status(ShippingOrder.OrderStatus.PENDING)
                .build();

        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            int qty = Math.max(1, ci.getQuantity());
            productTotal = productTotal.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
            totalWeight = totalWeight.add((p.getWeightKg() == null ? new BigDecimal("1.0") : p.getWeightKg()).multiply(BigDecimal.valueOf(qty)));
            
            OrderItem item = OrderItem.builder()
               .order(order)
               .product(p)
               .quantity(qty)
               .priceAtPurchase(p.getPrice())
               .productName(p.getName())
               .productImageUrl(p.getPrimaryImageUrl())
               .build();
            items.add(item);
        }
        
        order.setOrderItems(items);
        order.setProductTotal(productTotal);
        
        // For backwards compatibility until old features fully replaced
        if (!items.isEmpty()) {
            order.setProduct(items.getFirst().getProduct());
            order.setQuantity(items.getFirst().getQuantity());
        }

        BigDecimal shipping = integrationFacadeService.estimateShipping("Rwanda".equalsIgnoreCase(country) ? "LOCAL" : "FEDEX", country, totalWeight);
        order.setShippingCost(shipping);
        order.setTotalAmount(productTotal.add(shipping));

        ShippingOrder saved = shippingOrderRepository.save(order);
        PaymentGatewayService.PaymentResult payment = integrationFacadeService.initializePayment(
                paymentMethod, saved.getOrderReference(), saved.getTotalAmount(), saved.getCurrency(),
                java.util.Map.of("email", recipientEmail)
        );
        saved.setPaymentTransactionId(payment.externalReference());
        saved.setPaymentStatus(payment.success() ? ShippingOrder.PaymentStatus.PAID : ShippingOrder.PaymentStatus.UNPAID);
        if (!"Rwanda".equalsIgnoreCase(country)) {
            saved.setTrackingNumber(integrationFacadeService.createShipment("FEDEX", saved.getOrderReference()));
        }
        shippingOrderRepository.save(saved);
        notificationService.sendEmailAsync(recipientEmail, "Order received: " + saved.getOrderReference(),
                "Your order has been received and is being processed.");
        return saved;
    }

    public List<Experience> activeExperiences() {
        return experienceRepository.findByActiveTrueOrderByFeaturedDescTitleAsc();
    }

    public Optional<Experience> findExperience(String slug) {
        return experienceRepository.findBySlugAndActiveTrue(slug);
    }

    public List<Review> experienceReviews(Long experienceId) {
        return reviewRepository.findByExperienceIdAndApprovedTrueOrderByCreatedAtDesc(experienceId);
    }

    public List<AvailabilitySlot> upcomingSlots(Long experienceId) {
        if (experienceId == null) {
            return List.of();
        }
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(30);
        return availabilitySlotRepository.findByExperienceIdAndSlotDateBetweenOrderBySlotDateAsc(experienceId, from, to);
    }

    public Booking createBooking(Experience experience, User user, String guestName, String guestEmail, String guestPhone, String guestCountry,
                                 LocalDate preferredDate, LocalDate alternativeDate, Integer groupSize, String preferredLanguage,
                                 String paymentMethod, String specialRequests, String tourOperatorName, String tourOperatorEmail) {
        int safeGroupSize = Math.max(1, groupSize);
        BigDecimal totalPrice = experience.getPricePerPerson() == null
                ? null
                : experience.getPricePerPerson().multiply(BigDecimal.valueOf(safeGroupSize));

        Booking booking = Booking.builder()
                .bookingReference("BOOK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .user(user)
                .guestName(guestName)
                .guestEmail(guestEmail)
                .guestPhone(guestPhone)
                .guestCountry(guestCountry)
                .experience(experience)
                .preferredDate(preferredDate)
                .alternativeDate(alternativeDate)
                .groupSize(safeGroupSize)
                .preferredLanguage(preferredLanguage == null || preferredLanguage.isBlank() ? "English" : preferredLanguage)
                .specialRequests(specialRequests)
                .tourOperatorName(tourOperatorName)
                .tourOperatorEmail(tourOperatorEmail)
                .paymentMethod(paymentMethod)
                .paymentStatus(Booking.PaymentStatus.UNPAID)
                .status(Booking.BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .build();
        Booking saved = bookingRepository.save(booking);
        complianceService.recordConsent(guestEmail, "BOOKING_TERMS", true, "experience-booking-form");
        complianceService.audit(guestEmail, "BOOKING_CREATED", "Booking", saved.getId(),
                "Experience=" + experience.getSlug() + ", groupSize=" + safeGroupSize + ", date=" + preferredDate);
        availabilityService.applyBookingToSlot(experience, preferredDate, safeGroupSize);
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            PaymentGatewayService.PaymentResult payment = integrationFacadeService.initializePayment(
                    paymentMethod, saved.getBookingReference(), saved.getTotalPrice() == null ? BigDecimal.ZERO : saved.getTotalPrice(), "USD",
                    java.util.Map.of("email", guestEmail)
            );
            saved.setPaymentStatus(payment.success() ? Booking.PaymentStatus.PAID : Booking.PaymentStatus.UNPAID);
            bookingRepository.save(saved);
        }
        integrationFacadeService.sendEmail(guestEmail, "Booking request received: " + saved.getBookingReference(),
                "Thank you for your booking request. Our operations team will confirm shortly.");
        return saved;
    }

    public Donation createDonation(User user, String donorName, String donorEmail, String donorCountry, BigDecimal amount, String currency,
                                   Donation.DonationPurpose purpose, String message, Boolean isRecurring,
                                   Donation.RecurringFrequency recurringFrequency, String paymentMethod) {
        Donation donation = Donation.builder()
                .donorName(user == null ? donorName : user.getFullName())
                .donorEmail(user == null ? donorEmail : user.getEmail())
                .donorCountry(user == null ? donorCountry : user.getCountry())
                .user(user)
                .amount(amount.max(new BigDecimal("1.00")))
                .currency(currency)
                .purpose(purpose)
                .message(message)
                .isRecurring(isRecurring)
                .recurringFrequency(isRecurring ? recurringFrequency : null)
                .paymentMethod(paymentMethod)
                .status(Donation.DonationStatus.PENDING)
                .build();
        Donation saved = donationRepository.save(donation);
        complianceService.recordConsent(donorEmail, "DONATION_TERMS", true, "conservation-donation-form");
        complianceService.audit(donorEmail, "DONATION_CREATED", "Donation", saved.getId(),
                "Purpose=" + purpose + ", amount=" + saved.getAmount() + " " + saved.getCurrency());
        PaymentGatewayService.PaymentResult payment = integrationFacadeService.initializePayment(
                paymentMethod, "DON-" + saved.getId(), saved.getAmount(), saved.getCurrency(),
                java.util.Map.of("email", donorEmail)
        );
        saved.setTransactionId(payment.externalReference());
        saved.setStatus(payment.success() ? Donation.DonationStatus.COMPLETED : Donation.DonationStatus.PENDING);
        donationRepository.save(saved);
        integrationFacadeService.sendEmail(donorEmail, "Donation received", "Thank you for supporting Volcano Arts Center.");
        return saved;
    }

    public List<ShippingOrder> ordersForUser(User user) {
        return shippingOrderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Booking> bookingsForUser(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Donation> donationsForUser(User user) {
        return donationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Review> reviewsForUser(User user) {
        return reviewRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void saveProductForUser(User user, Product product) {
        savedItemRepository.findByUserAndProduct(user, product)
                .orElseGet(() -> savedItemRepository.save(SavedItem.builder().user(user).product(product).build()));
    }

    @Transactional
    public void removeSavedProductForUser(User user, Product product) {
        savedItemRepository.deleteByUserAndProduct(user, product);
    }

    public List<SavedItem> savedItemsForUser(User user) {
        return savedItemRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public TalentApplication createTalentApplication(String fullName, String email, String phone, String ageRange, String gender,
                                                     String location, TalentApplication.ApplicantCategory applicantCategory,
                                                     TalentApplication.TalentArea talentArea, String experienceDescription,
                                                     String motivation, String availabilityDetails, String accessibilityNeeds) {
        TalentApplication application = TalentApplication.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .ageRange(ageRange)
                .gender(gender)
                .location(location)
                .applicantCategory(applicantCategory)
                .talentArea(talentArea)
                .experienceDescription(experienceDescription)
                .motivation(motivation)
                .availabilityDetails(availabilityDetails)
                .accessibilityNeeds(accessibilityNeeds)
                .status(TalentApplication.ApplicationStatus.PENDING)
                .build();
        TalentApplication saved = talentApplicationRepository.save(application);
        complianceService.recordConsent(email, "TALENT_APPLICATION_CONSENT", true, "talent-application-form");
        complianceService.audit(email, "TALENT_APPLICATION_CREATED", "TalentApplication", saved.getId(),
                "Category=" + applicantCategory + ", area=" + talentArea);
        return saved;
    }

    public List<BlogPost> publishedPosts() {
        return blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc();
    }

    public Optional<BlogPost> viewBlogPost(String slug) {
        Optional<BlogPost> postOpt = blogPostRepository.findBySlugAndPublishedTrue(slug);
        postOpt.ifPresent(post -> {
            post.setViewCount(post.getViewCount() + 1);
            blogPostRepository.save(post);
        });
        return postOpt;
    }

    public void createContactInquiry(String name, String email, String phone, String subject, String message) {
        ContactInquiry inquiry = contactInquiryRepository.save(ContactInquiry.builder()
                .fullName(name)
                .email(email)
                .phone(phone)
                .subject(subject)
                .message(message)
                .status(ContactInquiry.InquiryStatus.NEW)
                .build());
        complianceService.recordConsent(email, "CONTACT_COMMUNICATION", true, "contact-form");
        complianceService.audit(email, "CONTACT_INQUIRY_CREATED", "ContactInquiry", inquiry.getId(),
                "Subject=" + subject);
    }

    public TourOperatorRequest createTourOperatorRequest(String companyName, String contactName, String contactEmail, String contactPhone,
                                                         String country, String requestedExperienceSlug, Integer estimatedGroupSize,
                                                         LocalDate estimatedDate, Boolean invoiceRequired, String requestDetails) {
        TourOperatorRequest request = TourOperatorRequest.builder()
                .companyName(companyName)
                .contactName(contactName)
                .contactEmail(contactEmail)
                .ownerEmail(contactEmail.trim().toLowerCase(Locale.ROOT))
                .contactPhone(contactPhone)
                .country(country)
                .requestedExperienceSlug(requestedExperienceSlug)
                .estimatedGroupSize(estimatedGroupSize)
                .estimatedDate(estimatedDate)
                .invoiceRequired(invoiceRequired == null || invoiceRequired)
                .requestDetails(requestDetails)
                .partnerPriceCurrency("USD")
                .status(TourOperatorRequest.RequestStatus.SUBMITTED)
                .build();
        TourOperatorRequest saved = tourOperatorRequestRepository.save(request);
        complianceService.recordConsent(contactEmail, "OPERATOR_REQUEST_CONSENT", true, "tour-operator-form");
        complianceService.audit(contactEmail, "OPERATOR_REQUEST_CREATED", "TourOperatorRequest", saved.getId(),
                "Company=" + companyName + ", experienceSlug=" + requestedExperienceSlug);
        integrationFacadeService.sendEmail(contactEmail, "Tour operator request submitted #" + saved.getId(),
                "Your B2B request was received and is now under review.");
        return saved;
    }

    public List<TourOperatorRequest> operatorRequestsForOwner(String email) {
        if (email == null || email.isBlank()) {
            return java.util.List.of();
        }
        return tourOperatorRequestRepository.findByOwnerEmailOrderByCreatedAtDesc(email.trim().toLowerCase(Locale.ROOT));
    }

    public Optional<User> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    public User saveUserProfile(User user) {
        return userRepository.save(user);
    }
}
