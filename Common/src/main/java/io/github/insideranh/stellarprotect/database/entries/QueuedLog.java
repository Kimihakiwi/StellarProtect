package io.github.insideranh.stellarprotect.database.entries;

@lombok.Builder
@lombok.Getter
public class QueuedLog {

    private long id;
    private long playerId;
    private int worldId;
    private double x;
    private double y;
    private double z;
    private int actionType;
    private boolean restored;
    private String extraJson;
    private long createdAt;
    private Integer blockId;
    private Integer oldBlockId;
    private Long itemId;
    private Integer amount;
    private String entityType;
    private Long chunkKey;

}