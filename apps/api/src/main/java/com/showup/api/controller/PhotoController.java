package com.showup.api.controller;

import com.showup.api.dto.PhotoSummary;
import com.showup.api.dto.UploadPhotoRequest;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.PhotoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/photos")
public class PhotoController {

    private final PhotoService photos;

    PhotoController(PhotoService photos) {
        this.photos = photos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoSummary uploadPhoto(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                    @Valid @RequestBody UploadPhotoRequest request) {
        return photos.addPhoto(actor, eventId, request);
    }

    @GetMapping
    public List<PhotoSummary> list(@PathVariable UUID eventId) {
        return photos.forEvent(eventId);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@CurrentMember UUID actor, @PathVariable UUID eventId,
                            @PathVariable UUID photoId) {
        photos.deletePhoto(actor, eventId, photoId);
    }
}
