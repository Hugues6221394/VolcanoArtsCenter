package com.volcanoartscenter.platform.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndRoleRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientRoutesRequireRegisteredClientRole() throws Exception {
        mockMvc.perform(get("/client/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        mockMvc.perform(get("/client/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("ops@user").roles("OPS_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tourOperatorAndTalentDashboardsAreRoleProtected() throws Exception {
        mockMvc.perform(get("/tour-operators/portal")
                        .with(SecurityMockMvcRequestPostProcessors.user("client@user").roles("REGISTERED_CLIENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/talent/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("operator@user").roles("TOUR_OPERATOR")))
                .andExpect(status().isForbidden());
    }
}
