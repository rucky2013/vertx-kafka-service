/**
 * Copyright (C) 2016 Etaia AS (oss@hubrick.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubrick.vertx.kafka.consumer;

import com.google.common.base.Strings;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

/**
 * Vert.x Module to read from a Kafka Topic.
 *
 * @author Marcus Thiesen
 * @since 1.0.0
 */
public class KafkaMessageConsumer extends AbstractVerticle {

    public static final int DEFAULT_ZOOKEEPER_TIMEOUT_MS = 100000;
    private VertxKafkaConsumer consumer;
    private KafkaConsumerConfiguration configuration;
    private JsonObject config;

    @Override
    public void start() throws Exception {
        super.start();

        config = vertx.getOrCreateContext().config();

        configuration = KafkaConsumerConfiguration.create(
                getMandatoryStringConfig(KafkaConsumerConfiguration.KEY_GROUP_ID),
                getMandatoryStringConfig(KafkaConsumerConfiguration.KEY_KAFKA_TOPIC),
                getMandatoryStringConfig(KafkaConsumerConfiguration.KEY_VERTX_ADDRESS),
                getMandatoryStringConfig(KafkaConsumerConfiguration.KEY_ZOOKEEPER),
                config.getString(KafkaConsumerConfiguration.KEY_OFFSET_RESET, "largest"),
                config.getInteger(KafkaConsumerConfiguration.KEY_ZOOKEPER_TIMEOUT_MS, DEFAULT_ZOOKEEPER_TIMEOUT_MS),
                config.getInteger(KafkaConsumerConfiguration.KEY_MAX_UNACKNOWLEDGED, 100),
                config.getLong(KafkaConsumerConfiguration.KEY_MAX_UNCOMMITTED_OFFSETS, 1000L),
                config.getLong(KafkaConsumerConfiguration.KEY_ACK_TIMEOUT_MINUTES, 10L),
                config.getLong(KafkaConsumerConfiguration.KEY_COMMIT_TIMEOUT_MS, 5 * 60 * 1000L));

        consumer = VertxKafkaConsumer.create(configuration, this::handler);

        consumer.start();
    }

    private String getMandatoryStringConfig(final String key) {
        final String value = config.getString(key);
        if (Strings.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("No configuration for key " + key + "found");
        }
        return value;
    }

    private void handler(final String vertxAddress, final String jsonMessage, final Runnable ack) {
        vertx.eventBus().send(vertxAddress, jsonMessage, (result) -> {
            ack.run();
        });
    }

    @Override
    public void stop() throws Exception {
        if (consumer != null) {
            consumer.stop();
        }
        super.stop();
    }
}
