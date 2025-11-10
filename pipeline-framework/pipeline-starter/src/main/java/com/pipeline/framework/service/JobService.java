package com.pipeline.framework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.framework.entity.JobEntity;
import com.pipeline.framework.mapper.JobMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Job服务类。
 * <p>
 * 注意：虽然底层使用MyBatis Plus（同步），但对外提供响应式API。
 * 阻塞操作通过Schedulers.boundedElastic()隔离。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Service
public class JobService {

    private final JobMapper jobMapper;

    public JobService(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    /**
     * 根据任务ID查询（响应式API）。
     * <p>
     * 将阻塞的MyBatis调用包装为响应式Mono。
     * </p>
     *
     * @param jobId 任务ID
     * @return 任务实体的Mono
     */
    public Mono<JobEntity> getByJobId(String jobId) {
        return Mono.fromCallable(() -> jobMapper.selectByJobId(jobId))
            .subscribeOn(Schedulers.boundedElastic());  // 在专用线程池执行
    }

    /**
     * 查询所有运行中的任务。
     *
     * @return 任务实体流
     */
    public Flux<JobEntity> getRunningJobs() {
        return Mono.fromCallable(jobMapper::selectRunningJobs)
            .flatMapMany(Flux::fromIterable)
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 保存任务。
     *
     * @param job 任务实体
     * @return 保存完成信号
     */
    public Mono<Void> save(JobEntity job) {
        return Mono.fromRunnable(() -> jobMapper.insert(job))
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    /**
     * 更新任务。
     *
     * @param job 任务实体
     * @return 更新完成信号
     */
    public Mono<Void> update(JobEntity job) {
        return Mono.fromRunnable(() -> jobMapper.updateById(job))
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    /**
     * 删除任务（逻辑删除）。
     *
     * @param jobId 任务ID
     * @return 删除完成信号
     */
    public Mono<Void> delete(String jobId) {
        return Mono.fromCallable(() -> jobMapper.selectByJobId(jobId))
            .flatMap(job -> {
                if (job != null) {
                    return Mono.fromRunnable(() -> jobMapper.deleteById(job.getId()));
                }
                return Mono.empty();
            })
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    /**
     * 查询指定状态的任务列表。
     *
     * @param status 任务状态
     * @return 任务列表流
     */
    public Flux<JobEntity> getByStatus(String status) {
        return Mono.fromCallable(() -> jobMapper.selectByStatus(status))
            .flatMapMany(Flux::fromIterable)
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 分页查询任务（同步API示例）。
     * <p>
     * 对于管理后台这种低频调用，可以保留同步API。
     * </p>
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 任务列表
     */
    public List<JobEntity> listByPage(int pageNum, int pageSize) {
        LambdaQueryWrapper<JobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobEntity::getIsDeleted, false)
               .orderByDesc(JobEntity::getCreateTime);
        
        // 这里可以使用MyBatis Plus的分页插件
        return jobMapper.selectList(wrapper);
    }
}
