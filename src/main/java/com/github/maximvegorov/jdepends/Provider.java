package com.github.maximvegorov.jdepends;

import java.util.List;

@FunctionalInterface
public interface Provider {
    List<ServiceDef> provides();
}
