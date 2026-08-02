package com.showup.api.attendance;

import com.showup.api.member.MemberMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface AttendanceMapper {

    @Mapping(target = "revoked", expression = "java(ticket.getRevokedAt() != null)")
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "ticketCode", source = "ticket.code")
    CheckInResponse toResponse(CheckIn checkIn);

    StaffAssignmentSummary toSummary(StaffAssignment assignment);
}
