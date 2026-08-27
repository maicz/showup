package com.showup.api.controller;

import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.PhotoSummary;
import com.showup.api.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhotoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PhotoControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PhotoService photos;

    @Test
    void listPhotosReturnsPhotosForEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        MemberSummary uploader = new MemberSummary(UUID.randomUUID(), "Bob", "bob.png");
        PhotoSummary photo = new PhotoSummary(photoId, uploader, "https://cdn.example.com/photo.jpg", "Stage setup", 1200, 800, Instant.now());

        when(photos.forEvent(eventId)).thenReturn(List.of(photo));

        mvc.perform(get("/api/events/{eventId}/photos", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(photoId.toString()))
                .andExpect(jsonPath("$[0].url").value("https://cdn.example.com/photo.jpg"))
                .andExpect(jsonPath("$[0].caption").value("Stage setup"));
    }
}
