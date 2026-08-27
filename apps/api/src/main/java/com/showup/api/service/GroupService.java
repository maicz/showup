package com.showup.api.service;

import com.showup.api.dto.CreateGroupRequest;
import com.showup.api.dto.GroupDetail;
import com.showup.api.dto.GroupMemberSummary;
import com.showup.api.dto.GroupSummary;
import com.showup.api.dto.JoinGroupRequest;
import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.PageResponse;
import com.showup.api.dto.TopicSummary;
import com.showup.api.dto.UpdateGroupRequest;
import com.showup.api.dto.UpdateMemberRoleRequest;
import com.showup.api.entity.Category;
import com.showup.api.entity.Group;
import com.showup.api.entity.GroupMembership;
import com.showup.api.entity.GroupTopic;
import com.showup.api.entity.Member;
import com.showup.api.entity.Topic;
import com.showup.api.enums.GroupMemberRole;
import com.showup.api.enums.GroupMembershipStatus;
import com.showup.api.enums.GroupStatus;
import com.showup.api.enums.GroupVisibility;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.GroupMapper;
import com.showup.api.mapper.MemberMapper;
import com.showup.api.mapper.TopicMapper;
import com.showup.api.repository.CategoryRepository;
import com.showup.api.repository.GroupMembershipRepository;
import com.showup.api.repository.GroupRepository;
import com.showup.api.repository.GroupTopicRepository;
import com.showup.api.repository.TopicRepository;
import com.showup.api.util.GeoPoints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final GroupTopicRepository groupTopics;
    private final CategoryRepository categories;
    private final TopicRepository topics;
    private final MemberService memberService;
    private final GroupAccessGuard guard;
    private final GroupMapper groupMapper;
    private final TopicMapper topicMapper;
    private final MemberMapper memberMapper;

    GroupService(GroupRepository groups, GroupMembershipRepository memberships,
                 GroupTopicRepository groupTopics, CategoryRepository categories, TopicRepository topics,
                 MemberService memberService, GroupAccessGuard guard,
                 GroupMapper groupMapper, TopicMapper topicMapper, MemberMapper memberMapper) {
        this.groups = groups;
        this.memberships = memberships;
        this.groupTopics = groupTopics;
        this.categories = categories;
        this.topics = topics;
        this.memberService = memberService;
        this.guard = guard;
        this.groupMapper = groupMapper;
        this.topicMapper = topicMapper;
        this.memberMapper = memberMapper;
    }

    public GroupDetail create(UUID actorId, CreateGroupRequest request) {
        if (groups.existsByUrlname(request.urlname())) {
            throw new ConflictException("urlname '" + request.urlname() + "' is taken");
        }
        Member actor = memberService.require(actorId);
        Group group = new Group(request.urlname(), request.name(), category(request.categoryId()), request.timeZone());
        group.setDescription(request.description());
        group.setCity(request.city());
        group.setCountry(request.country());
        group.setLocation(GeoPoints.toJts(request.location()));
        group.setVisibility(request.visibility());
        group.setJoinPolicy(request.joinPolicy());
        // The creator is the first organizer, so the group is never left without one.
        group.setMemberCount(1);
        groups.save(group);
        memberships.save(new GroupMembership(group, actor, GroupMemberRole.ORGANIZER, null));
        replaceTopics(group, request.topicIds());
        return detail(group.getId(), Optional.of(actorId));
    }

    public GroupDetail update(UUID actorId, UUID groupId, UpdateGroupRequest request) {
        guard.requireGroupAdmin(groupId, actorId);
        Group group = require(groupId);
        group.setName(request.name());
        group.setDescription(request.description());
        group.setCategory(category(request.categoryId()));
        group.setCity(request.city());
        group.setCountry(request.country());
        group.setLocation(GeoPoints.toJts(request.location()));
        group.setTimeZone(request.timeZone());
        group.setVisibility(request.visibility());
        group.setJoinPolicy(request.joinPolicy());
        replaceTopics(group, request.topicIds());
        return detail(groupId, Optional.of(actorId));
    }

    @Transactional(readOnly = true)
    public GroupDetail detail(UUID groupId, Optional<UUID> actorId) {
        return buildDetail(require(groupId), actorId);
    }

    @Transactional(readOnly = true)
    public GroupDetail detailByUrlname(String urlname, Optional<UUID> actorId) {
        Group group = groups.findByUrlname(urlname)
                .orElseThrow(() -> NotFoundException.of("group", urlname));
        return buildDetail(group, actorId);
    }

    private GroupDetail buildDetail(Group group, Optional<UUID> actorId) {
        requireViewable(group, actorId);
        MemberSummary organizer = memberships
                .findFirstByGroupIdAndRoleAndStatusOrderByCreatedAtAsc(
                        group.getId(), GroupMemberRole.ORGANIZER, GroupMembershipStatus.ACTIVE)
                .map(m -> memberMapper.toSummary(m.getMember()))
                .orElse(null);
        GroupMembership viewerMembership = actorId
                .flatMap(id -> memberships.findByGroupIdAndMemberId(group.getId(), id))
                .orElse(null);
        GroupMemberRole viewerRole = viewerMembership != null ? viewerMembership.getRole() : null;
        GroupMembershipStatus viewerStatus = viewerMembership != null ? viewerMembership.getStatus() : null;
        return groupMapper.toDetail(group, topicsOf(group.getId()), organizer, viewerRole, viewerStatus);
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupSummary> list(int page, int size, String query, String categorySlug,
                                           String city, String sort) {
        Sort ordering = switch (sort == null ? "popular" : sort) {
            case "rating" -> Sort.by(Sort.Order.desc("ratingAverage").nullsLast(), Sort.Order.desc("ratingCount"));
            case "newest" -> Sort.by(Sort.Order.desc("foundedAt"));
            case "name" -> Sort.by(Sort.Order.asc("name"));
            default -> Sort.by(Sort.Order.desc("memberCount"), Sort.Order.asc("name"));
        };
        Page<Group> found = groups.searchPublic(
                GroupVisibility.PUBLIC,
                GroupStatus.ACTIVE,
                normalize(query),
                normalize(categorySlug),
                normalize(city),
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), ordering));
        return PageResponse.of(
                found.getContent().stream().map(groupMapper::toSummary).toList(),
                found.getNumber(), found.getSize(), found.getTotalElements());
    }

    private void requireViewable(Group group, Optional<UUID> actorId) {
        if (group.getVisibility() == GroupVisibility.PUBLIC && group.getStatus() == GroupStatus.ACTIVE) {
            return;
        }
        boolean activeMember = actorId
                .flatMap(id -> memberships.findByGroupIdAndMemberId(group.getId(), id))
                .map(GroupMembership::getStatus)
                .filter(status -> status == GroupMembershipStatus.ACTIVE)
                .isPresent();
        if (!activeMember) {
            throw NotFoundException.of("group", group.getId());
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    public GroupMemberSummary join(UUID actorId, UUID groupId, JoinGroupRequest request) {
        Group group = require(groupId);
        GroupMembership existing = memberships.findByGroupIdAndMemberId(groupId, actorId).orElse(null);
        if (existing != null) {
            return switch (existing.getStatus()) {
                case ACTIVE -> throw new ConflictException("already a member of this group");
                case PENDING_APPROVAL -> throw new ConflictException("a join request is already pending");
                case BANNED -> throw new ForbiddenException("banned from this group");
                // Rejoining reuses the row; the unique (group_id, member_id) index forbids a second one.
                case LEFT -> {
                    existing.setStatus(admitted(group));
                    existing.setRole(GroupMemberRole.MEMBER);
                    if (existing.getStatus() == GroupMembershipStatus.ACTIVE) {
                        group.setMemberCount(group.getMemberCount() + 1);
                    }
                    yield groupMapper.toMemberSummary(existing);
                }
            };
        }
        if (group.getJoinPolicy() == com.showup.api.enums.GroupJoinPolicy.INVITE_ONLY) {
            throw new ForbiddenException("this group is invite only");
        }
        GroupMembership membership = new GroupMembership(
                group, memberService.require(actorId), GroupMemberRole.MEMBER, request.introduction());
        membership.setStatus(admitted(group));
        if (membership.getStatus() == GroupMembershipStatus.ACTIVE) {
            group.setMemberCount(group.getMemberCount() + 1);
        }
        return groupMapper.toMemberSummary(memberships.save(membership));
    }

    public void leave(UUID actorId, UUID groupId) {
        GroupMembership membership = guard.requireActiveMember(groupId, actorId);
        requireAnotherOrganizerRemains(groupId, membership);
        membership.setStatus(GroupMembershipStatus.LEFT);
        Group group = require(groupId);
        group.setMemberCount(Math.max(group.getMemberCount() - 1, 0));
    }

    @Transactional(readOnly = true)
    public List<GroupMemberSummary> members(UUID actorId, UUID groupId, GroupMembershipStatus status) {
        guard.requireActiveMember(groupId, actorId);
        return memberships.findAllByGroupIdAndStatus(groupId, status).stream()
                .map(groupMapper::toMemberSummary)
                .toList();
    }

    public GroupMemberSummary updateMemberRole(UUID actorId, UUID groupId, UUID memberId,
                                               UpdateMemberRoleRequest request) {
        guard.requireGroupAdmin(groupId, actorId);
        GroupMembership target = memberships.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new NotFoundException("member " + memberId + " is not in this group"));
        if (request.role() != GroupMemberRole.ORGANIZER) {
            requireAnotherOrganizerRemains(groupId, target);
        }
        target.setRole(request.role());
        return groupMapper.toMemberSummary(target);
    }

    /** Approves a {@code PENDING_APPROVAL} membership created under an approval-required policy. */
    public GroupMemberSummary approveMember(UUID actorId, UUID groupId, UUID memberId) {
        guard.requireGroupAdmin(groupId, actorId);
        GroupMembership target = memberships.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new NotFoundException("member " + memberId + " is not in this group"));
        if (target.getStatus() != GroupMembershipStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("membership is " + target.getStatus() + ", not PENDING_APPROVAL");
        }
        target.setStatus(GroupMembershipStatus.ACTIVE);
        Group group = require(groupId);
        group.setMemberCount(group.getMemberCount() + 1);
        return groupMapper.toMemberSummary(target);
    }

    Group require(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> NotFoundException.of("group", groupId));
    }

    private static GroupMembershipStatus admitted(Group group) {
        return group.getJoinPolicy() == com.showup.api.enums.GroupJoinPolicy.APPROVAL_REQUIRED
                ? GroupMembershipStatus.PENDING_APPROVAL
                : GroupMembershipStatus.ACTIVE;
    }

    /** A group with no organizer cannot be administered back into existence, so refuse the last exit. */
    private void requireAnotherOrganizerRemains(UUID groupId, GroupMembership membership) {
        if (membership.getRole() != GroupMemberRole.ORGANIZER) {
            return;
        }
        long organizers = memberships.countByGroupIdAndRoleAndStatus(
                groupId, GroupMemberRole.ORGANIZER, GroupMembershipStatus.ACTIVE);
        if (organizers <= 1) {
            throw new BusinessRuleException("a group must keep at least one active ORGANIZER");
        }
    }

    private Category category(UUID categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> NotFoundException.of("category", categoryId));
    }

    private void replaceTopics(Group group, List<UUID> topicIds) {
        groupTopics.deleteAllByGroupId(group.getId());
        groupTopics.flush();
        if (topicIds == null || topicIds.isEmpty()) {
            return;
        }
        List<Topic> chosen = topics.findAllById(topicIds);
        if (chosen.size() != topicIds.size()) {
            throw new NotFoundException("one or more topic ids do not exist");
        }
        groupTopics.saveAll(chosen.stream().map(topic -> new GroupTopic(group, topic)).toList());
    }

    private List<TopicSummary> topicsOf(UUID groupId) {
        return groupTopics.findAllByGroupId(groupId).stream()
                .map(GroupTopic::getTopic)
                .map(topicMapper::toSummary)
                .toList();
    }
}
