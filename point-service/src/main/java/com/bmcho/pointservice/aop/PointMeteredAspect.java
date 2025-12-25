package com.bmcho.pointservice.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
@Aspect
@RequiredArgsConstructor
public class PointMeteredAspect {

    private final MeterRegistry meterRegistry;

    // 단순 포인트 적립, 사용 횟수 모니터링 aop
    @Around("@annotation(PointMetered)")
    public Object measurePointOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start();
        String version = extractVersion(joinPoint);
        String operation = extractOperation(joinPoint);

        try {
            Object result = joinPoint.proceed();

            // 포인트 처리 성공 메트릭
            Counter.builder("point.operation.success")
                    .tag("version", version)
                    .tag("operation", operation)
                    .register(this.meterRegistry)
                    .increment();

            sample.stop(Timer.builder("point.operation.duration")
                    .tag("version", version)
                    .tag("operation", operation)
                    .register(meterRegistry));

            return result;
        }
        catch (Exception e) {
            Counter.builder("point.operation.failure")
                    .tag("version", version)
                    .tag("operation", operation)
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();
            throw e;
        }
    }

    private String extractVersion(ProceedingJoinPoint joinPoint) {
        PointMetered pointMetered = ((MethodSignature) joinPoint)
                .getMethod()
                .getAnnotation(PointMetered.class);
        return pointMetered.version();
    }

    private String extractOperation(ProceedingJoinPoint joinPoint) {
        return  joinPoint.getSignature().getName();
    }
}
