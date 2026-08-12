package io.stackgres.slony.cri;

import jakarta.json.JsonArray;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class EtcdRequests {

    private static final System.Logger logger = System.getLogger("EtcdRequests");

    private final Jsonb jsonb = JsonbBuilder.create();

    public boolean isHealthy(String jsonOutput) {
        try {
            JsonArray array = jsonb.fromJson(jsonOutput, JsonArray.class);
            return array.getJsonObject(0).getBoolean("health");
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Error occurred checking etcd health: " + e.getMessage(), e);
            return false;
        }
    }

}