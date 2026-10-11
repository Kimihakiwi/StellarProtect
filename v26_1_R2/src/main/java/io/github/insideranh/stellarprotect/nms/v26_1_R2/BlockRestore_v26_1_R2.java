package io.github.insideranh.stellarprotect.nms.v26_1_R2;

import io.github.insideranh.stellarprotect.restore.BlockRestore;

public class BlockRestore_v26_1_R2 extends BlockRestore {

    public BlockRestore_v26_1_R2(String data) {
        super(data);
    }

    public BlockRestore_v26_1_R2(String data, byte extraType, String extraData) {
        super(data, extraType, extraData);
    }

    public BlockRestore_v26_1_R2(String data, byte extraType, String extraData, boolean isPlace) {
        super(data, extraType, extraData, isPlace);
    }

    public BlockRestore_v26_1_R2(
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
