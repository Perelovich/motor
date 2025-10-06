package com.icars.bot.repo;

import com.icars.bot.domain.TimelineEvent;
import com.icars.bot.domain.OrderStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface TimelineRepository {

    @SqlUpdate("""
    INSERT INTO timeline (order_id, event_status, description, is_public)
    VALUES (:orderId, CAST(:eventStatus AS order_status), :description, :isPublic)
    """)
    @GetGeneratedKeys("id")
    long insert(@BindBean TimelineEvent event);

    @SqlQuery("""
    SELECT * FROM timeline
     WHERE order_id = :orderId
       AND is_public = true
     ORDER BY event_timestamp DESC
    """)
    @RegisterBeanMapper(TimelineEvent.class)
    List<TimelineEvent> findPublicEventsByOrderId(@Bind("orderId") long orderId);

    // если есть другие апдейты статуса — тоже с CAST:
    @SqlUpdate("UPDATE timeline SET event_status = CAST(:status AS order_status) WHERE id = :id")
    void updateStatus(@Bind("id") long id, @Bind("status") OrderStatus status);
}
