package com.ajo.platform.modules.groups.repository;

import com.ajo.platform.modules.groups.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCreatedById(Long userId);

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.id = :userId")
    List<Group> findGroupsByMemberId(@Param("userId") Long userId);

    boolean existsByNameAndCreatedById(String name, Long createdById);
}