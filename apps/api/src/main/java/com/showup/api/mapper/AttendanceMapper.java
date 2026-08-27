package com.showup.api.mapper;

import com.showup.api.dto.CheckInResponse;
import com.showup.api.dto.StaffAssignmentSummary;
import com.showup.api.dto.TicketResponse;
import com.showup.api.entity.CheckIn;
import com.showup.api.entity.StaffAssignment;
import com.showup.api.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface AttendanceMapper {

    @Mapping(target = "revoked", expression = "java(ticket.getRevokedAt() != null)")
    @Mapping(target = "checkedIn", ignore = true)
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "ticketCode", source = "ticket.code")
    @Mapping(target = "alreadyCheckedIn", ignore = true)
    @Mapping(target = "attendeeName", ignore = true)
    CheckInResponse toResponse(CheckIn checkIn);

    StaffAssignmentSummary toSummary(StaffAssignment assignment);
}
