package com.showup.api.service;

import com.showup.api.dto.PhotoSummary;
import com.showup.api.dto.UploadPhotoRequest;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventPhoto;
import com.showup.api.entity.Member;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.EventPhotoMapper;
import com.showup.api.repository.EventPhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PhotoService {

    private final EventPhotoRepository photos;
    private final EventService eventService;
    private final MemberService memberService;
    private final GroupAccessGuard guard;
    private final EventPhotoMapper mapper;

    PhotoService(EventPhotoRepository photos, EventService eventService,
                 MemberService memberService, GroupAccessGuard guard, EventPhotoMapper mapper) {
        this.photos = photos;
        this.eventService = eventService;
        this.memberService = memberService;
        this.guard = guard;
        this.mapper = mapper;
    }

    public PhotoSummary addPhoto(UUID actorId, UUID eventId, UploadPhotoRequest request) {
        Event event = eventService.require(eventId);
        Member uploadedBy = memberService.require(actorId);

        EventPhoto photo = new EventPhoto(
                event,
                uploadedBy,
                request.url().trim(),
                request.caption() == null ? null : request.caption().trim(),
                request.width(),
                request.height());

        EventPhoto saved = photos.save(photo);
        return mapper.toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<PhotoSummary> forEvent(UUID eventId) {
        eventService.require(eventId);
        return photos.findAllByEventId(eventId).stream()
                .map(mapper::toSummary)
                .toList();
    }

    public void deletePhoto(UUID actorId, UUID eventId, UUID photoId) {
        Event event = eventService.require(eventId);
        EventPhoto photo = photos.findById(photoId)
                .orElseThrow(() -> new NotFoundException("photo not found"));

        if (!photo.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("photo does not belong to this event");
        }

        boolean isUploader = photo.getUploadedBy().getId().equals(actorId);
        boolean isOrganizer = guard.isEventAdmin(event.getGroup().getId(), actorId);

        if (!isUploader && !isOrganizer) {
            throw new ForbiddenException("you cannot delete this photo");
        }

        photos.delete(photo);
    }
}
