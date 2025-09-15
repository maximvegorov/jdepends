package com.github.maximvegorov.jdepends;

import lombok.NonNull;

public record ServiceDef(@NonNull ServiceId serviceId, @NonNull Factory factory) {
}
