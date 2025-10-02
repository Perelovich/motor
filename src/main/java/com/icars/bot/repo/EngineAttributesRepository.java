package com.icars.bot.repo;

import com.icars.bot.domain.EngineAttributes;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

public interface EngineAttributesRepository {

    @SqlUpdate("INSERT INTO engine_attributes (order_id, vin, make, model, year, engine_code_or_details, fuel_type, is_turbo, injection_type, euro_standard, kit_details) " +
            "VALUES (:orderId, :vin, :make, :model, :year, :engineCodeOrDetails, :fuelType, :isTurbo, :injectionType, :euroStandard, :kitDetails)")
    void insert(@BindBean EngineAttributes attributes);

    @SqlQuery("SELECT * FROM engine_attributes WHERE order_id = :orderId")
    @RegisterBeanMapper(EngineAttributes.class)
    Optional<EngineAttributes> findByOrderId(@Bind("orderId") long orderId);
}
