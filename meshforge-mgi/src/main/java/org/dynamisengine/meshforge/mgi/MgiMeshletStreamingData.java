package org.dynamisengine.meshforge.mgi;

import java.util.List;

/**
 * Optional meshlet streaming metadata payload.
 *
 * @param units ordered stream units
 */
public record MgiMeshletStreamingData(List<MgiMeshletStreamUnit> units) {
    public MgiMeshletStreamingData {
        if (units == null) {
            throw new NullPointerException("units");
        }
        if (units.isEmpty()) {
            throw new IllegalArgumentException("units must not be empty");
        }
        units = List.copyOf(units);

        int previousId = -1;
        long previousMeshletEnd = -1;
        long previousPayloadEnd = -1;
        for (MgiMeshletStreamUnit unit : units) {
            if (unit == null) {
                throw new NullPointerException("unit");
            }
            if (unit.streamUnitId() <= previousId) {
                throw new IllegalArgumentException("stream unit ids must be strictly increasing");
            }
            long meshletStart = unit.meshletStart();
            long meshletEnd = meshletStart + unit.meshletCount();
            if (meshletStart < previousMeshletEnd) {
                throw new IllegalArgumentException("stream unit meshlet ranges must be non-overlapping and ordered");
            }
            long payloadStart = unit.payloadByteOffset();
            long payloadEnd = payloadStart + unit.payloadByteSize();
            if (payloadStart < previousPayloadEnd) {
                throw new IllegalArgumentException("stream unit payload ranges must be non-overlapping and ordered");
            }
            previousId = unit.streamUnitId();
            previousMeshletEnd = meshletEnd;
            previousPayloadEnd = payloadEnd;
        }
    }
}
