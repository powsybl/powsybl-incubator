package com.example.networkeditor;

import com.fasterxml.jackson.databind.JsonNode;

public final class Dto {

    private Dto()
    {}

    public record loadNetworkRequest(String file_path)
    {}

    public record loadNetworkResponse(String source, NetworkInfo networkInfo) {}

    public record NetworkInfo(String id, int substation_count, int vl_count) {}

    public record SldRequest(String vlId)
    {}

    public record SldResponse(String svg, JsonNode metadata)
    {}
}
