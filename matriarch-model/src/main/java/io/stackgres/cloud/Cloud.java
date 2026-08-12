package io.stackgres.cloud;

public enum Cloud {

    AWS("aws"),
    GCP("gcp"),
    AZURE("azure");

    private final String id;

    Cloud(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

}
