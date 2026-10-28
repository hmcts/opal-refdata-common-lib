# opal-opal-refdata-common-lib
## DTO generation

The DTO generator is available as a Gradle task. Run it with:

```bash
./gradlew generateRefDataDtos
```

It generates DTOs from `src/main/resources/jsonSchemas/ref-data/ref_data_schema.json` under `build/generated/sources/refDataDtos/main`.

messaging layer