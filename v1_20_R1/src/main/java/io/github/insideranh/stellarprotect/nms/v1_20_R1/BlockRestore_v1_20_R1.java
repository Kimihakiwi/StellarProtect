package io.github.insideranh.stellarprotect.nms.v1_20_R1;

import io.github.insideranh.stellarprotect.restore.BlockRestore;

public class BlockRestore_v1_20_R1 extends BlockRestore {

    public BlockRestore_v1_20_R1(String data) {
        super(data);
    }

    public BlockRestore_v1_20_R1(String data, byte extraType, String extraData) {
        super(data, extraType, extraData);
    }

    public BlockRestore_v1_20_R1(String data, byte extraType, String extraData, boolean isPlace) {
        super(data, extraType, extraData, isPlace);
    }

    public BlockRestore_v1_20_R1(
            String data,
            byte extraType,
            String extraData,
            boolean isPlace,
            String oldData,
            String blockEntityNbt,
            String oldBlockEntityNbt
    ) {
        super(data, extraType, extraData, isPlace, oldData, blockEntityNbt, oldBlockEntityNbt);
    }
}
