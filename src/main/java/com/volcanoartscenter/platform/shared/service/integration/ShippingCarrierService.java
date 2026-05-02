package com.volcanoartscenter.platform.shared.service.integration;

import java.math.BigDecimal;

public interface ShippingCarrierService {
    String provider();
    BigDecimal estimate(String destinationCountry, BigDecimal weightKg);
    String createShipment(String reference);
}
