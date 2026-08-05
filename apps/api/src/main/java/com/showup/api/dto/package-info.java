/**
 * Request and response records that make up the wire format. Kept separate from
 * {@link com.showup.api.entity} so the schema can change without breaking the API, and so no
 * credential field or lazy association can serialize by accident. See docs/domain-model.md#dtos.
 */
package com.showup.api.dto;
