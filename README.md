# opal-opal-refdata-common-lib

Prompt:
“Using the attached schema.json, generate matching excerpt files following the existing SCHEMA_WORK/V3.0 structure and naming conventions. Create:
•
header/header.json from properties.header
•
objects/*.json from reusable object definitions in $defs
•
records/*.json from record definitions in $defs
Preserve each excerpt exactly, including all properties, required fields, constraints, $refs, allOf, and additionalProperties. After generating them, verify every excerpt matches its source section in schema.json and report any issues.”