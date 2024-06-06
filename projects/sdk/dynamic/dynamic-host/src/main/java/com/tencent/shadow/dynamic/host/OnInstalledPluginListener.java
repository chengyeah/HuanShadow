package com.tencent.shadow.dynamic.host;

import java.util.Map;

public interface OnInstalledPluginListener {
    /**
     * @param plugins key: partKey, value: 插件大小
     */
    void onPluginListener(Map<String, Long> plugins);
}
