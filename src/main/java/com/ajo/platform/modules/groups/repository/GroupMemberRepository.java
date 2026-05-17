package com.ajo.platform.modules.groups.repository;

import com.ajo.platform.modules.groups.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupId(Long groupId);
    List<GroupMember> findByGroupIdAndStatus(Long groupId, String status);


    int countByGroupId(Long groupId);
}