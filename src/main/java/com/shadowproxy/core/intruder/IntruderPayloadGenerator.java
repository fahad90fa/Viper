package com.shadowproxy.core.intruder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntruderPayloadGenerator {
    private static final Pattern MARKER = Pattern.compile("§(.*?)§", Pattern.DOTALL);

    private IntruderPayloadGenerator() {
    }

    public static List<String> extractPositions(String rawRequest) {
        if (rawRequest == null || rawRequest.isBlank()) {
            return List.of();
        }
        List<String> positions = new ArrayList<>();
        Matcher matcher = MARKER.matcher(rawRequest);
        while (matcher.find()) {
            positions.add(matcher.group(1));
        }
        return List.copyOf(positions);
    }

    public static List<IntruderGeneratedRequest> buildPayloadRequests(String rawRequest, IntruderAttackType attackType, List<String> payloads) {
        List<String> positions = extractPositions(rawRequest);
        if (positions.isEmpty() || payloads.isEmpty()) {
            return List.of();
        }
        return switch (attackType) {
            case SNIPER -> buildSniper(rawRequest, payloads, positions.size());
            case BATTERING_RAM -> buildBatteringRam(rawRequest, payloads, positions.size());
            case PITCHFORK -> buildPitchfork(rawRequest, payloads, positions.size());
            case CLUSTER_BOMB -> buildClusterBomb(rawRequest, payloads, positions.size());
        };
    }

    private static List<IntruderGeneratedRequest> buildSniper(String rawRequest, List<String> payloads, int positionCount) {
        List<IntruderGeneratedRequest> requests = new ArrayList<>();
        for (int position = 0; position < positionCount; position++) {
            for (String payload : payloads) {
                List<String> replacement = new ArrayList<>(Collections.nCopies(positionCount, null));
                replacement.set(position, payload);
                requests.add(new IntruderGeneratedRequest(applyPayloads(rawRequest, replacement), "pos " + (position + 1) + ": " + payload));
            }
        }
        return requests;
    }

    private static List<IntruderGeneratedRequest> buildBatteringRam(String rawRequest, List<String> payloads, int positionCount) {
        List<IntruderGeneratedRequest> requests = new ArrayList<>();
        for (String payload : payloads) {
            List<String> repeated = new ArrayList<>(Collections.nCopies(positionCount, payload));
            requests.add(new IntruderGeneratedRequest(applyPayloads(rawRequest, repeated), "all positions: " + payload));
        }
        return requests;
    }

    private static List<IntruderGeneratedRequest> buildPitchfork(String rawRequest, List<String> payloads, int positionCount) {
        List<IntruderGeneratedRequest> requests = new ArrayList<>();
        int max = payloads.size();
        for (int i = 0; i < max; i++) {
            List<String> values = new ArrayList<>(Collections.nCopies(positionCount, payloads.get(i)));
            for (int position = 0; position < positionCount; position++) {
                values.set(position, payloads.get(i));
            }
            requests.add(new IntruderGeneratedRequest(applyPayloads(rawRequest, values), "set " + (i + 1) + ": " + payloads.get(i)));
        }
        return requests;
    }

    private static List<IntruderGeneratedRequest> buildClusterBomb(String rawRequest, List<String> payloads, int positionCount) {
        List<IntruderGeneratedRequest> requests = new ArrayList<>();
        backtrack(rawRequest, payloads, positionCount, 0, new ArrayList<>(), requests);
        return requests;
    }

    private static void backtrack(String rawRequest, List<String> payloads, int positionCount, int depth, List<String> current, List<IntruderGeneratedRequest> requests) {
        if (depth == positionCount) {
            requests.add(new IntruderGeneratedRequest(applyPayloads(rawRequest, new ArrayList<>(current)), String.join(" | ", current)));
            return;
        }
        for (String payload : payloads) {
            current.add(payload);
            backtrack(rawRequest, payloads, positionCount, depth + 1, current, requests);
            current.remove(current.size() - 1);
        }
    }

    private static String applyPayloads(String rawRequest, List<String> payloadsByPosition) {
        Matcher matcher = MARKER.matcher(rawRequest);
        StringBuffer buffer = new StringBuffer();
        int positionIndex = 0;
        while (matcher.find()) {
            String replacement = matcher.group(1);
            if (positionIndex < payloadsByPosition.size() && payloadsByPosition.get(positionIndex) != null) {
                replacement = payloadsByPosition.get(positionIndex);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            positionIndex++;
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace("§", "");
    }
}
