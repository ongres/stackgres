# CRDs OpenAPI documentation

We follow this convention when writing the documentation for the CRDs OpenAPI validation:

*  Every leaf node (i.e., that is **not** of `type == "object"`) must have a `description` field set.
*  The description is by default a YAML multiline, and as such must start by `>` or `|` and continue, with indentation, in the following line.
*  As per the YAML spec, Common Markup is permitted as part of the `description` text.
*  Branch nodes (i.e., nodes where `type == "object"`) may not have a `description`. But, if set, it will be displayed in the documentation.
