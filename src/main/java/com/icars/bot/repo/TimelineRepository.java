package com.icars.bot.repo;

import com.icars.bot.domain.TimelineEvent;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface TimelineRepository {
    @SqlUpdate("INSERT INTO timeline (order_id, event_status, description, is_public) " +
            "VALUES (:orderId, :eventStatus, :description, :isPublic)")
    void insert(@BindBean TimelineEvent event);

    @SqlQuery("SELECT * FROM timeline WHERE order_id = :orderId AND is_public = TRUE ORDER BY event_timestamp DESC LIMIT 5")
    @RegisterBeanMapper(TimelineEvent.class)
    List<TimelineEvent> findPublicEventsByOrderId(@Bind("orderId") long orderId);
}
