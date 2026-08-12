package io.stackgres.matriarch.quarkus.resources;

import com.google.protobuf.ByteString;

import java.util.UUID;

/**
 * Converts between {@link UUID} and the legacy {@code common.UUID}. StackGres encodes
 * the value as the UTF-8 bytes of the UUID's canonical string form — matching
 * slony's {@code Mappers.mapUUID} — NOT the 16 raw bytes.
 */
public final class UuidCodec {

    private UuidCodec() {
    }

    public static common.Common.UUID toProto(UUID u) {
        return common.Common.UUID.newBuilder()
                .setValue(ByteString.copyFromUtf8(u.toString()))
                .build();
    }

    public static UUID fromProto(common.Common.UUID p) {
        return UUID.fromString(p.getValue().toStringUtf8());
    }
}
