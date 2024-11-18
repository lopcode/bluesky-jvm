package com.lopcode.bluesky.jetstream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    defaultImpl = JetstreamEvent.Unknown.class,
    property = "kind",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JetstreamEvent.Commit.class, name = "commit"),
    @JsonSubTypes.Type(value = JetstreamEvent.Identity.class, name = "identity"),
    @JsonSubTypes.Type(value = JetstreamEvent.Account.class, name = "account")
})
public class JetstreamEvent {
    @JsonProperty("did")
    String did;
    @JsonProperty("kind")
    String kind;
    @JsonProperty("time_us")
    long unixTimeMicroseconds;

    public static class Commit extends JetstreamEvent {
        @JsonProperty("commit")
        CommitInfo commit;

        @Override
        public String toString() {
            return "Commit{" +
                "commit=" + commit +
                ", did='" + did + '\'' +
                ", kind='" + kind + '\'' +
                ", unixTimeMicroseconds=" + unixTimeMicroseconds +
                '}';
        }
    }

    public static class CommitInfo {
        @JsonProperty("rev")
        String rev;
        @JsonProperty("rkey")
        String rkey;
        @JsonProperty("operation")
        String operation;
        @JsonProperty("collection")
        String collection;
        @JsonProperty("cid")
        String cid;
        @JsonProperty("record")
        JsonNode record; // todo: model atproto records

        @Override
        public String toString() {
            return "CommitInfo{" +
                "rev='" + rev + '\'' +
                ", rkey='" + rkey + '\'' +
                ", operation='" + operation + '\'' +
                ", collection='" + collection + '\'' +
                ", cid='" + cid + '\'' +
                ", record=" + record +
                '}';
        }
    }

    public static class Identity extends JetstreamEvent {
        @JsonProperty("identity")
        IdentityInfo identity;

        @Override
        public String toString() {
            return "Identity{" +
                "identity=" + identity +
                ", did='" + did + '\'' +
                ", kind='" + kind + '\'' +
                ", unixTimeMicroseconds=" + unixTimeMicroseconds +
                '}';
        }
    }

    public static class IdentityInfo {
        @JsonProperty("did")
        String did;
        @JsonProperty("handle")
        String handle;
        @JsonProperty("seq")
        Long seq;
        @JsonProperty("time")
        String time;

        @Override
        public String toString() {
            return "IdentityInfo{" +
                "did='" + did + '\'' +
                ", handle='" + handle + '\'' +
                ", seq=" + seq +
                ", time='" + time + '\'' +
                '}';
        }
    }

    public static class Account extends JetstreamEvent {
        @JsonProperty("account")
        AccountInfo account;

        @Override
        public String toString() {
            return "Account{" +
                "account=" + account +
                '}';
        }
    }

    public static class AccountInfo {
        @JsonProperty("active")
        boolean active;
        @JsonProperty("did")
        String did;
        @JsonProperty("seq")
        Long seq;
        @JsonProperty("time")
        String time;
        @JsonProperty("status")
        String status;

        @Override
        public String toString() {
            return "AccountInfo{" +
                "active=" + active +
                ", did='" + did + '\'' +
                ", seq=" + seq +
                ", time='" + time + '\'' +
                ", status='" + status + '\'' +
                '}';
        }
    }

    public static class Unknown extends JetstreamEvent {
        @Override
        public String toString() {
            return "Unknown{" +
                "did='" + did + '\'' +
                ", kind='" + kind + '\'' +
                ", unixTimeMicroseconds=" + unixTimeMicroseconds +
                '}';
        }
    }
}