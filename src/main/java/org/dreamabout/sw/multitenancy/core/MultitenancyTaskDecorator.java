package org.dreamabout.sw.multitenancy.core;

import org.springframework.core.task.TaskDecorator;

/**
 * Decorator for tasks that need to be executed in a specific multitenancy context.
 */
public class MultitenancyTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } finally {
                MultitenancyThreadContext.clear();
            }
        };
    }
}
