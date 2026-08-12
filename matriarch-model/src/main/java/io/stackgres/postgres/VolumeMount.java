package io.stackgres.postgres;

import java.util.Arrays;
import java.util.List;

public record VolumeMount(String hostPath, String mountPath) {

    public static List<VolumeMount> parseMounts(List<String> mounts) {
        return mounts.stream().map(VolumeMount::split)
                .map(p -> new VolumeMount(p[0], p[1]))
                .toList();
    }

    public static List<VolumeMount> parseMounts(String commaSeparatedList) {
        return Arrays.stream(commaSeparatedList.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(VolumeMount::split)
                .map(p -> new VolumeMount(p[0], p[1]))
                .toList();
    }

    private static String[] split(String mount) {
        String[] parts = mount.split(":");
        if (parts.length < 2)
            throw new IllegalArgumentException("Invalid mount format: " + mount + ", needs to be in format: <host-path>:<mount-path>");
        return parts;
    }

}
