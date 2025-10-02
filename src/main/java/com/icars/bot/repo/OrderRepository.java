package com.icars.bot.repo;

import com.icars.bot.domain.Order;
import com.icars.bot.domain.OrderCategory;
import com.icars.bot.domain.OrderStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    @SqlUpdate("INSERT INTO orders (public_id, telegram_user_id, telegram_chat_id, category, status, customer_name, customer_phone, delivery_city) " +
            "VALUES (:publicId, :telegramUserId, :telegramChatId, :category, :status, :customerName, :customerPhone, :deliveryCity)")
    @GetGeneratedKeys("id")
    long insert(@BindBean Order order);

    @SqlUpdate("UPDATE orders SET status = :status, updated_at = NOW() WHERE id = :id")
    void updateStatus(@Bind("id") long orderId, @Bind("status") OrderStatus status);

    @SqlQuery("SELECT * FROM orders WHERE id = :id")
    @RegisterBeanMapper(Order.class)
    Optional<Order> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM orders WHERE public_id = :publicId")
    @RegisterBeanMapper(Order.class)
    Optional<Order> findByPublicId(@Bind("publicId") String publicId);

    @SqlQuery("SELECT * FROM orders WHERE customer_phone = :phone ORDER BY created_at DESC LIMIT 1")
    @RegisterBeanMapper(Order.class)
    Optional<Order> findLastByPhone(@Bind("phone") String phone);

    @SqlQuery("SELECT COUNT(*) FROM orders WHERE created_at >= date_trunc('day', NOW())")
    int countToday();

    @SqlQuery("SELECT * FROM orders ORDER BY created_at DESC LIMIT :limit")
    @RegisterBeanMapper(Order.class)
    List<Order> findLast(@Bind("limit") int limit);

    @SqlQuery("SELECT o.* FROM orders o LEFT JOIN engine_attributes ea ON o.id = ea.order_id " +
            "WHERE o.public_id ILIKE :query OR o.customer_phone ILIKE :query OR ea.vin ILIKE :query " +
            "ORDER BY o.created_at DESC")
    @RegisterBeanMapper(Order.class)
    List<Order> search(@Bind("query") String query);
}
