package com.jellystudy.companion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssessmentDimension {

    ACCURACY("accuracy", "概念是否准确"),
    COMPLETENESS("completeness", "要点是否完整"),
    DEPTH("depth", "理解是否深入（是否涉及底层原理）"),
    CLARITY("clarity", "表达是否清晰（是否用自己的话）"),
    ABILITY_TO_EXAMPLE("abilityToExample", "能否举例说明");

    private final String code;
    private final String description;
}
