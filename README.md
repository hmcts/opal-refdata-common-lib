# opal-opal-refdata-common-lib

Service Bus consumers require explicit configuration. Missing or invalid settings fail Spring startup when the consumer is enabled:

```yaml
opal:
  ref-data:
    service-bus:
      consumer-enabled: true
      topic-name: topic.refdata
      subscription-name: subscription.refdata
      fully-qualified-namespace: example.servicebus.windows.net
      max-messages-per-session: 50
      initial-delay-seconds: 300
      fixed-delay-seconds: 300
      receive-timeout-seconds: 3s
```

For connection-string authentication, specify `connection-string` instead of `fully-qualified-namespace`.
Exactly one of these authentication properties must be nonblank; specifying both prevents startup.
The initial delay may be zero; the batch size, fixed delay, and receive timeout must be positive.
