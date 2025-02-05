package zenhub

enum class Pipeline(val id: String) {
    IDEA_BOX("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTEzNjc"),
    NEW_REQUEST("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTEyODY"),
    EPICS("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTE0NTM"),
    WORKSHOP_BACKLOG("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTE1MzE"),
    WORKSHOP_IN_PROGRESS("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTE2MDg"),
    ESTIMATE_READY("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTE2OTI"),
    DEV_BACKLOG("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTE3NzI"),
    DEV_IN_PROGRESS("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTE4NDc"),
    REVIEW("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzIyOTUyNzk"),
    MERGE_READY("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTIwODA"),
    UAT_READY("Z2lkOi8vcmFwdG9yL1BpcGVsaW5lLzEwMTIxNzI")
}
