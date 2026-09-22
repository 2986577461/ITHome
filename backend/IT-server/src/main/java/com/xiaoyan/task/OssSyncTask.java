package com.xiaoyan.task;

import com.xiaoyan.constant.StorageConstant;
import com.xiaoyan.mapper.PendingOssDeleteMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.utils.AsyncExecutors;
import com.xiaoyan.utils.LocalFileStorage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OSS 故障期间欠下的账，等它恢复了补做。两个方向：
 *
 * <ul>
 *   <li>上传侧：降级期间落在本机的文件补传上去（清单来自 {@code student_file} 里
 *       {@code storage_type=LOCAL} 的行）</li>
 *   <li>删除侧：删除时没能从 OSS 上删掉的对象重删一次（清单来自
 *       {@code pending_oss_delete}——记录那行在业务事务里已经删了，没有别的线索）</li>
 * </ul>
 *
 * <p>两个任务每轮都先探一次 OSS：不可用就直接返回，不拿 N 个必败的请求去砸它。
 * 上传的探通之后逐个丢给 {@link AsyncExecutors#uploadExecutor()} 并发传；删除的本来就
 * 是一次批量调用，直接同步做——单次请求有 1000 个 key 的上限，所以每轮只取一批，
 * 剩下的下一轮再说。</p>
 *
 * <p>没有加分布式锁。单实例部署下没问题；多实例会重复上传或重复删除同一个 objectName——
 * OSS 同名覆盖、删除不存在的对象都是幂等的，代价只是白跑一次，不值得为它引一个锁。</p>
 *
 * <p>用 {@code fixedDelay} 而不是 {@code fixedRate}：配合默认的单线程调度器，
 * 这轮没跑完就不会起下一轮，天然不会自我重叠。</p>
 */
@Component
@Slf4j
@AllArgsConstructor
public class OssSyncTask {

    /** 一次 DeleteObjects 请求能带多少个 key，OSS 的硬上限 */
    private static final int DELETE_BATCH_SIZE = 1000;

    private AliOssUtil aliOssUtil;

    private LocalFileStorage localFileStorage;

    private StudentFileMapper studentFileMapper;

    private PendingOssDeleteMapper pendingOssDeleteMapper;

    @Scheduled(fixedDelayString = "${xiaoyan.oss-sync.interval:300000}")
    public void syncLocalFiles() {
        List<StudentFile> pending = studentFileMapper.selectPendingSync();
        if (pending.isEmpty()) {
            // 没积压就不探测，省一次 OSS 调用
            return;
        }
        if (!aliOssUtil.isAvailable()) {
            log.warn("OSS 仍不可用，{} 个本地文件等待补传", pending.size());
            return;
        }

        log.info("开始补传 {} 个本地文件到 OSS", pending.size());
        for (StudentFile file : pending) {
            AsyncExecutors.uploadExecutor().execute(() -> syncOne(file));
        }
    }

    /**
     * 重删上一轮没从 OSS 上删掉的对象。
     *
     * <p>删成功才平账。平账前挂掉最多是下一轮再删一次，OSS 删不存在的对象是幂等的。</p>
     */
    @Scheduled(fixedDelayString = "${xiaoyan.oss-sync.interval:300000}")
    public void retryPendingDeletes() {
        List<String> pending = pendingOssDeleteMapper.selectPending(DELETE_BATCH_SIZE);
        if (pending.isEmpty()) {
            // 没有欠账就不探测，和补传那边一个道理
            return;
        }
        if (!aliOssUtil.isAvailable()) {
            log.warn("OSS 仍不可用，{} 个对象等待删除", pending.size());
            return;
        }

        try {
            aliOssUtil.deleteObjects(pending);
        } catch (RuntimeException e) {
            // 整批留着，下一轮再来。异常里看不出是哪几个没删成，所以一条都不平账——
            // 全留着最多是下次白删一遍，漏平账才会留下真正的孤儿
            log.error("重试删除 {} 个 OSS 对象失败，等待下一轮", pending.size(), e);
            return;
        }

        pendingOssDeleteMapper.deleteByObjectNames(pending);
        log.info("重试删除 {} 个 OSS 对象成功", pending.size());
    }

    private void syncOne(StudentFile file) {
        String objectName = file.getObjectName();

        byte[] bytes = localFileStorage.read(objectName);
        if (bytes == null) {
            // 只能人工介入：本地没文件、OSS 也没文件，这份数据已经丢了
            log.error("本地降级文件丢失，无法补传 studentFileId={} objectName={}",
                    file.getId(), objectName);
            return;
        }

        String fileUrl;
        try {
            fileUrl = aliOssUtil.upload(bytes, objectName);
        } catch (Exception e) {
            // 库里还是 LOCAL，下一轮会重新捞到它
            log.error("补传失败，等待下一轮 objectName={} 原因={}", objectName, e.getMessage());
            return;
        }

        StudentFile update = new StudentFile();
        update.setId(file.getId());
        update.setStorageType(StorageConstant.OSS);
        update.setFileUrl(fileUrl);
        try {
            studentFileMapper.updateById(update);
        } catch (Exception e) {
            // 本地副本还在，所以重传是安全的；库里没改成 OSS，下一轮还会捞到它
            log.error("补传成功但更新记录失败 objectName={} 原因={}", objectName, e.getMessage());
            return;
        }

        // 本地副本最后才删：上面任何一步失败，它都必须还在
        try {
            localFileStorage.delete(objectName);
        } catch (RuntimeException e) {
            // 不影响正确性——库里已经是 OSS 了，这只是留在盘上的垃圾
            log.warn("补传成功但本地副本删除失败 objectName={} 原因={}", objectName, e.getMessage());
        }
        log.info("本地文件补传成功 objectName={}", objectName);
    }
}
