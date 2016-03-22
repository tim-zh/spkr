package models

import org.apache.kafka.clients.producer.Producer

trait PP extends Producer[String, String] //todo don't use guice
