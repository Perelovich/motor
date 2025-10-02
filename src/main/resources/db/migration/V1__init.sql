-- Enum types for order status and category
CREATE TYPE order_status AS ENUM (
    'NEW',
    'NEED_INFO',
    'QUOTING',
    'WAITING_PREPAY',
    'PURCHASED',
    'IN_TRANSIT_CN',
    'CUSTOMS',
    'IN_TRANSIT',
    'READY_FOR_PICKUP',
    'DONE',
    'CANCELLED'
);

CREATE TYPE order_category AS ENUM (
    'ENGINE',
    'TRANSMISSION',
    'OTHER'
);

-- Main orders table
CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        public_id VARCHAR(20) UNIQUE NOT NULL,
                        telegram_user_id BIGINT NOT NULL,
                        telegram_chat_id BIGINT NOT NULL,
                        category order_category NOT NULL,
                        status order_status NOT NULL DEFAULT 'NEW',
                        customer_name VARCHAR(255),
                        customer_phone VARCHAR(20),
                        delivery_city VARCHAR(255),
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Attributes specific to engine orders
CREATE TABLE engine_attributes (
                                   id BIGSERIAL PRIMARY KEY,
                                   order_id BIGINT UNIQUE NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                   vin VARCHAR(17),
                                   make VARCHAR(100),
                                   model VARCHAR(100),
                                   year INT,
                                   engine_code_or_details TEXT,
                                   fuel_type VARCHAR(50), -- e.g., 'PETROL', 'DIESEL', 'HYBRID'
                                   is_turbo BOOLEAN,
                                   injection_type VARCHAR(50), -- e.g., 'DIRECT', 'MPI', 'COMMON_RAIL'
                                   euro_standard VARCHAR(20), -- e.g., 'EURO5', 'EURO6'
                                   kit_details TEXT -- Description of what's included
);

-- Timeline of events for each order
CREATE TABLE timeline (
                          id BIGSERIAL PRIMARY KEY,
                          order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                          event_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          event_status order_status NOT NULL,
                          description TEXT,
                          is_public BOOLEAN DEFAULT TRUE -- Whether to show this event to the customer
);

-- Indexes for faster lookups
CREATE INDEX idx_orders_public_id ON orders(public_id);
CREATE INDEX idx_orders_telegram_user_id ON orders(telegram_user_id);
CREATE INDEX idx_orders_customer_phone ON orders(customer_phone);
CREATE INDEX idx_engine_attributes_vin ON engine_attributes(vin);
CREATE INDEX idx_timeline_order_id ON timeline(order_id);
