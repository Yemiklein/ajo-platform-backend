package com.ajo.platform.modules.groups.repository;

import com.ajo.platform.modules.groups.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    int countByGroupId(Long groupId);

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByGroupIdAndStatus(Long groupId, String status);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupIdAndInvitedEmail(Long groupId, String invitedEmail);
}