package com.xiaoyan.task;

import com.xiaoyan.constant.StorageConstant;
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
 * OSS 恢复之后，把降级期间落在本机的文件补传上去。
 *
 * <p>每轮先探一次 OSS：不可用就直接返回，不拿 N 个必败的上传去砸它。探通了才逐个丢给
 * {@link AsyncExecutors#uploadExecutor()} 并发补传。</p>
 *
 * <p>没有加分布式锁。单实例部署下没问题；多实例会重复上传同一个 objectName——
 * OSS 同名覆盖是幂等的，代价只是白传一次，不值得为它引一个锁。</p>
 *
 * <p>用 {@code fixedDelay} 而不是 {@code fixedRate}：配合默认的单线程调度器，
 * 这轮没跑完就不会起下一轮，天然不会自我重叠。</p>
 */
@Component
@Slf4j
@AllArgsConstructor
public class OssSyncTask {

    private AliOssUtil aliOssUtil;

    private LocalFileStorage localFileStorage;

    private StudentFileMapper studentFileMapper;

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
