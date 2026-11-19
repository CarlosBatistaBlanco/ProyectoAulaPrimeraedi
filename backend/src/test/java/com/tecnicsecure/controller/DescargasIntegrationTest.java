package com.tecnicsecure.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DescargasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@tecnicsecure.com", roles = "ADMINISTRADOR")
    void respaldoYReportesDescarganSinError() throws Exception {
        mockMvc.perform(get("/api/respaldo/completo")).andExpect(status().isOk());
        mockMvc.perform(get("/api/reportes/ventas/export").param("formato", "csv")).andExpect(status().isOk());
        mockMvc.perform(get("/api/reportes/ventas/export").param("formato", "xml")).andExpect(status().isOk());
        mockMvc.perform(get("/api/reportes/inventario/export")).andExpect(status().isOk());
        mockMvc.perform(get("/api/auditoria/export")).andExpect(status().isOk());
    }
}
