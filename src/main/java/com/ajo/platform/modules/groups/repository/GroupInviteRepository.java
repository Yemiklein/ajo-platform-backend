package com.ajo.platform.modules.groups.repository;

import com.ajo.platform.modules.groups.model.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    Optional<GroupInvite> findByInviteCodeAndStatus(String inviteCode, GroupInvite.InviteStatus status);
    void deleteByGroupIdAndInvitedEmail(Long groupId, String email);
}