package com.showup.api.service;

import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.PhotoSummary;
import com.showup.api.dto.UploadPhotoRequest;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventPhoto;
import com.showup.api.entity.Group;
import com.showup.api.entity.Member;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.mapper.EventPhotoMapper;
import com.showup.api.repository.EventPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private EventPhotoRepository photos;
    @Mock
    private EventService eventService;
    @Mock
    private MemberService memberService;
    @Mock
    private GroupAccessGuard guard;
    @Mock
    private EventPhotoMapper mapper;

    private PhotoService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID photoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PhotoService(photos, eventService, memberService, guard, mapper);
    }

    @Test
    void addingPhotoPersistsAndMaps() {
        Event event = mock(Event.class);
        Member uploader = mock(Member.class);
        EventPhoto savedPhoto = mock(EventPhoto.class);
        MemberSummary memberSummary = new MemberSummary(actorId, "Alice", "alice.jpg");
        PhotoSummary expected = new PhotoSummary(photoId, memberSummary, "https://cdn.example.com/p.jpg", "Group photo", 800, 600, Instant.now());

        when(eventService.require(eventId)).thenReturn(event);
        when(memberService.require(actorId)).thenReturn(uploader);
        when(photos.save(any(EventPhoto.class))).thenReturn(savedPhoto);
        when(mapper.toSummary(savedPhoto)).thenReturn(expected);

        PhotoSummary result = service.addPhoto(actorId, eventId, new UploadPhotoRequest("https://cdn.example.com/p.jpg", "Group photo", 800, 600));

        assertThat(result).isEqualTo(expected);
        verify(photos).save(any(EventPhoto.class));
    }

    @Test
    void deletingPhotoByUploaderDeletesRow() {
        Event event = mock(Event.class);
        Group group = mock(Group.class);
        Member uploader = mock(Member.class);
        EventPhoto photo = new EventPhoto(event, uploader, "https://cdn.example.com/p.jpg", "caption", 800, 600);

        when(eventService.require(eventId)).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        when(event.getGroup()).thenReturn(group);
        when(group.getId()).thenReturn(UUID.randomUUID());
        when(uploader.getId()).thenReturn(actorId);
        when(photos.findById(photoId)).thenReturn(Optional.of(photo));
        when(guard.isEventAdmin(any(), any())).thenReturn(false);

        service.deletePhoto(actorId, eventId, photoId);

        verify(photos).delete(photo);
    }
}
