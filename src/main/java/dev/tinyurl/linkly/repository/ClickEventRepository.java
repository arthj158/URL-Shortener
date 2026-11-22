package dev.tinyurl.linkly.repository;

import dev.tinyurl.linkly.entity.ClickEvent;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
  @Query(value = "select count(*) from click_events c where c.link_id=:id", nativeQuery = true)
  long total(@Param("id") long id);

  @Query(
      value = "select count(distinct visitor_hash) from click_events c where c.link_id=:id",
      nativeQuery = true)
  long uniqueVisitors(@Param("id") long id);

  @Query(
      value =
          "select cast(date_trunc('day', clicked_at) as date) as click_day, count(*) as clicks from"
              + " click_events where link_id=:id and clicked_at >= :since group by click_day order"
              + " by click_day",
      nativeQuery = true)
  List<Object[]> clicksPerDay(@Param("id") long id, @Param("since") Instant since);

  @Query(
      value =
          "select coalesce(browser,'Unknown'), count(*) from click_events where link_id=:id group"
              + " by browser order by count(*) desc",
      nativeQuery = true)
  List<Object[]> browsers(@Param("id") long id);

  @Query(
      value =
          "select coalesce(device_type,'Unknown'), count(*) from click_events where link_id=:id"
              + " group by device_type order by count(*) desc",
      nativeQuery = true)
  List<Object[]> devices(@Param("id") long id);

  @Query(
      value =
          "select coalesce(referrer,'Direct'), count(*) from click_events where link_id=:id group"
              + " by referrer order by count(*) desc",
      nativeQuery = true)
  List<Object[]> referrers(@Param("id") long id);
}
