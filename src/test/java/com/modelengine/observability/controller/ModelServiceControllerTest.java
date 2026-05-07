package com.modelengine.observability.controller;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.service.ModelServiceService;
import com.modelengine.observability.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelServiceController.class)
class ModelServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelServiceService modelServiceService;

    @MockBean
    private MetricsService metricsService;

    // ───────── Valid requests ─────────

    @Test
    void validRequestReturns200WithCorrectStructure() throws Exception {
        ModelServiceDTO dto = ModelServiceDTO.builder()
                .instanceName("svc-a")
                .status("Running")
                .currentReplicas(2)
                .desiredReplicas(3)
                .build();

        PageDTO<ModelServiceDTO> page = PageDTO.of(List.of(dto), 1, 20, 1);

        when(modelServiceService.listServices(any(PaginationRequest.class),
                eq(null), eq(null), eq(null))).thenReturn(page);

        mockMvc.perform(get("/model-services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.data[0].instanceName").value("svc-a"))
                .andExpect(jsonPath("$.data.data[0].status").value("Running"))
                .andExpect(jsonPath("$.data.data[0].currentReplicas").value(2))
                .andExpect(jsonPath("$.data.data[0].desiredReplicas").value(3))
                .andExpect(jsonPath("$.data.data[0].metrics").doesNotExist());
    }

    @Test
    void validRequestWithDefaultParams() throws Exception {
        when(modelServiceService.listServices(any(PaginationRequest.class),
                eq(null), eq(null), eq(null)))
                .thenReturn(PageDTO.of(List.of(), 1, 20, 0));

        mockMvc.perform(get("/model-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void filterParamsPassedToService() throws Exception {
        when(modelServiceService.listServices(any(PaginationRequest.class),
                eq("ns1"), eq("VLLM"), eq("Running")))
                .thenReturn(PageDTO.of(List.of(), 1, 20, 0));

        mockMvc.perform(get("/model-services")
                        .param("namespace", "ns1")
                        .param("framework", "VLLM")
                        .param("status", "Running"))
                .andExpect(status().isOk());
    }

    @Test
    void sortParamParsedCorrectly() throws Exception {
        when(modelServiceService.listServices(any(PaginationRequest.class),
                eq(null), eq(null), eq(null)))
                .thenReturn(PageDTO.of(List.of(), 1, 20, 0));

        mockMvc.perform(get("/model-services")
                        .param("sort", "status,asc"))
                .andExpect(status().isOk());
    }

    // ───────── Invalid pagination params ─────────

    @Test
    void pageZeroRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/model-services")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorCode").value("0108001"));
    }

    @Test
    void pageNegativeRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/model-services")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("0108001"));
    }

    @Test
    void sizeZeroRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/model-services")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("0108001"));
    }

    @Test
    void size200RejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/model-services")
                        .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("0108001"));
    }

    @Test
    void size101RejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/model-services")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("0108001"));
    }
}
