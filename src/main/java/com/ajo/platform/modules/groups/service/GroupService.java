package com.ajo.platform.modules.groups.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.groups.dto.CreateGroupRequest;
import com.ajo.platform.modules.groups.dto.GroupResponse;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.model.GroupMember;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (groupRepository.existsByNameAndCreatedById(request.getName(), user.getId())) {
            throw new RuntimeException("You already have a group with this name");
        }

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .contributionAmount(request.getContributionAmount())
                .cycleType(request.getCycleType())
                .maxMembers(request.getMaxMembers())
                .status(Group.GroupStatus.PENDING)
                .createdBy(user)
                .build();

        groupRepository.save(group);

        GroupMember creatorMember = GroupMember.builder()
                .group(group)
                .user(user)
                .payoutPosition(1)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(creatorMember);

        return mapToResponse(group, 1);
    }

    @Transactional
    public GroupResponse joinGroup(Long groupId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (group.getStatus() != Group.GroupStatus.PENDING) {
            throw new RuntimeException("Group is no longer accepting members");
        }

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are already a member of this group");
        }

        int currentMembers = groupMemberRepository.countByGroupId(groupId);

        if (currentMembers >= group.getMaxMembers()) {
            throw new RuntimeException("Group is full");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .payoutPosition(currentMembers + 1)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(member);

        if (currentMembers + 1 >= group.getMaxMembers()) {
            group.setStatus(Group.GroupStatus.ACTIVE);
            groupRepository.save(group);
        }

        return mapToResponse(group, currentMembers + 1);
    }

    public List<GroupResponse> getMyGroups(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return groupRepository.findGroupsByMemberId(user.getId())
                .stream()
                .map(group -> mapToResponse(group,
                        groupMemberRepository.countByGroupId(group.getId())))
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long groupId, String email) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        int memberCount = groupMemberRepository.countByGroupId(groupId);
        return mapToResponse(group, memberCount);
    }

    private GroupResponse mapToResponse(Group group, int currentMembers) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .contributionAmount(group.getContributionAmount())
                .cycleType(group.getCycleType())
                .status(group.getStatus())
                .maxMembers(group.getMaxMembers())
                .currentMembers(currentMembers)
                .createdByName(group.getCreatedBy().getFirstName()
                        + " " + group.getCreatedBy().getLastName())
                .createdAt(group.getCreatedAt())
                .build();
    }
}