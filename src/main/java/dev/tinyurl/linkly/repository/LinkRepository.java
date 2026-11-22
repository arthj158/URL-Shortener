package dev.tinyurl.linkly.repository;

import dev.tinyurl.linkly.entity.ShortLink;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<ShortLink, Long> {
  Optional<ShortLink> findByCode(String code);

  boolean existsByCode(String code);

  @Query(
      value =
          "select l, (select count(c) from ClickEvent c where c.link=l) from ShortLink l where"
              + " l.user.id=:userId",
      countQuery = "select count(l) from ShortLink l where l.user.id=:userId")
  Page<Object[]> findOwnedWithClicks(@Param("userId") Long userId, Pageable pageable);
}
