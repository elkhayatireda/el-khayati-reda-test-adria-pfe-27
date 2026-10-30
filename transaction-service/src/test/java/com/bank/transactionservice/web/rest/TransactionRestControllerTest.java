package com.bank.transactionservice.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.transactionservice.TransactionServiceApplication;
import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.service.TransactionService;
import org.example.transactionservice.web.rest.TransactionRestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionRestController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = TransactionServiceApplication.class)
class TransactionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldCreateTransaction() throws Exception {
        UUID accountId = UUID.randomUUID();
        TransactionRestController.CreateTransactionRequest request =
                new TransactionRestController.CreateTransactionRequest(
                        accountId,
                        new BigDecimal("150.0"),
                        "MAD",
                        "RESTAURANT",
                        "MA");
        Transaction transaction = new Transaction(
                accountId, new BigDecimal("150.0"), "MAD", "RESTAURANT", "MA");

        when(transactionService.processTransaction(
                any(UUID.class), any(BigDecimal.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.currency").value("MAD"));

        verify(transactionService).processTransaction(
                accountId, new BigDecimal("150.0"), "MAD", "RESTAURANT", "MA");
    }
}
