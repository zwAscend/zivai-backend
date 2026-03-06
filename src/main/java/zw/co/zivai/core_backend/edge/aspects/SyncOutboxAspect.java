package zw.co.zivai.core_backend.edge.aspects;

import java.util.Collection;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;
import zw.co.zivai.core_backend.edge.services.sync.SyncOutboxService;

@Aspect
@Component
@Profile("edge")
@RequiredArgsConstructor
public class SyncOutboxAspect {
    private final SyncOutboxService syncOutboxService;

    @AfterReturning(
        pointcut = "execution(* zw.co.zivai.core_backend.common.repositories..*.save(..)) || execution(* zw.co.zivai.core_backend.common.repositories..*.saveAndFlush(..))",
        returning = "result"
    )
    public void captureSingleSave(Object result) {
        if (result instanceof BaseEntity entity) {
            syncOutboxService.enqueue(entity);
        }
    }

    @AfterReturning(
        pointcut = "execution(* zw.co.zivai.core_backend.common.repositories..*.saveAll(..))",
        returning = "result"
    )
    public void captureBulkSave(Object result) {
        if (result instanceof Collection<?> collection) {
            collection.stream()
                .filter(BaseEntity.class::isInstance)
                .map(BaseEntity.class::cast)
                .forEach(syncOutboxService::enqueue);
        }
    }
}
