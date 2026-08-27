package com.showup.api.service;

import com.showup.api.dto.GroupSummary;
import com.showup.api.dto.MemberProfile;
import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.TopicSummary;
import com.showup.api.dto.UpdateInterestsRequest;
import com.showup.api.dto.UpdateProfileRequest;
import com.showup.api.entity.Member;
import com.showup.api.entity.MemberInterest;
import com.showup.api.entity.Topic;
import com.showup.api.enums.GroupMembershipStatus;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.GroupMapper;
import com.showup.api.mapper.MemberMapper;
import com.showup.api.mapper.TopicMapper;
import com.showup.api.repository.GroupMembershipRepository;
import com.showup.api.repository.MemberInterestRepository;
import com.showup.api.repository.MemberRepository;
import com.showup.api.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MemberService {

    private final MemberRepository members;
    private final MemberInterestRepository interests;
    private final TopicRepository topics;
    private final GroupMembershipRepository memberships;
    private final MemberMapper memberMapper;
    private final TopicMapper topicMapper;
    private final GroupMapper groupMapper;

    MemberService(MemberRepository members, MemberInterestRepository interests, TopicRepository topics,
                  GroupMembershipRepository memberships, MemberMapper memberMapper,
                  TopicMapper topicMapper, GroupMapper groupMapper) {
        this.members = members;
        this.interests = interests;
        this.topics = topics;
        this.memberships = memberships;
        this.memberMapper = memberMapper;
        this.topicMapper = topicMapper;
        this.groupMapper = groupMapper;
    }

    @Transactional(readOnly = true)
    public MemberProfile profile(UUID memberId) {
        return memberMapper.toProfile(require(memberId));
    }

    @Transactional(readOnly = true)
    public MemberSummary summary(UUID memberId) {
        return memberMapper.toSummary(require(memberId));
    }

    public MemberProfile updateProfile(UUID actorId, UpdateProfileRequest request) {
        Member member = require(actorId);
        member.setDisplayName(request.displayName());
        member.setBio(request.bio());
        member.setPhotoUrl(request.photoUrl());
        member.setHomeCity(request.homeCity());
        member.setHomeCountry(request.homeCountry());
        return memberMapper.toProfile(member);
    }

    @Transactional(readOnly = true)
    public List<TopicSummary> interests(UUID memberId) {
        return interests.findAllByMemberId(memberId).stream()
                .map(MemberInterest::getTopic)
                .map(topicMapper::toSummary)
                .toList();
    }

    /**
     * Replace-all rather than diff: the client sends the complete set it wants, which keeps the
     * endpoint idempotent and sidesteps add/remove ordering bugs.
     */
    public List<TopicSummary> updateInterests(UUID actorId, UpdateInterestsRequest request) {
        Member member = require(actorId);
        List<Topic> chosen = topics.findAllById(request.topicIds());
        if (chosen.size() != request.topicIds().size()) {
            throw new NotFoundException("one or more topic ids do not exist");
        }
        interests.deleteAllByMemberId(actorId);
        // Force the deletes out before the inserts, or the unique (member_id, topic_id) index
        // rejects a topic that is being both removed and re-added in the same flush.
        interests.flush();
        interests.saveAll(chosen.stream().map(topic -> new MemberInterest(member, topic)).toList());
        return chosen.stream().map(topicMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> groups(UUID memberId) {
        return memberships.findAllByMemberIdAndStatus(memberId, GroupMembershipStatus.ACTIVE).stream()
                .map(membership -> groupMapper.toSummary(membership.getGroup()))
                .toList();
    }

    Member require(UUID memberId) {
        return members.findById(memberId).orElseThrow(() -> NotFoundException.of("member", memberId));
    }
}
