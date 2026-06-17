package com.jellystudy.companion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HivePatternType {

    BOTTLENECK("瓶颈模式", "某知识点在特定时间点遇到困难"),
    PATH("路径模式", "最优学习顺序发现"),
    ABANDONMENT("放弃模式", "早期放弃风险识别"),
    BREAKTHROUGH("突破模式", "有效学习方法发现"),
    FORGETTING("遗忘模式", "遗忘规律发现");

    private final String description;
    private final String detail;
}
