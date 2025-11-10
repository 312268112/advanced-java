package com.pipeline.framework.checkpoint;

import java.time.Instant;
import java.util.Map;

/**
 * 检查点接口。
 * <p>
 * 表示某个时刻的状态快照。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Checkpoint {

    /**
     * 获取检查点ID。
     *
     * @return 检查点ID
     */
    String getCheckpointId();

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    Instant getCreateTime();

    /**
     * 获取状态快照。
     *
     * @return 状态快照
     */
    Map<String, Object> getStateSnapshot();

    /**
     * 获取检查点大小（字节）。
     *
     * @return 检查点大小
     */
    long getSize();

    /**
     * 获取存储路径。
     *
     * @return 存储路径
     */
    String getStoragePath();

    /**
     * 判断检查点是否有效。
     *
     * @return true如果有效
     */
    boolean isValid();
}
