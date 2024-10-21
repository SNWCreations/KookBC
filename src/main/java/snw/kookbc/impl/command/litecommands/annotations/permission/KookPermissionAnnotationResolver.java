package snw.kookbc.impl.command.litecommands.annotations.permission;

import dev.rollczi.litecommands.annotations.AnnotationInvoker;
import dev.rollczi.litecommands.annotations.AnnotationProcessor;
import dev.rollczi.litecommands.meta.Meta;
import snw.jkook.permissions.Permissions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class KookPermissionAnnotationResolver<SENDER> implements AnnotationProcessor<SENDER> {
    @Override
    public AnnotationInvoker<SENDER> process(AnnotationInvoker<SENDER> invoker) {
        return invoker.on(KookPermission.class, (annotation, metaHolder) -> {
            metaHolder.meta().listEditor(Meta.PERMISSIONS)
                    .addAll(Arrays.stream(annotation.value()).map(Permissions::getPermission).collect(Collectors.toList()))
                    .apply();
        });
    }
}
